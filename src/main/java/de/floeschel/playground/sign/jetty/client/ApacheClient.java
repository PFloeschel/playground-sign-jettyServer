//package de.floeschel.playground.sign.jetty.client;
//
//import com.google.common.io.ByteStreams;
//import de.floeschel.playground.sign.pdf.PAdES;
//import de.floeschel.playground.sign.util.Configuration;
//import de.floeschel.sign.Response;
//import de.floeschel.sign.SignRequest;
//import de.floeschel.playground.sign.util.StreamUtil;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.Properties;
//import javax.net.ssl.SSLContext;
//import org.apache.hc.client5.http.classic.methods.HttpPost;
//import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
//import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
//import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
//import org.apache.hc.client5.http.io.HttpClientConnectionManager;
//import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
//import org.apache.hc.core5.http.ClassicHttpResponse;
//import org.apache.hc.core5.http.HttpEntity;
//import org.apache.hc.core5.http.io.HttpClientResponseHandler;
//import org.apache.hc.core5.http.io.entity.InputStreamEntity;
//import org.apache.hc.core5.ssl.SSLContexts;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class ApacheClient {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ApacheClient.class);
//
//    public final static void main(String[] args) throws Exception {
//        Properties clientProperties = Configuration.load("clientApache");
//        Properties signProperties = Configuration.load("sign");
//
////        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
////        root.setLevel(Level.INFO);
//        SSLContext sslContext = SSLContexts
//                .custom()
//                .loadTrustMaterial(new File(clientProperties.getProperty("truststoreFile")),
//                        clientProperties.getProperty("truststorePassword").toCharArray())
//                .build();
//
//        SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(sslContext);
//        HttpClientConnectionManager clientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create().setSSLSocketFactory(sslcsf).build();
//
//        try (CloseableHttpClient httpclient = HttpClientBuilder
//                .create()
//                .useSystemProperties()
//                .setConnectionManager(clientConnectionManager)
//                .build()) {
//
//            HttpPost httpPost = new HttpPost(clientProperties.getProperty("uri"));
//
//            SignRequest sr = SignRequest.newBuilder()
//                    .setCertificate(signProperties.getProperty("certificate"))
//                    .setPin(signProperties.getProperty("pin"))
//                    .setType(PAdES.convert(signProperties.getProperty("type")))
//                    .build();
//
//            InputStream data = StreamUtil.buildProtobufStream(sr, new FileInputStream(signProperties.getProperty("file")));
//
//            httpPost.setEntity(new InputStreamEntity(data));
//
//            // Create a custom response handler
//            HttpClientResponseHandler<String> responseHandler = (ClassicHttpResponse response) -> {
//                HttpEntity entity = response.getEntity();
//                try (InputStream responseContent = entity.getContent();
//                        OutputStream os = new FileOutputStream(signProperties.getProperty("signed"))) {
//                    Response signResponse = StreamUtil.parseStream(responseContent, Response.class);
//                    LOG.info("Result: (" + signResponse.getResult() + ") " + signResponse.getMsg());
//                    ByteStreams.copy(responseContent, os);
//                }
//
//                return response.getReasonPhrase();
//            };
//
//            String response = httpclient.execute(httpPost, responseHandler);
//            LOG.info(response);
//        }
//    }
//}
