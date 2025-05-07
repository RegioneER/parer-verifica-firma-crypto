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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import it.eng.crypto.controller.bean.DocumentAndTimeStampInfoBean;
import it.eng.crypto.controller.bean.OutputSignerBean;
import it.eng.crypto.controller.bean.TrustChainCheck;
import it.eng.crypto.controller.bean.ValidationInfos;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.data.signature.ISignature;
import it.eng.crypto.data.type.SignerType;
import it.eng.crypto.exception.CryptoSignerException;
import it.eng.crypto.manager.SignatureManager;
import it.eng.crypto.utils.OIDsMapConstants;
import it.eng.crypto.utils.VerificheEnums;
import it.eng.parer.crypto.model.CryptoEnums;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.CryptoAroContrFirmaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroContrMarcaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroControfirmaFirma;
import it.eng.parer.crypto.model.verifica.CryptoAroFirmaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroMarcaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroUsoCertifCaContrComp;
import it.eng.parer.crypto.model.verifica.CryptoAroUsoCertifCaContrMarca;
import it.eng.parer.crypto.model.verifica.CryptoFirCertifCa;
import it.eng.parer.crypto.model.verifica.CryptoFirCertifFirmatario;
import it.eng.parer.crypto.model.verifica.CryptoFirCrl;
import it.eng.parer.crypto.model.verifica.CryptoFirFilePerFirma;
import it.eng.parer.crypto.model.verifica.CryptoFirIssuer;
import it.eng.parer.crypto.model.verifica.CryptoFirUrlDistribCrl;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidate;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;
import it.eng.parer.crypto.model.verifica.input.CryptoDocumentoVersato;
import it.eng.parer.crypto.model.verifica.input.CryptoProfiloVerifica;
import it.eng.parer.crypto.service.model.CryptoDataToValidateData;
import it.eng.parer.crypto.service.model.CryptoDataToValidateFile;
import it.eng.parer.crypto.service.verifica.OldCryptoInvoker;
import it.eng.parer.crypto.service.verifica.SpringTikaSingleton;

/**
 * Servizio di verifica delle firme tramite cryptolibrary.
 *
 * @author Snidero_L
 */
@Service
public class VerificaFirmaService {

    private final Logger log = LoggerFactory.getLogger(VerificaFirmaService.class);

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
	    .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    @Autowired
    private OldCryptoInvoker cryptoInvoker;

    @Autowired
    private SpringTikaSingleton tika;

    @Autowired
    private SignerUtil signerUtil;

    @Autowired
    private Environment env;

    @Autowired
    private BuildProperties buildProperties;

    /**
     * Metodo utilizzato per ricondurre la versione "/api/" con json delle API di verifica alla
     * nuova versione "/api" con multipart (quella che suppporta il multipart).
     *
     * @param input Bean di input per la versione v1
     *              ({@link #verificaFirma(it.eng.parer.crypto.service.model.CryptoDataToValidateData, it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata) }
     *
     * @return Modello del report di verifica (comune alla versione 1 e 2).
     *
     * @throws CryptoParerException in caso di errore grave
     */
    public CryptoAroCompDoc verificaFirma(CryptoDataToValidate input) throws CryptoParerException {
	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();
	metadata.setProfiloVerifica(input.getProfiloVerifica());
	metadata.setTipologiaDataRiferimento(input.getTipologiaDataRiferimento());
	metadata.setUuid(input.getUuid());

	CryptoDataToValidateFile signedFile = new CryptoDataToValidateFile();
	List<CryptoDataToValidateFile> detachedSignature = new ArrayList<>();
	List<CryptoDataToValidateFile> detachedTimeStamp = new ArrayList<>();

	try {
	    final String suffix = ".crypto";

	    Path principale = Files.createTempFile("contenuto-", suffix, attr);
	    FileUtils.writeByteArrayToFile(principale.toFile(),
		    input.getContenuto().getContenuto());

	    signedFile.setNome(input.getContenuto().getNome());
	    signedFile.setContenuto(principale.toFile());

	    if (input.getSottoComponentiFirma() != null) {
		for (CryptoDocumentoVersato firma : input.getSottoComponentiFirma()) {
		    Path sig = Files.createTempFile("firma-", suffix, attr);
		    FileUtils.writeByteArrayToFile(sig.toFile(), firma.getContenuto());

		    detachedSignature
			    .add(new CryptoDataToValidateFile(firma.getNome(), sig.toFile()));
		}
	    }
	    if (input.getSottoComponentiMarca() != null) {
		for (CryptoDocumentoVersato marca : input.getSottoComponentiMarca()) {
		    Path ts = Files.createTempFile("marca-", suffix, attr);
		    FileUtils.writeByteArrayToFile(ts.toFile(), marca.getContenuto());

		    detachedTimeStamp
			    .add(new CryptoDataToValidateFile(marca.getNome(), ts.toFile()));
		}
	    }

	    CryptoDataToValidateData data = new CryptoDataToValidateData();
	    data.setContenuto(signedFile);
	    data.setSottoComponentiFirma(detachedSignature);
	    data.setSottoComponentiMarca(detachedTimeStamp);

	    return verificaFirma(data, metadata);

	} catch (IOException ex) {
	    throw new CryptoParerException(ex)
		    .withCode(ParerError.ErrorCode.SIGNATURE_VERIFICATION_IO)
		    .withMessage("Eccezione di IO durante la creazione di un file da verificare");
	} finally {
	    final String noDelete = "Impossibile eliminare ";
	    try {
		if (signedFile.getContenuto() != null) {
		    Files.deleteIfExists(signedFile.getContenuto().toPath());
		}
	    } catch (IOException e1) {
		log.atWarn().log("{}", noDelete + signedFile.getContenuto().getName());
	    }
	    detachedSignature.forEach(s -> {
		try {
		    Files.deleteIfExists(s.getContenuto().toPath());
		} catch (IOException e) {
		    log.atWarn().log("{}", noDelete + s.getContenuto().getName());
		}
	    });
	    detachedTimeStamp.forEach(s -> {
		try {
		    Files.deleteIfExists(s.getContenuto().toPath());
		} catch (IOException e) {
		    log.atWarn().log("{}", noDelete + s.getContenuto().getName());
		}
	    });
	}

    }

    /**
     * Entry point pubblico della verifica delle firme.Lo scopo di questo metodo è creare i file
     * necessari per la verifica effettuata tramite la cryptolibrary. Effettua la verifica delle
     * firme apposte. La verifica supporta i seguenti casi:
     * <ul>
     * <li>unico file da verificare firmato. Questo è un caso di firma <em>embedded</em> che
     * produrrà:
     * <ul>
     * <li>una busta</li>
     * <li>una o più firme per la busta</li>
     * </ul>
     * </li>
     * <li>un file da verificare (<u>firmato e non</u>) + un timestamp detached. Anche questo è un
     * caso di firma <em>embedded</em> che produrrà:
     * <ul>
     * <li>una busta</li>
     * <li>zero o più firme per la busta</li>
     * <li>una marca (al massimo per un limite della cryptolibrary) per la busta</li>
     * </ul>
     * </li>
     * <li>un file da verificare <u>non</u> firmato + una firma detached. Questo è un caso di firma
     * <em>detached</em> che produrrà:
     * <ul>
     * <li>una busta</li>
     * <li>una firma per la busta</li>
     * </ul>
     * </li>
     * <li>un file da verificare <u>non</u>firmato + una marca detached + una firma detached. Questo
     * tipo di validazione deve trattare il documento originale + la marca come <em>embedded</em>
     * mentre il documento originale più la firma come <em>detached</em>. Alla fine della verifica
     * produrrà:
     * <ul>
     * <li>una busta</li>
     * <li>una firma associata alla busta</li>
     * <li>una marca associata alla busta</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param data     Bean contenente i file da verificare. <em>Nota Bene</em> riempimento e
     *                 cancellazione dei file deve venire effettuato esternamente al metodo.
     *
     * @param metadata Bean contentente tutti i dati di configurazione per la verifica delle
     *                 firme/marche.
     *
     * @return Entity analoga alla "AroCompDoc" presente su sacer/ws.
     *
     * @throws CryptoParerException In caso di errore.
     */
    public CryptoAroCompDoc verificaFirma(CryptoDataToValidateData data,
	    CryptoDataToValidateMetadata metadata) throws CryptoParerException {
	CryptoAroCompDoc result = new CryptoAroCompDoc();
	final LocalDateTime inizioValidazione = LocalDateTime.now(ZoneId.systemDefault());
	try {
	    // Step 1 - Prepara input
	    result.setInizioValidazione(
		    Date.from(inizioValidazione.atZone(ZoneId.systemDefault()).toInstant()));
	    log.atDebug().log(
		    "Inizio validazione documento con identificativo [{}] - data/ora inizio {}",
		    metadata.getComponentePrincipale().getId(), inizioValidazione);
	    CryptoProfiloVerifica profiloVerifica = metadata.getProfiloVerifica();
	    // Salva nell'esito il profilo di verifica utilizzato
	    result.setProfiloValidazione(profiloVerifica);

	    // Step 2 - effettua la verifica
	    result.setIdComponente(data.getContenuto().getNome());
	    if (result.getAroFirmaComps() == null) {
		result.setAroFirmaComps(new ArrayList<>());
	    }
	    if (result.getAroMarcaComps() == null) {
		result.setAroMarcaComps(new ArrayList<>());
	    }
	    MutableInt pgFirma = new MutableInt(0);
	    MutableInt pgMarca = new MutableInt(0);

	    // Firme embedded e marche detached
	    processaFirmeEmbedded(data, metadata, pgFirma, pgMarca, result);
	    // Firme detached
	    processaFirmeDetached(data, metadata, pgFirma, pgMarca, result);
	    result.setValidatorVersion(validatorVersion());
	    result.setLibraryVersion(libraryVersion());
	    return result;

	} catch (Exception e) {
	    throw new CryptoParerException(metadata, e).withCode(ParerError.ErrorCode.GENERIC_ERROR)
		    .withMessage("Eccezione non gestita durante la verifica");
	} finally {

	    // Step 3 - imposta la data di fine validazione
	    LocalDateTime fineValidazione = LocalDateTime.now(ZoneId.systemDefault());
	    result.setFineValidazione(
		    Date.from(fineValidazione.atZone(ZoneId.systemDefault()).toInstant()));
	    final long msTrascorsi = Duration.between(inizioValidazione, fineValidazione)
		    .toMillis();
	    log.atInfo().log(
		    "Fine validazione documento con identificativo [{}] - data/ora fine {} (totale : {} ms)",
		    metadata.getComponentePrincipale().getId(), fineValidazione, msTrascorsi);

	}

    }

    /**
     * Effettua la verifiche delle firme embedded con o senza marche detached. In questo metodo
     * viene, inoltre calcolato il formato del componente principale.
     *
     * @param data     Bean contenente i file da verificare. <em>Nota Bene</em> riempimento e
     *                 cancellazione dei file deve venire effettuato esternamente al metodo.
     *
     * @param metadata Bean contentente tutti i dati di configurazione per la verifica delle
     *                 firme/marche.
     * @param pgFirma  progressivo firma
     * @param pgMarca  progressimo marca
     * @param result   bean che viene popolato come risultato
     */
    private void processaFirmeEmbedded(CryptoDataToValidateData data,
	    CryptoDataToValidateMetadata metadata, MutableInt pgFirma, MutableInt pgMarca,
	    CryptoAroCompDoc result) {
	CryptoProfiloVerifica profiloVerifica = metadata.getProfiloVerifica();
	OutputSignerBean out = null;
	final CryptoEnums.TipoRifTemporale referenceDateType = metadata
		.getTipologiaDataRiferimento().getReferenceDateType();
	long dtRif = metadata.getTipologiaDataRiferimento().getDataRiferimento();
	final Date referenceDate = new Date(dtRif);
	final boolean useSigningDate = metadata.getTipologiaDataRiferimento().isUseSigningDate();

	boolean isXml = false;

	try {
	    // Componente senza marche detached.
	    if (data.getSottoComponentiMarca() == null
		    || data.getSottoComponentiMarca().isEmpty()) {

		try {
		    out = cryptoInvoker.verFirmeEmbeddedVers(data.getContenuto().getContenuto(),
			    null, referenceDate, useSigningDate, referenceDateType.name());
		    isXml = SignatureManager.getIsXml();
		    this.extractVerifyInfo(out, result, pgFirma, pgMarca, null, false, null,
			    profiloVerifica);
		} catch (Exception ex) {
		    this.extractVerifyInfo(out, result, pgFirma, pgMarca, null, false, ex,
			    profiloVerifica);

		} finally {
		    SignatureManager.cleanIsXml();
		}
	    } else {

		// Verifica con Marche detached.
		for (CryptoDataToValidateFile marcheDetached : data.getSottoComponentiMarca()) {
		    CryptoAroCompDoc sottoCompMarcaDetached = new CryptoAroCompDoc();
		    sottoCompMarcaDetached.setIdComponente(marcheDetached.getNome());
		    try {
			out = cryptoInvoker.verFirmeEmbeddedVers(data.getContenuto().getContenuto(),
				marcheDetached.getContenuto(), referenceDate, useSigningDate,
				referenceDateType.name());
			isXml = SignatureManager.getIsXml();
			this.extractVerifyInfo(out, result, pgFirma, pgMarca,
				sottoCompMarcaDetached, false, null, profiloVerifica);
		    } catch (Exception ex) {
			this.extractVerifyInfo(out, result, pgFirma, pgMarca,
				sottoCompMarcaDetached, false, ex, profiloVerifica);

		    } finally {
			SignatureManager.cleanIsXml();
		    }
		}
	    }

	    // formato documento principale
	    String tikaDetect = tikaDetect(out, data.getContenuto().getContenuto(), isXml);
	    result.setTikaMimeComponentePrincipale(tikaDetect);
	} finally {
	    // elimina i file generati duranti la fase di verifica.
	    cleanOutput(out);
	}
    }

    /**
     * Effettua la verifiche delle firme detached.
     *
     * @param data     Bean contenente i file da verificare. <em>Nota Bene</em> riempimento e
     *                 cancellazione dei file deve venire effettuato esternamente al metodo.
     *
     * @param metadata Bean contentente tutti i dati di configurazione per la verifica delle
     *                 firme/marche.
     * @param pgFirma  progressivo firma
     * @param pgMarca  progressimo marca
     * @param result   bean che viene popolato come risultato
     */
    private void processaFirmeDetached(CryptoDataToValidateData data,
	    CryptoDataToValidateMetadata metadata, MutableInt pgFirma, MutableInt pgMarca,
	    CryptoAroCompDoc result) {
	CryptoProfiloVerifica profiloVerifica = metadata.getProfiloVerifica();
	OutputSignerBean out = null;
	final CryptoEnums.TipoRifTemporale referenceDateType = metadata
		.getTipologiaDataRiferimento().getReferenceDateType();
	final Date referenceDate = new Date(
		metadata.getTipologiaDataRiferimento().getDataRiferimento());
	final boolean useSigningDate = metadata.getTipologiaDataRiferimento().isUseSigningDate();

	// Verifica firme detached.
	for (CryptoDataToValidateFile detachedSignatureVersata : data.getSottoComponentiFirma()) {
	    CryptoAroCompDoc sottoCompFirma = new CryptoAroCompDoc();
	    sottoCompFirma.setIdComponente(detachedSignatureVersata.getNome());
	    try {
		out = cryptoInvoker.verFirmeDetachedVers(data.getContenuto().getContenuto(),
			detachedSignatureVersata.getContenuto(), referenceDate, useSigningDate,
			referenceDateType.name());
		this.extractVerifyInfo(out, result, pgFirma, pgMarca, sottoCompFirma, true, null,
			profiloVerifica);
	    } catch (Exception ex) {
		this.extractVerifyInfo(out, result, pgFirma, pgMarca, sottoCompFirma, true, ex,
			profiloVerifica);
	    } finally {
		SignatureManager.cleanIsXml();
		// elimina i file generati duranti la fase di verifica.
		cleanOutput(out);
	    }
	}
    }

    private String validatorVersion() {
	return env.getProperty("git.build.version");
    }

    private String libraryVersion() {
	return buildProperties.get("eng-cryptolibrary");
    }

    private String tikaDetect(OutputSignerBean out, File contenuto, boolean isXml) {
	String tikaMime = null;

	if (out != null) {
	    tikaMime = this.detectMimeType(out);
	}

	if (tikaMime == null) {
	    tikaMime = tika.detectMimeType(contenuto);
	}

	// tikamime potrebbe essere nullo se sono in presenza di un pdf firmato o di una
	// marca detached
	if (tikaMime != null) {
	    // Verifico poi se il formato rilevato da TIKA è text/plain o application/xml ..
	    if (tikaMime.equals(CryptoEnums.TEXT_PLAIN_MIME)
		    || tikaMime.equals(CryptoEnums.XML_MIME)) {
		// .. se l'XMLSigner ha parsato correttamente l'xml allora si tratta di un XML
		// altrimenti di un
		// text/plain
		tikaMime = (isXml) ? CryptoEnums.XML_MIME : CryptoEnums.TEXT_PLAIN_MIME;
	    } else if (tikaMime.contains(CryptoEnums.DXF_MIME)) {
		/*
		 * tikaMime per il formato DXF può essere "image/vnd.dxf;format=ascii" o
		 * "image/vnd.dxf;format=binary" in entrambi i casi prendiamo solo il mymetype
		 * escludendo il tipo di formato
		 */
		tikaMime = CryptoEnums.DXF_MIME;
	    }
	}

	log.atDebug().log("Formato rilevato : {}", tikaMime);

	return tikaMime;
    }

    private String detectMimeType(OutputSignerBean out) {
	File file = null;
	OutputSignerBean cycleOut = out;
	while (cycleOut != null && cycleOut.getContent() != null) {
	    // Contenuto sbustato
	    file = cycleOut.getContent().getContentFile();
	    cycleOut = cycleOut.getChild();
	}
	if (file != null) {
	    return tika.detectMimeType(file);
	} else {
	    return null;
	}
    }

    private void cleanOutput(OutputSignerBean out) {
	File file = null;
	OutputSignerBean cycleOut = out;
	while (cycleOut != null && cycleOut.getContent() != null) {
	    // Contenuto sbustato
	    file = cycleOut.getContent().getContentFile();
	    try {
		if (file != null) {
		    log.atDebug().log("Sto per eliminare il file {}", file.getName());
		    Files.deleteIfExists(file.toPath());
		}
	    } catch (IOException e) {
		log.atError().log("Si è verificato un errore durante la pulizia dei dati", e);
	    }
	    cycleOut = cycleOut.getChild();
	}

    }

    @SuppressWarnings("unchecked")
    private void extractVerifyInfo(OutputSignerBean output, CryptoAroCompDoc componente,
	    MutableInt pgFirma, MutableInt pgMarca, CryptoAroCompDoc sottoComponente,
	    boolean isDetachedSignature, Exception ex, CryptoProfiloVerifica profiloVerifica) {

	int pgBusta = 1;
	// Creo anche il nuovo oggetto Busta crittografica
	String idBustaSottoComponente = null;
	if (sottoComponente != null) {
	    idBustaSottoComponente = sottoComponente.getIdComponente();

	}
	if (output == null) {
	    if (ex != null) {
		log.atWarn().log("Passata eccezione a extractVerifyInfo", ex);
	    } else if (sottoComponente != null) {
		// Caso firma
		if (isDetachedSignature) {
		    CryptoAroFirmaComp firmaSconosciuta = this
			    .buildFirmaSconosciuta(idBustaSottoComponente, pgFirma);
		    componente.getAroFirmaComps().add(firmaSconosciuta);
		} // Caso marca
		else {
		    CryptoAroMarcaComp marcaSconosciuta = this
			    .buildMarcaSconosciuta(idBustaSottoComponente, pgMarca);
		    componente.getAroMarcaComps().add(marcaSconosciuta);

		}
	    }

	} else {

	    while (output != null) {

		List<ISignature> signatures = (List<ISignature>) output
			.getProperty(OutputSignerBean.SIGNATURE_PROPERTY);
		this.buildComponente(signatures, pgBusta, output, pgFirma, pgMarca, componente,
			null, idBustaSottoComponente, isDetachedSignature, profiloVerifica);

		output = output.getChild();
		pgBusta++;
	    }
	    // VERIFICARE CHE LA NUOVA CONDIZIONE SIA ISOMORFA ALLA PRECEDENTE!!!
	    if (idBustaSottoComponente != null
		    && componente.getFirme(idBustaSottoComponente).isEmpty()
		    && componente.getMarche(idBustaSottoComponente).isEmpty()) {

		if (isDetachedSignature) {
		    CryptoAroFirmaComp firmaSconosciuta = this
			    .buildFirmaSconosciuta(idBustaSottoComponente, pgFirma);
		    componente.getAroFirmaComps().add(firmaSconosciuta);
		} // Caso marca
		else {
		    CryptoAroMarcaComp marcaSconosciuta = this
			    .buildMarcaSconosciuta(idBustaSottoComponente, pgMarca);
		    componente.getAroMarcaComps().add(marcaSconosciuta);
		}
	    }

	}

    }

    @SuppressWarnings("unchecked")
    private void buildComponente(List<ISignature> signatures, int pgBusta, OutputSignerBean output,
	    MutableInt pgFirma, MutableInt pgMarca, CryptoAroCompDoc componente,
	    CryptoAroControfirmaFirma controFirma, String idBustaSottoComponente,
	    boolean isDetachedSignature, CryptoProfiloVerifica profiloVerifica) {
	Map<ISignature, ValidationInfos> formatValidity = (Map<ISignature, ValidationInfos>) output
		.getProperty(OutputSignerBean.FORMAT_VALIDITY_PROPERTY);
	Map<ISignature, ValidationInfos> signatureValidations = (Map<ISignature, ValidationInfos>) output
		.getProperty(OutputSignerBean.SIGNATURE_VALIDATION_PROPERTY);
	Map<ISignature, ValidationInfos> certificateExpirations = (Map<ISignature, ValidationInfos>) output
		.getProperty(OutputSignerBean.CERTIFICATE_EXPIRATION_PROPERTY);
	Map<ISignature, ValidationInfos> crlValidation = (Map<ISignature, ValidationInfos>) output
		.getProperty(OutputSignerBean.CRL_VALIDATION_PROPERTY);
	Map<ISignature, ValidationInfos> unqualifiedSignatures = (Map<ISignature, ValidationInfos>) output
		.getProperty(OutputSignerBean.CERTIFICATE_UNQUALIFIED_PROPERTY);
	Map<ISignature, ValidationInfos> certificateAssociation = (Map<ISignature, ValidationInfos>) output
		.getProperty(OutputSignerBean.CERTIFICATE_VALIDATION_PROPERTY);
	List<DocumentAndTimeStampInfoBean> timeStampInfos = (List<DocumentAndTimeStampInfoBean>) output
		.getProperty(OutputSignerBean.TIME_STAMP_INFO_PROPERTY);
	Map<ISignature, List<TrustChainCheck>> caCertificateMap = (Map<ISignature, List<TrustChainCheck>>) output
		.getProperty(OutputSignerBean.CERTIFICATE_RELIABILITY_PROPERTY);
	Map<ISignature, X509CRL> crlMap = (Map<ISignature, X509CRL>) output
		.getProperty(OutputSignerBean.CRL_PROPERTY);
	Map<String, ValidationInfos> complianceChecks = (Map<String, ValidationInfos>) output
		.getProperty(OutputSignerBean.FORMAT_COMPLIANCE_PROPERTY);
	try {
	    String idBustaComponente = componente.getIdComponente();

	    if (signatures != null) {
		for (ISignature s : signatures) {

		    TimeStampToken tst = s.getTimeStamp();
		    CryptoAroMarcaComp marca = null;
		    DocumentAndTimeStampInfoBean tstInfo = null;
		    if (tst != null && timeStampInfos != null) {
			for (DocumentAndTimeStampInfoBean d : timeStampInfos) {
			    if (d.getTimeStampToken().equals(tst)) {
				tstInfo = d;
				timeStampInfos.remove(d);
				pgMarca.increment();
				marca = this.buildMarca(tstInfo, pgBusta, pgMarca, profiloVerifica);
				if (tstInfo.getTimeStampTokenType().equals(
					DocumentAndTimeStampInfoBean.TimeStampTokenType.DETACHED)) {
				    marca.setIdMarca(idBustaSottoComponente);
				} else {
				    marca.setIdMarca(idBustaComponente);
				}
				componente.getAroMarcaComps().add(marca);
				break;
			    }
			}

		    }
		    CryptoAroFirmaComp firma;
		    pgFirma.increment();
		    firma = this.buildFirma(s, pgBusta, caCertificateMap, crlMap, // crlCaMap,
			    formatValidity, signatureValidations, certificateExpirations,
			    crlValidation, unqualifiedSignatures, certificateAssociation, pgFirma,
			    profiloVerifica);// ,

		    if (isDetachedSignature) {
			firma.setIdFirma(idBustaSottoComponente);

		    } else {
			firma.setIdFirma(idBustaComponente);
		    }
		    // aggiungo la marca alla firma
		    componente.getAroFirmaComps().add(firma);

		    // Se questa è una controfirma ..
		    if (controFirma != null) {
			// nella relazione sarà il padre
			// e aggiunge la controfirma ai propri figli ..
			controFirma.setAroFirmaFiglio(firma);
		    }
		    List<ISignature> controFirme = s.getCounterSignatures();
		    // Se questa firma ha una controfirma (ie: se è un figlio)
		    if (!controFirme.isEmpty()) {
			CryptoAroControfirmaFirma controfirmaFirma = new CryptoAroControfirmaFirma();
			// nella relazione sarà il figlio

			firma.getAroControfirmaFirmaFiglios().add(controfirmaFirma);

			this.buildComponente(controFirme, pgBusta, output, pgFirma, pgMarca,
				componente, controfirmaFirma, idBustaSottoComponente,
				isDetachedSignature, profiloVerifica);

		    }

		}
	    }

	    // se non ci sono firme forse c'è stato un errore di conformità
	    if (complianceChecks != null) {
		CryptoAroFirmaComp firma;
		pgFirma.increment();
		firma = this.buildFirmaNonConforme(pgBusta, pgFirma, complianceChecks);
		if (isDetachedSignature) {
		    firma.setIdFirma(idBustaSottoComponente);
		} else {
		    firma.setIdFirma(idBustaComponente);
		}
		componente.getAroFirmaComps().add(firma);

	    }

	    // processo le marche restanti ma solo quando avrò finito con le controfirme
	    // e solo se non è una firma detached ..
	    if (timeStampInfos != null && controFirma == null && !isDetachedSignature) {
		for (DocumentAndTimeStampInfoBean tstInfo : timeStampInfos) {
		    pgMarca.increment();
		    CryptoAroMarcaComp marca = this.buildMarca(tstInfo, pgBusta, pgMarca,
			    profiloVerifica);
		    if (tstInfo.getTimeStampTokenType()
			    .equals(DocumentAndTimeStampInfoBean.TimeStampTokenType.DETACHED)) {
			marca.setIdMarca(idBustaSottoComponente);
		    } else {
			marca.setIdMarca(idBustaComponente);
		    }
		    componente.getAroMarcaComps().add(marca);
		}
	    }

	} catch (CRLException ex) {
	    log.atError().log("Eccezione nella lettura della CRL", ex);
	} catch (IOException ex) {
	    log.atError().log("Eccezione nella lettura del numero della CRL", ex);
	} catch (CertificateEncodingException ex) {
	    log.atError().log("Eccezione nella lettura del certificato X.509", ex);
	}
    }

    protected CryptoAroMarcaComp buildMarca(DocumentAndTimeStampInfoBean tstInfo, int pgBusta,
	    MutableInt pgMarca, CryptoProfiloVerifica profiloVerifica)
	    throws CertificateEncodingException, IOException, CRLException {
	// MARCA
	CryptoAroMarcaComp marca = new CryptoAroMarcaComp();
	marca.setAroContrMarcaComps(new ArrayList<>());
	X509Certificate tsaCert = (X509Certificate) tstInfo.getValidityInfo()
		.get(DocumentAndTimeStampInfoBean.PROP_CERTIFICATE);
	X509CRL crlTsa = (X509CRL) tstInfo.getValidityInfo()
		.get(DocumentAndTimeStampInfoBean.PROP_CRL);

	ValidationInfos crittograficoValInfo = (ValidationInfos) tstInfo.getValidityInfo()
		.get(VerificheEnums.TipoControlliMarca.CRITTOGRAFICO.name());
	ValidationInfos catenaValInfo = (ValidationInfos) tstInfo.getValidityInfo()
		.get(VerificheEnums.TipoControlliMarca.CATENA_TRUSTED.name());
	ValidationInfos certificatoValInfo = (ValidationInfos) tstInfo.getValidityInfo()
		.get(VerificheEnums.TipoControlliMarca.CERTIFICATO.name());
	ValidationInfos crlValidInfo = (ValidationInfos) tstInfo.getValidityInfo()
		.get(VerificheEnums.TipoControlliMarca.CRL.name());
	boolean esitoVerifiche = true;
	if (crittograficoValInfo != null && crittograficoValInfo
		.getEsito() != VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO) {

	    // CERTIFICATO TSA + BLOB
	    CryptoFirCertifCa certificatoTsa = this.buildFirCertifCa(tsaCert, profiloVerifica);

	    // CRL TSA + BLOB
	    CryptoFirCrl firCrlTsa = this.buildFirCrl(crlTsa, certificatoTsa, profiloVerifica);

	    // CONTROLLI MARCA - CRITTOGRAFICO signatureValidations
	    CryptoAroContrMarcaComp controlloCrittografico = new CryptoAroContrMarcaComp();
	    marca.getAroContrMarcaComps().add(controlloCrittografico);
	    controlloCrittografico
		    .setTiContr(VerificheEnums.TipoControlliMarca.CRITTOGRAFICO.name());

	    // CONTROLLI MARCA - CATENA_TRUSTED CertificateAssociation &&
	    // CertificateReliability
	    CryptoAroContrMarcaComp controlliCatenaTrusted = new CryptoAroContrMarcaComp();
	    controlliCatenaTrusted.setAroUsoCertifCaContrMarcas(new ArrayList<>());
	    marca.getAroContrMarcaComps().add(controlliCatenaTrusted);
	    controlliCatenaTrusted
		    .setTiContr(VerificheEnums.TipoControlliMarca.CATENA_TRUSTED.name());
	    CryptoAroUsoCertifCaContrMarca usoCertifCatena = new CryptoAroUsoCertifCaContrMarca();
	    controlliCatenaTrusted.getAroUsoCertifCaContrMarcas().add(usoCertifCatena);
	    usoCertifCatena.setFirCertifCa(certificatoTsa);
	    usoCertifCatena.setPgCertifCa(BigDecimal.ONE);
	    usoCertifCatena.setFirCrl(firCrlTsa);

	    // CONTROLLI MARCA - CERTIFICATO CertificateExpiration
	    CryptoAroContrMarcaComp controlliCertificato = new CryptoAroContrMarcaComp();
	    marca.getAroContrMarcaComps().add(controlliCertificato);
	    controlliCertificato.setTiContr(VerificheEnums.TipoControlliMarca.CERTIFICATO.name());
	    // CONTROLLI MARCA - CRL CertificateRevocation
	    CryptoAroContrMarcaComp controlliCRL = new CryptoAroContrMarcaComp();
	    marca.getAroContrMarcaComps().add(controlliCRL);
	    controlliCRL.setTiContr(VerificheEnums.TipoControlliMarca.CRL.name());
	    controlliCRL.setFirCrl(firCrlTsa);

	    // CONTROLLI MARCA - CONFORMITA' + CRITTOGRAFICO
	    if (crittograficoValInfo != null) {

		if (crittograficoValInfo.isValid()) {
		    controlloCrittografico
			    .setTiEsitoContrMarca(VerificheEnums.EsitoControllo.POSITIVO.name());
		    controlloCrittografico.setDsMsgEsitoContrMarca(
			    VerificheEnums.EsitoControllo.POSITIVO.message());
		} else {
		    esitoVerifiche = false;
		    controlloCrittografico
			    .setTiEsitoContrMarca(VerificheEnums.EsitoControllo.NEGATIVO.name());
		    controlloCrittografico
			    .setDsMsgEsitoContrMarca(crittograficoValInfo.getEsito() != null
				    ? crittograficoValInfo.getEsito().message()
				    : "" + crittograficoValInfo.getErrorsString() + " "
					    + crittograficoValInfo.getWarningsString());
		}
	    }

	    // CONTROLLI MARCA - CATENA_TRUSTED
	    if (catenaValInfo != null) {
		if (catenaValInfo.isValid()) {
		    controlliCatenaTrusted
			    .setTiEsitoContrMarca(VerificheEnums.EsitoControllo.POSITIVO.name());
		    controlliCatenaTrusted.setDsMsgEsitoContrMarca(
			    VerificheEnums.EsitoControllo.POSITIVO.message());
		    marca.setTiMarcaTemp(VerificheEnums.TipoMarca.A_NORMA.name());
		} else {
		    esitoVerifiche = false;
		    controlliCatenaTrusted
			    .setTiEsitoContrMarca(VerificheEnums.EsitoControllo.NEGATIVO.name());
		    controlliCatenaTrusted.setDsMsgEsitoContrMarca(
			    VerificheEnums.EsitoControllo.NEGATIVO.message() + " : "
				    + catenaValInfo.getErrorsString());

		}
	    }

	    // CONTROLLI MARCA - SCADENZA CERTIFICATO
	    if (certificatoValInfo != null) {
		if (certificatoValInfo.isValid()) {
		    controlliCertificato
			    .setTiEsitoContrMarca(VerificheEnums.EsitoControllo.POSITIVO.name());
		    controlliCertificato.setDsMsgEsitoContrMarca(
			    VerificheEnums.EsitoControllo.POSITIVO.message());

		} else {
		    esitoVerifiche = false;
		    controlliCertificato.setTiEsitoContrMarca(certificatoValInfo.getEsito().name());
		    controlliCertificato
			    .setDsMsgEsitoContrMarca(certificatoValInfo.getEsito().message() + ": "
				    + certificatoValInfo.getErrorsString());
		}
	    }

	    // CONTROLLI MARCA - CRL
	    if (crlValidInfo != null) {
		if (crlValidInfo.isValid()) {
		    controlliCRL
			    .setTiEsitoContrMarca(VerificheEnums.EsitoControllo.POSITIVO.name());
		    controlliCRL.setDsMsgEsitoContrMarca(
			    VerificheEnums.EsitoControllo.POSITIVO.message());
		} else {
		    esitoVerifiche = false;
		    controlliCRL.setTiEsitoContrMarca(crlValidInfo.getEsito().name());
		    controlliCRL.setDsMsgEsitoContrMarca(crlValidInfo.getEsito().message() + ": "
			    + crlValidInfo.getErrorsString());
		}
	    }
	    marca.setFirCertifCa(certificatoTsa);
	}

	// DA SETTARE LATO COMPONENTE
	// DA SETTARE PASSANDO PROGRESSIVO
	if (crittograficoValInfo
		.getEsito() != VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO) {
	    marca.setTiEsitoContrConforme(VerificheEnums.EsitoControllo.POSITIVO.name());
	    marca.setDsMsgEsitoContrConforme(VerificheEnums.EsitoControllo.POSITIVO.message());
	} else {
	    esitoVerifiche = false;
	    marca.setTiEsitoContrConforme(
		    VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO.name());
	    marca.setDsMsgEsitoContrConforme(crittograficoValInfo.getErrorsString());
	}
	if (marca.getTiMarcaTemp() == null) {
	    marca.setTiMarcaTemp(VerificheEnums.TipoMarca.SEMPLICE.name());
	}

	SignerInformation signer = (SignerInformation) tstInfo.getTimeStampToken().toCMSSignedData()
		.getSignerInfos().getSigners().iterator().next();
	marca.setDsMarcaBase64(new String(Base64.getEncoder().encode(signer.getSignature())));
	String alg = OIDsMapConstants.getDigestNames().get(signer.getDigestAlgOID()) + "with"
		+ OIDsMapConstants.getAlgorithmNames().get(signer.getEncryptionAlgOID());
	marca.setDsAlgoMarca(alg);
	marca.setTmMarcaTemp(tstInfo.getTimeStampToken().getTimeStampInfo().getGenTime());
	marca.setTiFormatoMarca(((SignerType) tstInfo.getValidityInfo()
		.get(DocumentAndTimeStampInfoBean.PROP_TIMESTAMP_FORMAT)).name());
	if (!Objects.isNull(tsaCert)) {
	    marca.setDtScadMarca(tsaCert.getNotAfter());
	}
	marca.setTiEsitoVerifMarca(esitoVerifiche ? VerificheEnums.EsitoControllo.POSITIVO.name()
		: VerificheEnums.EsitoControllo.WARNING.name());
	marca.setDsMsgEsitoVerifMarca(
		esitoVerifiche ? VerificheEnums.EsitoControllo.POSITIVO.message()
			: VerificheEnums.EsitoControllo.WARNING.message());
	marca.setPgBusta(BigDecimal.valueOf(pgBusta));
	marca.setPgMarca(BigDecimal.valueOf(pgMarca.intValue()));
	return marca;

    }

    protected CryptoAroFirmaComp buildFirma(ISignature s, int pgBusta,
	    Map<ISignature, List<TrustChainCheck>> trustChainMap, Map<ISignature, X509CRL> crlMap,
	    Map<ISignature, ValidationInfos> formatValidity,
	    Map<ISignature, ValidationInfos> signatureValidations,
	    Map<ISignature, ValidationInfos> certificateExpirations,
	    Map<ISignature, ValidationInfos> crlValidation,
	    Map<ISignature, ValidationInfos> unqualifiedSignatures,
	    Map<ISignature, ValidationInfos> certificateAssociation,
	    MutableInt pgFirma/* , BigDecimal idStrut */, CryptoProfiloVerifica profiloVerifica)
	    throws CertificateEncodingException, IOException, CRLException {

	boolean esitoVerifiche = true;
	X509Certificate caCert = trustChainMap.get(s).get(0).getCerificate();
	X509CRL crlCa = trustChainMap.get(s).get(0).getCrl();
	List<TrustChainCheck> trustChainList = trustChainMap.get(s);
	X509Certificate signerCert = s.getSignerBean().getCertificate();
	X509CRL crl = crlMap.get(s);

	// CERTIFICATO CA + BLOB
	CryptoFirCertifCa certificatoCa = this.buildFirCertifCa(caCert, profiloVerifica);
	// FIX MAC #
	if (certificatoCa.getFirIssuer().getDlDnIssuerCertifCa() == null) {
	    certificatoCa.getFirIssuer()
		    .setDlDnIssuerCertifCa(s.getSignerBean().getIusser().getName());
	}

	// CRL CA + BLOB
	CryptoFirCrl firCrlCa = this.buildFirCrl(crlCa, certificatoCa, profiloVerifica);

	// CERTIFICATO FIRMATARIO + BLOB
	CryptoFirCertifFirmatario firCertifFirmatario = this.buildFirCertifFirmatario(signerCert,
		certificatoCa, profiloVerifica);

	// CRL + BLOB
	CryptoFirCrl firCrl = this.buildFirCrl(crl, certificatoCa, profiloVerifica);

	// FIRMA
	CryptoAroFirmaComp firma = new CryptoAroFirmaComp();
	firma.setAroContrFirmaComps(new ArrayList<>());
	firma.setAroControfirmaFirmaFiglios(new ArrayList<>());

	// CONTROLLI FIRMA - CRITTOGRAFICO signatureValidations
	CryptoAroContrFirmaComp controlloCrittografico = new CryptoAroContrFirmaComp();
	firma.getAroContrFirmaComps().add(controlloCrittografico);
	controlloCrittografico.setTiContr(VerificheEnums.TipoControlli.CRITTOGRAFICO.name());

	// CONTROLLI FIRMA - CRITTOGRAFICO_ABILITATO signatureValidations
	CryptoAroContrFirmaComp controlloCrittograficoAbilitato = new CryptoAroContrFirmaComp();
	firma.getAroContrFirmaComps().add(controlloCrittograficoAbilitato);
	controlloCrittograficoAbilitato
		.setTiContr(VerificheEnums.TipoControlli.CRITTOGRAFICO_ABILITATO.name());

	// CONTROLLI FIRMA - CATENA_TRUSTED CertificateAssociation &&
	// CertificateReliability
	CryptoAroContrFirmaComp controlliCatenaTrusted = new CryptoAroContrFirmaComp();
	controlliCatenaTrusted.setAroUsoCertifCaContrComps(new ArrayList<>());
	firma.getAroContrFirmaComps().add(controlliCatenaTrusted);
	controlliCatenaTrusted.setTiContr(VerificheEnums.TipoControlli.CATENA_TRUSTED.name());

	CryptoAroUsoCertifCaContrComp usoCertifCatena = new CryptoAroUsoCertifCaContrComp();
	controlliCatenaTrusted.getAroUsoCertifCaContrComps().add(usoCertifCatena);
	usoCertifCatena.setFirCertifCa(certificatoCa);
	usoCertifCatena.setPgCertifCa(BigDecimal.ONE);
	usoCertifCatena.setFirCrl(firCrlCa);

	for (int i = 0; i < trustChainList.size(); i++) {
	    if (i == 0) {
		continue;
	    }
	    TrustChainCheck chainBean = trustChainList.get(i);
	    // CERTIFICATO CA + BLOB
	    certificatoCa = this.buildFirCertifCa(chainBean.getCerificate(), profiloVerifica);

	    // CRL CA + BLOB
	    firCrlCa = this.buildFirCrl(chainBean.getCrl(), certificatoCa, profiloVerifica);
	    usoCertifCatena = new CryptoAroUsoCertifCaContrComp();
	    controlliCatenaTrusted.getAroUsoCertifCaContrComps().add(usoCertifCatena);
	    usoCertifCatena.setFirCertifCa(certificatoCa);
	    usoCertifCatena.setPgCertifCa(BigDecimal.valueOf((i + 1)));
	    usoCertifCatena.setFirCrl(firCrlCa);
	}

	// CONTROLLI FIRMA - CATENA_TRUSTED_ABILITATO CertificateAssociation &&
	// CertificateReliability
	CryptoAroContrFirmaComp controlliCatenaTrustedAbilitato = new CryptoAroContrFirmaComp();
	firma.getAroContrFirmaComps().add(controlliCatenaTrustedAbilitato);
	controlliCatenaTrustedAbilitato
		.setTiContr(VerificheEnums.TipoControlli.CATENA_TRUSTED_ABILITATO.name());

	// CONTROLLI FIRMA - CERTIFICATO CertificateExpiration
	CryptoAroContrFirmaComp controlliCertificato = new CryptoAroContrFirmaComp();
	firma.getAroContrFirmaComps().add(controlliCertificato);
	controlliCertificato.setTiContr(VerificheEnums.TipoControlli.CERTIFICATO.name());

	// CONTROLLI FIRMA - CRL CertificateRevocation
	CryptoAroContrFirmaComp controlliCRL = new CryptoAroContrFirmaComp();
	firma.getAroContrFirmaComps().add(controlliCRL);
	controlliCRL.setTiContr(VerificheEnums.TipoControlli.CRL.name());
	controlliCRL.setFirCrl(firCrl);

	// CONTROLLI FIRMA - CRITTOGRAFICO
	if (signatureValidations != null) {
	    ValidationInfos signatureValidation = signatureValidations.get(s);
	    if (signatureValidation.isValid()) {
		controlloCrittografico
			.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.POSITIVO.name());

		if (!signatureValidation.isValid(true)) {
		    controlloCrittografico.setDsMsgEsitoContrFirma(
			    VerificheEnums.EsitoControllo.POSITIVO.message() + " - Warning: "
				    + signatureValidation.getWarningsString());
		} else {
		    controlloCrittografico.setDsMsgEsitoContrFirma(
			    VerificheEnums.EsitoControllo.POSITIVO.message());
		}
	    } else {
		esitoVerifiche = false;
		controlloCrittografico
			.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.NEGATIVO.name());
		controlloCrittografico
			.setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.NEGATIVO.message()
				+ ": " + signatureValidation.getErrorsString() + " "
				+ signatureValidation.getWarningsString());
	    }
	}

	// CONTROLLI FIRMA - CRITTOGRAFICO_ABILITATO
	controlloCrittograficoAbilitato
		.setTiEsitoContrFirma(controlloCrittografico.getTiEsitoContrFirma());
	controlloCrittograficoAbilitato
		.setDsMsgEsitoContrFirma(controlloCrittografico.getDsMsgEsitoContrFirma());

	// CONTROLLI FIRMA - CRITTOGRAFICO_DISABILTATO
	if (!profiloVerifica.isControlloCrittograficoAbilitato()) {

	    controlloCrittografico
		    .setTiEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.name());
	    controlloCrittografico
		    .setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.message());

	}
	// CONTROLLI FIRMA - CATENA_TRUSTED
	if (certificateAssociation != null && unqualifiedSignatures != null) {
	    ValidationInfos unqualifiedSignature = unqualifiedSignatures.get(s);
	    ValidationInfos certificateAssociationInfo = certificateAssociation.get(s);
	    if ((unqualifiedSignature == null || unqualifiedSignature.isValid())
		    && certificateAssociationInfo.isValid()) {
		controlliCatenaTrusted
			.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.POSITIVO.name());
		if (unqualifiedSignature != null && !unqualifiedSignature.isValid(true)) {
		    controlliCatenaTrusted.setDsMsgEsitoContrFirma(
			    VerificheEnums.EsitoControllo.POSITIVO.message() + " - Warning: "
				    + unqualifiedSignature.getWarningsString());
		} else {
		    controlliCatenaTrusted.setDsMsgEsitoContrFirma(
			    VerificheEnums.EsitoControllo.POSITIVO.message());
		}
	    } else {
		esitoVerifiche = false;
		controlliCatenaTrusted
			.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.NEGATIVO.name());
		controlliCatenaTrusted.setDsMsgEsitoContrFirma(
			StringUtils.trim(VerificheEnums.EsitoControllo.NEGATIVO.message() + ": "
				+ unqualifiedSignature.getErrorsString() + " "
				+ certificateAssociationInfo.getErrorsString() + " "
				+ unqualifiedSignature.getWarningsString() + " "));
	    }
	}

	// CONTROLLI FIRMA - CATENA_TRUSTED_ABILITATO
	controlliCatenaTrustedAbilitato
		.setTiEsitoContrFirma(controlliCatenaTrusted.getTiEsitoContrFirma());
	controlliCatenaTrustedAbilitato
		.setDsMsgEsitoContrFirma(controlliCatenaTrusted.getDsMsgEsitoContrFirma());

	// CONTROLLI FIRMA - CATENA_TRUSTED_DISABILITATO
	if (!profiloVerifica.isControlloCatenaTrustAbilitato()) {
	    controlliCatenaTrusted
		    .setTiEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.name());
	    controlliCatenaTrusted
		    .setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.message());
	}

	// CONTROLLI FIRMA - SCADENZA CERTIFICATO
	if (!profiloVerifica.isControlloCertificatoAbilitato()) {
	    controlliCertificato
		    .setTiEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.name());
	    controlliCertificato
		    .setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.message());
	} else if (certificateExpirations != null) {
	    ValidationInfos certificateExpiration = certificateExpirations.get(s);
	    if (certificateExpiration.isValid()) {
		controlliCertificato
			.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.POSITIVO.name());
		controlliCertificato
			.setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.POSITIVO.message());

	    } else {
		esitoVerifiche = false;
		controlliCertificato.setTiEsitoContrFirma(certificateExpiration.getEsito().name());
		controlliCertificato
			.setDsMsgEsitoContrFirma(certificateExpiration.getEsito().message() + ": "
				+ certificateExpiration.getErrorsString());
	    }
	}
	// CONTROLLI FIRMA - CRL
	boolean isCRLWarning = false;
	if (!profiloVerifica.isControlloCrlAbilitato()) {
	    controlliCRL.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.name());
	    controlliCRL
		    .setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.DISABILITATO.message());
	} else if (crlValidation != null) {
	    ValidationInfos crlValidationInfos = crlValidation.get(s);
	    if (crlValidationInfos.isValid()) {
		controlliCRL.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.POSITIVO.name());
		controlliCRL
			.setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.POSITIVO.message());
	    } else {
		if (crlValidationInfos.getEsito()
			.equals(VerificheEnums.EsitoControllo.CERTIFICATO_SCADUTO_3_12_2009)) {
		    isCRLWarning = true;
		} else {
		    esitoVerifiche = false;
		}
		controlliCRL.setTiEsitoContrFirma(crlValidationInfos.getEsito().name());
		controlliCRL.setDsMsgEsitoContrFirma(crlValidationInfos.getEsito().message() + ": "
			+ crlValidationInfos.getErrorsString());
	    }
	}

	X500Name subjectName = X500Name
		.getInstance(signerCert.getSubjectX500Principal().getEncoded());
	if (subjectName.getRDNs(BCStyle.SERIALNUMBER).length > 0
		&& subjectName.getRDNs(BCStyle.GIVENNAME).length > 0
		&& subjectName.getRDNs(BCStyle.SURNAME).length > 0) {
	    String serialnum = subjectName.getRDNs(BCStyle.SERIALNUMBER)[0].getFirst().getValue()
		    .toString();
	    String gn = subjectName.getRDNs(BCStyle.GIVENNAME)[0].getFirst().getValue().toString();
	    String surname = subjectName.getRDNs(BCStyle.SURNAME)[0].getFirst().getValue()
		    .toString();
	    firma.setCdFirmatario(serialnum);
	    firma.setNmCognomeFirmatario(surname);
	    firma.setNmFirmatario(gn);
	}

	firma.setDlDnFirmatario(subjectName.toString());

	firma.setPgFirma(BigDecimal.valueOf(pgFirma.intValue()));

	firma.setTiFirma(VerificheEnums.TipoFirma.DIGITALE.toString());

	// CONTROLLO DI CONFORMITA'
	if (formatValidity.get(s) == null || formatValidity.get(s).isValid()) {
	    firma.setTiEsitoContrConforme(VerificheEnums.EsitoControllo.POSITIVO.name());
	    firma.setDsMsgEsitoContrConforme("Formato riconosciuto e conforme");
	} else {
	    firma.setTiEsitoContrConforme(
		    VerificheEnums.EsitoControllo.NON_AMMESSO_DELIB_45_CNIPA.name());
	    firma.setDsMsgEsitoContrConforme(formatValidity.get(s).getErrorsString());
	}
	firma.setDsFirmaBase64(new String(Base64.getEncoder().encode(s.getSignatureBytes())));

	firma.setDsAlgoFirma(s.getSigAlgorithm());
	firma.setTiFormatoFirma(s.getFormatoFirma().name());
	firma.setFirCertifFirmatario(firCertifFirmatario);
	firma.setDtFirma(s.getDateSignature());
	firma.setTmRifTempUsato(s.getReferenceDate());
	firma.setTiRifTempUsato(s.getReferenceDateType());

	if (esitoVerifiche) {
	    if (isCRLWarning) {
		firma.setTiEsitoVerifFirma(VerificheEnums.EsitoControllo.WARNING.name());
		firma.setDsMsgEsitoVerifFirma(VerificheEnums.EsitoControllo.WARNING.message());
	    } else {
		firma.setTiEsitoVerifFirma(VerificheEnums.EsitoControllo.POSITIVO.name());
		firma.setDsMsgEsitoVerifFirma(VerificheEnums.EsitoControllo.POSITIVO.message());
	    }
	} else {
	    firma.setTiEsitoVerifFirma(VerificheEnums.EsitoControllo.NEGATIVO.name());
	    firma.setDsMsgEsitoVerifFirma(VerificheEnums.EsitoControllo.NEGATIVO.message());
	}

	firma.setPgBusta(BigDecimal.valueOf(pgBusta));

	return firma;

    }

    protected CryptoAroMarcaComp buildMarcaSconosciuta(String idBustaSottoComponente,
	    MutableInt pgMarca) {
	CryptoAroMarcaComp marca = new CryptoAroMarcaComp();
	marca.setAroContrMarcaComps(new ArrayList<>());

	marca.setIdMarca(idBustaSottoComponente);

	for (VerificheEnums.TipoControlliMarca tipo : VerificheEnums.TipoControlliMarca.values()) {
	    CryptoAroContrMarcaComp controllo = new CryptoAroContrMarcaComp();
	    marca.getAroContrMarcaComps().add(controllo);
	    controllo.setTiContr(tipo.name());
	    controllo.setTiEsitoContrMarca(VerificheEnums.EsitoControllo.NON_ESEGUITO.name());
	    controllo.setDsMsgEsitoContrMarca(VerificheEnums.EsitoControllo.NON_ESEGUITO.message());
	}
	marca.setTiEsitoContrConforme(VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO.name());
	marca.setDsMsgEsitoContrConforme("Il formato della marca è sconosciuto");
	marca.setTiEsitoVerifMarca(VerificheEnums.EsitoControllo.WARNING.name());
	marca.setDsMsgEsitoVerifMarca(VerificheEnums.EsitoControllo.NON_ESEGUITO.message());
	marca.setPgBusta(BigDecimal.ONE);
	pgMarca.increment();
	marca.setPgMarca(BigDecimal.valueOf(pgMarca.intValue()));

	return marca;
    }

    protected CryptoAroFirmaComp buildFirmaSconosciuta(String idBustaSottoComponente,
	    MutableInt pgFirma) {
	CryptoAroFirmaComp firma = new CryptoAroFirmaComp();
	firma.setAroContrFirmaComps(new ArrayList<>());
	firma.setAroControfirmaFirmaFiglios(new ArrayList<>());
	firma.setIdFirma(idBustaSottoComponente);

	for (VerificheEnums.TipoControlli tipo : VerificheEnums.TipoControlli.values()) {
	    CryptoAroContrFirmaComp controlloCrittografico = new CryptoAroContrFirmaComp();
	    firma.getAroContrFirmaComps().add(controlloCrittografico);
	    controlloCrittografico.setTiContr(tipo.name());
	    controlloCrittografico
		    .setTiEsitoContrFirma(VerificheEnums.EsitoControllo.NON_ESEGUITO.name());
	    controlloCrittografico
		    .setDsMsgEsitoContrFirma(VerificheEnums.EsitoControllo.NON_ESEGUITO.message());
	}
	firma.setCdFirmatario(VerificheEnums.EsitoControllo.SCONOSCIUTO.name());
	firma.setNmCognomeFirmatario(VerificheEnums.EsitoControllo.SCONOSCIUTO.name());
	firma.setNmFirmatario(VerificheEnums.EsitoControllo.SCONOSCIUTO.name());
	firma.setDlDnFirmatario(VerificheEnums.EsitoControllo.SCONOSCIUTO.name());

	firma.setTiEsitoContrConforme(VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO.name());
	firma.setDsMsgEsitoContrConforme("Il formato della firma è sconosciuto");
	firma.setTiEsitoVerifFirma(VerificheEnums.EsitoControllo.WARNING.name());
	firma.setDsMsgEsitoVerifFirma(VerificheEnums.EsitoControllo.NON_ESEGUITO.message());
	pgFirma.increment();
	firma.setPgFirma(BigDecimal.valueOf(pgFirma.intValue()));
	firma.setPgBusta(BigDecimal.ONE);

	return firma;

    }

    protected CryptoAroFirmaComp buildFirmaNonConforme(int pgBusta, MutableInt pgFirma,
	    Map<String, ValidationInfos> complianceChecks) {

	ValidationInfos valInfo = new ValidationInfos();
	String formato = null;

	// CONTROLLO DI CONFORMITA'
	for (Map.Entry<String, ValidationInfos> f : complianceChecks.entrySet()) {
	    valInfo.addWarning(f.getValue().getWarningsString());
	    formato = f.getKey();
	}
	if (!valInfo.isValid(true) && formato != null) {
	    // FIRMA
	    CryptoAroFirmaComp firma = new CryptoAroFirmaComp();
	    firma.setAroContrFirmaComps(new ArrayList<>());
	    firma.setAroControfirmaFirmaFiglios(new ArrayList<>());

	    // CONTROLLO DI CONFORMITA'
	    for (VerificheEnums.TipoControlli tipo : VerificheEnums.TipoControlli.values()) {
		CryptoAroContrFirmaComp controlloCrittografico = new CryptoAroContrFirmaComp();
		firma.getAroContrFirmaComps().add(controlloCrittografico);
		controlloCrittografico.setTiContr(tipo.name());
		controlloCrittografico
			.setTiEsitoContrFirma(VerificheEnums.EsitoControllo.NON_ESEGUITO.name());
		controlloCrittografico.setDsMsgEsitoContrFirma(
			VerificheEnums.EsitoControllo.NON_ESEGUITO.message());
	    }
	    final String sconosciuto = "Sconosciuto";
	    firma.setCdFirmatario(sconosciuto);
	    firma.setNmCognomeFirmatario(sconosciuto);
	    firma.setNmFirmatario(sconosciuto);
	    firma.setDlDnFirmatario(sconosciuto);
	    firma.setPgFirma(BigDecimal.valueOf(pgFirma.intValue()));

	    firma.setTiEsitoContrConforme(
		    VerificheEnums.EsitoControllo.FORMATO_NON_CONFORME.name());
	    firma.setDsMsgEsitoContrConforme(valInfo.getWarningsString());
	    firma.setTiEsitoVerifFirma(VerificheEnums.EsitoControllo.WARNING.name());
	    firma.setDsMsgEsitoVerifFirma(VerificheEnums.EsitoControllo.NON_ESEGUITO.message());

	    firma.setTiFormatoFirma(SignerUtil.enumSigner2SignerType(formato).name());
	    firma.setPgBusta(BigDecimal.valueOf(pgBusta));
	    return firma;
	} else {
	    return null;
	}

    }

    private CryptoFirCertifCa buildFirCertifCa(X509Certificate caCert,
	    CryptoProfiloVerifica profiloVerifica)
	    throws IOException, CertificateEncodingException {
	CryptoFirIssuer issuer = new CryptoFirIssuer();
	CryptoFirCertifCa certificatoCa = new CryptoFirCertifCa();
	certificatoCa.setFirUrlDistribCrls(new ArrayList<>());
	if (caCert != null) {
	    String issuerDN = caCert.getIssuerX500Principal().getName();
	    String subjectDN = caCert.getSubjectX500Principal().getName();
	    issuer.setDlDnIssuerCertifCa(issuerDN);
	    issuer.setDlDnSubjectCertifCa(subjectDN);
	    certificatoCa.setNiSerialCertifCa(new BigDecimal(caCert.getSerialNumber()));
	    certificatoCa.setDtIniValCertifCa(caCert.getNotBefore());
	    certificatoCa.setDtFinValCertifCa(caCert.getNotAfter());
	    certificatoCa.setDsSubjectKeyId(SignerUtil.getSubjectKeyId(caCert));

	    certificatoCa.setFirIssuer(issuer);
	    try {
		List<String> urls = signerUtil.getURLCrlDistributionPoint(caCert);
		int i = 1;
		for (String url : urls) {
		    CryptoFirUrlDistribCrl firUrl = new CryptoFirUrlDistribCrl();
		    firUrl.setDlUrlDistribCrl(url);
		    firUrl.setNiOrdUrlDistribCrl(BigDecimal.valueOf(i));
		    certificatoCa.getFirUrlDistribCrls().add(firUrl);
		    i++;
		}

	    } catch (CryptoSignerException ex) {
		log.atError().log(
			"Errore nel reperimento del distribution point CRL dal certificato della CA",
			ex);
	    }

	    if (profiloVerifica.isIncludeCertificateAndRevocationValues()) {
		CryptoFirFilePerFirma blobCertCa = new CryptoFirFilePerFirma();
		blobCertCa.setTiFilePerFirma(VerificheEnums.TipoFileEnum.CERTIF_CA.name());
		blobCertCa.setBlFilePerFirma(caCert.getEncoded());
		certificatoCa.setFirFilePerFirma(blobCertCa);
	    }
	} else {
	    Calendar cal = Calendar.getInstance();
	    cal.set(2444, Calendar.DECEMBER, 31);
	    certificatoCa.setNiSerialCertifCa(BigDecimal.ONE);
	    certificatoCa.setDsSubjectKeyId("NON_VALORIZZATO");
	    certificatoCa.setDtIniValCertifCa(cal.getTime());
	    certificatoCa.setDtFinValCertifCa(cal.getTime());
	    certificatoCa.setFirIssuer(issuer);
	}

	return certificatoCa;
    }

    private CryptoFirCertifFirmatario buildFirCertifFirmatario(X509Certificate signerCert,
	    CryptoFirCertifCa certificatoCa, CryptoProfiloVerifica profiloVerifica)
	    throws CertificateEncodingException {
	CryptoFirCertifFirmatario firCertifFirmatario = new CryptoFirCertifFirmatario();
	firCertifFirmatario
		.setNiSerialCertifFirmatario(new BigDecimal(signerCert.getSerialNumber()));
	firCertifFirmatario.setDtIniValCertifFirmatario(signerCert.getNotBefore());
	firCertifFirmatario.setDtFinValCertifFirmatario(signerCert.getNotAfter());
	firCertifFirmatario.setFirCertifCa(certificatoCa);

	if (profiloVerifica.isIncludeCertificateAndRevocationValues()) {
	    CryptoFirFilePerFirma blobCertFirmatario = new CryptoFirFilePerFirma();
	    blobCertFirmatario
		    .setTiFilePerFirma(VerificheEnums.TipoFileEnum.CERTIF_FIRMATARIO.name());
	    blobCertFirmatario.setBlFilePerFirma(signerCert.getEncoded());
	    firCertifFirmatario.setFirFilePerFirma(blobCertFirmatario);
	}
	return firCertifFirmatario;
    }

    private CryptoFirCrl buildFirCrl(X509CRL crl, CryptoFirCertifCa issuerCert,
	    CryptoProfiloVerifica profiloVerifica) throws IOException, CRLException {
	CryptoFirCrl firCrl = null;
	if (crl != null) {
	    firCrl = new CryptoFirCrl();
	    byte[] crlNumByte = crl.getExtensionValue(X509Extension.cRLNumber.getId());
	    BigInteger crlNum = crlNumByte != null
		    ? DERInteger.getInstance(X509ExtensionUtil.fromExtensionValue(crlNumByte))
			    .getValue()
		    : null;
	    firCrl.setDtIniCrl(crl.getThisUpdate());
	    firCrl.setDtScadCrl(crl.getNextUpdate());
	    firCrl.setNiSerialCrl(crlNum != null ? new BigDecimal(crlNum) : null);
	    firCrl.setFirCertifCa(issuerCert);

	    if (profiloVerifica.isIncludeCertificateAndRevocationValues()) {
		CryptoFirFilePerFirma blobCrl = new CryptoFirFilePerFirma();
		blobCrl.setTiFilePerFirma(VerificheEnums.TipoFileEnum.CRL.name());
		blobCrl.setBlFilePerFirma(crl.getEncoded());
		firCrl.setFirFilePerFirma(blobCrl);
	    }

	    String authKeyId = SignerUtil.getAuthorityKeyId(crl);
	    String subjectDN = crl.getIssuerX500Principal().getName();
	    firCrl.setSubjectKeyID(authKeyId);
	    firCrl.setSubjectDN(subjectDN);
	    firCrl.setUniqueId(calcolaUniqueId(subjectDN, authKeyId));
	}
	return firCrl;
    }

    /**
     * Helper function per calcolare l'id univoco della crl partendo da subjectDN e authKeyID.
     *
     * @param subjectDN DN dell'authority che ha emesso il certificato.
     * @param authKeyID ID dell'authority che ha emesso il certificato.
     *
     * @return uniqueID della CRL, ovvero l'MD5 tra subjectDN a authKeyID.
     */
    public static String calcolaUniqueId(String subjectDN, String authKeyID) {
	try {
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    String toHash = subjectDN + authKeyID;
	    md.update(toHash.getBytes(StandardCharsets.UTF_8));
	    // Convert hash bytes to hex format
	    StringBuilder sb = new StringBuilder();
	    for (byte b : md.digest()) {
		sb.append(String.format("%02x", b));
	    }
	    return sb.toString();
	} catch (Exception e) {
	    throw new IllegalArgumentException("Errore durante il calcolo dell'id univoco", e);
	}
    }
}
