package de.sgs.heirs.controller;

import de.sgs.heirs.tools.DinBriefGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
public class BriefController {


    @RequestMapping("/brief")
    public void testLetter() throws IOException {
        File pdfFile = new File("brief.pdf");

        DinBriefGenerator generator = new DinBriefGenerator();

        try {
            System.out.println("Starte PDF-Generierung...");
            long start = System.currentTimeMillis();

            generator.erstelleBriefPdf("brief.xml", "brief.xsl", pdfFile);

            long dauer = System.currentTimeMillis() - start;
            System.out.println("Erfolgreich erzeugt: " + pdfFile.getAbsolutePath());
            System.out.println("Dauer: " + dauer + " ms");

        } catch (Exception e) {
            System.err.println("Fehler bei der PDF-Erstellung!");
            e.printStackTrace();
        }

    }
}
