@Grapes([
   @GrabResolver(name='catais.org', root='http://www.catais.org/maven/repository/release/', m2Compatible='true'),
   @Grab(group='commons-fileupload', module='commons-fileupload', version='1.3.1'),
   @Grab(group='commons-io', module='commons-io', version='2.4'),
   @Grab(group='org.xerial', module='sqlite-jdbc', version='3.8.11.2'),
   @Grab('ch.interlis:ili2c:4.5.21'),
   @Grab('ch.interlis:ili2gpkg:3.0.0'),
   //@GrabConfig(systemClassLoader = true)
])

import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletOutputStream
import java.util.logging.Logger
import java.nio.file.Path
import java.nio.file.Files
import org.apache.commons.io.IOUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.util.Streams
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.FileUploadException
import ch.ehi.ili2db.base.Ili2db
import ch.ehi.ili2db.base.Ili2dbException
import ch.ehi.ili2db.gui.Config
import ch.ehi.ili2db.mapping.NameMapping
import ch.ehi.basics.logging.EhiLogger

Logger logger = Logger.getLogger("do_ili2gpkg.groovy")
logger.setUseParentHandlers(true)
logger.info ("Starts at: " + new Date())

if (ServletFileUpload.isMultipartContent(request)) {
    ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory())
    //upload.setSizeMax(52428800) // 50MB
    upload.setSizeMax(2*5242880) // 2*5MB

    List<FileItem> items = null

    try {
        items = upload.parseRequest(request);
    } catch (FileUploadException e) {
        logger.severe e.getMessage()
        response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage())
        return
    }

    for (item in items) {
        if (item.isFormField()) { // 'normal' form fields
            String fieldName = item.getFieldName()
            String value = item.getString()

            params[fieldName] = value
            logger.info fieldName.toString()
            logger.info value.toString()
            logger.info item.getClass().toString()

        } else { // files
            String fieldName = item.getFieldName()
            String fileName = FilenameUtils.getName(item.getName())

            if (fileName.equalsIgnoreCase("")) {
                // return 'bad request' (400) if no file was sent
                String errorMessage = "No file chosen."
                logger.severe errorMessage
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage)
                return
            }

            // get the file as input stream
            InputStream fileContent = item.getInputStream()

            // create random temporary directory
            String tmpDirPrefix = "ili2gpkg_";
            Path tmpDir = Files.createTempDirectory(tmpDirPrefix);

            // copy input stream into target file
            String targetFileName = fileName
            File targetFile = new File(tmpDir.toString() + File.separator + targetFileName)

            try {
                FileUtils.copyInputStreamToFile(fileContent, targetFile)
                logger.info "Uploaded file: " + targetFile.toString()
                logger.info "Uploaded file size [KB]: " + (int) (targetFile.length() / 1024)
            } catch (java.io.IOException e) {
                FileUtils.deleteDirectory(tmpDir.toFile())

                logger.severe e.getMessage()
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage())
                return
            }

            // create configuration for ili2gpkg
            def config = initConfig(params)

            // Set the name of the geopackage.
            // Same name as input file but with *.gpkg extension instead.
            String gpkgFileName = FilenameUtils.removeExtension(targetFileName) + ".gpkg"
            gpkgFullFileName = tmpDir.toString() + File.separator + gpkgFileName
            config.setDbfile(gpkgFullFileName)
            config.setDburl("jdbc:sqlite:"+config.getDbfile())
            config.setXtffile(targetFile.toString())

            String fileExtension = FilenameUtils.getExtension(targetFileName)
            System.out.println(fileExtension)
            if (fileExtension.equalsIgnoreCase("itf")) {
                config.setItfTranferfile(true)
            }

            // Now create the GeoPackage.
            try {
                //EhiLogger.getInstance().setTraceFilter(false)
                Ili2db.runImport(config, "")
            } catch (Ili2dbException e) {
                logger.severe e.getMessage()
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage())
                return
            }

            // Send GeoPackage to browser.
            response.setContentType("application/x-sqlite3")
            response.setHeader("Content-Disposition", "attachment; filename=" + gpkgFileName);
            ServletOutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(gpkgFullFileName);
            try {
                int buffSize = 1024
                byte[] buffer = new byte[buffSize]
                int len
                while ((len = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, len)
                    os.flush()
                    response.flushBuffer()
                }
            } catch (Exception e) {
                logger.severe e.getMessage()
            }
            finally {
                FileUtils.deleteDirectory(tmpDir.toFile())
            }
        }
    }
}

def initConfig(params) {
    def config = new Config()
    config.setModeldir("http://models.geo.admin.ch/;http://models.geo.gl.ch/;http://www.catais.org/models")
    config.setModels(Ili2db.XTF)
    config.setSqlNull("enable");
    config.setDefaultSrsAuthority("EPSG");
    config.setDefaultSrsCode("21781");
    config.setMaxSqlNameLength(Integer.toString(NameMapping.DEFAULT_NAME_LENGTH));
    config.setIdGenerator(ch.ehi.ili2db.base.TableBasedIdGen.class.getName());
    config.setInheritanceTrafo(config.INHERITANCE_TRAFO_SMART1);
    config.setCatalogueRefTrafo(Config.CATALOGUE_REF_TRAFO_COALESCE);
    config.setMultiSurfaceTrafo(Config.MULTISURFACE_TRAFO_COALESCE);
    config.setMultilingualTrafo(Config.MULTILINGUAL_TRAFO_EXPAND);

    config.setGeometryConverter(ch.ehi.ili2gpkg.GpkgColumnConverter.class.getName());
    config.setDdlGenerator(ch.ehi.sqlgen.generator_impl.jdbc.GeneratorGeoPackage.class.getName());
    config.setJdbcDriver("org.sqlite.JDBC");
    config.setIdGenerator(ch.ehi.ili2db.base.TableBasedIdGen.class.getName());
    config.setIli2dbCustomStrategy(ch.ehi.ili2gpkg.GpkgMapping.class.getName());
    config.setOneGeomPerTable(true);

    for (param in params) {
        def key = param.key

        if (key.equalsIgnoreCase("strokeArcs")) {
            config.setStrokeArcs(config.STROKE_ARCS_ENABLE)
        }

        if (key.equalsIgnoreCase("nameByTopic")) {
            config.setNameOptimization(config.NAME_OPTIMIZATION_TOPIC)
        }

        if (key.equalsIgnoreCase("skipPolygonBuilding")) {
            config.setDoItfLineTables(true);
            config.setAreaRef(config.AREA_REF_KEEP);
        }

        if (key.equalsIgnoreCase("reference_frame")) {
            if (param.value.equalsIgnoreCase("LV95")) {
                config.setDefaultSrsCode("2056");
            }
        }
    }
    return config
}

logger.info ("Stops at: " + new Date())
