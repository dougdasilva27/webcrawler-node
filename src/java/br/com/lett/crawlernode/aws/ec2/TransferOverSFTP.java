package br.com.lett.crawlernode.aws.ec2;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class TransferOverSFTP {
    JSch jsch;
    Session session;

    public TransferOverSFTP(String user, String host, String sslKeysBucket, String sslKey, Integer port) throws AmazonServiceException, IOException, JSchException {
        createJschSession(user, host, sslKeysBucket, sslKey, port);
    }

    public TransferOverSFTP(String user, String host, String sslKeysBucket, String sslKey) throws AmazonServiceException, IOException, JSchException {
        createJschSession(user, host, sslKeysBucket, sslKey, 22);
    }

    private static void getSSLKeyFromS3(String sslKeysBucket, String sslKey, File sslKeyFile) throws AmazonServiceException, IOException {
        final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        S3Object o = s3.getObject(sslKeysBucket, sslKey);
        S3ObjectInputStream s3is = o.getObjectContent();
        try (FileOutputStream fos = new FileOutputStream(sslKeyFile)) {
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
        }
    }


    private void createJschSession(String user, String host, String sslKeysBucket, String sslKey, Integer port) throws AmazonServiceException, IOException, JSchException {
        jsch = new JSch();
        File sslKeyFile = new File(sslKey);
        if (!sslKeyFile.exists()) {
            getSSLKeyFromS3(sslKeysBucket, sslKey, sslKeyFile);
        }
        jsch.addIdentity(sslKeyFile.getAbsolutePath());
        session = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
    }

    public void sendFile(String localFile, String remoteFile) throws SftpException, JSchException {
        // creates parent path
        ChannelExec execChannel = (ChannelExec) session.openChannel("exec");
        execChannel.setCommand("mkdir -p " + Paths.get(remoteFile).getParent());
        execChannel.setInputStream(null);
        execChannel.connect();
        execChannel.disconnect();

        // uploads file
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(localFile, remoteFile);
        sftpChannel.disconnect();

    }

    public void connect() throws JSchException {
        session.connect();
    }

    public void disconnect() {
        session.disconnect();
    }
}

