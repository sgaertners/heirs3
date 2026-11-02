Hier ist die vollständige und detaillierte **`README.md`**, die den gesamten Ablauf von der Erstellung der Konten über die Zuordnung bis hin zum verschlüsselten Upload und dem Login des Erben Schritt für Schritt dokumentiert.

Speichere diesen Inhalt als **`README.md`** im Stammverzeichnis deines Projekts ab:

```markdown
# 🏛️ SGS HEIRS – Digitales Nachlass- & Ausweissystem

SGS HEIRS ist eine sichere Plattform zur Verwaltung und Weitergabe von digitalen Nachlässen. Die Anwendung kombiniert **passwortlosen Login via Ed25519-QR-Codes**, automatische **DIN-5008-Briefgenerierung mit Apache PDFBox** sowie eine **selektive AES-256-GCM-Dateiverschlüsselung**.

Dieses Dokument beschreibt den vollständigen End-to-End-Workflow:
1. **Benutzer anlegen (Erblasser & Erbe)**
2. **Zuordnung des Erblassers zum Erben**
3. **Dokumenten-Upload durch den Erblasser** (inkl. Audio/Video-Erkennung)
4. **Passwortloser Login des Erben per QR-Code**
5. **Kategorisierte Anzeige & Abruf der Nachlassdokumente**

---

## 📋 Übersicht des Gesamtablaufs

```text
┌─────────────────┐       ┌────────────────────────┐       ┌──────────────────────┐
│  Administrator  │ ────► │  Erblasser-Dashboard   │ ────► │    Erben-Dashboard   │
│                 │       │                        │       │                      │
│ 1. User anlegen │       │ 3. Wählt Erben &       │       │ 4. Scannt QR-Code    │
│    (Erblasser & │       │    Kategorie           │       │    aus DIN-5008-     │
│     Erbe)       │       │ 4. Lädt Dateien hoch   │       │    Brief             │
│ 2. Erblasser    │       │    - Dokumente: AES    │       │ 5. Sieht Dokumente   │
│    dem Erben    │       │    - Audio/Video: Plain│       │    nach Kategorien   │
│    zuordnen     │       │                        │       │ 6. Entschlüsselt/    │
└─────────────────┘       └────────────────────────┘       │    spielt Media ab   │
                                                           └──────────────────────┘
```

---

## 🛠️ Schritt-für-Schritt-Anleitung

### Schritt 1: Administrator-Login

1. Starte die Spring-Boot-Anwendung.
2. Öffne im Browser die Startseite:
   ```text
   http://localhost:8080/master
   ```
3. Melde dich in der rechten Box (**Administrator**) an:
    * **Benutzername:** `admin` (bzw. dein via `/wyarf` konfigurierter Admin)
    * **Passwort:** `admin123`
4. Du wirst direkt in das **Admin-Kontrollzentrum (`/admin/dashboard`)** weitergeleitet.

---

### Schritt 2: Erblasser und Erben anlegen

Es werden zwei Konten benötigt: Ein **Erblasser** und mindestens ein **Erbe**.

#### 2.1 Erblasser anlegen:
1. Klicke im Admin-Dashboard auf **"Neuen User anlegen"** (`/admin/create-user`).
2. Fülle die Felder aus:
    * **Benutzername / Kennung:** `erblasser_kurt`
    * **Vollständiger Name:** `Herr Kurt Testmann`
    * **Adresse:** `Kaiserstraße 10`, `46485`, `Wesel`
    * **Gültigkeit:** `1 Jahr`
    * **Checkbox:** ☑️ *Diesen Benutzer direkt als Erblasser kennzeichnen* aktivieren.
3. Klicke auf **"Ausweis & DIN 5008 Brief generieren"**.
4. Der druckfertige Brief wird im Ordner `./generated-letters/erblasser_kurt.pdf` abgelegt und kann über den roten Button sofort eingesehen werden.

#### 2.2 Erben anlegen:
1. Rufe erneut **"Neuen User anlegen"** auf.
2. Fülle die Felder für den Erben aus:
    * **Benutzername / Kennung:** `erbe_anna`
    * **Vollständiger Name:** `Frau Anna Testmann`
    * **Adresse:** `Rheinstraße 5`, `46485`, `Wesel`
    * **Checkbox:** ⬜ *Erblasser-Checkbox deaktiviert lassen*.
3. Klicke auf **"Ausweis & DIN 5008 Brief generieren"**.
4. Der Brief für die Erbin wird unter `./generated-letters/erbe_anna.pdf` gespeichert. **Halte diesen Brief bzw. den QR-Code für Schritt 5 bereit!**

---

### Schritt 3: Erblasser dem Erben zuordnen

Damit ein Erblasser Dateien gezielt für einen Erben hinterlegen kann, muss die Verknüpfung hergestellt werden:

1. Klicke in der oberen Menüleiste auf **"Benutzerliste"** (`/admin/users`).
2. Suche in der Tabelle die Zeile von **`erbe_anna`**.
3. Da Anna aktiv, nicht verstorben und kein Erblasser ist, steht in der Spalte **"Erbe von"** der blaue Button **`+ Zuordnen`**.
4. Klicke auf **`+ Zuordnen`**: Ein modaler Dialog öffnet sich.
5. Wähle im Dropdown den Erblasser **`erblasser_kurt (Herr Kurt Testmann)`** aus.
6. Klicke auf **"Zuordnung speichern"**.
7. In der Tabelle erscheint nun bei `erbe_anna` das Badge: `🔗 erblasser_kurt`.

---

### Schritt 4: Dokumenten-Upload durch den Erblasser

Nun meldet sich der Erblasser an, um Dokumente für seine Erbin zu hinterlegen.

1. **Abmelden:** Melde dich als Administrator oben rechts über **"Abmelden"** ab.
2. **Login als Erblasser:**
    * Scanne den QR-Code aus dem Brief `generated-letters/erblasser_kurt.pdf` auf der Login-Seite `/master` mit der Webcam **ODER**
    * Rufe direkt die URL aus dem QR-Code im Browser auf (z. B. `/qr-auth?u=erblasser_kurt&exp=...&sig=...`).
3. **Erblasser-Dashboard:**
    * Da Kurt als `Erblasser = Ja` hinterlegt ist, erscheint die Box **"Verschlüsselter Dokumenten-Upload"**.
4. **Datei hochladen:**
    * **Erbe (Empfänger):** Wähle `erbe_anna (Frau Anna Testmann)`.
    * **Kategorie:** Wähle z. B. `Testament & Verfügungen`.
    * **Dokument:** Wähle eine Datei aus (z. B. `testament.pdf` oder eine Videobotschaft `botschaft.mp4`).
    * Klicke auf **"Verschlüsselt ablegen"**.

#### 🔒 Was im Hintergrund passiert:
* **PDFs, Bilder & Dokumente:** Werden mit dem privaten AES-256-GCM-Schlüssel von Kurt verschlüsselt unter `./data/erblasser_kurt/erbe_anna/testament.pdf` gespeichert.
* **Audio & Video (MP3, MP4 etc.):** Werden automatisch erkannt und zur Schonung von Ressourcen **unverschlüsselt** abgelegt, damit sie später im Browser gestreamt werden können.
* Der Index wird in `./data/erblasser_kurt/files.csv` vermerkt.
* Eine DSGVO-konforme Audit-Zeile wird unter `./audit/erblasser_kurt/actions.log` geschrieben.

5. Melde dich oben rechts über **"Abmelden"** ab.

---

### Schritt 5: Passwortloser Login des Erben

Die Erbin `erbe_anna` greift nun auf ihren digitalen Nachlass zu:

1. Öffne die Startseite: `http://localhost:8080/master`.
2. Klicke in der linken Box auf **"Kamera aktivieren"** und halte den QR-Code aus dem Brief `./generated-letters/erbe_anna.pdf` vor die Kamera.
   *(Alternativ: Scanne den Code einfach mit der normalen Foto-Kamera-App deines Smartphones).*
3. **Verifikation:** Der Server validiert die kryptografische Ed25519-Master-Signatur und das Ablaufdatum.
4. Du wirst ohne Eingabe von Passwörtern sofort auf das **Benutzer-Dashboard (`/dashboard`)** weitergeleitet.

---

### Schritt 6: Anzeigen, Entschlüsseln und Abspielen

Auf Annas Dashboard wird ihr Status und die für sie hinterlegten Daten angezeigt:

1. **Kategorie-Übersicht:**
    * Unter **"Hinterlegte Dokumente für dich"** (Erblasser: `erblasser_kurt`) werden alle Dateien sauber nach Kategorien (z. B. *Testament & Verfügungen*, *Finanzen*) sortiert dargestellt.
2. **Dateityp-Erkennung & Status:**
    * Jede Datei besitzt ein typisiertes Icon (PDF, Word, Bild, Video, Audio).
    * Bei PDF/Dokumenten signalisiert ein grünes Badge `🛡️ AES-256`, dass die Datei verschlüsselt ist.
    * Bei Videos/Audios signalisiert ein Badge `▶️ Audio/Video`, dass es sich um Streaming-Medien handelt.
3. **Abruf & Entschlüsselung:**
    * Klickt Anna auf **"Entschlüsseln"** (bei einem Dokument), entschlüsselt der Server die Datei `testament.pdf` im flüchtigen RAM mit dem Erblasser-Schlüssel und öffnet das Originaldokument direkt im Browser.
    * Klickt Anna auf **"Abspielen / Öffnen"** (bei einer MP4/MP3-Datei), wird das Video bzw. Audio direkt und ohne Verzögerung gestreamt.
4. **Audit:** Der Lese- bzw. Entschlüsselungsvorgang wird im Audit-Log `./audit/erbe_anna/actions.log` revisionssicher vermerkt.

---

## 📁 Relevante Verzeichnisstruktur auf der Festplatte

```text
sgs-heirs/
├── audit/                        # Getrennte Audit-Logs pro Benutzer
│   ├── erblasser_kurt/
│   │   └── actions.log           # Loggt Uploads, Logins (ohne sensible Daten)
│   └── erbe_anna/
│       └── actions.log           # Loggt Entschlüsselungen & Lesezugriffe
├── data/                         # Nachlass-Ablage
│   └── erblasser_kurt/           # Erblasser-Hauptordner
│       ├── files.csv             # Index: Name des Erben;Dateiname;Kategorie
│       └── erbe_anna/            # Dokumente speziell für diese Erbin
│           ├── testament.pdf     # AES-256-GCM verschlüsselt
│           └── botschaft.mp4     # Unverschlüsseltes Video zum Direkt-Streamen
├── generated-letters/            # Druckfertige A4-PDF-Briefe nach DIN 5008
│   ├── erblasser_kurt.pdf
│   └── erbe_anna.pdf
├── master_private.key            # Zentraler Ed25519-Master-Key des Servers
├── master_public.key             # Master-Public-Key
├── users_secure.csv              # Benutzerdaten (Adressen AES-verschlüsselt)
└── wyarf.irs                     # Verschlüsselte Administrator-Zugangsdaten
```

---

## 🔒 Sicherheitsmerkmale im Überblick

* **Zero-Knowledge-Passwörter:** Benutzer besitzen keine Passwörter, die ausgespäht oder erraten werden können.
* **DIN 5008 Physischer Zugang:** Der ausgedruckte Brief ist das greifbare Zugangsmedium für den Nachlassfall.
* **In-Memory-Entschlüsselung:** Dokumente werden niemals im Klartext auf der Server-Festplatte zwischengespeichert.
* **Replay-Schutz:** Jeder Brief besitzt ein striktes kryptografisches Ablaufdatum.
* **Auto-Logoff bei Fehlern:** Bei Sicherheitsverletzungen (403), URL-Manipulationen oder Serverfehlern wird die Session sofort zerstört und auf die Login-Seite umgeleitet.
```