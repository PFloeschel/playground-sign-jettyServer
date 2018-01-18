package de.floeschel.playground.sign.util;

import java.io.FileInputStream;
import java.util.Properties;

public class Configuration {

    public static Properties load(String filename) {
        synchronized (filename) {
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(filename + ".properties")) {
                p.load(fis);
            } catch (Exception ex) {
            }
            return p;
        }
    }
}
