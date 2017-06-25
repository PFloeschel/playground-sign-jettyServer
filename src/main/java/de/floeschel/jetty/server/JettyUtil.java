package de.floeschel.jetty.server;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.security.Security;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.net.ssl.SSLEngine;
import org.conscrypt.Conscrypt;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 *
 * @author pfloeschel
 */
public class JettyUtil {

    private static final Logger LOG = LoggerFactory.getLogger(JettyUtil.class);

    private static final String CONSCRYPT = "CONSCRYPT";

    private static boolean checkALPN() {
        boolean alpn = true;
        try {
            NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
        } catch (IllegalStateException ise) {
            LOG.info(ise.getMessage());
            alpn = false;
        }
        return alpn;
    }

    public static void logServerStarted(Class clazz, ServerConnector serverConnector, SslContextFactory sslContextFactory)
            throws IOException {

        if (sslContextFactory != null) {
            SSLEngine engine = sslContextFactory.newSSLEngine();
            String enabledProtocols[] = engine.getEnabledProtocols();
            String supportedProtocols[] = engine.getSupportedProtocols();
            String enabledCiphers[] = engine.getEnabledCipherSuites();
            String supportedCiphers[] = engine.getSupportedCipherSuites();
            LOG.info("TLS Protocols (ENABLED): " + Arrays.toString(enabledProtocols));
            LOG.info("TLS Protocols (SUPPORTED): " + Arrays.toString(supportedProtocols));
            LOG.info("TLS Ciphers (ENABLED): " + Arrays.toString(enabledCiphers));
            LOG.info("TLS Ciphers (SUPPORTED): " + Arrays.toString(supportedCiphers));
        }

        ServerSocketChannel ssc = (ServerSocketChannel) serverConnector.getTransport();
        String protocols = serverConnector.getProtocols().stream().collect(Collectors.joining(", "));
        LOG.info(String.format("%s started in mode [%s] listening on %s", clazz.getSimpleName(), protocols, ssc.getLocalAddress()));
    }

    public static JettyServer initJettyServer(Class clazz, JettyConfiguration configuration) throws Exception {

        LOG.info(String.format("Starting %s version: %s", clazz.getSimpleName(), clazz.getPackage().getImplementationVersion()));

        if (!SLF4JBridgeHandler.isInstalled()) {
            //Remove existing handlers attached to j.u.l root logger
            SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
            // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
            // the initialization phase of your application
            SLF4JBridgeHandler.install();
        }

        if (configuration == null) {
            String msg = "Configuration not available";
            LOG.error(msg);
            throw new Exception(msg);
        }

        ServerConnector serverConnector;
        SslContextFactory sslContextFactory = null;

        Server server = new Server(new QueuedThreadPool(configuration.getMaxWorkerThreads(), configuration.getMinWorkerThreads(), configuration.getWorkerIdleTimeout()));
        server.setStopAtShutdown(true);

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.setSendServerVersion(false);
        httpConfiguration.setSendXPoweredBy(false);

        boolean useSSL = true;

        String keyStoreFilename = configuration.getKeystoreFilename();
        String keyStorePassword = configuration.getKeystorePassword();

        if (keyStoreFilename == null || keyStoreFilename.isEmpty()) {
            useSSL = false;
        }

        if (useSSL && (keyStorePassword == null || keyStorePassword.isEmpty())) {
            String msg = "Unable to open keystore filename without a valid password";
            LOG.error(msg);
            throw new Exception(msg);
        }
        if (useSSL) {
            sslContextFactory = new SslContextFactory();
            sslContextFactory.setUseCipherSuitesOrder(true);
            sslContextFactory.setKeyStorePath(new File(keyStoreFilename).getAbsolutePath());
            sslContextFactory.setKeyStorePassword(keyStorePassword);

            if (configuration.isUseNativeTLS()) {
                Security.addProvider(Conscrypt.newProvider(CONSCRYPT));
                sslContextFactory.setProvider(CONSCRYPT);
            }

            if (keyStoreFilename.toUpperCase(Locale.ROOT).endsWith(".P12")
                    || keyStoreFilename.toUpperCase(Locale.ROOT).endsWith(".PFX")) {
                sslContextFactory.setKeyStoreType("PKCS12");
            }

            System.setProperty("jdk.tls.ephemeralDHKeySize", Integer.toString(configuration.getSslEphemeralDHKeySize()));

            SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
            secureRequestCustomizer.setStsMaxAge(12, TimeUnit.HOURS);
            secureRequestCustomizer.setStsIncludeSubDomains(true);
            httpConfiguration.addCustomizer(secureRequestCustomizer);

            if (!configuration.isUseNativeTLS() && checkALPN()) {
                sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
                ALPNServerConnectionFactory alpnFactory = new ALPNServerConnectionFactory();
                HTTP2ServerConnectionFactory http2Factory = new HTTP2ServerConnectionFactory(httpConfiguration);
                HttpConnectionFactory http1Factory = new HttpConnectionFactory(httpConfiguration);

                SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, alpnFactory.getProtocol());
                serverConnector = new ServerConnector(
                        server, sslConnectionFactory, alpnFactory, http2Factory, http1Factory);
            } else {
                HttpConnectionFactory http1Factory = new HttpConnectionFactory(httpConfiguration);
                SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, http1Factory.getProtocol());

                serverConnector = new ServerConnector(
                        server, sslConnectionFactory, http1Factory);
            }
        } else {
            HttpConnectionFactory http1Factory = new HttpConnectionFactory(httpConfiguration);
            HTTP2CServerConnectionFactory http2cFactory = new HTTP2CServerConnectionFactory(httpConfiguration);
            serverConnector = new ServerConnector(server, http1Factory, http2cFactory);
        }

        serverConnector.setHost(configuration.getHostname());
        serverConnector.setPort(configuration.getPort());
        serverConnector.setIdleTimeout(configuration.getMaxIdleTime());

        server.addConnector(serverConnector);

        server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", Integer.MAX_VALUE);
        return new JettyServer(server, serverConnector, sslContextFactory);
    }

}
