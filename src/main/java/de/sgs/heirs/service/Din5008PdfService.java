package de.sgs.heirs.service;

import de.sgs.heirs.model.SenderInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class Din5008PdfService {

    private final SenderInfo senderInfo;
    private final ServerMasterKeyService masterKeyService;

    private static final String OUTPUT_DIR = "generated-letters";

    // 1 mm entspricht ca. 2.83465 Punkten in PDFBox (72 DPI)
    private static final float MM_TO_PT = 2.83465f;

    public Din5008PdfService(SenderInfo senderInfo, ServerMasterKeyService masterKeyService) {
        this.senderInfo = senderInfo;
        this.masterKeyService = masterKeyService;
    }

    public File createLetterPdf(String username, String fullName,
                                String street,
                                String zipCode,
                                String city,
                                String erbeVon,
                                LocalDate validUntil, BufferedImage qrImage) throws IOException {
        // 1. Dateinamen bereinigen: Nur Buchstaben und Unterstrich erlauben
        String safeFilename = sanitizeFilename(username) + ".pdf";

        // Ausgabe-Ordner anlegen
        Path dirPath = Paths.get(OUTPUT_DIR);
//        if (!Files.exists(dirPath)) Files.createDirectories(dirPath);
        File targetFile = dirPath.resolve(safeFilename).toFile();

        DateTimeFormatter germanDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        byte[] pdfPlainBytes;

        // 2. PDF Dokument erstellen (DIN A4: 595.28 x 841.89 pt) im RAM
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float pageHeight = page.getMediaBox().getHeight(); // 841.89 pt
                float leftMargin = 25 * MM_TO_PT;                 // DIN 5008: 25mm linker Rand

                // --- 1. RÜCKSENDEANGABE (45 mm von oben, 7 pt klein) ---
                cs.beginText();
                cs.setFont(fontRegular, 7);
                cs.newLineAtOffset(leftMargin, pageHeight - (45 * MM_TO_PT));
                cs.showText(senderInfo.getAbsender()); // <-- Stefan Gärtners • Scharnhorststraße 21e • 46485 Wesel
                cs.endText();

                // 2. DYNAMISCHES EMPFÄNGERFELD (55 mm von oben)
                String recipientName = (fullName != null && !fullName.isBlank()) ? fullName : username;
                String recipientStreet = (street != null && !street.isBlank()) ? street : "Postfach / Hausadresse";
                String recipientLocation = ((zipCode != null ? zipCode : "") + " " + (city != null ? city : "")).trim();
                if (recipientLocation.isBlank()) recipientLocation = "Deutschland";

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setLeading(14);
                cs.newLineAtOffset(leftMargin, pageHeight - (55 * MM_TO_PT));
                cs.showText(recipientName);
                cs.newLine();
                cs.showText(recipientStreet);
                cs.newLine();
                cs.showText(recipientLocation);
                cs.endText();

                // --- 3. DATUM (ca. 85 mm von oben, rechtsbündig bei 170 mm) ---
                LocalDate today = LocalDate.now();
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(145 * MM_TO_PT, pageHeight - (85 * MM_TO_PT));
                cs.showText(senderInfo.getDatumszeile(today));
                cs.endText();

                // --- 4. BETREFF (ab 103,5 mm von oben, Fettdruck) ---
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(leftMargin, pageHeight - (103.5f * MM_TO_PT));
                cs.showText("Dein digitaler Zugangsausweis ");
                cs.endText();

                // --- 5. BRIEFTEXT (2 Zeilen unter Betreff) ---
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setLeading(15);
                cs.newLineAtOffset(leftMargin, pageHeight - (120 * MM_TO_PT));
                cs.showText("Guten Tag " + username + ",");
                cs.newLine();
                cs.newLine();
                cs.showText("anbei erhältst du deinen persönlichen Zugangsausweis.");
                cs.newLine();
                cs.newLine();
                cs.showText("Damit kannst Du nach dem Tod vom Erblasser " + erbeVon + " auf dessen digitalen Nachlass zugreifen.");
                cs.newLine();
                cs.newLine();
                cs.showText("Der unten stehende QR-Code ist kryptografisch signiert und gültig bis: " + validUntil.format(germanDate));
                cs.newLine();
                cs.newLine();
                cs.showText("Scanne diesen Code einfach mit deiner Smartphone-Kamera, um dich direkt anzumelden.");
                cs.newLine();
                cs.newLine();
                cs.showText("Bitte bewahre diesen Brief vertraulich auf!");
                cs.endText();

                // QR-Code einfügen
                PDImageXObject pdQrImage = LosslessFactory.createFromImage(document, qrImage);
                float qrSizePt = 50 * MM_TO_PT;
                float qrY = pageHeight - (225 * MM_TO_PT);
                cs.drawImage(pdQrImage, leftMargin, qrY, qrSizePt, qrSizePt);

                // --- 7. GRUSSFORMEL ---
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setLeading(14);
                cs.newLineAtOffset(leftMargin, qrY - (20 * MM_TO_PT));
                cs.showText("Mit freundlichen Grüßen");
                cs.newLine();
                cs.newLine();
                cs.setFont(fontBold, 10);
                cs.showText("Die Administration");
                cs.endText();
            }

            document.save(baos);
            pdfPlainBytes = baos.toByteArray();
        }

        // 3. VERSCHLÜSSELUNG: Byte-Array mit AES-256-GCM verschlüsseln und speichern
        byte[] encryptedBytes = masterKeyService.encryptBytes(pdfPlainBytes);
        Files.write(targetFile.toPath(), encryptedBytes);

        return targetFile;
    }

    /**
     * Lässt im Dateinamen AUSSCHLIESSLICH Buchstaben (A-Z, a-z), Zahlen und Unterstriche (_) zu.
     */
    public String sanitizeFilename(String rawUsername) {
        if (rawUsername == null) return "unbekannt";
        String cleaned = rawUsername.replaceAll("[^a-zA-Z_0-9]", "");
        return cleaned.isBlank() ? "benutzer" : cleaned;
    }

    public File createVerwalterLetterPdf(String username, String fullName, String street, String zipCode, String city,
                                         LocalDate validUntil, BufferedImage qrImage) throws IOException {
        String safeFilename = sanitizeFilename(username) + ".pdf";
        Path dirPath = Paths.get(OUTPUT_DIR);
//        if (!Files.exists(dirPath)) Files.createDirectories(dirPath);
        File targetFile = dirPath.resolve(safeFilename).toFile();

        byte[] pdfPlainBytes;

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float pageHeight = page.getMediaBox().getHeight();
                float leftMargin = 25 * MM_TO_PT;

                // 1. Absender
                cs.beginText();
                cs.setFont(fontRegular, 7);
                cs.newLineAtOffset(leftMargin, pageHeight - (45 * MM_TO_PT));
                cs.showText(senderInfo.getAbsender());
                cs.endText();

                // 2. Empfänger (Notar/Verwalter)
                String recipientName = (fullName != null && !fullName.isBlank()) ? fullName : username;
                String recipientStreet = (street != null && !street.isBlank()) ? street : "Kanzleiadresse";
                String recipientLocation = ((zipCode != null ? zipCode : "") + " " + (city != null ? city : "")).trim();

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setLeading(14);
                cs.newLineAtOffset(leftMargin, pageHeight - (55 * MM_TO_PT));
                cs.showText("Herrn/Frau Notar(in) / Erbschaftsverwalter(in)");
                cs.newLine();
                cs.showText(recipientName);
                cs.newLine();
                cs.showText(recipientStreet);
                cs.newLine();
                cs.showText(recipientLocation);
                cs.endText();

                // 3. Datum
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(145 * MM_TO_PT, pageHeight - (85 * MM_TO_PT));
                cs.showText(senderInfo.getDatumszeile(LocalDate.now()));
                cs.endText();

                // 4. Betreff
                cs.beginText();
                cs.setFont(fontBold, 12);
                cs.newLineAtOffset(leftMargin, pageHeight - (103.5f * MM_TO_PT));
                cs.showText("Bestallungsurkunde & Notar-Zugangsausweis (SUPERUSER)");
                cs.endText();

                // 5. Brieftext
                DateTimeFormatter germanDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setLeading(15);
                cs.newLineAtOffset(leftMargin, pageHeight - (120 * MM_TO_PT));
                cs.showText("Sehr geehrte(r) " + recipientName + ",");
                cs.newLine();
                cs.newLine();
                cs.showText("Sie wurden im System als Erbschaftsverwalter / Notar registriert (Kennung: " + username + ").");
                cs.newLine();
                cs.showText("Mit dem untenstehenden kryptografischen Ed25519-Ausweis erhalten Sie Zugang zur Verwalter-Konsole.");
                cs.newLine();
                cs.showText("In dieser Konsole sind Sie legitimiert, für Ihnen anvertraute Erblasser den Eintritt");
                cs.newLine();
                cs.showText("des Todesfalls verbindlich mit Datum festzustellen. Dieser Ausweis ist gültig bis: " + validUntil.format(germanDate));
                cs.newLine();
                cs.newLine();
                cs.showText("Bitte verwahren Sie diesen Bestallungsausweis in Ihren vertraulichen Kanzleiakten.");
                cs.endText();

                // 6. QR-Code
                PDImageXObject pdQrImage = LosslessFactory.createFromImage(document, qrImage);
                float qrSizePt = 50 * MM_TO_PT;
                float qrY = pageHeight - (215 * MM_TO_PT);
                cs.drawImage(pdQrImage, leftMargin, qrY, qrSizePt, qrSizePt);

                // 7. Grußformel
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.setLeading(14);
                cs.newLineAtOffset(leftMargin, qrY - (20 * MM_TO_PT));
                cs.showText("Mit kollegialen Grüßen");
                cs.newLine();
                cs.newLine();
                cs.setFont(fontBold, 10);
                cs.showText("Zentrales Nachlassregister");
                cs.endText();
            }

            document.save(baos);
            pdfPlainBytes = baos.toByteArray();
        }

        // 3. VERSCHLÜSSELUNG: Byte-Array mit AES-256-GCM verschlüsseln und speichern
        byte[] encryptedBytes = masterKeyService.encryptBytes(pdfPlainBytes);
        Files.write(targetFile.toPath(), encryptedBytes);

        return targetFile;
    }
}