package de.floeschel.playground.sign.jetty.server.servlet;

import com.google.protobuf.GeneratedMessageV3;
import java.io.RandomAccessFile;

public interface Processor {

    public ProcessResult process(GeneratedMessageV3 msg, RandomAccessFile raf);
}
