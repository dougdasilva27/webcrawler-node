package br.com.lett.crawlernode.aws.ec2;

import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TransferOverFTPS {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalConfigurations.class);
    FTPSClient ftpClient;

    public TransferOverFTPS(String username, String password, String host) throws IOException {
        ftpClient = new FTPSClient();
        ftpClient.connect(host, 21);
        ftpClient.login(username, password);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.execPROT("P");
    }

    // https://www.codejava.net/java-se/ftp/creating-nested-directory-structure-on-a-ftp-server
    public boolean mkdirsAndCWD(String dirPath) throws IOException {
        String[] pathElements = StringUtils.removeStart(dirPath, "/").split("/");
        if (pathElements.length > 0) {
            for (String singleDir : pathElements) {
                boolean existed = ftpClient.changeWorkingDirectory(singleDir);
                if (!existed) {
                    boolean created = ftpClient.makeDirectory(singleDir);
                    if (created) {
                        ftpClient.changeWorkingDirectory(singleDir);
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Thread sendFileAsyncAndCloseConnection(String localFile, String remoteFile, boolean deleteLocalFile) {
        return sendFileAsync(localFile, remoteFile, deleteLocalFile);
    }

    public Thread sendFileAsyncAndCloseConnection(String localFile, String remoteFile) {
        return sendFileAsync(localFile, remoteFile, false);
    }

    private Thread sendFileAsync(String localFile, String remoteFile, boolean deleteLocalFile) {
        Thread sendFileThread = new Thread(() -> {
            try {
                sendFile(localFile, remoteFile);
                disconnect();
                if (deleteLocalFile) {
                    File fileToDelete = new File(localFile);
                    if (fileToDelete.exists()) {
                        Files.delete(Paths.get(fileToDelete.getAbsolutePath()));
                    }
                }
            } catch (IOException e) {
                Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
            }
        });
        sendFileThread.start();
        return sendFileThread;
    }

    public void sendFile(String localFile, String remoteFile) throws IOException {
        Path remoteFilePath = Paths.get(remoteFile);
        String remoteParentDirectory = remoteFilePath.getParent().toString();
        String remoteFileName = remoteFilePath.getFileName().toString();

        if (!ftpClient.changeWorkingDirectory(remoteParentDirectory)) {
            mkdirsAndCWD(remoteParentDirectory);
            ftpClient.changeWorkingDirectory(remoteParentDirectory);
        }

        OutputStream outputStream;
        try (InputStream inputStream = new FileInputStream(localFile)) {
            outputStream = ftpClient.storeFileStream(remoteFileName);

            byte[] bytesIn = new byte[4096];

            int read;
            while ((read = inputStream.read(bytesIn)) != -1) {
                outputStream.write(bytesIn, 0, read);
            }
        }
        outputStream.close();
    }

    public void disconnect() throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }
}

