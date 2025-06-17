package it.unisa.diem.wordageddon_g16.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties PROPS = new Properties();
    public static String DB_URL;

    static {
        try (InputStream input = Config.class.getResourceAsStream("/config.properties")) {
            PROPS.load(input);
        } catch (IOException e) {
            SystemLogger.log("Unable to load config", e);
        }
        DB_URL=PROPS.getProperty("db.url");
    }

    private static String get(String key) {
        return PROPS.getProperty(key);
    }
}
