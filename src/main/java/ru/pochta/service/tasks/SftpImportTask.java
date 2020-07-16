package ru.pochta.service.tasks;

import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.pochta.config.YmlProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Slf4j
public class SftpImportTask implements Runnable {

    private YmlProperties properties;
    private final String CSV_FILE_EXT = ".csv";
    private final String HASH_SUM_FILE_NAME = "hashsum.txt";
    private final String CHANGED_FILES_DIR_NAME = "changed";
    private final String CURRENT_FILES_DIR_NAME = "current";
    private final String currentFilesPath;
    private final String changedFilesPath;


    @Autowired
    public SftpImportTask(YmlProperties properties) {
        this.properties = properties;
        this.currentFilesPath = properties.getLocalFilesDirPath() + "/" + CURRENT_FILES_DIR_NAME;
        this.changedFilesPath = properties.getLocalFilesDirPath() + "/" + CHANGED_FILES_DIR_NAME;
    }

    @Override
    public void run() {
        try {

            System.out.println("=== START IMPORTING AND CHECKING FILES ===");

            checkLocalDirs();
            downloadAllFiles();
            checkSums();

            System.out.println("=== END IMPORTING AND CHECKING FILES ===");

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void checkLocalDirs() {
        File localFilesDir = new File(properties.getLocalFilesDirPath());
        if (!localFilesDir.exists()) {
            localFilesDir.mkdir();
        }
        File changedFilesDir = new File(changedFilesPath);
        if (!changedFilesDir.exists()) {
            changedFilesDir.mkdir();
        }
        File currentFilesDir = new File(currentFilesPath);
        if (!currentFilesDir.exists()) {
            currentFilesDir.mkdir();
        }
    }

    private void checkSums() throws IOException {
        File hashSumsFile = getHashSumsFile();
        Set<String> hashSet = new HashSet<>(Files.readAllLines(hashSumsFile.toPath()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(hashSumsFile, true));
        List<Path> filesToRemove = new ArrayList<>();
        log.info("Check and move files...");
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
                                filesToRemove.add(path);
                            }
                        } catch (IOException | NoSuchAlgorithmException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        for (Path fileToRemove : filesToRemove) {
            Files.delete(fileToRemove);
        }
        log.info("Current dir was cleared!");

        writer.close();
    }

    private void moveChangedFile(Path changedFilePath, String hex) throws IOException {
        Path targetPath = Paths.get(changedFilesPath +
                "/" + changedFilePath.getFileName().toString().replace(CSV_FILE_EXT, "_" + hex + CSV_FILE_EXT));
        Files.move(changedFilePath, targetPath);
        log.info("Changed file detected and moved: " + targetPath.getFileName());
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

    private void downloadAllFiles() throws IOException {
        SSHClient sshClient = setupSshj();
        SFTPClient sftpClient = sshClient.newSFTPClient();
        List<RemoteResourceInfo> lsInfo = sftpClient.ls(properties.getRemoteFilesDirPath());
        for (RemoteResourceInfo item : lsInfo) {
            sftpClient.get(item.getPath(), currentFilesPath);
            log.info("Downloaded file: " + item.getName());
        }
        sftpClient.close();
        sshClient.disconnect();
    }

    private SSHClient setupSshj() throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(properties.getHostname());
        client.authPassword(properties.getUsername(), properties.getPassword());
        return client;
    }
}
