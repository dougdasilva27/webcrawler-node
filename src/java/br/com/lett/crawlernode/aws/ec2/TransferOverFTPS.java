package br.com.lett.crawlernode.aws.ec2;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TransferOverFTPS {
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
        return _sendFileAsync(localFile, remoteFile, deleteLocalFile);
    }

    public Thread sendFileAsyncAndCloseConnection(String localFile, String remoteFile) {
        return _sendFileAsync(localFile, remoteFile, false);
    }

    private Thread _sendFileAsync(String localFile, String remoteFile, boolean deleteLocalFile) {
        Thread sendFileThread = new Thread(() -> {
            try {
                sendFile(localFile, remoteFile);
                disconnect();
                if (deleteLocalFile) {
                    File fileToDelete = new File(localFile);
                    if (fileToDelete.exists()) {
                        fileToDelete.delete();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
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

        InputStream inputStream = new FileInputStream(localFile);
        OutputStream outputStream = ftpClient.storeFileStream(remoteFileName);

        byte[] bytesIn = new byte[4096];

        int read;
        while ((read = inputStream.read(bytesIn)) != -1) {
            outputStream.write(bytesIn, 0, read);
        }

        inputStream.close();
        outputStream.close();
    }

    public void disconnect() throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }
}

