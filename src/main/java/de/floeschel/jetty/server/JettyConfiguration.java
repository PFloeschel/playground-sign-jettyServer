package de.floeschel.jetty.server;

public class JettyConfiguration {

    private final String hostname;
    private final int port;
    private final long maxIdleTime;
    private final String keystoreFilename;
    private final String keystorePassword;
    private final int sslEphemeralDHKeySize;
    private final int maxWorkerThreads;
    private final int minWorkerThreads;
    private final int workerIdleTimeout;

    public JettyConfiguration(String hostname, int port, long maxIdleTime, String keystoreFilename, String keystorePassword, int sslEphemeralDHKeySize, int maxWorkerThreads, int minWorkerThreads, int workerIdleTimeout) {
        this.hostname = hostname;
        this.port = port;
        this.maxIdleTime = maxIdleTime;
        this.keystoreFilename = keystoreFilename;
        this.keystorePassword = keystorePassword;
        this.sslEphemeralDHKeySize = sslEphemeralDHKeySize;
        this.maxWorkerThreads = maxWorkerThreads;
        this.minWorkerThreads = minWorkerThreads;
        this.workerIdleTimeout = workerIdleTimeout;
    }

    public JettyConfiguration(String hostname, int port, long maxIdleTime, String keystoreFilename, String keystorePassword) {
        this(hostname, port, maxIdleTime, keystoreFilename, keystorePassword, 2048, 200, 8, 60000);
    }

    public JettyConfiguration(String hostname, int port, long maxIdleTime) {
        this(hostname, port, maxIdleTime, null, null);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    public String getKeystoreFilename() {
        return keystoreFilename;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public int getSslEphemeralDHKeySize() {
        return sslEphemeralDHKeySize;
    }

    public int getMaxWorkerThreads() {
        return maxWorkerThreads;
    }

    public int getMinWorkerThreads() {
        return minWorkerThreads;
    }

    public int getWorkerIdleTimeout() {
        return workerIdleTimeout;
    }

}
