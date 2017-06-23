package de.floeschel.jetty;

import de.floeschel.jetty.server.servlet.SignServlet;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import ch.qos.logback.classic.Level;
import de.floeschel.jetty.server.JettyConfiguration;
import de.floeschel.jetty.server.JettyServer;
import de.floeschel.jetty.server.JettyUtil;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.server.AsyncNCSARequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private static final Class CLAZZ = Server.class;

    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);

    public static void main(String[] args) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        Security.addProvider(new BouncyCastleProvider());

        JettyConfiguration config = new JettyConfiguration("0.0.0.0", 8080, 120_000);//, "../localhost_123456.p12", "123456");
        JettyServer jettyServer = JettyUtil.initJettyServer(CLAZZ, config);

        HandlerWrapper servletHandler = createServletHandlerWithServlet();
        HandlerWrapper gzipHandler = createGzipHandler();
        gzipHandler.setHandler(servletHandler);
        jettyServer.getServer().setHandler(gzipHandler);
//        jettyServer.getServer().setHandler(servletHandler);

        jettyServer.getServer().setRequestLog(new AsyncNCSARequestLog("request.log"));
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
        context.addServlet(SignServlet.class, "/Sign");
        context.setContextPath("/");

        return context;
    }

}
