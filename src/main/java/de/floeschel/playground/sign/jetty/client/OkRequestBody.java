package de.floeschel.playground.sign.jetty.client;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public abstract class OkRequestBody extends RequestBody {

    //Okio seems to have a bug with InputStream / SequenceInputStream, so just write header as byte[] and let it read the File
    public static RequestBody create(@Nullable final MediaType contentType, final byte[] header, final File file) {
        if (header == null || file == null) {
            throw new NullPointerException("content == null");
        }
        return new RequestBody() {
            @Override
            @Nullable
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return -1l;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                sink.write(header);
                Source source = null;
                try {
                    source = Okio.source(file);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }

        };
    }
}
