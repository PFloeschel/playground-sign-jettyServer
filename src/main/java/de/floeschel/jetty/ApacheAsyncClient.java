package de.floeschel.jetty;

import ch.qos.logback.classic.Level;
import com.google.common.io.ByteStreams;
import de.floeschel.sign.Response;
import de.floeschel.sign.SignRequest;
import de.floeschel.sign.StreamUtil;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.Future;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
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

            SignRequest sr = SignRequest.newBuilder()
                    .setCertificate("PF_123456")
                    .setPin("123456")
                    .setType(SignRequest.Type.PAdES_B)
                    .build();

            InputStream data = StreamUtil.buildProtobufStream(sr, new FileInputStream("test3.pdf"));

            URI uri = URI.create("http://localhost:8080/Sign");
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new InputStreamEntity(data));
            HttpHost target = URIUtils.extractHost(uri);

            HttpAsyncRequestProducer httpAsyncPost = new BasicAsyncRequestProducer(target, httpPost);

            //TODO: use a Consumer, similar to ZeroCopyConsumer but decode ProtoBuf response 
            Future<HttpResponse> future = httpclient.execute(httpPost, null);

            HttpResponse result = future.get();
            HttpEntity entity = result.getEntity();
            try (InputStream responseContent = entity.getContent();
                    OutputStream os = new FileOutputStream("signed.pdf")) {
                Response signResponse = StreamUtil.parseStream(responseContent, Response.class);
                LOG.info("Result: (" + signResponse.getResult() + ") " + signResponse.getMsg());
                ByteStreams.copy(responseContent, os);
            }

            LOG.info("Shutting down");
        }
    }
}
