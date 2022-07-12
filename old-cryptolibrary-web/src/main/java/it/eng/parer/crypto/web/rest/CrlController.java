package it.eng.parer.crypto.web.rest;

import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.ParerCRL;
import it.eng.parer.crypto.service.CertificateService;
import it.eng.parer.crypto.service.CrlService;

/**
 * Endpoint relativo alla gestione delle CRL.
 *
 * @author Snidero_L
 */
@Tag(name = "Crl", description = "Gestione delle CRL")
@RestController
@RequestMapping("/v1")
public class CrlController {

    private final Logger log = LoggerFactory.getLogger(CrlController.class);

    @Autowired
    CrlService crlService;

    @Autowired
    CertificateService certificateService;

    @Operation(summary = "CRL", method = "Inserimento CRL", tags = { "crl" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CRL inserita correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCRL.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            "/crl" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCRL addCrlByUrls(@RequestBody List<String> urls) {
        return crlService.addCrlByURL(urls);
    }

    @Operation(summary = "Trova CRL", method = "Ottieni la crl utilizzando cerificato del firmatario codificato in base64 url encoded", tags = {
            "crl" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CRL restituita correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCRL.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "CRL non trovata", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @GetMapping(value = { "/crl" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCRL getCrl(@RequestParam("certifFirmatarioBase64UrlEncoded") String certifFirmatarioBase64UrlEncoded,
            HttpServletRequest request, UriComponentsBuilder builder) {
        log.debug("Lunghezza della stringa codificata passata come queryString: "
                + certifFirmatarioBase64UrlEncoded.length());
        byte[] extvalue = Base64.getUrlDecoder().decode(certifFirmatarioBase64UrlEncoded);
        String subjectDN = certificateService.getCertificateSubjectDN(extvalue);
        String authKeyId = certificateService.getCertificateKeyId(extvalue);

        return crlService.getCrl(subjectDN, authKeyId);
    }

    @Operation(summary = "Trova CRL", method = "Ottieni la CRL utilizzando l'id composto da MD5(subjectDN + authKeyId)", tags = {
            "crl" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CRL restituita correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCRL.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "CRL non trovata", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @GetMapping(value = { "/crl/{crlId}" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCRL ottieniCrlPuntuale(@PathVariable("crlId") String crlId) {
        return crlService.getCRL(crlId);
    }

}
