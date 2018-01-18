package de.floeschel.playground.sign.util;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.MappedByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;

public class StreamUtil {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(StreamUtil.class);

    public static byte[] buildProtobufStream(GeneratedMessageV3 protoMsg) throws IOException {
        byte[] requestData = protoMsg.toByteArray();
        return ByteStreams.toByteArray(buildProtobufStream(requestData, null));
    }

    public static InputStream buildProtobufStream(GeneratedMessageV3 protoMsg, InputStream data) {
        byte[] requestData = protoMsg.toByteArray();
        return buildProtobufStream(requestData, data);
    }

    public static <T extends GeneratedMessageV3> T parseStream(InputStream data, Class<T> msgType) throws IOException {
        byte[] protoData = parseProtobufStream(data);
        return parseMessage(msgType, protoData);
    }

    public static <T extends GeneratedMessageV3> T parseStream(RandomAccessFile raf, Class<T> msgType) throws IOException {
        byte[] protoData = parseProtobufStream(raf);
        return parseMessage(msgType, protoData);
    }

    public static InputStream buildProtobufStream(byte[] protoData, InputStream data) {
        InputStream headerIs = new ByteArrayInputStream(Ints.toByteArray(protoData.length)); //4 bytes coding the proto message length
        InputStream requestIs = new ByteArrayInputStream(protoData);

        List<InputStream> streams = new LinkedList<>();
        streams.add(headerIs);
        streams.add(requestIs);
        if (data != null) {
            streams.add(data);
        }
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    public static void unmap(MappedByteBuffer buffer) {

    }

    private static byte[] parseProtobufStream(InputStream data) throws IOException {
        int protoMessageLength = readInt(data);
        byte[] protoData = new byte[protoMessageLength];
        data.read(protoData);
        return protoData;
    }

    private static byte[] parseProtobufStream(RandomAccessFile raf) throws IOException {
        int protoMessageLength = raf.readInt();
        byte[] protoData = new byte[protoMessageLength];
        raf.read(protoData);
        return protoData;
    }

    private static <T extends GeneratedMessageV3> T parseMessage(Class<T> msgType, byte[] protoData) {
        try {
            Constructor<T> constructor = msgType.getDeclaredConstructor(new Class[0]);
            constructor.setAccessible(true);
            T msg = constructor.newInstance(new Object[0]);
            return (T) msg.getParserForType().parseFrom(protoData);
        } catch (InvalidProtocolBufferException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
        return null;
    }

    private static int readInt(InputStream is) throws IOException {
        int ch1 = is.read();
        int ch2 = is.read();
        int ch3 = is.read();
        int ch4 = is.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
    }
}
