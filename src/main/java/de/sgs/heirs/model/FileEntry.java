package de.sgs.heirs.model;

public class FileEntry {
    private String erblasserUsername;
    private String heirUsername;
    private String filename;
    private String category;
    private String mimeType;
    private long sizeBytes;

    public FileEntry(String erblasserUsername, String heirUsername, String filename, String category, String mimeType, long sizeBytes) {
        this.erblasserUsername = erblasserUsername;
        this.heirUsername = heirUsername;
        this.filename = filename;
        this.category = category;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    public String getErblasserUsername() { return erblasserUsername; }
    public String getHeirUsername() { return heirUsername; }
    public String getFilename() { return filename; }
    public String getCategory() { return category; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }

    /**
     * Wählt das passende Icon basierend auf Dateiendung und MIME-Type
     */
    public String getIconClass() {
        String lowerName = filename.toLowerCase();
        if (mimeType != null && mimeType.contains("pdf") || lowerName.endsWith(".pdf")) {
            return "bi-file-earmark-pdf-fill text-danger";
        } else if (mimeType != null && mimeType.startsWith("image/") || lowerName.matches(".*\\.(png|jpg|jpeg|webp|gif)")) {
            return "bi-file-earmark-image-fill text-primary";
        } else if (lowerName.matches(".*\\.(doc|docx|odt)")) {
            return "bi-file-earmark-word-fill text-info";
        } else if (lowerName.matches(".*\\.(xls|xlsx|ods|csv)")) {
            return "bi-file-earmark-excel-fill text-success";
        } else if (lowerName.matches(".*\\.(zip|tar|gz|7z|rar)")) {
            return "bi-file-earmark-zip-fill text-warning";
        } else if (lowerName.matches(".*\\.(txt|rtf|md)")) {
            return "bi-file-earmark-text-fill text-secondary";
        }
        return "bi-file-earmark-lock-fill text-secondary";
    }
}