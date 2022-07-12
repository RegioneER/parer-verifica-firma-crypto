/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.eng.parer.crypto.web.unit;

import it.eng.parer.crypto.service.verifica.OldCryptoInvoker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.cms.CMSTimeStampedData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test di unita sul cryptoInvoker (attualmente utilizzato solo per TST e TSD)
 * 
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
public class OldCryptoInvokerTest {

    @Autowired
    private OldCryptoInvoker oldCryptoInvoker;

    @Test
    public void testInvokerTST() throws Exception {
        byte[] content = { 67, 105, 97, 111, 33 };

        TimeStampToken tst = oldCryptoInvoker.requestTST(content);

        assertNotNull(tst.getTimeStampInfo().getGenTime());
        assertNotNull(tst.getTimeStampInfo().getSerialNumber());
    }

    @Test
    public void testInvokerTSD() throws Exception {
        byte[] content = { 67, 105, 97, 111, 33 };

        CMSTimeStampedData generateTSD = oldCryptoInvoker.generateTSD(content);
        int expectedTs = 1;
        int actualTs = generateTSD.getTimeStampTokens().length;
        assertEquals(expectedTs, actualTs);
    }

}
