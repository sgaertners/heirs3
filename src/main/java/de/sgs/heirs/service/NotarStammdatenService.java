package de.sgs.heirs.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.sgs.heirs.model.NotarStammdatenConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class NotarStammdatenService {

    private static final String XML_FILE_PATH = "notar_stammdaten.xml";
    private final XmlMapper xmlMapper = new XmlMapper();
    private NotarStammdatenConfig currentConfig = new NotarStammdatenConfig();

    @PostConstruct
    public void init() {
        File file = new File(XML_FILE_PATH);
        if (file.exists()) {
            try {
                this.currentConfig = xmlMapper.readValue(file, NotarStammdatenConfig.class);
            } catch (Exception e) {
                System.err.println("Fehler beim Laden der Stammdaten-XML: " + e.getMessage());
            }
        } else {
            saveConfig(this.currentConfig);
        }
    }

    public synchronized NotarStammdatenConfig getConfig() {
        return this.currentConfig;
    }

    public synchronized void saveConfig(NotarStammdatenConfig config) {
        try {
            this.currentConfig = config;
            xmlMapper.writerWithDefaultPrettyPrinter().writeValue(new File(XML_FILE_PATH), config);
        } catch (IOException e) {
            throw new RuntimeException("Konnte XML nicht schreiben: " + e.getMessage(), e);
        }
    }
}