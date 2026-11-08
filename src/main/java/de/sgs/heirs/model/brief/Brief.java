package de.sgs.heirs.model.brief;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "brief")
@XmlAccessorType(XmlAccessType.FIELD)
public class Brief {

    @XmlElement(name = "absender")
    private Absender absender;

    @XmlElementWrapper(name = "empfaenger")
    @XmlElement(name = "zeile")
    private List<String> empfaenger = new ArrayList<>();

    @XmlElement(name = "metadaten")
    private Metadaten metadaten;

    @XmlElement(name = "inhalt")
    private Inhalt inhalt;

    @XmlElementWrapper(name = "fusszeile")
    @XmlElement(name = "spalte")
    private List<String> fusszeile = new ArrayList<>();

    public Absender getAbsender() { return absender; }
    public void setAbsender(Absender absender) { this.absender = absender; }
    public List<String> getEmpfaenger() { return empfaenger; }
    public void setEmpfaenger(List<String> empfaenger) { this.empfaenger = empfaenger; }
    public Metadaten getMetadaten() { return metadaten; }
    public void setMetadaten(Metadaten metadaten) { this.metadaten = metadaten; }
    public Inhalt getInhalt() { return inhalt; }
    public void setInhalt(Inhalt inhalt) { this.inhalt = inhalt; }
    public List<String> getFusszeile() { return fusszeile; }
    public void setFusszeile(List<String> fusszeile) { this.fusszeile = fusszeile; }
}
