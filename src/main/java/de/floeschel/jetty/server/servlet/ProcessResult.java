package de.floeschel.jetty.server.servlet;

import com.google.protobuf.GeneratedMessageV3;
import java.io.File;

public class ProcessResult {

    private final File file;
    private final GeneratedMessageV3 protoMsg;

    public ProcessResult(File file, GeneratedMessageV3 protoMsg) {
        this.file = file;
        this.protoMsg = protoMsg;
    }

    public File getFile() {
        return file;
    }

    public GeneratedMessageV3 getProtoMsg() {
        return protoMsg;
    }

}
