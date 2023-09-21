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

package it.eng.parer.crypto.web.util;

import static it.eng.parer.crypto.web.util.EndPointCostants.URL_ERRORS;
import static it.eng.parer.crypto.service.util.Constants.STD_MSG_GENERIC_ERROR;

import java.util.List;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.model.exceptions.ParerError.ErrorCode;
import it.eng.parer.crypto.web.bean.RestExceptionResponse;

public class RestUtil {

    private RestUtil() {
        throw new IllegalStateException("RestUtil class");
    }

    public static RestExceptionResponse buildParerResponseEntity(CryptoParerException cpex, WebRequest request) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        cpex.withDetail(((ServletWebRequest) request).getRequest().getRequestURL().toString())
                .withMoreInfo(baseUrl + URL_ERRORS + "/" + cpex.getCode().urlFriendly());
        return new RestExceptionResponse(cpex);
    }

    public static RestExceptionResponse buildGenericResponseEntity(WebRequest request) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        RestExceptionResponse errorDetails = new RestExceptionResponse();
        errorDetails.withMessage(STD_MSG_GENERIC_ERROR).withCode(ErrorCode.GENERIC_ERROR)
                .withDetail(((ServletWebRequest) request).getRequest().getRequestURL().toString())
                .withMoreInfo(baseUrl + URL_ERRORS + "/" + ParerError.ErrorCode.GENERIC_ERROR.urlFriendly());
        return errorDetails;
    }

    public static RestExceptionResponse buildValidationException(String message, List<String> details) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        CryptoParerException exception = new CryptoParerException().withCode(ParerError.ErrorCode.VALIDATION_ERROR)
                .withMessage(message).withDetails(details)
                .withMoreInfo(baseUrl + URL_ERRORS + "/" + ParerError.ErrorCode.VALIDATION_ERROR.urlFriendly());
        return new RestExceptionResponse(exception);
    }

}
