package it.unisa.diem.wordageddon_g16.utility;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Classe di utilità per la gestione delle risorse dell'applicazione,
 * come asset, stili e percorsi di upload.
 */
public class Resources {

    /**
     * Costruttore privato per impedire l'istanziazione della classe di utilità.
     */
    private Resources() {}

    /** Percorso base delle risorse nel classpath. */
    static final String RES_PATH = "/it/unisa/diem/wordageddon_g16/";

    /** Percorso base della cartella di upload sul filesystem. */
    static final Path UPLOADS_PATH = Path.of("uploads/");

    /**
     * Restituisce uno stream di input per un asset presente nella cartella delle risorse.
     *
     * @param filename il nome del file asset da caricare (ad esempio "logo.png")
     * @return uno {@link InputStream} per leggere il file asset, oppure {@code null} se non trovato
     */
    public static InputStream getAsset(String filename) {
        return Resources.class.getResourceAsStream(RES_PATH + "assets/" + filename);
    }

    /**
     * Restituisce il percorso esterno (URL) di un file di stile CSS presente nelle risorse.
     *
     * @param name il nome del file di stile (senza estensione)
     * @return la stringa URL del file CSS da usare, ad esempio per {@code Scene.getStylesheets().add()}
     */
    public static String getStyle(String name) {
        return Objects.requireNonNull(Resources.class.getResource(RES_PATH + "style/" + name + ".css")).toExternalForm();
    }

    /**
     * Restituisce il percorso della sottocartella "documents" all'interno della cartella di upload.
     *
     * @return il {@link Path} relativo a "uploads/documents"
     */
    public static Path getDocsPath() {
        return UPLOADS_PATH.resolve("documents");
    }
}
