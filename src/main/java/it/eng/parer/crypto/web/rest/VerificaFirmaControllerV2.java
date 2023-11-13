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

package it.eng.parer.crypto.web.rest;

import static it.eng.parer.crypto.web.util.EndPointCostants.ETAG_RV10;
import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_REPORT_VERIFICA;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_API_BASE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadataFile;
import it.eng.parer.crypto.service.VerificaFirmaService;
import it.eng.parer.crypto.service.model.CryptoDataToValidateData;
import it.eng.parer.crypto.service.model.CryptoDataToValidateFile;
import it.eng.parer.crypto.service.util.Constants;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Endopint relativo alle verifica delle firme.
 *
 * @author Snidero_L
 */
@Tag(name = "Verifica", description = "Report verifica firma")
@RestController
@Validated
@RequestMapping(URL_API_BASE)
public class VerificaFirmaControllerV2 {

    private final Logger log = LoggerFactory.getLogger(VerificaFirmaControllerV2.class);

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    @Autowired
    VerificaFirmaService verificaFirmaService;

    /**
     * Metodo per effettuare la verifica delle firme.
     *
     * <em>Nota sui metadati</em> Per collegare il file caricato con i relativi metadati (opzionali), viene fatta
     * l'assunzione che il caricamento dei file avvenga in ordine. La specifica HTTP consente ciò:
     *
     * A "multipart/form-data" message contains a series of parts, each representing a successful control. The parts are
     * sent to the processing agent in the same order the corresponding controls appear in the document stream. Part
     * boundaries should not occur in any of the data; how this is done lies outside the scope of this specification.
     *
     * Fonte: https://www.w3.org/TR/html4/interact/forms.html#h-17.13.4
     *
     *
     * @param metadati
     *            informazioni opzionali sui file
     * @param contenuto
     *            file da verificare, unico elemento obbligatorio
     * @param firme
     *            lista di file contenenti le firme detached
     * @param marche
     *            lista di file contenenti le marche detached
     * @param request
     *            uso interno (per impostare il selfLink)
     *
     * @return report della verifica crypto
     */
    @Operation(summary = "Report Verifica", method = "Effettua la verifica dei file passati in input. La risorsa ottenuta da questa chiamata è il report di verifica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Esito verifica documento firmato", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = CryptoAroCompDoc.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "417", description = "File eccede dimensioni consentite", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "500", description = "Documento firmato non riconosciuto", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = RESOURCE_REPORT_VERIFICA, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CryptoAroCompDoc> verificaFirma(
            @Valid @RequestPart(name = "metadati", required = false) @Parameter(schema = @Schema(type = "string", format = "binary")) CryptoDataToValidateMetadata metadati,
            @RequestPart(name = "contenuto", required = true) MultipartFile contenuto,
            @RequestPart(name = "firme", required = false) List<MultipartFile> firme,
            @RequestPart(name = "marche", required = false) List<MultipartFile> marche, HttpServletRequest request) {

        // init list
        if (firme == null) {
            firme = Collections.emptyList();
        }
        if (marche == null) {
            marche = Collections.emptyList();
        }

        if (metadati == null) {
            metadati = new CryptoDataToValidateMetadata();
            metadati.setComponentePrincipale(new CryptoDataToValidateMetadataFile("contenuto"));
            List<CryptoDataToValidateMetadataFile> sottoComponentiFirma = new ArrayList<>(firme.size());
            for (int i = 0; i < firme.size(); i++) {
                sottoComponentiFirma.add(new CryptoDataToValidateMetadataFile("firma_" + i));
            }
            List<CryptoDataToValidateMetadataFile> sottoComponentiMarca = new ArrayList<>(marche.size());
            for (int i = 0; i < marche.size(); i++) {
                sottoComponentiMarca.add(new CryptoDataToValidateMetadataFile("marca_" + i));
            }
            metadati.setSottoComponentiFirma(sottoComponentiFirma);
            metadati.setSottoComponentiMarca(sottoComponentiMarca);
        }

        // Prima di invocare il service mi assicuro che tutto sia coerente
        validazioneCoerenzaInput(metadati, firme, marche);

        // Controllo che i metadati siano coerenti con i dati
        // log UUID
        MDC.put(Constants.UUID_LOG_MDC, metadati.getUuid());
        CryptoDataToValidateFile signedFile = new CryptoDataToValidateFile();
        List<CryptoDataToValidateFile> detachedSignature = new ArrayList<>(firme.size());
        List<CryptoDataToValidateFile> detachedTimeStamp = new ArrayList<>(marche.size());

        try {
            final String suffix = ".crypto";
            Path principale = Files.createTempFile("contenuto-", suffix, attr);
            contenuto.transferTo(principale);
            signedFile.setNome(metadati.getComponentePrincipale().getId());
            signedFile.setContenuto(principale.toFile());

            for (int i = 0; i < firme.size(); i++) {
                MultipartFile firma = firme.get(i);
                Path sig = Files.createTempFile("firma-", suffix, attr);
                firma.transferTo(sig);
                CryptoDataToValidateMetadataFile metadatiSottoComponenteFirma = metadati.getSottoComponentiFirma()
                        .get(i);

                detachedSignature.add(new CryptoDataToValidateFile(metadatiSottoComponenteFirma.getId(), sig.toFile()));
            }

            for (int i = 0; i < marche.size(); i++) {
                MultipartFile marca = marche.get(i);
                Path ts = Files.createTempFile("timestamp-", suffix, attr);
                marca.transferTo(ts);
                CryptoDataToValidateMetadataFile metadatiSottoComponenteMarca = metadati.getSottoComponentiMarca()
                        .get(i);

                detachedTimeStamp.add(new CryptoDataToValidateFile(metadatiSottoComponenteMarca.getId(), ts.toFile()));
            }

            CryptoDataToValidateData dati = new CryptoDataToValidateData();
            dati.setContenuto(signedFile);
            dati.setSottoComponentiFirma(detachedSignature);
            dati.setSottoComponentiMarca(detachedTimeStamp);

            CryptoAroCompDoc verificaFirma = verificaFirmaService.verificaFirma(dati, metadati);
            String selfLink = request.getRequestURL().toString();
            // HATEOAS de no attri
            verificaFirma.setLink(selfLink);
            return ResponseEntity.ok().lastModified(verificaFirma.getFineValidazione().toInstant()).eTag(ETAG_RV10)
                    .body(verificaFirma);
        } catch (IllegalStateException | IOException ex) {
            throw new CryptoParerException(metadati, ex).withCode(ParerError.ErrorCode.SIGNATURE_VERIFICATION_IO)
                    .withMessage("Eccezione di IO durante la creazione di un file da verificare");
        } finally {
            final String CANT_DELETE = "Impossibile eliminare {}";
            try {
                if (signedFile.getContenuto() != null) {
                    Files.deleteIfExists(signedFile.getContenuto().toPath());
                }
            } catch (IOException e) {
                log.atWarn().log(CANT_DELETE, signedFile.getContenuto().getName());
            }
            detachedSignature.forEach(s -> {
                try {
                    Files.deleteIfExists(s.getContenuto().toPath());
                } catch (IOException e) {
                    log.atWarn().log(CANT_DELETE, s.getContenuto().getName());
                }
            });
            detachedTimeStamp.forEach(s -> {
                try {
                    Files.deleteIfExists(s.getContenuto().toPath());
                } catch (IOException e) {
                    log.atWarn().log(CANT_DELETE, s.getContenuto().getName());
                }
            });

        }
    }

    /**
     * Effettua la validazione di coerenza tra i metadati ed i dati passati in input.
     *
     * @param metadati
     *            metadati di configurazione passati in input
     * @param firme
     *            lista di file delle firme detached
     * @param marche
     *            lista di file delle marche detached
     */
    private void validazioneCoerenzaInput(CryptoDataToValidateMetadata metadati, List<MultipartFile> firme,
            List<MultipartFile> marche) {

        assert metadati != null;

        if (StringUtils.isBlank(metadati.getComponentePrincipale().getId())) {
            throw new CryptoParerException(metadati).withCode(ParerError.ErrorCode.SIGNATURE_WRONG_PARAMETER)
                    .withMessage("Identificativo componente principale mancante")
                    .withDetail("Necessario impostare l'identificativo del componentene principale");

        }
        int numeroFirmeMetadata = metadati.getSottoComponentiFirma() != null ? metadati.getSottoComponentiFirma().size()
                : 0;
        int numeroFirmeCaricate = firme.size();

        if (numeroFirmeMetadata != numeroFirmeCaricate) {
            throw new CryptoParerException(metadati).withCode(ParerError.ErrorCode.SIGNATURE_WRONG_PARAMETER)
                    .withMessage("Numero sotto componenti firma errato").withDetail(
                            "Numero di sotto componenti di tipo firma indicato nei metadati non corrisponde al numero di sotto componenti di tipo firma effettivamente caricati");
        }

        int numeroMarcheMetadata = metadati.getSottoComponentiMarca() != null
                ? metadati.getSottoComponentiMarca().size() : 0;
        int numeroMarcheCaricate = marche.size();

        if (numeroMarcheMetadata != numeroMarcheCaricate) {
            throw new CryptoParerException(metadati).withCode(ParerError.ErrorCode.SIGNATURE_WRONG_PARAMETER)
                    .withMessage("Numero sotto componenti marca errato").withDetail(
                            "Numero di sotto componenti di tipo marca indicato nei metadati non corrisponde al numero di sotto componenti di tipo marca effettivamente caricati");
        }

    }

}
