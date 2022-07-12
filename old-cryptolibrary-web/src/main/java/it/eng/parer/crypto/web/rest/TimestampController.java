package it.eng.parer.crypto.web.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * Endpoint relativo alla gestione dei timestamp (TSR e TSD).
 *
 * @author Snidero_L
 */
@Tag(name = "Timestamp", description = "Gestione TSR/TSD")
@RestController
@RequestMapping("/v1")
public class TimestampController {

    private final Logger log = LoggerFactory.getLogger(TimestampController.class);

    @Autowired
    TimeService timeService;

    @Operation(summary = "Timestamp", method = "Ottieni un timestamp per lo stream di dati passato in input", tags = {
            "timestamp" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timestamp inserito correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerTST.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            "/tst" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerTST requestTst(@RequestParam(name = "description", required = true) String description,
            @RequestParam(name = "file", required = true) MultipartFile file) {
        log.info("Applico timestamp al file con descrizione " + description);
        byte[] content = null;
        try {
            Path temp = Files.createTempFile("tst-", ".crypto");
            file.transferTo(temp);
            content = Files.readAllBytes(temp);
            Files.delete(temp);

        } catch (IOException ex) {
            throw new CryptoParerException().withCode(ParerError.ErrorCode.TSP_IO).withMessage(ex.getMessage());
        }
        return timeService.getTst(content);
    }

    @Operation(summary = "Timestamp", method = "Ottieni l'oggetto passato in input stream marcato", tags = {
            "timestamp" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TSD inserito correttamente", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ParerTSD.class)) }),
            @ApiResponse(responseCode = "400", description = "Richiesta non valida", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            "/tsd" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerTSD generateTsd(@RequestParam(name = "description", required = true) String description,
            @RequestParam(name = "file", required = true) MultipartFile file) {
        log.info("Crea il TSD al file con descrizione " + description);
        byte[] content = null;
        try {
            Path temp = Files.createTempFile("tsd-", ".crypto");
            file.transferTo(temp);
            content = Files.readAllBytes(temp);
            Files.delete(temp);
        } catch (IOException ex) {
            throw new CryptoParerException().withCode(ParerError.ErrorCode.TSP_IO).withMessage(ex.getMessage());
        }
        return timeService.getTsd(content);
    }
}
