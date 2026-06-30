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

package it.eng.parer.crypto.service.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateConverter {

    private static final Logger LOG = LoggerFactory.getLogger(DateConverter.class);

    /*
     * MAX DATE supported by Oracle DB
     *
     * https://www.techonthenet.com/oracle/datatypes.php
     */
    public static final LocalDateTime NEVERENDING = LocalDateTime.of(9999, Month.DECEMBER, 31, 23,
            59, 59);

    /**
     * Come sopra ma con il default ZoneId
     *
     * @param dateToCheck da verificare
     *
     * @return date da verificare
     */
    public static LocalDateTime verifyOverZoneId(LocalDateTime dateToCheck) {
        return verifyOverZoneId(dateToCheck, ZoneId.systemDefault());
    }

    /**
     * Verifica se una certa LocalDateTime (ZoneId di sistema) convertita con lo zoneIdToCheck se
     * oltre la data massima permessa NEVERENDING altrimenti restituisce NEVERENDING
     *
     * @param dateToCheck   data da verificare
     * @param zoneIdToCheck id Locale
     *
     * @return LocalDateTime
     */
    public static LocalDateTime verifyOverZoneId(LocalDateTime dateToCheck, ZoneId zoneIdToCheck) {
        if (dateToCheck == null) {
            return null;
        }
        // Convert to Date for checking
        Date dateAsDate = asDate(dateToCheck);
        Date dateToCheckOverZid = convert(dateAsDate, zoneIdToCheck);
        if (dateToCheckOverZid.equals(asDate(NEVERENDING))
                || dateToCheckOverZid.after(asDate(NEVERENDING))) {
            LOG.warn("Data: {} oltre il limite massimo consentito {}", dateToCheck, NEVERENDING);
            return NEVERENDING;
        }
        return dateToCheck;
    }

    private static Date convert(Date dateToCheck, ZoneId zid) {
        return asDate(asLocalDateTime(dateToCheck, zid));
    }

    private static Date asDate(LocalDateTime localDateTime) {
        return asDate(localDateTime, ZoneId.systemDefault());
    }

    private static Date asDate(LocalDateTime localDateTime, ZoneId zid) {
        return Date.from(localDateTime.atZone(zid).toInstant());
    }

    private static LocalDate asLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Converts a Date to LocalDateTime using the system default ZoneId
     *
     * @param date the date to convert
     * @return LocalDateTime or null if date is null
     */
    public static LocalDateTime asLocalDateTime(Date date) {
        return date == null ? null : asLocalDateTime(date, ZoneId.systemDefault());
    }

    /**
     * Converts a Date to LocalDateTime using the specified ZoneId
     *
     * @param date the date to convert
     * @param zid  the zone ID to use for conversion
     * @return LocalDateTime or null if date is null
     */
    public static LocalDateTime asLocalDateTime(Date date, ZoneId zid) {
        return date == null ? null
                : Instant.ofEpochMilli(date.getTime()).atZone(zid).toLocalDateTime();
    }
}