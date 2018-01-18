package de.floeschel.playground.sign.jetty.server.servlet;

import com.google.protobuf.GeneratedMessageV3;
import de.floeschel.sign.SignRequest;

public class ProcessorFactory {

    public static Processor getProcessor(GeneratedMessageV3 msg) {
        if (msg instanceof SignRequest) {
            return new SignProcessor();
        }
        return null;
    }

}
