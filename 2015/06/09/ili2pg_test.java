package org.catais.interlis;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.ehi.ili2pg.converter.PostgisGeometryConverter;
import ch.ehi.sqlgen.generator_impl.jdbc.GeneratorPostgresql;


public class Ili2pgTest {

	public static void main(String[] args) {

        Config config = new Config();
        config.setDbdatabase("xanadu2");
        config.setDbhost("localhost");
        config.setDbport("5432");
        config.setDbusr("stefan");
        config.setDbpwd("ziegler12");
        config.setDbschema("test5");
        config.setModels("DM01AVCH24LV95D");
        config.setModeldir("/home/stefan/Downloads/");

        config.setGeometryConverter(PostgisGeometryConverter.class.getName());
        config.setDdlGenerator(GeneratorPostgresql.class.getName());
        config.setJdbcDriver("org.postgresql.Driver");

        config.setNameOptimization("topic");
        config.setMaxSqlNameLength("60");

        config.setDefaultSrsAuthority("EPSG");
        config.setDefaultSrsCode("2056");

        config.setXtffile("/home/stefan/Downloads/ch_lv95_254900.itf");

        String dburl = "jdbc:postgresql://" + config.getDbhost() + ":" + config.getDbport() + "/" + config.getDbdatabase();
        config.setDburl(dburl);

        try {
            Ili2db ili2pg = new Ili2db();
            ili2pg.runImport(config, "");
        } catch (Ili2dbException e) {
            e.printStackTrace();
        }
    }
}
