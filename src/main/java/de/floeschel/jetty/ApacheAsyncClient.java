package de.floeschel.jetty;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheAsyncClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheAsyncClient.class);

    public final static void main(String[] args) throws Exception {

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault()) {
            httpclient.start();
            HttpAsyncRequestProducer httpPost = HttpAsyncMethods.createZeroCopyPost(
                    "http://localhost:8080/",
                    new File("C:\\Users\\HTPC\\Downloads\\Windows.iso"),
                    ContentType.DEFAULT_BINARY);

            Future<File> future = httpclient.execute(httpPost, new ZeroCopyConsumer<File>(new File("content")) {
                @Override
                protected File process(HttpResponse response, File file, ContentType contentType) throws Exception {
                    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        throw new ClientProtocolException("Upload failed: " + response.getStatusLine());
                    }
                    return file;
                }
                
            }, null);

            File result = future.get();
            LOG.info("Response file length: " + result.length());
            LOG.info("Shutting down");
        }
    }
}
