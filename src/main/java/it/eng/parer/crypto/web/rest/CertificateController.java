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

import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_CERTIFICATE;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_API_BASE;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.ParerCertificate;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.service.CertificateService;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

/**
 * Endpoint relativo alla gestione delle CRL.
 *
 * @author Snidero_L
 */
@Tag(name = "Certificate", description = "Gestione dei certificati")
@RestController
@RequestMapping(URL_API_BASE)
public class CertificateController {

    @Autowired
    CertificateService certificateService;

    @Operation(summary = "Certificate", method = "Inserimento Certificato")
    @ApiResponses(value = {
	    @ApiResponse(responseCode = "200", description = "Certificato inserito", content = {
		    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerCertificate.class)) }),
	    @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
		    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
	    @ApiResponse(responseCode = "417", description = "File eccede dimensioni consentite", content = {
		    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
	    @ApiResponse(responseCode = "500", description = "Certificato in formato errato", content = {
		    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = RESOURCE_CERTIFICATE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerCertificate addCertificate(
	    @RequestPart(name = "certificato", required = true) MultipartFile certificato) {
	try {
	    return certificateService.addCaCertificate(certificato.getBytes());
	} catch (IllegalStateException | IOException ex) {
	    throw new CryptoParerException(ex)
		    .withCode(ParerError.ErrorCode.SIGNATURE_VERIFICATION_IO)
		    .withMessage("Eccezione di IO durante la creazione di certificato da caricare");
	}

    }
}
