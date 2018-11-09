package de.floeschel.playground.sign.jetty.client;

import com.google.common.io.ByteStreams;
import de.floeschel.playground.sign.pdf.PAdES;
import de.floeschel.playground.sign.util.Configuration;
import de.floeschel.sign.Response;
import de.floeschel.sign.SignRequest;
import de.floeschel.playground.sign.util.StreamUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pascal
 */
public class JettyWebsocketClient {

    private static final Class CLAZZ = JettyWebsocketClient.class;

    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);

    public static void main(String[] args) throws Exception {
        Properties clientProperties = Configuration.load("clientJettyWS");

//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        root.setLevel(Level.INFO);
        HttpClient httpClient = new HttpClient(new HttpClientTransportOverHTTP(), new SslContextFactory(true));
        httpClient.start();
        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.getPolicy().setMaxBinaryMessageSize(Integer.MAX_VALUE);
        webSocketClient.getPolicy().setMaxBinaryMessageBufferSize(64 * 1024);
        webSocketClient.getPolicy().setInputBufferSize(64 * 1024);

        try {
            webSocketClient.start();
            LOG.info(webSocketClient.getPolicy().toString());

            SignWebSocket signWebSocket = new SignWebSocket(new CountDownLatch(1));
            Future<Session> future = webSocketClient.connect(signWebSocket, new URI(clientProperties.getProperty("uri")));
            try (Session session = future.get()) {
                signWebSocket.getReceiveLatch().await(120, TimeUnit.SECONDS);
                // Close session;
            }

        } finally {
            webSocketClient.stop();
            httpClient.stop();
        }
    }

    @WebSocket
    public static class SignWebSocket {

        private final CountDownLatch receiveLatch;
        private final Properties signProperties = Configuration.load("sign");

        public SignWebSocket(CountDownLatch receiveLatch) {
            this.receiveLatch = receiveLatch;
        }

        public CountDownLatch getReceiveLatch() {
            return receiveLatch;
        }

        @OnWebSocketConnect
        public void onWebSocketConnect(Session session) {
            SignRequest sr = SignRequest.newBuilder()
                    .setCertificate(signProperties.getProperty("certificate"))
                    .setPin(signProperties.getProperty("pin"))
                    .setType(PAdES.convert(signProperties.getProperty("type")))
                    .build();
            File f = new File(signProperties.getProperty("file"));

            try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
                byte[] header = StreamUtil.buildProtobufStream(sr);
                RemoteEndpoint endpoint = session.getRemote();
                endpoint.sendPartialBytes(ByteBuffer.wrap(header), false);
                MappedByteBuffer fileMappedByteBuffer = raf.getChannel().map(FileChannel.MapMode.PRIVATE, 0, f.length());
                endpoint.sendPartialBytes(fileMappedByteBuffer, true);
                StreamUtil.unmap(fileMappedByteBuffer);
            } catch (Exception ex) {
                LOG.error(ex.getLocalizedMessage(), ex);
            }
        }

        @OnWebSocketMessage
        public void onBinaryMethod(Session session, InputStream stream) {
            try {
                try (OutputStream os = new FileOutputStream(
                        File.createTempFile(
                                signProperties.getProperty("signedPrefix"),
                                signProperties.getProperty("signedSuffix"),
                                new File(signProperties.getProperty("signedFolder"))))) {
                    Response signResponse = StreamUtil.parseStream(stream, Response.class);
                    LOG.info("Result: (" + signResponse.getResult() + ") " + signResponse.getMsg());
                    ByteStreams.copy(stream, os);
                }
            } catch (IOException ex) {
                LOG.error(ex.getLocalizedMessage(), ex);
            } finally {
                receiveLatch.countDown();
            }
        }
    }
}
