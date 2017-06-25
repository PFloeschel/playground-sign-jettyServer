package de.floeschel.jetty.server.servlet;

import com.google.protobuf.GeneratedMessageV3;
import de.floeschel.sign.StreamUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.UUID;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamReadListener<T extends GeneratedMessageV3> implements ReadListener {

    private static final Logger LOG = LoggerFactory.getLogger(StreamReadListener.class);

    private final AsyncContext async;
    private final ServletRequest request;
    private final ServletResponse response;
    private final Class<T> type;

    private final ServletInputStream in;
    private final File tmpFile;
    private final OutputStream tmpOs;
    private final byte[] buffer = new byte[64 * 1024];
    private ProcessResult result;

    StreamReadListener(AsyncContext async, ServletRequest request, ServletResponse response, Class<T> type) throws IOException {
        this.async = async;
        this.request = request;
        this.response = response;
        this.type = type;

        in = request.getInputStream();
        tmpFile = File.createTempFile(UUID.randomUUID().toString(), null);
        tmpOs = new FileOutputStream(tmpFile);
        LOG.debug("tmpFile: " + tmpFile);
    }

    @Override
    public void onDataAvailable() throws IOException {
        // while we are able to read without blocking
        while (in.isReady()) {
            // read some content into the copy buffer
            int len = in.read(buffer);

            // If we are at EOF then complete
            if (len < 0) {
                in.close();
                return;
            }

            // write out the copy buffer. 
            tmpOs.write(buffer, 0, len);
        }
    }

    @Override
    public void onAllDataRead() throws IOException {
        tmpOs.close();

        //process the data and generate output
        RandomAccessFile raf = new RandomAccessFile(tmpFile, "r");

        T req = StreamUtil.parseStream(raf, type);

        Processor processor = ProcessorFactory.getProcessor(req);
        result = processor.process(req, raf);

        tmpFile.delete();
        GeneratedMessageV3 protoMsg = result.getProtoMsg();

        //generate response
        ServletOutputStream out = response.getOutputStream();
        out.setWriteListener(new StreamWriteListener(async, request, response, protoMsg, result.getFile()));

    }

    @Override
    public void onError(Throwable t) {
        try {
            tmpOs.close();
            if (!tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }

            GeneratedMessageV3 protoMsg = result.getProtoMsg();
            ServletOutputStream out = response.getOutputStream();
            out.setWriteListener(new StreamWriteListener(async, request, response, protoMsg, null));
            LOG.warn(t.getLocalizedMessage(), t);
        } catch (IOException ex) {
            LOG.warn(ex.getLocalizedMessage(), ex);
        }
    }

}
