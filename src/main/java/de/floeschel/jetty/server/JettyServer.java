package de.floeschel.jetty.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 *
 * @author HTPC
 */
public class JettyServer {
    
    private final Server server;
    private final ServerConnector serverConnector;
    private final SslContextFactory sslContextFactory;

    protected JettyServer(Server server, ServerConnector serverConnector, SslContextFactory sslContextFactory) {
        this.server = server;
        this.serverConnector = serverConnector;
        this.sslContextFactory = sslContextFactory;
    }

    public Server getServer() {
        return server;
    }

    public ServerConnector getServerConnector() {
        return serverConnector;
    }

    public SslContextFactory getSslContextFactory() {
        return sslContextFactory;
    }
    
}
