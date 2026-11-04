package de.sgs.heirs.model;


import java.time.LocalDate;

public class UserAccount {
    private String username;
    private String fullName;
    private String street;
    private String zipCode;
    private String city;
    private LocalDate validUntil;

    // NEU: Unverschlüsselte Status-Flags
    private boolean active = true;        // Standard: aktiv
    private boolean erblasser = false;     // Standard: nein
    private boolean verstorben = false;    // Standard: nein

    private String erbeVon; // Speichert den Benutzernamen des Erblassers

    private boolean verwalter = false;         // Ist dieser User ein Notar/Erbschaftsverwalter?
    private String verwalterUsername;          // Welcher Verwalter betreut diesen Erblasser?
    private LocalDate todesdatum;              // Vom Verwalter eingetragenes Sterbedatum


    public UserAccount() {}

    public UserAccount(String username, String fullName, String street, String zipCode, String city, LocalDate validUntil) {
        this.username = username;
        this.fullName = fullName;
        this.street = street;
        this.zipCode = zipCode;
        this.city = city;
        this.validUntil = validUntil;
        this.active = true;
        this.erblasser = false;
        this.verstorben = false;
    }

    // Getter und Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isErblasser() { return erblasser; }
    public void setErblasser(boolean erblasser) { this.erblasser = erblasser; }

    public boolean isVerstorben() { return verstorben; }
    public void setVerstorben(boolean verstorben) { this.verstorben = verstorben; }

    // Prüft die Kriterien: Aktiv, NICHT verstorben, NICHT selbst Erblasser
    public boolean isEligibleHeir() {
        return this.active && !this.verstorben && !this.erblasser;
    }

    public String getErbeVon() { return erbeVon; }
    public void setErbeVon(String erbeVon) { this.erbeVon = erbeVon; }

    // Getter und Setter
    public boolean isVerwalter() { return verwalter; }
    public void setVerwalter(boolean verwalter) { this.verwalter = verwalter; }

    public String getVerwalterUsername() { return verwalterUsername; }
    public void setVerwalterUsername(String verwalterUsername) { this.verwalterUsername = verwalterUsername; }

    public LocalDate getTodesdatum() { return todesdatum; }
    public void setTodesdatum(LocalDate todesdatum) { this.todesdatum = todesdatum; }
}