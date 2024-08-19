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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.eng.parer.crypto.web.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.cms.CMSTimeStampedData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import it.eng.parer.crypto.service.verifica.OldCryptoInvoker;

/**
 * Test di unita sul cryptoInvoker (attualmente utilizzato solo per TST e TSD)
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
class OldCryptoInvokerTest {

    @Autowired
    private OldCryptoInvoker oldCryptoInvoker;

    @Test
    void testInvokerTST() throws Exception {
        byte[] content = { 67, 105, 97, 111, 33 };

        TimeStampToken tst = oldCryptoInvoker.requestTST(content);

        assertNotNull(tst.getTimeStampInfo().getGenTime());
        assertNotNull(tst.getTimeStampInfo().getSerialNumber());
    }

    @Test
    void testInvokerTSD() throws Exception {
        byte[] content = { 67, 105, 97, 111, 33 };

        CMSTimeStampedData generateTSD = oldCryptoInvoker.generateTSD(content);
        int expectedTs = 1;
        int actualTs = generateTSD.getTimeStampTokens().length;
        assertEquals(expectedTs, actualTs);
    }

}
