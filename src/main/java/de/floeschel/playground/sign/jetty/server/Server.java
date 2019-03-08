package de.floeschel.playground.sign.jetty.server;

import de.floeschel.playground.sign.jetty.server.servlet.SignServlet;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import de.floeschel.playground.jetty.server.JettyConfiguration;
import de.floeschel.playground.jetty.server.JettyServer;
import de.floeschel.playground.jetty.server.JettyUtil;
import de.floeschel.playground.sign.util.Configuration;
import java.security.Security;
import java.util.Properties;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.server.AsyncRequestLogWriter;
import org.eclipse.jetty.server.CustomRequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private static final Class CLAZZ = Server.class;

    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);

    public static void main(String[] args) throws Exception {
        Properties props = Configuration.load("server");

//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        root.setLevel(Level.INFO);
        Security.addProvider(new BouncyCastleProvider());

        JettyConfiguration config = new JettyConfiguration(props);
        JettyServer jettyServer = JettyUtil.initJettyServer(CLAZZ, config);

        boolean useGZIP = false;

        HandlerWrapper servletHandler = createServletHandlerWithServlet();
        if (useGZIP) {
            HandlerWrapper gzipHandler = createGzipHandler();
            gzipHandler.setHandler(servletHandler);
            jettyServer.getServer().setHandler(gzipHandler);
        } else {
            jettyServer.getServer().setHandler(servletHandler);
        }
        jettyServer.getServer().setRequestLog(new CustomRequestLog(new AsyncRequestLogWriter("request.log"), CustomRequestLog.EXTENDED_NCSA_FORMAT));
        jettyServer.getServer().start();
        JettyUtil.logServerStarted(CLAZZ, jettyServer.getServerConnector(), jettyServer.getSslContextFactory());

        LOG.info("Press any key to stop Jetty.");
        System.in.read();
        jettyServer.getServer().stop();

    }

    private static GzipHandler createGzipHandler() {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setIncludedPaths("/*");
        gzipHandler.setMinGzipSize(0);
        return gzipHandler;
    }

    private static ServletContextHandler createServletHandlerWithServlet() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(SignServlet.class, "/Sign");
        return context;
    }

}
