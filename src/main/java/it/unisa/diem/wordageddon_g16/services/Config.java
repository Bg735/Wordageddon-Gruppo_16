package it.unisa.diem.wordageddon_g16.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties PROPS = new Properties();
    public static String DB_URL;

    static {
        try (InputStream input = Config.class.getResourceAsStream("/it/unisa/diem/wordageddon_g16/config.properties")) {

            if (input == null) {
                throw new RuntimeException("File config.properties non trovato nel classpath!");
            }
            PROPS.load(input);
        } catch (IOException e) {
            SystemLogger.log("Failed to find config. file", e);
        }
        DB_URL = PROPS.getProperty("db.url");
    }

    private static String get(String key) {
        return PROPS.getProperty(key);
    }
}
