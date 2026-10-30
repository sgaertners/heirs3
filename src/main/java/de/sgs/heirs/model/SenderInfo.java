package de.sgs.heirs.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "brief")
public class SenderInfo {

    // Entspricht: Absenderort=Wesel
    private String absenderort = "Wesel";

    // Entspricht: Absender=Stefan Gärtners • Scharnhorststraße 21e • 46485 Wesel
    private String absender = "Stefan Gärtners • Scharnhorststraße 21e • 46485 Wesel";

    // Standard-Konstruktor
    public SenderInfo() {}

    // Vollständiger Konstruktor
    public SenderInfo(String absenderort, String absender) {
        this.absenderort = absenderort;
        this.absender = absender;
    }

    // --- Getter und Setter ---

    public String getAbsenderort() {
        return absenderort;
    }

    public void setAbsenderort(String absenderort) {
        this.absenderort = absenderort;
    }

    public String getAbsender() {
        return absender;
    }

    public void setAbsender(String absender) {
        this.absender = absender;
    }

    // --- Hilfsmethode für den DIN 5008 Briefkopf ---
    /**
     * Erzeugt die DIN-5008-Datumszeile, z. B.: "Wesel, den 24.03.2026"
     */
    public String getDatumszeile(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return String.format("%s, den %s", this.absenderort, date.format(formatter));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SenderInfo that = (SenderInfo) o;
        return Objects.equals(absenderort, that.absenderort) && Objects.equals(absender, that.absender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absenderort, absender);
    }

    @Override
    public String toString() {
        return "SenderInfo{" +
                "absenderort='" + absenderort + '\'' +
                ", absender='" + absender + '\'' +
                '}';
    }
}
