package de.sgs.heirs.tools;

import org.springframework.core.io.ClassPathResource;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class XslResourceLoader {

    /**
     * Lädt eine XSL-Datei sicher aus dem Classpath/FAT-Jar.
     *
     * @param xslPath Pfad relativ zu src/main/resources (z.B. "xslt/meine-datei.xsl")
     * @return Ein javax.xml.transform.Source Objekt für den Transformer
     * @throws IOException wenn die Datei nicht existiert oder nicht lesbar ist
     */
    public Source loadXslFromFatJar(String xslPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(xslPath);

        // Prüfen, ob die Datei überhaupt im JAR vorhanden ist
        if (!resource.exists()) {
            throw new IOException("XSL-Datei nicht im Classpath gefunden: " + xslPath);
        }

        // WICHTIG für FAT-JARs: getInputStream() nutzen, NIEMALS getFile()!
        InputStream inputStream = resource.getInputStream();

        // Optional aber Best Practice: System-ID setzen, falls das XSL
        // relative Imports (<xsl:import href="...">) enthält.
        StreamSource streamSource = new StreamSource(inputStream);
        try {
            streamSource.setSystemId(resource.getURL().toExternalForm());
        } catch (Exception e) {
            // Fallback, falls URL nicht aufgelöst werden kann (selten)
            streamSource.setSystemId(xslPath);
        }

        return streamSource;
    }

    public String loadXslAsText(String xslPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(xslPath);
        // Liest den Stream automatisch und schließt ihn wieder
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }


}
