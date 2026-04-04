@Grapes([
   @Grab('org.postgresql:postgresql:9.4-1201-jdbc41'),
   @GrabConfig(systemClassLoader = true)
])

import groovy.sql.*

def dbhost = "localhost"
def dbport = "5432"
def dbdatabase = "xanadu2"
def dbusr = "stefan"
def dbpwd = "ziegler12"
def dbschema = "av_avdpool_test"

def dburl = "jdbc:postgresql://${dbhost}:${dbport}/${dbdatabase}?user=${dbusr}&password=${dbpwd}"

def query = "SELECT f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, type " +
  " FROM geometry_columns " +
  " WHERE f_table_schema = '$dbschema'" +
  " AND srid = 21781;"

def sql = Sql.newInstance(dburl)

def startTime = Calendar.instance.time
def endTime
println "Start: ${startTime}."

sql.withTransaction {
  sql.eachRow(query) {row ->
    def tableName = row.f_table_name
    def geomColumn = row.f_geometry_column

    def geomType = row.type
    if (row.coord_dimension == 3) {
      geomType += 'Z'
    }

    def alterQuery = "ALTER TABLE ${Sql.expand(dbschema)}.${Sql.expand(tableName)}" +
      " ALTER COLUMN ${Sql.expand(geomColumn)} TYPE geometry(${Sql.expand(geomType)},2056)" +
      " USING ST_Fineltra(${Sql.expand(geomColumn)}, 'av_chenyx06.chenyx06_triangles', 'the_geom_lv03', 'the_geom_lv95');"

    println "--- $dbschema.$tableName ---"
    sql.execute(alterQuery)

    endTime = Calendar.instance.time
    println "Elapsed time: ${(endTime.time - startTime.time)} ms"

  }
}

endTime = Calendar.instance.time
println "End: ${endTime}."
println "Total elapsed time: ${(endTime.time - startTime.time)} ms"

sql.connection.close()
sql.close()
