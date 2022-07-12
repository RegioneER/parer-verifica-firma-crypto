package it.eng.parer.crypto.web.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.ParerCRL;
import it.eng.parer.crypto.model.ParerCertificate;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.service.CertificateService;
import java.io.IOException;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint relativo alla gestione delle CRL.
 *
 * @author Snidero_L
 */
@Tag(name = "Certificate", description = "Gestione dei certificati")
@RestController
@RequestMapping("/v1")
public class CertificateController {

    private final Logger log = LoggerFactory.getLogger(CertificateController.class);

    @Autowired
    CertificateService certificateService;

    @Operation(summary = "Certificate", method = "Inserimento Certificato", tags = { "certificate" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Certificato inserito", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCertificate.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "417", description = "File eccede dimensioni consentite", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "500", description = "Certificato in formato errato", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            "/certificate" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCertificate addCertificate(
            @RequestPart(name = "certificato", required = true) MultipartFile certificato) {
        try {
            return certificateService.addCaCertificate(certificato.getBytes());
        } catch (IllegalStateException | IOException ex) {
            throw new CryptoParerException().withCode(ParerError.ErrorCode.SIGNATURE_VERIFICATION_IO)
                    .withMessage("Eccezione di IO durante la creazione di certificato da caricare")
                    .withDetail(ex.getMessage());

        }

    }
}
