@GrabConfig(systemClassLoader=true)
@GrabResolver(name='catais.org', root='http://www.catais.org/maven/repository/release/', m2Compatible='true')
@Grab(group='org.postgresql', module='postgresql', version='9.4-1201-jdbc41')
@Grab(group='ch.interlis', module='ili2c', version='4.5.12')
@Grab(group='ch.interlis', module='ili2pg', version='2.1.4')

import javax.servlet.http.*
import javax.servlet.*
import groovy.servlet.ServletCategory

import ch.ehi.ili2db.base.Ili2db
import ch.ehi.ili2db.base.Ili2dbException
import ch.ehi.ili2db.gui.Config
import ch.ehi.ili2pg.converter.PostgisGeometryConverter
import ch.ehi.sqlgen.generator_impl.jdbc.GeneratorPostgresql

class IliExport extends HttpServlet {
    def application

    void init(ServletConfig config) {
        super.init(config)
        application = config.servletContext
    }

    void doGet(HttpServletRequest request, HttpServletResponse response) {
        def requestedFileName = request.getRequestURI() - '/'

        def mapping = [
        '2583_schoenenwerd.xtf': ['modelname':'MOpublic03_ili2_v13', 'dbschema':'av_mopublic_export'],
        '2583_schoenenwerd.gml': ['modelname':'MOpublic03_ili2_v13', 'dbschema':'av_mopublic_export']
        ]
        def params = mapping.get(requestedFileName)
        def modelName = params['modelname']
        def dbschema = params['dbschema']

        def config = new Config()
        config.setDbhost("localhost")
        config.setDbdatabase("xanadu2")
        config.setDbport("5432")
        config.setDbusr("stefan")
        config.setDbpwd("ziegler12")
        config.setDbschema(dbschema)
        config.setDburl("jdbc:postgresql://localhost:5432/xanadu2")

        config.setModels(modelName);
        config.setModeldir("http://models.geo.admin.ch/");

        config.setGeometryConverter(PostgisGeometryConverter.class.getName())
        config.setDdlGenerator(GeneratorPostgresql.class.getName())
        config.setJdbcDriver("org.postgresql.Driver")

        config.setNameOptimization("topic")
        config.setMaxSqlNameLength("60")
        config.setStrokeArcs("enable")
        config.setSqlNull("enable");
        config.setValue("ch.ehi.sqlgen.createGeomIndex", "True");

        config.setDefaultSrsAuthority("EPSG")
        config.setDefaultSrsCode("21781")

        def tmpDir = System.getProperty('java.io.tmpdir')
        def fileName = tmpDir + File.separator + requestedFileName

        config.setXtffile(fileName)

        Ili2db.runExport(config, "")

        ServletOutputStream os = response.getOutputStream();
        FileInputStream fis = new FileInputStream(fileName);
        try {
            int buffSize = 1024;
            byte[] buffer = new byte[buffSize];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
                os.flush();
                response.flushBuffer();
            }
        } finally {
            os.close();
        }
    }
}
