
/**
 * Classe contendo métodos para cifrar e decifrar um array de bytes.
 * O método de cifra usado é o AES128, que corresponde a um tamanho de bloco
 * de 16 bytes, usado internamento no algoritmo.
 * 
 * @author Samir Leão
 */

package br.com.lett.crawlernode.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SymmetricCipher {
	
	public final int BLOCK_SIZE = 16;

	public SecretKeySpec generateKeySpec(String userPassword) {
		byte[] raw = userPassword.getBytes();
		
		return (new SecretKeySpec(raw, "AES"));
	}

	public IvParameterSpec generateIv()
			throws NoSuchAlgorithmException {

		//generate random IV using block size
		byte[] ivData = new byte[BLOCK_SIZE];
		SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
		rnd.nextBytes(ivData);
		
		return (new IvParameterSpec(ivData));
	}

	public byte[] encrypt(byte[] message, IvParameterSpec iv, SecretKeySpec secretKeySpec) {

		try {
			
			// define the type and padding
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			
			// initialize the encryption
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
			
			//  encrypt the raw containing the message
			byte[] encrypted = cipher.doFinal( message );
			
			return encrypted;
			
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;
	}

	public byte[] decrypt(byte[] encrypted, IvParameterSpec iv, SecretKeySpec secretKeySpec) {

		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
			byte[] msg = cipher.doFinal( encrypted );
			return msg;
			
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;

	} 

}
