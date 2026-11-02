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

    // In FileEntry.java:

    /**
     * Prüft, ob diese Datei Audio oder Video ist
     */
    public boolean isAudioOrVideo() {
        if (mimeType != null && (mimeType.startsWith("audio/") || mimeType.startsWith("video/"))) {
            return true;
        }
        String lower = filename.toLowerCase();
        return lower.matches(".*\\.(mp3|wav|ogg|m4a|flac|aac|wma|mp4|webm|mkv|avi|mov|wmv)");
    }

    /**
     * Wählt das passende Icon passend zum Dateityp
     */
    public String getIconClass() {
        String lowerName = filename.toLowerCase();
        if (mimeType != null && mimeType.startsWith("video/") || lowerName.matches(".*\\.(mp4|webm|mkv|mov|avi)")) {
            return "bi-film text-danger"; // Video Icon
        } else if (mimeType != null && mimeType.startsWith("audio/") || lowerName.matches(".*\\.(mp3|wav|ogg|m4a|flac)")) {
            return "bi-file-earmark-music-fill text-warning"; // Audio Icon
        } else if (mimeType != null && mimeType.contains("pdf") || lowerName.endsWith(".pdf")) {
            return "bi-file-earmark-pdf-fill text-danger";
        } else if (mimeType != null && mimeType.startsWith("image/") || lowerName.matches(".*\\.(png|jpg|jpeg|webp|gif)")) {
            return "bi-file-earmark-image-fill text-primary";
        } else if (lowerName.matches(".*\\.(doc|docx|odt)")) {
            return "bi-file-earmark-word-fill text-info";
        } else if (lowerName.matches(".*\\.(xls|xlsx|ods|csv)")) {
            return "bi-file-earmark-excel-fill text-success";
        } else if (lowerName.matches(".*\\.(zip|tar|gz|7z|rar)")) {
            return "bi-file-earmark-zip-fill text-secondary";
        }
        return "bi-file-earmark-lock-fill text-secondary";
    }

}