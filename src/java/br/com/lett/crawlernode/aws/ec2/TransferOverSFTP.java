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
import java.util.Properties;

public class TransferOverSFTP {
    JSch jsch;
    Session session;

    public TransferOverSFTP(String user, String host, String SSLKeysBucket, String SSLKey, Integer port) throws AmazonServiceException, IOException, JSchException {
        createJschSession(user, host, SSLKeysBucket, SSLKey, port);
    }

    public TransferOverSFTP(String user, String host, String SSLKeysBucket, String SSLKey) throws AmazonServiceException, IOException, JSchException {
        createJschSession(user, host, SSLKeysBucket, SSLKey, 22);
    }

    private static void getSSLKeyFromS3(String SSLKeysBucket, String SSLKey, File SSLKeyFile) throws AmazonServiceException, IOException {
        final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
        S3Object o = s3.getObject(SSLKeysBucket, SSLKey);
        S3ObjectInputStream s3is = o.getObjectContent();
        try (FileOutputStream fos = new FileOutputStream(SSLKeyFile)) {
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
        }
    }


    private void createJschSession(String user, String host, String SSLKeysBucket, String SSLKey, Integer port) throws AmazonServiceException, IOException, JSchException {
        jsch = new JSch();
        File SSLKeyFile = new File(SSLKey);
        if (!SSLKeyFile.exists()) {
            getSSLKeyFromS3(SSLKeysBucket, SSLKey, SSLKeyFile);
        }
        jsch.addIdentity(SSLKeyFile.getAbsolutePath());
        session = jsch.getSession(user, host, port);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
    }

    public void sendFile(String localFile, String remoteFile) throws SftpException, JSchException {
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

