package de.floeschel.playground.sign.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {

    private ByteBuffer byteBuffer;
    private int position;
    private int limit;

    public ByteBufferInputStream(ByteBuffer buffer) {
        this(buffer, buffer.limit() - buffer.position());
    }

    public ByteBufferInputStream(ByteBuffer buffer, int limit) {
        byteBuffer = buffer;
        this.limit = limit;
        position = byteBuffer.position();
    }

    @Override
    public int read() throws IOException {
        if (byteBuffer.position() - position > limit) {
            return -1;
        }
        return byteBuffer.get();
    }
}
