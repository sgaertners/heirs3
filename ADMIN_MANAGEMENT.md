# 🔐 SGS HEIRS: Administrator-Verwaltung & Lifecycle

Diese Dokumentation beschreibt die Erstanlage, Verwaltung, Autorisierung und den vollständigen Lebenszyklus von Administrator-Benutzern im System **SGS HEIRS**.

---

## 1. Architektur- & Sicherheitskonzept

Die Admin-Verwaltung kombiniert drei Sicherheitsmechanismen:
1. **MasterKey-Verschlüsselung (AES-256-GCM):** Administrator-Benutzernamen und Passwörter werden **niemals im Klartext** auf der Festplatte gespeichert. Sie werden mit dem aus dem Server-Masterkey (`master_private.key`) abgeleiteten 256-Bit AES-Schlüssel verschlüsselt in der Datei `wyarf.irs` abgelegt.
2. **Lock-File-Prinzip:** Der Einrichtungs-Endpunkt `/wyarf` ist eine One-Time-Setup-Schnittstelle. Er ist streng gesperrt, sobald die Sicherheitsdatei `wyarf.irs` auf der Festplatte existiert.
3. **Flüchtiger In-Memory-Betrieb:** Beim Booten der Anwendung liest Spring Security die Datei `wyarf.irs` ein, entschlüsselt die Daten im flüchtigen Arbeitsspeicher (RAM), hasht die Passwörter mit **BCrypt** und übergibt sie an den `InMemoryUserDetailsManager`. Auf der Festplatte verbleibt zu jeder Zeit ausschließlich Ciphertext.

---

## 2. Der Lebenszyklus im Überblick

```text
[ Erststart ] 
      │
      ▼
/wyarf/{user}/{pw}  ──►  Verschlüsselt Daten ──► Erzeugt 'wyarf.irs' ──► System beendet sich
                                                                               │
┌──────────────────────────────────────────────────────────────────────────────┘
▼
[ Neustart der App ] ──► SecurityConfig entschlüsselt 'wyarf.irs' ──► Admin aktiv im RAM
      │
      ├──► Login über Web-UI (/master) ──► Zugriff auf /admin/**
      ├──► Abfrage aller Admins über /wyarfList/{user}/{pw}
      └──► Löschen von Admins über /wyarfdelete/{user}/{pw}/{target}
                 │
                 └──► Letzter Admin gelöscht? ──► 'wyarf.irs' gelöscht ──► /wyarf wieder frei

```

## 3. Start

Beim allerersten Start des Systems (wenn noch keine wyarf.irs existiert) ist der Setup-Endpunkt aktiv.
### Syntax:

    GET/POST /wyarf/{username}/{password}
    
### Parameter:
    
    {username}: Der gewünschte Administrator-Benutzername.
    {password}: Das gewünschte Klartext-Passwort.

### Beispiel:
        curl -X POST http://localhost:8080/wyarf/superadmin/Geheim123!

Was im Hintergrund geschieht:

Das System prüft, ob wyarf.irs oder wyaf.irs existiert. Falls ja, wird der Zugriff mit 410 GONE verweigert.

username und password werden mit AES-256-GCM verschlüsselt.

Die Zeile encUser=encPassword wird in die Datei wyarf.irs geschrieben.

Das Programm wartet 1 Sekunde (um die HTTP-Antwort zuzustellen) und beendet sich automatisch (System.exit(0)).


## 4. Systemstart & Entschlüsselung (SecurityConfig)

Nachdem die Anwendung neu gestartet wurde, liest die Methode userDetailsService in SecurityConfig.java die Datei wyarf.irs ein:
   
    Wenn wyarf.irs existiert:
        Alle Einträge werden zeilenweise mit dem Masterkey entschlüsselt, mit BCryptPasswordEncoder gehasht und mit der Rolle ROLE_ADMIN registriert.
    
    Fallback (Nur vor der Erstanlage):    
        Existiert noch keine Datei, greift ein temporärer Notfall-Admin (admin / admin123), damit das System zur Initialisierung gestartet werden kann.


## 5. Reguläre Anmeldung an der Web-Oberfläche

Nach dem Neustart kann sich der Administrator über die Standard-Webseite anmelden:
Rufe im Browser die Startseite auf: http://localhost:8080/master

Nutze die rechte Anmeldebox (Administrator):
    
    Benutzername: {username} (z. B. superadmin)
    Passwort: {password} (z. B. Geheim123!)
    
Nach erfolgreichem Login erfolgt die automatische Weiterleitung in die Admin-Zentrale (/dashboard).

Der Administrator erhält Vollzugriff auf alle geschützten Verwaltungsseiten (/admin/**):

    /admin/dashboard: Kontrollzentrum
    /admin/create-user: Benutzeranlage & DIN-5008-Briefgenerierung
    /admin/users: Benutzerverwaltung, Status-Toggles & Erben-Zuordnung
    /admin/users/edit/{username}: Nachträgliche Datenänderung


## 6. Administratoren auflisten (/wyarfList)
Ermöglicht autorisierten Administratoren das Abfragen aller aktuell registrierten Admin-Konten.

### Syntax
    GET/POST /wyarfList/{authUser}/{authPassword}

### Autorisierung
Die ersten beiden Pfadvariablen dienen der Authentifizierung. Entsprechen sie keinem gültigen Admin-Konto in wyarf.irs, antwortet der Server mit 401 UNAUTHORIZED.

### Beispiel
    curl http://localhost:8080/wyarfList/superadmin/Geheim123!

### Antwort (200 OK JSON-Array):

JSON:
    [
      "superadmin"
    ]

(Aus Sicherheitsgründen werden ausschließlich Benutzernamen, niemals Passwörter zurückgegeben).

## 7. Administrator löschen & Reset (/wyarfdelete)
Ermöglicht das Entfernen eines Admin-Kontos aus der verschlüsselten Datei.

### Syntax
    GET/POST/DELETE /wyarfdelete/{authUser}/{authPassword}/{targetUser}

### Parameter
    {authUser}: Benutzername des autorisierenden Admins.
    {authPassword}: Passwort des autorisierenden Admins.
    {targetUser}: Benutzername des zu löschenden Admins.

### Beispiel A: Löschen bei mehreren vorhandenen Admins
    curl -X POST http://localhost:8080/wyarfdelete/superadmin/Geheim123!/backupadmin

### Verhalten: 
    backupadmin wird aus der Liste entfernt. Die verbleibenden Admins werden mit dem Masterkey neu verschlüsselt und in wyarf.irs überschrieben.

### Beispiel B: Löschen des letzten verbleibenden Admins (Factory Reset)
    curl -X POST http://localhost:8080/wyarfdelete/superadmin/Geheim123!/superadmin

### Verhalten:
Das System stellt fest, dass nach dem Löschen kein Admin mehr existiert.
Die Datei wyarf.irs wird physisch von der Festplatte gelöscht.

###Ergebnis: 
Der initiale Setup-Endpunkt /wyarf/{username}/{password} ist sofort wieder aktiv und kann zur Neuregistrierung genutzt werden.

## 8. Wichtige Betriebs- & Sicherheitshinweise

### Sonderzeichen im Passwort	
Werden bei /wyarf, /wyarfList oder /wyarfdelete Passwörter mit Sonderzeichen (wie #, /, ?, &) übergeben, müssen diese URL-kodiert sein (z. B. %2F statt / oder %23 statt #).

### Dateisicherheit	
Die Datei wyarf.irs kann ohne die Datei master_private.key nicht entschlüsselt werden. Ein Diebstahl der Datei alleine gefährdet die Zugangsdaten nicht.

### Audit-Trail	
Alle administrativen Handlungen (User erstellen, bearbeiten, löschen, Status-Toggles) werden im Verzeichnis audit/ unter Ausschluss schützenswerter Klartext-Daten protokolliert.

### Automatischer Logout	
Bei unberechtigtem Zugriff auf Admin-Pfade (403 Forbidden) oder schweren Systemfehlern (500) wird die Session sofort beendet und der Nutzer zur Login-Seite /master zurückgeführt.
