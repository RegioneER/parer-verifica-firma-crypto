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

package it.eng.parer.crypto.web.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.eng.parer.crypto.model.ParerTSD;
import it.eng.parer.crypto.model.ParerTST;
import it.eng.parer.crypto.service.TimeService;

/**
 * Test di unità per il servizio di erogazione marche temporali.
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
@Deprecated(forRemoval = true, since = "Da rimuovere API-Unit test obsoleto")
class TimestampTest {

    @Autowired
    private TimeService timeService;

    @Disabled(value = "API da dismettere")
    @Test
    @Deprecated
    void testTst() {
        byte[] content = { 67, 105, 97, 111, 33 };
        ParerTST tst = timeService.getTst(content);
        assertNotNull(tst.getTimeStampInfo().getGenTime());
    }

    @Disabled(value = "API da dismettere")
    @Test
    @Deprecated
    void testTsd() {
        byte[] content = { 67, 105, 97, 111, 33 };
        ParerTSD tsd = timeService.getTsd(content);
        ParerTST[] timeStampTokens = tsd.getTimeStampTokens();
        assertEquals(1, timeStampTokens.length);
        assertNotNull(tsd.getTimeStampTokens()[0].getTimeStampInfo().getGenTime());
    }
}
