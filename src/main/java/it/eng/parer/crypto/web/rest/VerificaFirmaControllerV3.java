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

import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_REPORT_VERIFICA;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_API_BASE;
import static it.eng.parer.crypto.web.util.EndPointCostants.ETAG_RV10;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateBody;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateDataUri;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadataFile;
import it.eng.parer.crypto.service.VerificaFirmaService;
import it.eng.parer.crypto.service.model.CryptoDataToValidateData;
import it.eng.parer.crypto.service.model.CryptoDataToValidateFile;
import it.eng.parer.crypto.service.util.Constants;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

/**
 * Endopint relativo alle verifica delle firme.
 *
 * @author Snidero_L
 */
@Tag(name = "Verifica", description = "Report verifica firma")
@RestController
@Validated
@RequestMapping(URL_API_BASE)
public class VerificaFirmaControllerV3 {

    private final Logger LOG = LoggerFactory.getLogger(VerificaFirmaControllerV3.class);

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private final WebClient webClient = WebClient.create();

    @Autowired
    VerificaFirmaService verificaFirmaService;

    // default 60 s
    @Value("${parer.crypto.webclient.timeout:360}")
    long webClientTimeout;

    // default 5 times
    @Value("${parer.crypto.webclient.backoff:10}")
    long webClientBackoff;

    // default 3 s
    @Value("${parer.crypto.webclient.backofftime:3}")
    long webClientBackoffTime;

    /**
     * Metodo per effettuare la verifica delle firme. In questo caso i file da verificare sono passati sotto-forma di
     * URI.
     *
     * <em>Nota sui metadati</em> Per collegare il file caricato con i relativi metadati (opzionali), viene fatta
     * l'assunzione che il caricamento dei file avvenga in ordine. La specifica HTTP consente ciò:
     *
     *
     * @param body
     *            corpo della richiesta di verifica della firma. Contiene dati e metadati.
     *
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
    @PostMapping(value = RESOURCE_REPORT_VERIFICA, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CryptoAroCompDoc> verificaFirma(
            @Valid @RequestBody(required = true) CryptoDataToValidateBody body, HttpServletRequest request) {

        CryptoDataToValidateDataUri dati = body.getData();
        CryptoDataToValidateMetadata metadati = body.getMetadata();

        URI contenuto = dati.getContenuto();
        List<URI> firme = dati.getFirme();
        List<URI> marche = dati.getMarche();

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
        // LOG UUID
        MDC.put(Constants.UUID_LOG_MDC, metadati.getUuid());
        CryptoDataToValidateFile signedFile = new CryptoDataToValidateFile();
        List<CryptoDataToValidateFile> detachedSignature = new ArrayList<>(firme.size());
        List<CryptoDataToValidateFile> detachedTimeStamp = new ArrayList<>(marche.size());

        try {
            final String suffix = ".crypto";

            Path principale = Files.createTempFile("contenuto-", suffix, attr);
            downloadSignedResource(contenuto, principale);

            signedFile.setNome(metadati.getComponentePrincipale().getId());
            signedFile.setContenuto(principale.toFile());

            for (int i = 0; i < firme.size(); i++) {
                URI firma = firme.get(i);
                Path sig = Files.createTempFile("firma-", suffix, attr);
                downloadSignedResource(firma, sig);

                CryptoDataToValidateMetadataFile metadatiSottoComponenteFirma = metadati.getSottoComponentiFirma()
                        .get(i);

                detachedSignature.add(new CryptoDataToValidateFile(metadatiSottoComponenteFirma.getId(), sig.toFile()));
            }

            for (int i = 0; i < marche.size(); i++) {
                URI marca = marche.get(i);
                Path ts = Files.createTempFile("timestamp-", suffix, attr);
                downloadSignedResource(marca, ts);

                CryptoDataToValidateMetadataFile metadatiSottoComponenteMarca = metadati.getSottoComponentiMarca()
                        .get(i);

                detachedTimeStamp.add(new CryptoDataToValidateFile(metadatiSottoComponenteMarca.getId(), ts.toFile()));
            }

            CryptoDataToValidateData datiVerifica = new CryptoDataToValidateData();
            datiVerifica.setContenuto(signedFile);
            datiVerifica.setSottoComponentiFirma(detachedSignature);
            datiVerifica.setSottoComponentiMarca(detachedTimeStamp);

            CryptoAroCompDoc verificaFirma = verificaFirmaService.verificaFirma(datiVerifica, metadati);
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
                LOG.warn(CANT_DELETE, signedFile.getContenuto().getName());
            }
            detachedSignature.forEach(s -> {
                try {
                    Files.deleteIfExists(s.getContenuto().toPath());
                } catch (IOException e) {
                    LOG.warn(CANT_DELETE, s.getContenuto().getName());
                }
            });
            detachedTimeStamp.forEach(s -> {
                try {
                    Files.deleteIfExists(s.getContenuto().toPath());
                } catch (IOException e) {
                    LOG.warn(CANT_DELETE, s.getContenuto().getName());
                }
            });

        }
    }

    private void downloadSignedResource(URI signedResource, Path localPath) {
        // Attenzione, se al posto dell'uri viene utilizzata una stringa ci possono
        // essere problemi di conversione dei
        // caratteri
        Flux<DataBuffer> dataBuffer = webClient.get().uri(signedResource).retrieve().bodyToFlux(DataBuffer.class);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sto per scaricare il pre-signed content da {}", signedResource.toASCIIString());
        }
        // scarica sul local path provando 5 volte aspettando almeno 3 secondi tra un
        // prova e l'altra
        DataBufferUtils.write(dataBuffer, localPath).timeout(Duration.ofSeconds(webClientTimeout))
                .retryWhen(Retry.backoff(webClientBackoff, Duration.ofSeconds(webClientBackoffTime))).share().block();
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
    private void validazioneCoerenzaInput(CryptoDataToValidateMetadata metadati, List<URI> firme, List<URI> marche) {

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
