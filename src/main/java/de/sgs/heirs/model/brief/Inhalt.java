package de.sgs.heirs.model.brief;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Inhalt {
    private String betreff;
    private String anrede;

    // Hier wird die gewünschte String-Liste für Absätze realisiert:
    @XmlElement(name = "absatz")
    private List<String> absaetze = new ArrayList<>();

    private String grussformel;
    private Unterschrift unterschrift;

    @XmlElementWrapper(name = "anlagen")
    @XmlElement(name = "anlage")
    private List<String> anlagen = new ArrayList<>();

    // Getter & Setter
    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }
    public String getAnrede() { return anrede; }
    public void setAnrede(String anrede) { this.anrede = anrede; }
    public List<String> getAbsaetze() { return absaetze; }
    public void setAbsaetze(List<String> absaetze) { this.absaetze = absaetze; }
    public String getGrussformel() { return grussformel; }
    public void setGrussformel(String grussformel) { this.grussformel = grussformel; }
    public Unterschrift getUnterschrift() { return unterschrift; }
    public void setUnterschrift(Unterschrift unterschrift) { this.unterschrift = unterschrift; }
    public List<String> getAnlagen() { return anlagen; }
    public void setAnlagen(List<String> anlagen) { this.anlagen = anlagen; }
}
