package de.sgs.heirs.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "NotarStammdaten")
public class NotarStammdatenConfig {

    // --- Notar Stammdaten ---
    private String kanzleiName = "Notariat & Erbschaftsverwaltung Stefan Gärtners";
    private String notarName = "Stefan Gärtners";
    private String amtsbezeichnung = "Notar";
    private String strasse = "Scharnhorststraße 21e";
    private String plz = "46485";
    private String ort = "Wesel";
    private String absenderzeile = "Stefan Gärtners • Scharnhorststraße 21e • 46485 Wesel";
    private String telefon = "0281 / 123456";
    private String email = "kanzlei@notar-gaertners.de";

    // --- Briefangaben: Erben-Brief ---
    private String betreffErbe = "Dein digitaler Zugangsausweis (Nachlass-Freigabe)";
    private String textErbe = "anbei erhältst du deinen persönlichen Zugangsausweis. Damit kannst du nach dem festgestellten Tod des Erblassers auf dessen digitalen Nachlass zugreifen.";

    // --- Briefangaben: Erblasser-Brief ---
    private String betreffErblasser = "Ihr digitaler Erblasser-Ausweis zur Dokumenten-Hinterlegung";
    private String textErblasser = "anbei erhalten Sie Ihren persönlichen Zugangsausweis. Mit diesem QR-Code können Sie sich passwortlos anmelden und Dokumente für Ihre Erben verschlüsselt hinterlegen.";

    // --- Briefangaben: Verwalter/Notar-Brief ---
    private String betreffVerwalter = "Bestallungsurkunde & Notar-Zugangsausweis (SUPERUSER)";
    private String textVerwalter = "Sie wurden im System als Erbschaftsverwalter / Notar registriert. Mit dem untenstehenden kryptografischen Ed25519-Ausweis erhalten Sie Zugang zur Verwalter-Konsole.";

    public NotarStammdatenConfig() {}

    // Getter und Setter
    public String getKanzleiName() { return kanzleiName; }
    public void setKanzleiName(String kanzleiName) { this.kanzleiName = kanzleiName; }

    public String getNotarName() { return notarName; }
    public void setNotarName(String notarName) { this.notarName = notarName; }

    public String getAmtsbezeichnung() { return amtsbezeichnung; }
    public void setAmtsbezeichnung(String amtsbezeichnung) { this.amtsbezeichnung = amtsbezeichnung; }

    public String getStrasse() { return strasse; }
    public void setStrasse(String strasse) { this.strasse = strasse; }

    public String getPlz() { return plz; }
    public void setPlz(String plz) { this.plz = plz; }

    public String getOrt() { return ort; }
    public void setOrt(String ort) { this.ort = ort; }

    public String getAbsenderzeile() { return absenderzeile; }
    public void setAbsenderzeile(String absenderzeile) { this.absenderzeile = absenderzeile; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBetreffErbe() { return betreffErbe; }
    public void setBetreffErbe(String betreffErbe) { this.betreffErbe = betreffErbe; }

    public String getTextErbe() { return textErbe; }
    public void setTextErbe(String textErbe) { this.textErbe = textErbe; }

    public String getBetreffErblasser() { return betreffErblasser; }
    public void setBetreffErblasser(String betreffErblasser) { this.betreffErblasser = betreffErblasser; }

    public String getTextErblasser() { return textErblasser; }
    public void setTextErblasser(String textErblasser) { this.textErblasser = textErblasser; }

    public String getBetreffVerwalter() { return betreffVerwalter; }
    public void setBetreffVerwalter(String betreffVerwalter) { this.betreffVerwalter = betreffVerwalter; }

    public String getTextVerwalter() { return textVerwalter; }
    public void setTextVerwalter(String textVerwalter) { this.textVerwalter = textVerwalter; }
}