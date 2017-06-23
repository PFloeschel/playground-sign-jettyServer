package de.floeschel.jetty;

import ch.qos.logback.classic.Level;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApacheClient {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheClient.class);

    public final static void main(String[] args) throws Exception {

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost("http://localhost:8080/");
            httpPost.setEntity(new InputStreamEntity(new FileInputStream(new File("C:\\Users\\HTPC\\Downloads\\Windows.iso"))));

            // Create a custom response handler
            ResponseHandler<String> responseHandler = (final HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream responseContent = entity.getContent();
                            OutputStream os = new FileOutputStream("content")) {
                        ByteStreams.copy(responseContent, os);
                    }
                    
                    return response.getStatusLine().toString();
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            LOG.info("Executing request " + httpPost.getRequestLine());
            String response = httpclient.execute(httpPost, responseHandler);
            LOG.info(response);
        }
    }

}
