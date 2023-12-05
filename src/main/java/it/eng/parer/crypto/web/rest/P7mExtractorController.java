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

import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_FILEXML;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_API_BASE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.service.ExtractorService;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

/**
 * Endpoint relativo all'estrazione dell'xml da un xml.p7m
 *
 * @author Snidero_L
 */
@Tag(name = "P7m", description = "Trasformatore p7m")
@RestController
@RequestMapping(URL_API_BASE)
public class P7mExtractorController {

    private final Logger log = LoggerFactory.getLogger(P7mExtractorController.class);

    @Autowired
    private ExtractorService service;

    @Operation(summary = "P7m XML Extractor", method = "Ottieni il file xml contenuto nel file xml.p7m")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File xml estratto correttamente", content = {
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = ResponseEntity.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "500", description = "Errore generico", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = RESOURCE_FILEXML, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> extractXmlFromP7m(
            @RequestPart(name = "xml-p7m", required = true) MultipartFile xmlP7mFile) {

        Path xmlDaSbustare = null;
        try {
            xmlDaSbustare = Files.createTempFile("da-sbustare", "xml.p7m");
            xmlP7mFile.transferTo(xmlDaSbustare);
            String xmlSbustato = service.extractXmlFromP7m(xmlDaSbustare);

            return new ResponseEntity<>(xmlSbustato, HttpStatus.OK);

        } catch (IOException e) {
            throw new CryptoParerException().withCode(ParerError.ErrorCode.SIGNATURE_FORMAT).withMessage(e.getMessage())
                    .withDetail("Impossibile sbustare il file in input");
        } finally {
            try {
                if (xmlDaSbustare != null && !Files.deleteIfExists(xmlDaSbustare)) {
                    log.warn("Impossibile eliminare il file {}", xmlDaSbustare.getFileName());
                }
            } catch (IOException e) {
                log.error("Errore generale durante l'eliminazione del file", e);
            }

        }

    }

}
