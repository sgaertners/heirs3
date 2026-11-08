package de.sgs.heirs.tools;


import org.apache.fop.apps.*;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class DinBriefGenerator {

    /**
     * Führt die XSLT-/FOP-Transformation durch und erzeugt ein DIN 5008 PDF.
     *
     * @param xmlFile  Die Quelldatei (brief.xml)
     * @param xsltFile Das Stylesheet (brief.xsl)
     * @param pdfOut   Die Zieldatei (z. B. output/brief.pdf)
     */
    public void erstelleBriefPdf(File xmlFile, File xsltFile, File pdfOut)
            throws IOException, TransformerException {

        // 1. Basis-URI für relative Pfade (z. B. eingebundene Bilder/Logos im XSL)
        URI baseUri = xsltFile.getParentFile() != null
                ? xsltFile.getParentFile().toURI()
                : new File(".").toURI();

        // 2. FopFactory instanziieren
        FopFactory fopFactory = FopFactory.newInstance(baseUri);
        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

        // 3. Zielverzeichnis erstellen, falls nicht vorhanden
        if (pdfOut.getParentFile() != null && !pdfOut.getParentFile().exists()) {
            pdfOut.getParentFile().mkdirs();
        }

        // 4. Output-Stream für die PDF-Datei öffnen
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(pdfOut))) {

            // FOP-Instanz für PDF-Generierung konfigurieren
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            // 5. Standard-JAXP XSLT-Transformer initialisieren
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xsltFile));

            // 6. Datenquellen verknüpfen (XML -> XSLT -> SAX Handler von FOP)
            Source src = new StreamSource(xmlFile);
            Result res = new SAXResult(fop.getDefaultHandler());

            // 7. Transformation und PDF-Generierung starten
            transformer.transform(src, res);
        } catch (FOPException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Main-Methode zum direkten Testen
     */
    public static void main(String[] args) {
        File xmlFile = new File("brief.xml");
        File xsltFile = new File("brief.xsl");
        File pdfFile = new File("brief.pdf");

        DinBriefGenerator generator = new DinBriefGenerator();

        try {
            System.out.println("Starte PDF-Generierung...");
            long start = System.currentTimeMillis();

            generator.erstelleBriefPdf(xmlFile, xsltFile, pdfFile);

            long dauer = System.currentTimeMillis() - start;
            System.out.println("Erfolgreich erzeugt: " + pdfFile.getAbsolutePath());
            System.out.println("Dauer: " + dauer + " ms");

        } catch (Exception e) {
            System.err.println("Fehler bei der PDF-Erstellung!");
            e.printStackTrace();
        }
    }
}