package it.eng.parer.crypto.web.bean;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import java.util.List;

/**
 *
 * @author sinatti_s
 */
@JsonIgnoreProperties(value = { "cryptoParerException", "localizedMessage", "suppressed", "cause", "stackTrace" })
public class RestExceptionResponse extends CryptoParerException implements Serializable {

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

    @Override
    public String toString() {
        return super.toString() + " - datetime=" + datetime;
    }

}
