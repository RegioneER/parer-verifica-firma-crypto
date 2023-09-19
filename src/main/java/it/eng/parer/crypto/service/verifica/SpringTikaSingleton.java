/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.crypto.service.verifica;

import static org.apache.tika.metadata.Metadata.RESOURCE_NAME_KEY;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

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

                metadata.set(RESOURCE_NAME_KEY, null);
                mimeType = tika.detect(is, metadata);

            } catch (IOException ex) {
                LOG.warn("Impossibile leggere il file durante il calcolo del MimeType", ex);
            }
        }
        return mimeType;
    }

}
