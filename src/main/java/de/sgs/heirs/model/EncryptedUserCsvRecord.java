package de.sgs.heirs.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "username", "active", "erblasser", "verstorben", "verwalter", "erbeVon", "verwalterUsername",
        "todesdatum", "encFullName", "encStreet", "encZipCode", "encCity", "encValidUntil"
})
public class EncryptedUserCsvRecord {

    @JsonProperty("username")
    private String username;

    @JsonProperty("active")
    private boolean active = true;

    @JsonProperty("erblasser")
    private boolean erblasser = false;

    @JsonProperty("verstorben")
    private boolean verstorben = false;

    @JsonProperty("verwalter")
    private boolean verwalter = false;

    @JsonProperty("erbeVon")
    private String erbeVon = "";

    @JsonProperty("verwalterUsername")
    private String verwalterUsername = "";

    @JsonProperty("todesdatum")
    private String todesdatum = "";

    // Verschlüsselt
    @JsonProperty("encFullName")
    private String encFullName;
    @JsonProperty("encStreet")
    private String encStreet;
    @JsonProperty("encZipCode")
    private String encZipCode;
    @JsonProperty("encCity")
    private String encCity;
    @JsonProperty("encValidUntil")
    private String encValidUntil;

    // Getter & Setter für die neuen Felder:
    public boolean isVerwalter() { return verwalter; }
    public void setVerwalter(boolean verwalter) { this.verwalter = verwalter; }

    public String getVerwalterUsername() { return verwalterUsername; }
    public void setVerwalterUsername(String verwalterUsername) { this.verwalterUsername = verwalterUsername; }

    public String getTodesdatum() { return todesdatum; }
    public void setTodesdatum(String todesdatum) { this.todesdatum = todesdatum; }


    public EncryptedUserCsvRecord() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isErblasser() { return erblasser; }
    public void setErblasser(boolean erblasser) { this.erblasser = erblasser; }

    public boolean isVerstorben() { return verstorben; }
    public void setVerstorben(boolean verstorben) { this.verstorben = verstorben; }

    public String getEncFullName() { return encFullName; }
    public void setEncFullName(String encFullName) { this.encFullName = encFullName; }
    public String getEncStreet() { return encStreet; }
    public void setEncStreet(String encStreet) { this.encStreet = encStreet; }
    public String getEncZipCode() { return encZipCode; }
    public void setEncZipCode(String encZipCode) { this.encZipCode = encZipCode; }
    public String getEncCity() { return encCity; }
    public void setEncCity(String encCity) { this.encCity = encCity; }
    public String getEncValidUntil() { return encValidUntil; }
    public void setEncValidUntil(String encValidUntil) { this.encValidUntil = encValidUntil; }

    // Getter & Setter für erbeVon:
    public String getErbeVon() { return erbeVon; }
    public void setErbeVon(String erbeVon) { this.erbeVon = erbeVon; }

}