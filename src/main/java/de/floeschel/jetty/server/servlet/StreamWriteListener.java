package de.floeschel.jetty.server.servlet;

import com.google.protobuf.GeneratedMessageV3;
import de.floeschel.sign.StreamUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StreamWriteListener implements WriteListener {

    private static final Logger LOG = LoggerFactory.getLogger(StreamWriteListener.class);

    private final AsyncContext async;
    private final ServletRequest request;
    private final ServletResponse response;
    private final File file;

    private final InputStream in;
    private final ServletOutputStream out;

    private final byte[] buffer = new byte[64 * 1024];

    public StreamWriteListener(AsyncContext async, ServletRequest request, ServletResponse response, GeneratedMessageV3 protoMsg, File file) throws IOException {
        this.async = async;
        this.request = request;
        this.response = response;
        this.file = file;
        if (file != null) {
            in = StreamUtil.buildProtobufStream(protoMsg.toByteArray(), new FileInputStream(file));
        } else {
            in = StreamUtil.buildProtobufStream(protoMsg.toByteArray(), null);
        }
        out = response.getOutputStream();
    }

    @Override
    public void onWritePossible() throws IOException {
        // while we are able to write without blocking
        while (out.isReady()) {
            // read some content into the copy buffer
            int len = in.read(buffer);

            // If we are at EOF then complete
            if (len < 0) {
                in.close();
                if (file != null) {
                    file.delete();
                }
                async.complete();
                return;
            }
            // write out the copy buffer. 
            out.write(buffer, 0, len);
        }
    }

    @Override
    public void onError(Throwable t) {
        LOG.error(t.getLocalizedMessage(), t);
        try {
            in.close();
        } catch (IOException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
        file.delete();
        async.complete();
    }
}
