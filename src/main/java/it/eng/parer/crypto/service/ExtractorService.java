/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna <p/> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. <p/> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details. <p/> You should
 * have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.crypto.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.crypto.data.AbstractSigner;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoSignerException;
import it.eng.parer.crypto.service.model.CryptoP7mUnsigned;
import it.eng.parer.crypto.service.verifica.SpringTikaSingleton;

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
     * @param xmlP7mFile file di tipo xml.p7m
     *
     * @return serializzazione in stringa del file xml
     *
     * @throws java.io.IOException in caso di file in input non xml.p7m oppure mime type dello
     *                             sbustato diverso da application/xml
     */
    public String extractXmlFromP7m(Path xmlP7mFile) throws IOException {
	File contentAsFile = null;
	try {
	    contentAsFile = extractUnsignP7m(xmlP7mFile);
	    String detectMimeType = detectMimeType(contentAsFile);

	    if (!MediaType.APPLICATION_XML.getBaseType().toString().equals(detectMimeType)) {
		throw new IOException(
			"Il mime type file sbustato non è application/xml ma " + detectMimeType);
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

    /**
     * Ottieni il file sbustato "unsigned" contenuto all'interno del p7m
     *
     * @param p7mFile          file di tipo xml.p7m
     * @param originalFileName nome del file "originale" inviato
     *
     * @return serializzazione in stringa del file xml
     *
     * @throws java.io.IOException in caso di file in input non xml.p7m oppure mime type dello
     *                             sbustato diverso da application/xml
     */
    public CryptoP7mUnsigned extractUnsignedFromP7m(Path p7mFile, String originalFileName)
	    throws IOException {
	File contentAsFile = null;
	try {
	    contentAsFile = extractUnsignP7m(p7mFile);
	    String detectMimeType = detectMimeType(contentAsFile);

	    return new CryptoP7mUnsigned(
		    StringUtils.isNotBlank(originalFileName) ? "unsigned_" + originalFileName
			    : contentAsFile.getName(),
		    detectMimeType, Files.newInputStream(contentAsFile.toPath(),
			    StandardOpenOption.DELETE_ON_CLOSE));
	} catch (CryptoSignerException | IOException ex) {
	    // in caso di eccezione il file temporaneo creato dal processo di estrazione viene
	    // eliminato
	    if (contentAsFile != null && !Files.deleteIfExists(contentAsFile.toPath())) {
		log.warn("Impossibile eliminare il file {}", contentAsFile.getName());
	    }
	    throw new IOException("Il file passato in input non è un P7M valido", ex);
	}
    }

    private File extractUnsignP7m(Path p7m) throws CryptoSignerException, IOException {
	AbstractSigner signerManager = signerUtil.getSignerManager(p7m.toFile());
	return signerManager.getContentAsFile();
    }

    private String detectMimeType(File p7m) {
	String detectMimeType = tika.detectMimeType(p7m);
	log.debug("Mime type del file estratto: {}", detectMimeType);
	return detectMimeType;
    }

}
