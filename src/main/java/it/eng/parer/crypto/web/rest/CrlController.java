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

package it.eng.parer.crypto.web.rest;

import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_CRL;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_API_BASE;

import java.util.Base64;
import java.util.List;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.ParerCRL;
import it.eng.parer.crypto.service.CertificateService;
import it.eng.parer.crypto.service.CrlService;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @deprecated (Endpoint non più utilizzato da processi esterni)
 *
 *             Endpoint relativo alla gestione delle CRL.
 *
 * @author Snidero_L
 */
@Tag(name = "Crl", description = "Gestione delle CRL")
@RestController
@RequestMapping(URL_API_BASE)
@Deprecated(forRemoval = true, since = "Non più invocato, verificare se migrare su altro microservice")
public class CrlController {

    private final Logger log = LoggerFactory.getLogger(CrlController.class);

    @Autowired
    CrlService crlService;

    @Autowired
    CertificateService certificateService;

    @Operation(summary = "CRL", method = "Inserimento CRL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CRL inserita correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCRL.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = RESOURCE_CRL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCRL addCrlByUrls(@RequestBody List<String> urls) {
        return crlService.addCrlByURL(urls);
    }

    @Operation(summary = "Trova CRL", method = "Ottieni la crl utilizzando cerificato del firmatario codificato in base64 url encoded")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CRL restituita correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCRL.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "CRL non trovata", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @GetMapping(value = RESOURCE_CRL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCRL getCrl(
            @RequestParam("certifFirmatarioBase64UrlEncoded") String certifFirmatarioBase64UrlEncoded,
            HttpServletRequest request, UriComponentsBuilder builder) {

        log.atDebug().log("Lunghezza della stringa codificata passata come queryString: {}",
                certifFirmatarioBase64UrlEncoded.length());

        // SPRING BOOT 4 FIX: In Spring Boot 4 i parametri query sono automaticamente decodificati
        // Per mantenere il comportamento originale (setUrlDecode(false)), dobbiamo ottenere il
        // valore raw
        String rawParam = getRawQueryParam(request, "certifFirmatarioBase64UrlEncoded");

        // Se abbiamo trovato il parametro raw, usiamo quello (non decodificato),
        // altrimenti usiamo quello decodificato automaticamente da Spring
        String paramToDecode = (rawParam != null) ? rawParam : certifFirmatarioBase64UrlEncoded;

        if (rawParam != null) {
            log.atDebug().log("Utilizzo parametro raw non decodificato: {}", rawParam);
        } else {
            log.atDebug().log("Utilizzo parametro decodificato da Spring: {}",
                    certifFirmatarioBase64UrlEncoded);
        }

        byte[] extvalue = Base64.getUrlDecoder().decode(paramToDecode);
        String subjectDN = certificateService.getCertificateSubjectDN(extvalue);
        String authKeyId = certificateService.getCertificateKeyId(extvalue);

        return crlService.getCrl(subjectDN, authKeyId);
    }

    @Operation(summary = "Trova CRL", method = "Ottieni la CRL utilizzando l'id composto da MD5(subjectDN + authKeyId)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CRL restituita correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCRL.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "404", description = "CRL non trovata", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @GetMapping(value = RESOURCE_CRL + "/{crlId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCRL ottieniCrlPuntuale(@PathVariable("crlId") String crlId,
            HttpServletRequest request) {
        // SPRING BOOT 4 FIX: Anche i path variables sono decodificati automaticamente
        // Se il crlId contiene caratteri encoded (es. %2F), vengono decodificati
        // Per mantenere il comportamento originale, otteniamo il valore raw dal path
        String rawPathVar = getRawPathVar(request, "crlId");

        String finalCrlId = (rawPathVar != null) ? rawPathVar : crlId;

        if (rawPathVar != null) {
            log.atDebug().log("Utilizzo path variable raw non decodificato: {}", rawPathVar);
        }

        return crlService.getCRL(finalCrlId);
    }

    /**
     * Estrae il valore raw (non decodificato) di un parametro query dalla query string originale.
     *
     * @param request   HttpServletRequest
     * @param paramName nome del parametro da estrarre
     * @return valore raw del parametro o null se non trovato
     */
    private String getRawQueryParam(HttpServletRequest request, String paramName) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return null;
        }

        // Cerco il parametro nella query string originale
        String pattern = paramName + "=";
        int start = queryString.indexOf(pattern);
        if (start == -1) {
            return null;
        }

        start += pattern.length();
        int end = queryString.indexOf("&", start);
        if (end == -1) {
            end = queryString.length();
        }

        String rawValue = queryString.substring(start, end);

        // NOTA: Il valore raw potrebbe ancora contenere percent-encoding
        // In Spring Boot 3 con setUrlDecode(false) veniva passato così com'è
        // In Spring Boot 4 lo passiamo ancora così com'è per mantenere compatibilità
        return rawValue;
    }

    /**
     * Estrae il valore raw (non decodificato) di un path variable dalla request URI.
     *
     * @param request HttpServletRequest
     * @param varName nome della path variable (non utilizzato direttamente, ma per chiarezza)
     * @return valore raw del path variable o null se non trovato
     */
    private String getRawPathVar(HttpServletRequest request, String varName) {
        String requestUri = request.getRequestURI();
        String pattern = RESOURCE_CRL + "/";
        int start = requestUri.indexOf(pattern);
        if (start == -1) {
            return null;
        }

        start += pattern.length();

        // Il path variable è tutto ciò che segue fino alla fine o fino al prossimo slash
        int end = requestUri.indexOf("/", start);
        if (end == -1) {
            end = requestUri.length();
        }

        String rawValue = requestUri.substring(start, end);

        // In Spring Boot 4, il path variable viene decodificato automaticamente
        // Questo metodo restituisce il valore raw come appare nell'URI originale
        // Ad esempio: se l'URI è /api/crl/id%2F123, restituirà "id%2F123"
        return rawValue;
    }

}