package de.sgs.heirs.tools;

import de.sgs.heirs.model.brief.Brief;
import de.sgs.heirs.model.brief.Inhalt;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.File;
import java.util.Arrays;

public class XmlMapperUtil {

    // Java-Objekt nach XML schreiben (Serialisierung)
    public static void briefToXmlFile(Brief brief, File datei) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Brief.class);
        Marshaller marshaller = context.createMarshaller();

        // Schön eingerücktes XML erzeugen
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        marshaller.marshal(brief, datei);
    }

    // Beispiel für den Aufruf
    public static void main(String[] args) throws Exception {
        Brief brief = new Brief();

        // Inhalt mit mehreren Absätzen über die String-Liste befüllen

        Inhalt inhalt = new Inhalt();
        inhalt.setBetreff("Neues Projekt");
        inhalt.setAnrede("Hallo Herr Müller,");

        // Liste von Absätzen zuweisen:
        inhalt.setAbsaetze(Arrays.asList(
                "Dies ist der erste Absatz aus der Java Stringliste.",
                "Hier folgt der zweite Absatz mit weiteren Details.",
                "Und der finale abschließende Absatz."
        ));

        brief.setInhalt(inhalt);

        // Nach XML serialisieren
        briefToXmlFile(brief, new File("generierter-brief.xml"));
        System.out.println("XML erfolgreich generiert!");
    }
}


//JAXBContext context = JAXBContext.newInstance(Brief.class);
//Source src = new jakarta.xml.bind.util.JAXBSource(context, brief);
//transformer.transform(src, res); // transformer aus dem vorherigen Schritt