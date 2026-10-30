package de.sgs.heirs.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Order(1)
@Component
public class Directories {

    private static final Logger log = LoggerFactory.getLogger(Directories.class);

    private String appDir;
    private HashMap<String, String> allDirs;
    private List<String> dirNames;

    public Directories() {
        initAllDirectories();
    }

    public String getAppDir() {
        return appDir;
    }

    public HashMap<String, String> getAllDirs() {
        return allDirs;
    }

    public List<String> getDirNames() {
        return dirNames;
    }

    public void setDirNames(List<String> dirNames) {
        this.dirNames = dirNames;
    }

    @Override
    public String toString() {
        return "Directories(appDir=" + this.getAppDir() + ", allDirs=" + this.getAllDirs() + ", dirNames=" + this.getDirNames() + ")";
    }


    @PreDestroy
    public void destroy() {
        log.info("Destroying Application");
        System.out.println("Destroying Application");
    }

    public void initAllDirectories() {
        log.info("=====> Directories created");
        log.debug("initAllDirectories");
        allDirs = new HashMap<>();
        setAppDir();
        allDirs.put("local", getAppDir());
        this.dirNames =  new ArrayList<>();
        setDirs(Objects.requireNonNull(getDirsFromResource()));
    }

    
    public void setAppDir() {
        String appDirectory = new File("").getAbsolutePath() + File.separator;
        log.info("APP-DIR: {}", appDirectory);
        this.appDir = appDirectory;
    }

    
    public void setDirs(List<String> dirs) {
        String singleDir = "";
        for (String dir : dirs) {
            try {
                File directory = new File(getAppDir() + dir);
                log.info("DIR: {}", directory.getAbsolutePath());
                if (!directory.exists()) {
                    if (directory.mkdirs()) {
                        log.info("DIR CREATED: {}", dir);
                    } else {
                        log.error("DIR CREATION FAILED: {}", dir);
                    }
                } else {
                    log.info("DIR EXISTS: {}", dir);
                }
                singleDir = dir.split(File.separator + File.separator)[dir.split(File.separator + File.separator).length - 1];
                this.allDirs.put(singleDir, directory.getAbsolutePath() + File.separator);
                this.dirNames.add(singleDir);
            } catch (Exception e) {
                this.allDirs.put(singleDir, "");
                log.error("DIR: {}", e.getMessage());
            }
        }
        Collections.sort(this.getDirNames());
        log.info(this.allDirs.toString());
    }


    public String getDir(String dir) {
        return allDirs.get(dir);
    }


    public void deleteDir(String dir) {
        try {
            String pfad = allDirs.get(dir);
            if (pfad != null) {
                Files.deleteIfExists(Paths.get(pfad));
                boolean result = FileSystemUtils.deleteRecursively(Paths.get(pfad));
            }
        } catch (Exception e) {
            log.error("DIR: {}", e.getMessage());
        }
    }

    /**
     * Reads directories from resource file directories.cfg into List<String>
     *
     * @return List<String> with all directories
     */
    private List<String> getDirsFromResource() {
        String fileData = "";
        try {
            ClassPathResource resource = new ClassPathResource("directories.cfg");
            try (InputStream is = resource.getInputStream()) {
                fileData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (fileData != null) {
                    return Arrays.asList(fileData.split("\n"));
                }
            }
        } catch (Exception e) {
           log.error("Error reading directories.cfg: {}", e.getMessage());
        }

        return null;
    }

}
