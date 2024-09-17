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

package it.eng.parer.crypto.jpa.entity.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeverendingDateConverter {

    private static final Logger LOG = LoggerFactory.getLogger(NeverendingDateConverter.class);

    /*
     * MAX DATE supported by Oracle DB
     *
     * https://www.techonthenet.com/oracle/datatypes.php
     */
    public static final LocalDateTime NEVERENDING = LocalDateTime.of(9999, Month.DECEMBER, 31, 23, 59, 59);

    /**
     * Verifica se una certa data (ZoneId di sistema) convertita con lo zoneIdToCheck se oltre la data massima permessa
     * NEVERENDING altrimenti restituisce NEVERENDING
     *
     * @param dateToCheck
     *            data da verificare
     * @param zoneIdToCheck
     *            id Locale
     *
     * @return date
     */
    public static Date verifyOverZoneId(Date dateToCheck, ZoneId zoneIdToCheck) {
        // equals OR after
        Date dateToCheckOverZid = convert(dateToCheck, zoneIdToCheck);
        if (dateToCheckOverZid.equals(asDate(NEVERENDING)) || dateToCheckOverZid.after(asDate(NEVERENDING))) {
            LOG.warn("Data: {} oltre il limite massimo consentito {}", dateToCheck, NEVERENDING);
            return asDate(NEVERENDING);
        }
        return dateToCheck;
    }

    /**
     * Come sopra ma con il default ZoneId
     *
     * @param dateToCheck
     *            da verificare
     *
     * @return date da verificare
     */
    public static Date verifyOverZoneId(Date dateToCheck) {
        return verifyOverZoneId(dateToCheck, ZoneId.systemDefault());
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

    private static LocalDateTime asLocalDateTime(Date date) {
        return asLocalDateTime(date, ZoneId.systemDefault());
    }

    private static LocalDateTime asLocalDateTime(Date date, ZoneId zid) {
        return Instant.ofEpochMilli(date.getTime()).atZone(zid).toLocalDateTime();
    }
}
