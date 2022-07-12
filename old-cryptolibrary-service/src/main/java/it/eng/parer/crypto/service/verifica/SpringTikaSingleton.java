package it.eng.parer.crypto.service.verifica;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.PostConstruct;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Quaranta_M
 *
 *         Singleton per caricare deploy-time Spring e Tika
 */
@Component
public class SpringTikaSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(SpringTikaSingleton.class);

    private Tika tika;

    @PostConstruct
    protected void initSingleton() {
        tika = new Tika();
    }

    /**
     * Metodo rivisto e corretto a partire da quello esistente su sacerws.
     *
     * @param document
     *            file di input
     * 
     * @return il mime type identificato o null
     */
    public String detectMimeType(File document) {
        String mimeType = null;

        if (document != null) {

            Metadata metadata = new Metadata();
            // Uso il TikaInputStream per riconoscere il mimetype application/msword (SUE #25694)
            try (InputStream is = TikaInputStream.get(document.toURI().toURL(), metadata);) {

                metadata.set(Metadata.RESOURCE_NAME_KEY, null);
                mimeType = tika.detect(is, metadata);

            } catch (IOException ex) {
                LOG.warn("Impossibile leggere il file durante il calcolo del MimeType", ex);
            }
        }
        return mimeType;
    }

}
