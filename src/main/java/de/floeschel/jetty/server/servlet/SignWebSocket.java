package de.floeschel.jetty.server.servlet;

import com.google.common.io.ByteStreams;
import com.google.protobuf.GeneratedMessageV3;
import de.floeschel.sign.SignRequest;
import de.floeschel.sign.StreamUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

@WebSocket(inputBufferSize = 64 * 1024, maxBinaryMessageSize = Integer.MAX_VALUE)
public class SignWebSocket {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SignWebSocket.class);

    @OnWebSocketMessage
    public void onBinaryMethod(Session session, InputStream stream) {
        try {
            final File tmpFile = File.createTempFile(UUID.randomUUID().toString(), null);
            try (OutputStream tmpOs = new FileOutputStream(tmpFile)) {
                ByteStreams.copy(stream, tmpOs);
            }
            //process the data and generate output
            RandomAccessFile raf = new RandomAccessFile(tmpFile, "r");

            SignRequest req = StreamUtil.parseStream(raf, SignRequest.class);

            Processor processor = ProcessorFactory.getProcessor(req);
            ProcessResult result = processor.process(req, raf);

            tmpFile.delete();
            tmpFile.deleteOnExit();

            File resultFile = result.getFile();
            GeneratedMessageV3 protoMsg = result.getProtoMsg();

            byte[] resultHeader = new byte[0];
            try {
                resultHeader = StreamUtil.buildProtobufStream(protoMsg);
            } catch (IOException ex) {
                LOG.error(ex.getLocalizedMessage(), ex);
            }

            RemoteEndpoint endpoint = session.getRemote();
            if (resultFile != null) {
                endpoint.sendPartialBytes(ByteBuffer.wrap(resultHeader), false);
                RandomAccessFile resultRaf = new RandomAccessFile(resultFile, "rw");
                MappedByteBuffer resultByteBuffer = resultRaf.getChannel().map(FileChannel.MapMode.PRIVATE, 0, resultFile.length());
                endpoint.sendPartialBytes(resultByteBuffer, true);

                try {
                    StreamUtil.unmap(resultByteBuffer);
                    resultRaf.close();
                } catch (RuntimeException | IOException ex) {
                    LOG.error(ex.getLocalizedMessage(), ex);
                }
                resultFile.delete();
                resultFile.deleteOnExit();
            } else {
                endpoint.sendBytes(ByteBuffer.wrap(resultHeader));
            }

        } catch (IOException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }
}
