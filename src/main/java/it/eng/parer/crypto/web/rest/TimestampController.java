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

import static it.eng.parer.crypto.web.util.EndPointCostants.URL_API_BASE;
import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_TST;
import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_TSD;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.crypto.model.ParerTSD;
import it.eng.parer.crypto.model.ParerTST;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.service.TimeService;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

/**
 * @deprecated (Endpoint non più utilizzato da processi esterni)
 *
 *             Endpoint relativo alla gestione dei timestamp (TSR e TSD).
 *
 * @author Snidero_L
 */
@Tag(name = "Timestamp", description = "Gestione TSR/TSD")
@RestController
@RequestMapping(URL_API_BASE)
@Deprecated(forRemoval = true, since = "Non più invocato, verificare se migrare su altro microservice")
public class TimestampController {

    private final Logger log = LoggerFactory.getLogger(TimestampController.class);

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    @Autowired
    TimeService timeService;

    @Operation(summary = "Timestamp", method = "Ottieni un timestamp per lo stream di dati passato in input")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timestamp inserito correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerTST.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = RESOURCE_TST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerTST requestTst(@RequestParam(name = "description", required = true) String description,
            @RequestParam(name = "file", required = true) MultipartFile file) {
        log.atInfo().log("Applico timestamp al file con descrizione {}", description);
        byte[] content = null;
        try {
            Path temp = Files.createTempFile("tst-", ".crypto", attr);
            file.transferTo(temp);
            content = Files.readAllBytes(temp);
            Files.delete(temp);

        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_IO);
        }
        return timeService.getTst(content);
    }

    @Operation(summary = "Timestamp", method = "Ottieni l'oggetto passato in input stream marcato")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TSD inserito correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerTSD.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = RESOURCE_TSD, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerTSD generateTsd(@RequestParam(name = "description", required = true) String description,
            @RequestParam(name = "file", required = true) MultipartFile file) {
        log.atInfo().log("Crea il TSD al file con descrizione {}", description);
        byte[] content = null;
        try {
            Path temp = Files.createTempFile("tsd-", ".crypto", attr);
            file.transferTo(temp);
            content = Files.readAllBytes(temp);
            Files.delete(temp);
        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_IO);
        }
        return timeService.getTsd(content);
    }
}
