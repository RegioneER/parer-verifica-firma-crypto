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

package it.eng.parer.crypto.service;

import it.eng.crypto.data.AbstractSigner;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoSignerException;
import it.eng.parer.crypto.service.verifica.SpringTikaSingleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service layer per l'estrazione del contenuto dei p7m
 *
 * @author Snidero_L
 */
@Service
public class ExtractorService {

    private final Logger log = LoggerFactory.getLogger(ExtractorService.class);

    @Autowired
    private SignerUtil signerUtil;

    @Autowired
    private SpringTikaSingleton tika;

    /**
     * Ottieni il file xml contenuto all'interno del p7m
     *
     * @param xmlP7mFile
     *            file di tipo xml.p7m
     * 
     * @return serializzazione in stringa del file xml
     * 
     * @throws java.io.IOException
     *             in caso di file in input non xml.p7m oppure mime type dello sbustato diverso da application/xml
     */
    public String extractXmlFromP7m(Path xmlP7mFile) throws IOException {
        File contentAsFile = null;
        try {
            AbstractSigner signerManager = signerUtil.getSignerManager(xmlP7mFile.toFile());

            contentAsFile = signerManager.getContentAsFile();

            String detectMimeType = tika.detectMimeType(contentAsFile);
            log.debug("Mime type del file estratto: {}", detectMimeType);

            if (!MediaType.APPLICATION_XML.getBaseType().toString().equals(detectMimeType)) {
                throw new IOException("Il mime type file sbustato non è application/xml ma " + detectMimeType);
            }
            byte[] readAllBytes = Files.readAllBytes(contentAsFile.toPath());

            return new String(readAllBytes);

        } catch (CryptoSignerException | IOException ex) {
            throw new IOException("Il file passato in input non è un XML.P7M valido", ex);
        } finally {
            if (contentAsFile != null && !Files.deleteIfExists(contentAsFile.toPath())) {
                log.warn("Impossibile eliminare il file {}", contentAsFile.getName());
            }
        }

    }

}
