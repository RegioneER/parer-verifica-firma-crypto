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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import it.eng.parer.crypto.service.ExtractorService;

/**
 * Test di unità per l'estrattore di P7M
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
@TestInstance(Lifecycle.PER_CLASS)
class ExtractorTest {

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ExtractorService service;

    private Resource xmlP7mBase64File;
    private Resource xmlP7mFile;
    private Resource notXmlP7mFile;

    private String xmlSbustatoExpectedb64;
    private String xmlSbustatoExpected;

    @BeforeAll
    public void loadTestFiles() throws IOException {
        xmlP7mBase64File = resourceLoader.getResource("classpath:firme/p7m_b64.xml.p7m");
        xmlP7mFile = resourceLoader.getResource("classpath:firme/cades_bes.xml.p7m");
        notXmlP7mFile = resourceLoader.getResource("classpath:firme/p7m_pem_sha256.pdf.p7m");

        Resource sbustatob64 = resourceLoader.getResource("classpath:firme/p7m_b64_sbustato.xml");
        byte[] xmlBytesb64 = Files.readAllBytes(sbustatob64.getFile().toPath());
        xmlSbustatoExpectedb64 = new String(xmlBytesb64);

        Resource sbustato = resourceLoader.getResource("classpath:firme/cades_bes_sbustato.xml");
        byte[] xmlBytes = Files.readAllBytes(sbustato.getFile().toPath());
        xmlSbustatoExpected = new String(xmlBytes);
    }

    @Test
    void testXmlExtractionBase64() throws IOException {
        String actualXml = service.extractXmlFromP7m(xmlP7mBase64File.getFile().toPath());
        assertEquals(xmlSbustatoExpectedb64, actualXml);
    }

    @Test
    void testXmlExtraction() throws IOException {
        String actualXml = service.extractXmlFromP7m(xmlP7mFile.getFile().toPath());
        assertEquals(xmlSbustatoExpected, actualXml);
    }

    @Test
    void testWrongFile() throws IOException {
        // String actualXml = service.extractXmlFromP7m(notXmlP7mFile.getFile().toPath());
        // assertEquals(xmlSbustatoExpected, actualXml);
        Path notXmlP7mFileAsPath = notXmlP7mFile.getFile().toPath();
        assertThrows(IOException.class, () -> {
            service.extractXmlFromP7m(notXmlP7mFileAsPath);
        });
    }

}
