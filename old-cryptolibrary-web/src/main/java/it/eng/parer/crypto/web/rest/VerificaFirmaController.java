package it.eng.parer.crypto.web.rest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidate;
import it.eng.parer.crypto.service.VerificaFirmaService;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

/**
 * Endopint relativo alle verifica delle firme.
 *
 * @author Snidero_L
 */
@Tag(name = "v1", description = "Report verifica firma")
@RestController
@Validated
@RequestMapping("/v1")
public class VerificaFirmaController {

    private Logger LOG = LoggerFactory.getLogger(VerificaFirmaController.class);

    @Autowired
    VerificaFirmaService verificaFirmaService;

    @Operation(summary = "Report Verifica", method = "Effettua la verifica dei file passati in input. La risorsa ottenuta da questa chiamata è il report di verifica", tags = {
            "verifica" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Esito verifica documento firmato", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = CryptoAroCompDoc.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "417", description = "File eccede dimensioni consentite", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "500", description = "Documento firmato non riconosciuto", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            "/report-verifica" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CryptoAroCompDoc verificaFirma(@Valid @RequestBody(required = true) CryptoDataToValidate parerVerificaInput,

            HttpServletRequest request) {
        // LOG UUID
        MDC.put("uuid", parerVerificaInput.getUuid());
        CryptoAroCompDoc verificaFirma = verificaFirmaService.verificaFirma(parerVerificaInput);
        String selfLink = request.getRequestURL().toString();
        // HATEOAS de no attri
        verificaFirma.setLink(selfLink);
        return verificaFirma;
    }
}
