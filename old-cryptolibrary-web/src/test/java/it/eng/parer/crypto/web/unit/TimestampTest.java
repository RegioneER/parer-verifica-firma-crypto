package it.eng.parer.crypto.web.unit;

import it.eng.parer.crypto.model.ParerTSD;
import it.eng.parer.crypto.model.ParerTST;
import it.eng.parer.crypto.service.TimeService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test di unità per il servizio di erogazione marche temporali.
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
public class TimestampTest {

    @Autowired
    private TimeService timeService;

    @Test
    public void testTst() {
        byte[] content = { 67, 105, 97, 111, 33 };
        ParerTST tst = timeService.getTst(content);
        assertNotNull(tst.getTimeStampInfo().getGenTime());
    }

    @Test
    public void testTsd() {
        byte[] content = { 67, 105, 97, 111, 33 };
        ParerTSD tsd = timeService.getTsd(content);
        ParerTST[] timeStampTokens = tsd.getTimeStampTokens();
        assertEquals(1, timeStampTokens.length);
        assertNotNull(tsd.getTimeStampTokens()[0].getTimeStampInfo().getGenTime());
    }
}
