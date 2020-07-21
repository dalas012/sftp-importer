package ru.pochta.service.tasks;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pochta.config.YmlProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Slf4j
public class SftpImportTask implements Runnable {

    private static final String CSV_FILE_EXT = ".csv";
    private static final String HASH_SUM_FILE_NAME = "hashsum.txt";
    private static final String ATTRIBUTES_FILE_NAME = "attributes.txt";
    private static final String CHANGED_FILES_DIR_NAME = "changed";
    private static final String RENAMED_FILES_DIR_NAME = "renamed";
    private static final String CURRENT_FILES_DIR_NAME = "current";

    private final YmlProperties properties;
    private final String currentFilesPath;
    private final String changedFilesPath;
    private final String renamedFilesPath;


    @Autowired
    public SftpImportTask(YmlProperties properties) {
        this.properties = properties;
        this.currentFilesPath = properties.getLocalFilesDirPath() + "/" + CURRENT_FILES_DIR_NAME;
        this.changedFilesPath = properties.getLocalFilesDirPath() + "/" + CHANGED_FILES_DIR_NAME;
        this.renamedFilesPath = properties.getLocalFilesDirPath() + "/" + RENAMED_FILES_DIR_NAME;
    }

    /**
     * Main runner
     */
    @Override
    public void run() {
        try {
            log.info("=== START IMPORTING AND CHECKING FILES ===");
            checkLocalDirs();
            downloadAllFiles();
            checkSums();
            log.info("=== END IMPORTING AND CHECKING FILES ===");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Check local dirs. If there are no dirs then create
     */
    private void checkLocalDirs() {
        Stream.of(properties.getLocalFilesDirPath(), changedFilesPath, renamedFilesPath, currentFilesPath)
                .forEach(path -> {
                    File pathFile = new File(path);
                    if (!pathFile.exists()) {
                        pathFile.mkdir();
                    }
                });
    }

    /**
     * Downloads files from sftp. Skip file if:
     * - the file has not been edited since the last processing
     * - thi file is uploading into sftp right now
     *
     * @throws IOException
     */
    private void downloadAllFiles() throws IOException {
        File attributesFile = getAttributesFile();
        Set<String> attributesSet = new HashSet<>(Files.readAllLines(attributesFile.toPath()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(attributesFile, true));
        SSHClient sshClient = setupSshj();
        SFTPClient sftpClient = sshClient.newSFTPClient();
        log.info("Downloading files start...");
        sftpClient.ls(properties.getRemoteFilesDirPath())
                .stream()
                .filter(item -> item.getName().endsWith(CSV_FILE_EXT))
                .forEach(item -> {
                    String itemAttribute = item.getName() + "_" + item.getAttributes().getMtime();
                    try {
                        if (!attributesSet.contains(itemAttribute)) {
                            attributesSet.add(itemAttribute);
                            writer.write(itemAttribute + System.lineSeparator());
                            sftpClient.get(item.getPath(), currentFilesPath);
                            log.info(" - " + itemAttribute + " - downloaded");
                        } else {
                            log.debug(" - " + itemAttribute + " - skipped");
                        }
                    } catch (SFTPException e) {
                        log.warn(" - " + itemAttribute + " - skipped. " + e.getMessage());
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                });
        log.info("Downloading files end!");
        sftpClient.close();
        sshClient.disconnect();
        writer.close();
    }

    private File getAttributesFile() throws IOException {
        File attributesFile = new File(properties.getLocalFilesDirPath() + "/" + ATTRIBUTES_FILE_NAME);
        if (!attributesFile.exists()) {
            attributesFile.createNewFile();
        }
        return attributesFile;
    }

    private SSHClient setupSshj() throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(properties.getHostname());
        client.authPassword(properties.getUsername(), properties.getPassword());
        return client;
    }

    /**
     * Check downloaded files by checksum. If checksum has changed
     * then move the file into /changed dir
     *
     * @throws IOException
     */
    private void checkSums() throws IOException {
        File hashSumsFile = getHashSumsFile();
        Set<String> hashSet = new HashSet<>(Files.readAllLines(hashSumsFile.toPath()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(hashSumsFile, true));
        log.info("Check and move files start...");
        try (Stream<Path> paths = Files.walk(Paths.get(currentFilesPath))) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toFile().getName().endsWith(CSV_FILE_EXT))
                    .forEach(path -> {
                        try {
                            String hex = getHashSum(path);
                            if (!hashSet.contains(hex)) {
                                hashSet.add(hex);
                                writer.write(hex + System.lineSeparator());
                                moveChangedFile(path, hex);
                            } else {
                                moveRenamedFile(path, hex);
                            }
                        } catch (IOException | NoSuchAlgorithmException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.info("Check and move files end!");
        writer.close();
    }

    private File getHashSumsFile() throws IOException {
        File hashSumsFile = new File(properties.getLocalFilesDirPath() + "/" + HASH_SUM_FILE_NAME);
        if (!hashSumsFile.exists()) {
            hashSumsFile.createNewFile();
        }
        return hashSumsFile;
    }

    private String getHashSum(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(path));
        byte[] digest = md.digest();
        return Hex.encodeHexString(digest).toUpperCase();
    }

    private void moveChangedFile(Path changedFilePath, String hex) throws IOException {
        Path targetPath = Paths.get(changedFilesPath +
                "/" + changedFilePath.getFileName().toString().replace(CSV_FILE_EXT, "_" + hex + CSV_FILE_EXT));
        Files.move(changedFilePath, targetPath);
        log.info(" - " + targetPath.getFileName() + " - changed");
    }

    private void moveRenamedFile(Path renamedFilePath, String hex) throws IOException {
        Path targetPath = Paths.get(renamedFilesPath +
                "/" + renamedFilePath.getFileName().toString().replace(CSV_FILE_EXT, "_" + hex + CSV_FILE_EXT));
        try {
            Files.move(renamedFilePath, targetPath);
            log.info(" - " + targetPath.getFileName() + " - renamed");
        } catch (FileAlreadyExistsException e) {
            log.info(" - " + renamedFilePath + " - renamed, already exists");
            Files.delete(renamedFilePath);
        }
    }

}
