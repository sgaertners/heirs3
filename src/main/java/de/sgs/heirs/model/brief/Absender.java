package de.sgs.heirs.model.brief;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Absender {
    private String firma;
    private String ruecksendezeile;
    private String ansprechpartner;
    private String telefon;
    private String email;

    // Getter & Setter
    public String getFirma() { return firma; }
    public void setFirma(String firma) { this.firma = firma; }
    public String getRuecksendezeile() { return ruecksendezeile; }
    public void setRuecksendezeile(String ruecksendezeile) { this.ruecksendezeile = ruecksendezeile; }
    public String getAnsprechpartner() { return ansprechpartner; }
    public void setAnsprechpartner(String ansprechpartner) { this.ansprechpartner = ansprechpartner; }
    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
