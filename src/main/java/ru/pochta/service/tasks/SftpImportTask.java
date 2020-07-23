package ru.pochta.service.tasks;

import com.opencsv.CSVWriter;
import com.opencsv.bean.*;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.pochta.config.YmlProperties;
import ru.pochta.model.IHerbManifestEntry;
import ru.pochta.model.UniversalManifestEntry;
import ru.pochta.repository.IHerbManifestEntryRepository;
import ru.pochta.repository.UniversalManifestEntryRepository;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Slf4j
public class SftpImportTask implements Runnable {

    private final IHerbManifestEntryRepository iHerbRepository;
    private final UniversalManifestEntryRepository universalRepository;
    private final DataSource dataSource;

    private static final String CSV_FILE_EXT = ".csv";
    private static final String HASH_SUM_FILE_NAME = "checksums.txt";
    private static final String ATTRIBUTES_FILE_NAME = "attributes.txt";
    private static final String DOWNLOADED_FILES_DIR_NAME = "01_downloaded";
    private static final String CHANGED_FILES_DIR_NAME = "02_changed";
    private static final String RENAMED_FILES_DIR_NAME = "03_renamed";
    private static final String REMAPPED_FILES_DIR_NAME = "04_remapped";
    private static final String CREATED_FILES_DIR_NAME = "05_created";
    private static final String EXPORTED_FILES_DIR_NAME = "06_exported";

    private final YmlProperties properties;
    private final String remapQuery;
    private final String downloadedFilesPath;
    private final String changedFilesPath;
    private final String renamedFilesPath;
    private final String remappedFilesPath;
    private final String createdFilesPath;
    private final String exportedFilesPath;


    @Autowired
    public SftpImportTask(YmlProperties properties,
                          IHerbManifestEntryRepository iHerbRepository,
                          UniversalManifestEntryRepository universalRepository,
                          DataSource dataSource
    ) throws IOException {
        this.iHerbRepository = iHerbRepository;
        this.universalRepository = universalRepository;
        this.dataSource = dataSource;
        this.properties = properties;
        this.remapQuery = Files.readString(Paths.get(properties.getRemapQueryFilePath()), StandardCharsets.UTF_8);
        this.downloadedFilesPath = properties.getLocalFilesDirPath() + "/" + DOWNLOADED_FILES_DIR_NAME;
        this.changedFilesPath = properties.getLocalFilesDirPath() + "/" + CHANGED_FILES_DIR_NAME;
        this.renamedFilesPath = properties.getLocalFilesDirPath() + "/" + RENAMED_FILES_DIR_NAME;
        this.remappedFilesPath = properties.getLocalFilesDirPath() + "/" + REMAPPED_FILES_DIR_NAME;
        this.createdFilesPath = properties.getLocalFilesDirPath() + "/" + CREATED_FILES_DIR_NAME;
        this.exportedFilesPath = properties.getLocalFilesDirPath() + "/" + EXPORTED_FILES_DIR_NAME;
    }

    /**
     * Main runner
     */
    @Override
    public void run() {
        try {
            log.info("=== START IMPORTING AND CHECKING FILES ===");
            checkLocalDirs();
            download();
            checkSums();
            remap();
//            export();
            log.info("=== END IMPORTING AND CHECKING FILES ===");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Check local dirs. If there are no dirs then create
     */
    private void checkLocalDirs() {
        Stream.of(
                properties.getLocalFilesDirPath(),
                downloadedFilesPath,
                changedFilesPath,
                renamedFilesPath,
                remappedFilesPath,
                createdFilesPath,
                exportedFilesPath
        ).forEach(path -> {
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                pathFile.mkdir();
            }
        });
    }

    /**
     * Downloads files from sftp. Skip file if:
     * - the file has non-csv extension
     * - the file has not been edited since the last processing (logged in attributes.txt)
     * - thi file is uploading into sftp right now
     *
     * @throws IOException
     */
    private void download() throws IOException {
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
                            sftpClient.get(item.getPath(), downloadedFilesPath);
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
     * then move the file into /changed dir (checksums are logging in checksums.txt)
     *
     * @throws IOException
     */
    private void checkSums() throws IOException {
        File hashSumsFile = getHashSumsFile();
        Set<String> hashSet = new HashSet<>(Files.readAllLines(hashSumsFile.toPath()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(hashSumsFile, true));
        log.info("Check and move files start...");
        try (Stream<Path> paths = Files.walk(Paths.get(downloadedFilesPath))) {
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

    /**
     * Remap csv-to-csv according to the given rules
     */
    private void remap() {
        HeaderColumnNameMappingStrategy<IHerbManifestEntry> strategy
                = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(IHerbManifestEntry.class);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        log.info("Remap files start...");
        try (Stream<Path> paths = Files.walk(Paths.get(changedFilesPath))) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            CsvToBean<IHerbManifestEntry> csvToBean = new CsvToBeanBuilder<IHerbManifestEntry>(br)
                                    .withMappingStrategy(strategy)
                                    .withIgnoreLeadingWhiteSpace(true)
                                    .build();
                            List<IHerbManifestEntry> iHerbEntries = csvToBean.parse();
                            iHerbRepository.saveAll(iHerbEntries);
                            template.update(remapQuery);
                            Path createdPath = Files.createFile(
                                    Paths.get(createdFilesPath + "/" +
                                            "um_" + path.toFile().getName().replace(CSV_FILE_EXT, "_" + LocalDateTime.now().toString() + CSV_FILE_EXT))
                            );
                            exportToCSV(createdPath, universalRepository.findAll());
                            universalRepository.deleteAll();
                            iHerbRepository.deleteAll();
                            moveRemappedFiles(path);
                        } catch (IOException | RuntimeException e) {
                            log.error(e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.info("Remap files end!");
    }

    private void exportToCSV(Path createdPath, List<UniversalManifestEntry> universalManifestEntries) {
        try (var writer = Files.newBufferedWriter(createdPath, StandardCharsets.UTF_8)) {
            StatefulBeanToCsv<UniversalManifestEntry> beanToCsv = new StatefulBeanToCsvBuilder<UniversalManifestEntry>(writer)
                    .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                    .build();
            beanToCsv.write(universalManifestEntries);
            log.info(" - " + createdPath.getFileName() + " - created");
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException |
                IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void moveRemappedFiles(Path remappedFilePath) throws IOException {
        Path targetPath = Paths.get(remappedFilesPath + "/" + remappedFilePath.getFileName());
        try {
            Files.move(remappedFilePath, targetPath);
            log.info(" - " + targetPath.getFileName() + " - remapped");
        } catch (FileAlreadyExistsException e) {
            log.info(" - " + remappedFilePath + " - remapped, already exists");
            Files.delete(remappedFilePath);
        }
    }

    /**
     * Export files into sftp
     */
    private void export() {
        // exporting
    }

}
