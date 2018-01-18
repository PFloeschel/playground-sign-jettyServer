package de.floeschel.playground.sign.pdf;

import de.floeschel.sign.SignRequest;

public enum PAdES {
    B,
    B_T,
    B_LT,
    B_LTA;

    public static SignRequest.Type convert(String type) {
        switch (type.toUpperCase()) {
            default:
            case "PADES":
            case "PADES_B":
                return SignRequest.Type.PAdES_B;
            case "PADES_B_T":
                return SignRequest.Type.PAdES_B_T;
            case "PADES_B_LT":
                return SignRequest.Type.PAdES_B_LT;
            case "PADES_B_LTA":
                return SignRequest.Type.PAdES_B_LTA;
        }
    }
}
