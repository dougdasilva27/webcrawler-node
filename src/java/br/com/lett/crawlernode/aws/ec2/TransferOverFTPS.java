package br.com.lett.crawlernode.aws.ec2;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public boolean makeDirectories(String dirPath) throws IOException {
        String[] pathElements = dirPath.split("/");
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

    public void sendFile(String localFile, String remoteFile) throws IOException {
        Path remoteFilePath = Paths.get(remoteFile);
        String remoteParentDirectory = remoteFilePath.getParent().toString();
        String remoteFileName = remoteFilePath.getFileName().toString();

        if (!ftpClient.changeWorkingDirectory(remoteParentDirectory)) {
            makeDirectories(remoteParentDirectory);
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

