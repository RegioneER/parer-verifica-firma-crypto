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

package it.eng.parer.crypto.web.bean;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;

/**
 *
 * @author sinatti_s
 */
@JsonIgnoreProperties(value = { "cryptoParerException", "localizedMessage", "suppressed", "cause", "stackTrace" })
public class RestExceptionResponse extends CryptoParerException {

    /**
     *
     */
    private static final long serialVersionUID = -3360790536313000487L;

    private final CryptoParerException cryptoParerException;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private final LocalDateTime datetime = LocalDateTime.now();

    public RestExceptionResponse() {
        super();
        this.cryptoParerException = new CryptoParerException();
    }

    public RestExceptionResponse(CryptoParerException cryptoParerException) {
        super();
        this.cryptoParerException = cryptoParerException;
    }

    @Override
    public String getMessage() {
        return getCryptoParerException().getMessage();
    }

    @Override
    public String getMoreInfo() {
        return getCryptoParerException().getMoreInfo();
    }

    @Override
    public ErrorCode getCode() {
        return getCryptoParerException().getCode();
    }

    @Override
    public List<String> getDetails() {
        return getCryptoParerException().getDetails();
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    @Schema(hidden = true, accessMode = AccessMode.READ_ONLY)
    public CryptoParerException getCryptoParerException() {
        return cryptoParerException;
    }

    @Override
    public CryptoParerException withMoreInfo(String moreInfo) {
        return getCryptoParerException().withMoreInfo(moreInfo);
    }

    @Override
    public CryptoParerException withCode(ErrorCode code) {
        return getCryptoParerException().withCode(code);
    }

    @Override
    public CryptoParerException withMessage(String message) {
        return getCryptoParerException().withMessage(message);
    }

    @Override
    public CryptoParerException withDetail(String message) {
        return getCryptoParerException().withDetail(message);
    }

    @Schema(hidden = true, accessMode = AccessMode.READ_ONLY)
    @Override
    public String getLocalizedMessage() {
        return super.getLocalizedMessage();
    }

    @JsonInclude(Include.NON_NULL)
    @Override
    public CryptoDataToValidateMetadata getMetadata() {
        return getCryptoParerException().getMetadata();
    }

    @Override
    public String toString() {
        return super.toString() + " - datetime=" + datetime;
    }

}
