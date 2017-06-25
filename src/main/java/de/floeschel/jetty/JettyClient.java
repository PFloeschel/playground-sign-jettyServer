package de.floeschel.jetty;

import ch.qos.logback.classic.Level;
import com.google.common.io.ByteStreams;
import de.floeschel.sign.SignRequest;
import de.floeschel.sign.SignResponse;
import de.floeschel.sign.StreamUtil;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author HTPC
 */
public class JettyClient {
    
    private static final Class CLAZZ = JettyClient.class;
    
    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);
    
    public static void main(String[] args) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        
        HTTP2Client h2Client = new HTTP2Client();
        h2Client.setSelectors(1);
        HttpClientTransport transport = new HttpClientTransportOverHTTP2(h2Client);
//        HttpClientTransport transport = new HttpClientTransportOverHTTP();

        HttpClient httpClient = new HttpClient(transport, new SslContextFactory(true));
        httpClient.start();
        
        InputStreamResponseListener listener = new InputStreamResponseListener();
        
        SignRequest sr = SignRequest.newBuilder()
                .setCertificate("PF_123456")
                .setPin("123456")
                .setType(SignRequest.Type.PAdES_B_LTA)
                .build();
        
        InputStream data = StreamUtil.buildProtobufStream(sr, new FileInputStream("test3.pdf"));
        
        LOG.info(httpClient.getUserAgentField().toString());
        httpClient
                .newRequest("http://localhost:8080/Sign")
                .method(HttpMethod.POST)
                .content(new InputStreamContentProvider(data))
                .send(listener);
        
        try {
            Response response = listener.get(600, TimeUnit.SECONDS);
            if (response.getStatus() == HttpStatus.OK_200) {
                // Use try-with-resources to close input stream.
                try (InputStream responseContent = listener.getInputStream();
                        OutputStream os = new FileOutputStream("signed.pdf")) {
                    SignResponse signResponse = StreamUtil.parseStream(responseContent, SignResponse.class);
                    LOG.info("Result: (" + signResponse.getResult() + ") " + signResponse.getMsg());
                    ByteStreams.copy(responseContent, os);
                }
            } else {
                LOG.error(response.getReason());
            }
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
        httpClient.stop();
    }
}
