package de.floeschel.jetty;

import ch.qos.logback.classic.Level;
import com.google.common.io.ByteStreams;
import de.floeschel.sign.SignRequest;
import de.floeschel.sign.SignResponse;
import de.floeschel.sign.StreamUtil;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * @author HTPC
 */
public class JettyWebsocketClient {
    
    private static final Class CLAZZ = JettyWebsocketClient.class;
    
    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);
    
    public static void main(String[] args) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        
        WebSocketClient webSocketClient = new WebSocketClient();
        webSocketClient.getPolicy().setMaxBinaryMessageSize(Integer.MAX_VALUE);
        webSocketClient.getPolicy().setMaxBinaryMessageBufferSize(64 * 1024);
        webSocketClient.getPolicy().setInputBufferSize(64 * 1024);
        
        try {
            webSocketClient.start();
            LOG.info(webSocketClient.getPolicy().toString());
            
            SignWebSocket signWebSocket = new SignWebSocket(new CountDownLatch(1));
            Future<Session> future = webSocketClient.connect(signWebSocket, new URI("ws://localhost:8080/Sign"));
            try (Session session = future.get()) {
                
                signWebSocket.getReceiveLatch().await(120, TimeUnit.SECONDS);
                // Close session
            }
            
        } finally {
            webSocketClient.stop();
        }
    }
    
    @WebSocket
    public static class SignWebSocket {
        
        private final CountDownLatch receiveLatch;
        
        public SignWebSocket(CountDownLatch receiveLatch) {
            this.receiveLatch = receiveLatch;
        }
        
        public CountDownLatch getReceiveLatch() {
            return receiveLatch;
        }
        
        @OnWebSocketConnect
        public void onWebSocketConnect(Session session) {
            SignRequest sr = SignRequest.newBuilder()
                    .setCertificate("PF_123456")
                    .setPin("123456")
                    .setType(SignRequest.Type.PAdES_B_LTA)
                    .build();
            File f = new File("test3.pdf");
            
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
                try (OutputStream os = new FileOutputStream("signed_socket.pdf")) {
                    SignResponse signResponse = StreamUtil.parseStream(stream, SignResponse.class);
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
