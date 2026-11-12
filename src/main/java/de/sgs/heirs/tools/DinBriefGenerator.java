package de.sgs.heirs.tools;


import org.apache.fop.apps.*;
import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;
import java.nio.file.Paths;

public class DinBriefGenerator {

    /**
     * Führt die XSLT-/FOP-Transformation durch und erzeugt ein DIN 5008 PDF.
     *
     * @param xmlFile  Die Quelldatei (brief.xml)
     * @param xsltFile Das Stylesheet (brief.xsl)
     * @param pdfOut   Die Zieldatei (z. B. output/brief.pdf)
     */
    public void erstelleBriefPdf(String xmlFile, String xsltFile, File pdfOut)
            throws IOException, TransformerException, FOPException {

        URI baseUri = Paths.get(".").toAbsolutePath().toUri();

        // 2. FopFactory instanziieren
        FopFactory fopFactory = FopFactory.newInstance(baseUri);
        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

        // 3. Zielverzeichnis erstellen, falls nicht vorhanden
        if (pdfOut.getParentFile() != null && !pdfOut.getParentFile().exists()) {
            pdfOut.getParentFile().mkdirs();
        }

        // 4. Ressourcen aus dem Classpath/FAT-Jar referenzieren
        ClassPathResource xslRes = new ClassPathResource(xsltFile);
        ClassPathResource xmlRes = new ClassPathResource(xmlFile);

        // 5. Alle Streams öffnen (werden dank try-with-resources automatisch geschlossen)
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(pdfOut));
             InputStream xslIn = xslRes.getInputStream();
             InputStream xmlIn = xmlRes.getInputStream()) {

            // FOP-Instanz für PDF-Generierung konfigurieren
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            // Standard-JAXP XSLT-Transformer initialisieren (mit InputStream aus dem JAR)
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xslIn));

            // Datenquellen verknüpfen (XML aus dem JAR -> XSLT -> SAX Handler von FOP)
            Source src = new StreamSource(xmlIn);
            Result res = new SAXResult(fop.getDefaultHandler());

            // Transformation und PDF-Generierung starten
            transformer.transform(src, res);
        }
    }


}