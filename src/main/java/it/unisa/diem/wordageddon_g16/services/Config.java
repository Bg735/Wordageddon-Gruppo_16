package it.unisa.diem.wordageddon_g16.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties PROPS = new Properties();

    public enum Props {
        DB_URL("db.url"),
        PW_CHAR_MIN_LENGTH("auth.char_min_length"),
        USR_CHAR_MAX_LENGTH("auth.char_max_length"),
        SESSION_FILE("session.url");
        private final String key;

        Props(String key) {
            this.key = key;
        }

        public String get() {
            return key;
        }
    }

    static {
        try (InputStream input = Config.class.getResourceAsStream("/it/unisa/diem/wordageddon_g16/config.properties")) {
            if (input == null) {
                throw new RuntimeException("Could not find config.properties file in classpath");
            }
            PROPS.load(input);
        } catch (IOException e) {
            SystemLogger.log("Failed to find config. file", e);
        }
    }

    public static String get(Props key) {
        return PROPS.getProperty(key.get());
    }
}
