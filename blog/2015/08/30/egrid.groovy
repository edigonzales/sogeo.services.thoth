@Grapes([
   @GrabResolver(name='catais.org', root='http://www.catais.org/maven/repository/release/', m2Compatible='true'),
   @Grab('org.postgresql:postgresql:9.4-1201-jdbc41'),
   @Grab('ch.interlis:ili2c:4.5.12'),
   @Grab('ch.interlis:ili2pg:2.1.4'),
   @GrabConfig(systemClassLoader = true)
])

import ch.ehi.ili2db.base.Ili2db
import ch.ehi.ili2db.base.Ili2dbException
import ch.ehi.ili2db.gui.Config
import ch.ehi.ili2pg.converter.PostgisGeometryConverter
import ch.ehi.sqlgen.generator_impl.jdbc.GeneratorPostgresql
import groovy.sql.*

def csv = "/Users/stefan/tmp/gb_egridexport150730_2407.csv"
def numberOfCsvRows = 3717
def municipality = "oensingen"
def fosnr = 2407

def dbhost = "localhost"
def dbport = "5432"
def dbdatabase = "xanadu2"
def dbusr = "stefan"
def dbpwd = "ziegler12"
def dbschema = "av_egrid"
def modelName = "GB2AV"

def dburl = "jdbc:postgresql://${dbhost}:${dbport}/${dbdatabase}?user=${dbusr}&password=${dbpwd}"

/*
* 1. Create empty database tables with ili2db.
*/
def query = "DROP SCHEMA IF EXISTS ${Sql.expand(dbschema)} CASCADE;"
def sql = Sql.newInstance(dburl)
sql.execute(query)

def config = new Config()
config.setDbhost(dbhost)
config.setDbdatabase(dbdatabase)
config.setDbport(dbport)
config.setDbusr(dbusr)
config.setDbpwd(dbpwd)
config.setDbschema(dbschema)
config.setDburl(dburl)

config.setModels(modelName);
config.setModeldir("http://models.geo.admin.ch/");

config.setGeometryConverter(PostgisGeometryConverter.class.getName())
config.setDdlGenerator(GeneratorPostgresql.class.getName())
config.setJdbcDriver("org.postgresql.Driver")

config.setNameOptimization("topic")
config.setMaxSqlNameLength("60")
config.setSqlNull("enable");

config.setDefaultSrsAuthority("EPSG")
config.setDefaultSrsCode("21781")

Ili2db.runSchemaImport(config, "")

/*
* 2. Create foreign table from CSV.
*/
query = """\
DROP FOREIGN TABLE IF EXISTS ${Sql.expand(dbschema)}.${Sql.expand(municipality)};

CREATE FOREIGN TABLE ${Sql.expand(dbschema)}.${Sql.expand(municipality)}  (
 bfsnr integer,
 kreisnr integer,
 grundstuecknummer varchar,
 grundstuecknummerzusatz varchar,
 grundstuecknummer3 varchar,
 grundstuecknummer4 varchar,
 egrid varchar(14)
) SERVER file_fdw_server
OPTIONS (format 'csv', header 'true',
         filename '${Sql.expand(csv)}',
         delimiter ',', null '');
"""
sql.execute(query)

// Check number of rows in foreign table.
query = "SELECT count(*) FROM ${Sql.expand(dbschema)}.${Sql.expand(municipality)};"
assert sql.firstRow(query).count == numberOfCsvRows

/*
* 3. Assign EGRID values to cadastral data and insert data into ili2db tables.
*/
query = """\
DELETE FROM ${Sql.expand(dbschema)}.eigentumsverhaeltnis_grundstueck;
DELETE FROM ${Sql.expand(dbschema)}.gb2av_grundstuecknummer;

WITH av AS (
 SELECT g.ogc_fid as av_id, g.nbident, g.nummer as av_nummer, l.flaechenmass, l.geometrie, l.gem_bfs, l.lieferdatum
 FROM av_avdpool_ch.liegenschaften_grundstueck as g, av_avdpool_ch.liegenschaften_liegenschaft as l
 WHERE g.gem_bfs = ${Sql.expand(fosnr)}
 AND l.gem_bfs = ${Sql.expand(fosnr)}
 AND g.tid = l.liegenschaft_von
),
gb AS (
 SELECT row_number() OVER () as gb_id, bfsnr, grundstuecknummer as gb_nummer, egrid
 FROM ${Sql.expand(dbschema)}.${Sql.expand(municipality)}
 WHERE grundstuecknummerzusatz IS NULL
),
eigentumsverhaeltnis_grundstueck AS (
 INSERT INTO ${Sql.expand(dbschema)}.eigentumsverhaeltnis_grundstueck (t_id, art)
 SELECT gb_id, 0::integer as art
 FROM av, gb
 WHERE av.av_nummer = gb.gb_nummer
)
INSERT INTO ${Sql.expand(dbschema)}.gb2av_grundstuecknummer(t_id, t_seq, egrid, nummer, gb2aveigntmsvrhltnis_grundstueck_nummer)
SELECT (gb_id+1000000) as t_id, 0::integer as t_seq, gb.egrid, gb.gb_nummer, gb_id
FROM av, gb
WHERE av.av_nummer = gb.gb_nummer;
"""
sql.execute(query)

// Check if we were able to assign an EGRID to all Liegenschaften from cadastral
// survyeing. If this is true the number of objects in all three tables is equal.
query = "SELECT count(*) FROM av_avdpool_ch.liegenschaften_liegenschaft WHERE gem_bfs = ${Sql.expand(fosnr)};"
def numberOfLiegenschaften = sql.firstRow(query).count

query = "SELECT count(*) FROM ${Sql.expand(dbschema)}.eigentumsverhaeltnis_grundstueck;"
def numberOfEigentumsverhaeltnisGrundstueck = sql.firstRow(query).count

query = "SELECT count(*) FROM ${Sql.expand(dbschema)}.gb2av_grundstuecknummer;"
def numberOfGrundstuecknummer = sql.firstRow(query).count

assert numberOfLiegenschaften == numberOfEigentumsverhaeltnisGrundstueck
assert numberOfLiegenschaften == numberOfGrundstuecknummer

// Close database connection.
sql.connection.close()
sql.close()

/*
* 4. Export data to an INTERLIS/XTF file.
*/
config.setXtffile("/Users/stefan/tmp/egrid_${municipality}.xtf")
Ili2db.runExport(config, "")
