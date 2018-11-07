//package de.floeschel.playground.sign.jetty.client;
//
//import com.google.common.io.ByteStreams;
//import de.floeschel.playground.sign.pdf.PAdES;
//import de.floeschel.playground.sign.util.ByteBufferInputStream;
//import de.floeschel.playground.sign.util.Configuration;
//import de.floeschel.sign.Response;
//import de.floeschel.sign.SignRequest;
//import de.floeschel.playground.sign.util.StreamUtil;
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.URI;
//import java.nio.ByteBuffer;
//import java.util.Properties;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.Future;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLEngine;
//import org.apache.hc.client5.http.async.methods.AbstractBinResponseConsumer;
//import org.apache.hc.client5.http.async.methods.AsyncRequestBuilder;
//import org.apache.hc.client5.http.classic.methods.HttpPost;
//import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
//import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
//import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
//import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
//import org.apache.hc.client5.http.ssl.H2TlsStrategy;
//import org.apache.hc.client5.http.utils.URIUtils;
//import org.apache.hc.core5.concurrent.FutureCallback;
//import org.apache.hc.core5.http.ContentType;
//import org.apache.hc.core5.http.HttpEntity;
//import org.apache.hc.core5.http.HttpException;
//import org.apache.hc.core5.http.HttpHost;
//import org.apache.hc.core5.http.HttpResponse;
//import org.apache.hc.core5.http.config.CharCodingConfig;
//import org.apache.hc.core5.http.io.entity.InputStreamEntity;
//import org.apache.hc.core5.http.nio.AsyncRequestProducer;
//import org.apache.hc.core5.http.nio.StreamChannel;
//import org.apache.hc.core5.http.nio.entity.AbstractBinAsyncEntityProducer;
//import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer;
//import org.apache.hc.core5.http.nio.entity.DigestingEntityProducer;
//import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
//import org.apache.hc.core5.io.ShutdownType;
//import org.apache.hc.core5.reactor.ssl.TlsDetails;
//import org.apache.hc.core5.ssl.SSLContexts;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class ApacheAsyncClient {
//
//    private static final Logger LOG = LoggerFactory.getLogger(ApacheAsyncClient.class);
//
//    public final static void main(String[] args) throws Exception {
//
//        Properties clientProperties = Configuration.load("clientApacheAsync");
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
//        TlsStrategy tlsStrategy = new H2TlsStrategy(
//                sslContext,
//                H2TlsStrategy.getDefaultHostnameVerifier()) {
//
//            // IMPORTANT uncomment the following method when running Java 9 or older
//            // in order to avoid the illegal reflective access operation warning
//            @Override
//            protected TlsDetails createTlsDetails(final SSLEngine sslEngine) {
//                return new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol());
//            }
//        };
//
//        PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
//                .setTlsStrategy(tlsStrategy)
//                .build();
//
//        try (CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().setConnectionManager(connectionManager).build()) {
//            httpclient.start();
//
//            SignRequest sr = SignRequest.newBuilder()
//                    .setCertificate(signProperties.getProperty("certificate"))
//                    .setPin(signProperties.getProperty("pin"))
//                    .setType(PAdES.convert(signProperties.getProperty("type")))
//                    .build();
//
//            InputStream data = StreamUtil.buildProtobufStream(sr, new FileInputStream(signProperties.getProperty("file")));
//
//            //TODO: use a streaming Producer and Consumer, similar to ZeroCopyConsumer but decode ProtoBuf response 
//            AsyncRequestProducer requestProducer = AsyncRequestBuilder.post(clientProperties.getProperty("uri"))
//                    .setEntity(new BasicAsyncEntityProducer(data.readAllBytes()))
//                    .build();
//
//            AbstractBinResponseConsumer<HttpResponse> responseConsumer = new AbstractBinResponseConsumer<HttpResponse>() {
//                HttpResponse response;
//
//                @Override
//                protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
//                    this.response = response;
//                }
//
//                @Override
//                protected HttpResponse buildResult() {
//                    return response;
//                }
//
//                @Override
//                protected int capacity() {
//                    return Integer.MAX_VALUE;
//                }
//
//                @Override
//                protected void data(ByteBuffer data, boolean endOfStream) throws IOException {
//                    try (ByteBufferInputStream bbis = new ByteBufferInputStream(data);
//                            OutputStream os = new FileOutputStream(signProperties.getProperty("signed"));) {
//                        Response signResponse = StreamUtil.parseStream(bbis, Response.class);
//                        LOG.info("Result: (" + signResponse.getResult() + ") " + signResponse.getMsg());
//                        ByteStreams.copy(bbis, os);
//                    }
//                }
//
//                @Override
//                public void releaseResources() {
//                }
//
//                @Override
//                public HttpResponse getResult() {
//                    return response;
//                }
//            };
//
//            final CountDownLatch countDownLatch = new CountDownLatch(1);
//
//            httpclient.execute(requestProducer, responseConsumer, new FutureCallback<HttpResponse>() {
//
//                @Override
//                public void completed(final HttpResponse response3) {
//                    countDownLatch.countDown();
//                }
//
//                @Override
//                public void failed(final Exception ex) {
//                    countDownLatch.countDown();
//                }
//
//                @Override
//                public void cancelled() {
//                    countDownLatch.countDown();
//                }
//
//            });
//            countDownLatch.await();
//
//            httpclient.shutdown(ShutdownType.GRACEFUL);
//            LOG.info("Shutting down");
//        }
//    }
//}
