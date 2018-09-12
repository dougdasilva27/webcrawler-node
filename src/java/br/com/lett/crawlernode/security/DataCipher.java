package br.com.lett.crawlernode.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

/**
 * 
 * @author Samir Leao
 *
 */
public class DataCipher {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataCipher.class);
  private SymmetricCipher symmetricCipher;

  public DataCipher() {
    symmetricCipher = new SymmetricCipher();
  }

  public String fetchRemoteKey(String url) {
    URL urlObject = null;
    URLConnection con = null;
    InputStream is = null;

    try {
      urlObject = new URL(url);
      con = urlObject.openConnection();
      is = con.getInputStream();

      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      // trimming the key to 128bits
      return br.readLine().substring(0, 16);

    } catch (Exception e) {
      Logging.printLogError(LOGGER, "Erro ao pegar chave da Amazon");
      Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
    }

    return null;
  }

  public String encryptData(String userPassword, String data) throws IOException, NoSuchAlgorithmException, InvalidKeyException {

    // generate a key spec using the key passed
    SecretKeySpec secretKeySpec = symmetricCipher.generateKeySpec(userPassword);

    // generate an iv
    IvParameterSpec iv = symmetricCipher.generateIv();

    // encrypt the chunck
    byte[] encryptedChunck = symmetricCipher.encrypt(data.getBytes(), iv, secretKeySpec);

    // create the resulting byte array
    byte[] result = concatenate(iv.getIV(), encryptedChunck);

    return Base64.encodeBase64String(result);

  }

  public String decryptData(String userPassword, String encryptedData) throws IOException {

    byte[] encryptedBytes = Base64.decodeBase64(encryptedData);
    byte[] ivBytes = new byte[symmetricCipher.BLOCK_SIZE];
    byte[] dataChunck = new byte[encryptedBytes.length - symmetricCipher.BLOCK_SIZE];

    // generate the key spec
    SecretKeySpec secretKeySpec = symmetricCipher.generateKeySpec(userPassword);

    // read the iv bytes
    for (int i = 0; i < symmetricCipher.BLOCK_SIZE; i++) {
      ivBytes[i] = encryptedBytes[i];
    }
    IvParameterSpec iv = new IvParameterSpec(ivBytes);

    // read the encrypted portion
    for (int i = symmetricCipher.BLOCK_SIZE, j = 0; i < encryptedBytes.length; i++, j++) {
      dataChunck[j] = encryptedBytes[i];
    }

    // decrypt the data chunck
    byte[] decryptedChunck = symmetricCipher.decrypt(dataChunck, iv, secretKeySpec);

    return new String(decryptedChunck);
  }

  private byte[] concatenate(byte[] byteArray1, byte[] byteArray2) {
    byte[] result = new byte[byteArray1.length + byteArray2.length];

    for (int i = 0; i < byteArray1.length; i++) {
      result[i] = byteArray1[i];
    }
    for (int i = byteArray1.length, j = 0; i < result.length; i++, j++) {
      result[i] = byteArray2[j];
    }

    return result;
  }

}
