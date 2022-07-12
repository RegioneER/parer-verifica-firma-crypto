package it.eng.parer.crypto.web.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;

/**
 * Gestione delle eccezioni.
 *
 * @author Snidero_L
 */
@ControllerAdvice(basePackages = { "it.eng.parer.crypto.web.rest" })
@RequestMapping(produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
public class AdviceHandler {

    @ExceptionHandler(CryptoParerException.class)
    public final ResponseEntity<RestExceptionResponse> handleCryptoParerException(CryptoParerException ex,
            WebRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getCode().exceptionType().equals(ParerError.ExceptionType.NOT_FOUND)) {
            status = HttpStatus.NOT_FOUND;
        }
        CryptoParerException entity = ex
                .withMoreInfo(((ServletWebRequest) request).getRequest().getRequestURL().toString() + "/errors/"
                        + ex.getCode().urlFriendly());

        return new ResponseEntity<>(new RestExceptionResponse(entity), status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public final ResponseEntity<RestExceptionResponse> handleValidationException(MethodArgumentNotValidException ex,
            WebRequest request) {
        CryptoParerException exception = new CryptoParerException();
        exception.setCode(ParerError.ErrorCode.VALIDATION_ERROR);
        exception.setMessage("Errore di validazione");
        // exception.addDetail(ex.getMessage());
        exception.setMoreInfo(((ServletWebRequest) request).getRequest().getRequestURL().toString() + "/errors/"
                + exception.getCode().urlFriendly());
        return new ResponseEntity<>(new RestExceptionResponse(exception), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public final ResponseEntity<RestExceptionResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
            WebRequest request) {
        CryptoParerException exception = new CryptoParerException();
        exception.setCode(ParerError.ErrorCode.VALIDATION_ERROR);
        exception.setMessage("Media type non valido");
        exception.addDetail(ex.getMessage());
        exception.setMoreInfo(((ServletWebRequest) request).getRequest().getRequestURL().toString() + "/errors/"
                + exception.getCode().urlFriendly());

        return new ResponseEntity<>(new RestExceptionResponse(exception), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestExceptionResponse> handleMaxSizeException(MaxUploadSizeExceededException ex,
            WebRequest request) {
        CryptoParerException exception = new CryptoParerException();
        exception.setCode(ParerError.ErrorCode.GENERIC_ERROR);
        exception.setMessage("File eccede la dimensione consentita");
        exception.setMoreInfo(((ServletWebRequest) request).getRequest().getRequestURL().toString() + "/errors/"
                + exception.getCode().urlFriendly());
        return new ResponseEntity<>(new RestExceptionResponse(exception), HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<RestExceptionResponse> handleGenericException(Exception ex, WebRequest request) {
        CryptoParerException exception = new CryptoParerException();
        exception.setCode(ParerError.ErrorCode.GENERIC_ERROR);
        exception.setMessage("Errore generico");
        // exception.addDetail(ex.getMessage());
        exception.setMoreInfo(((ServletWebRequest) request).getRequest().getRequestURL().toString() + "/errors/"
                + exception.getCode().urlFriendly());
        return new ResponseEntity<>(new RestExceptionResponse(exception), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
