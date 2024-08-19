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
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_DEPRECATE_BASE;

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
import it.eng.parer.crypto.service.util.Constants;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Endopint relativo alle verifica delle firme.
 *
 * @deprecated (Deprecato a partire dall'introduzione di {@link VerificaFirmaControllerV3})
 *
 * @author Snidero_L
 */
@Tag(name = "Verifica", description = "Report verifica firma")
@RestController
@Validated
@RequestMapping(URL_DEPRECATE_BASE)
@Deprecated(forRemoval = true)
public class VerificaFirmaController {

    @Autowired
    VerificaFirmaService verificaFirmaService;

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
    public CryptoAroCompDoc verificaFirma(@Valid @RequestBody(required = true) CryptoDataToValidate parerVerificaInput,
            HttpServletRequest request) {
        // LOG UUID
        MDC.put(Constants.UUID_LOG_MDC, parerVerificaInput.getUuid());
        CryptoAroCompDoc verificaFirma = verificaFirmaService.verificaFirma(parerVerificaInput);
        String selfLink = request.getRequestURL().toString();
        // HATEOAS de no attri
        verificaFirma.setLink(selfLink);
        return verificaFirma;
    }
}
