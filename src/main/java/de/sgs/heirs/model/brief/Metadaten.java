package de.sgs.heirs.model.brief;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Metadaten {
    @XmlElement(name = "ihr-zeichen")
    private String ihrZeichen;

    @XmlElement(name = "ihre-nachricht-vom")
    private String ihreNachrichtVom;

    @XmlElement(name = "unser-zeichen")
    private String unserZeichen;

    @XmlElement(name = "unsere-nachricht-vom")
    private String unsereNachrichtVom;

    @XmlElement(name = "kunden-nr")
    private String kundenNr;

    private String datum;

    // Getter & Setter
    public String getIhrZeichen() { return ihrZeichen; }
    public void setIhrZeichen(String ihrZeichen) { this.ihrZeichen = ihrZeichen; }
    public String getIhreNachrichtVom() { return ihreNachrichtVom; }
    public void setIhreNachrichtVom(String ihreNachrichtVom) { this.ihreNachrichtVom = ihreNachrichtVom; }
    public String getUnserZeichen() { return unserZeichen; }
    public void setUnserZeichen(String unserZeichen) { this.unserZeichen = unserZeichen; }
    public String getUnsereNachrichtVom() { return unsereNachrichtVom; }
    public void setUnsereNachrichtVom(String unsereNachrichtVom) { this.unsereNachrichtVom = unsereNachrichtVom; }
    public String getKundenNr() { return kundenNr; }
    public void setKundenNr(String kundenNr) { this.kundenNr = kundenNr; }
    public String getDatum() { return datum; }
    public void setDatum(String datum) { this.datum = datum; }
}