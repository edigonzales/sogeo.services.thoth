#!/usr/bin/env groovy

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
import groovy.servlet.*

@Grab(group='org.eclipse.jetty.aggregate', module='jetty-all', version='9.2.10.v20150310')
def startJetty() {
    def server = new Server(8080)
    def handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.contextPath = '/'
    handler.resourceBase = '.'
    handler.addServlet(IliExport, '/*')

    server.handler = handler
    server.start()
    server.join()
}

println "Starting Jetty, press Ctrl+C to stop."
startJetty()
