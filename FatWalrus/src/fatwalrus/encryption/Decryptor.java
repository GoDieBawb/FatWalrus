/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author Bob
 */
public class Decryptor {
    
        private Cipher cipher;
        
	public Decryptor() throws NoSuchAlgorithmException, NoSuchPaddingException {
            this.cipher = Cipher.getInstance("RSA");
	}
    
	// https://docs.oracle.com/javase/8/docs/api/java/security/spec/PKCS8EncodedKeySpec.html
	public PrivateKey getPrivate(String filename) throws Exception {
            byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
	}

	// https://docs.oracle.com/javase/8/docs/api/java/security/spec/X509EncodedKeySpec.html
	public PublicKey getPublic(String filename) throws Exception {
            byte[] keyBytes = Files.readAllBytes(new File(filename).toPath());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
	}
        
        public void decryptFile(byte[] input, File output, PrivateKey key) 
                throws IOException, GeneralSecurityException {
            this.cipher.init(Cipher.DECRYPT_MODE, key);
            writeToFile(output, this.cipher.doFinal(input));
	}
        
        public String decryptText(String msg, PrivateKey key)
                throws InvalidKeyException, UnsupportedEncodingException, 
                IllegalBlockSizeException, BadPaddingException {
            this.cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(msg)));
	}

    private void writeToFile(File output, byte[] toWrite)
                throws IllegalBlockSizeException, BadPaddingException, IOException {
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(toWrite);
            fos.flush();
            fos.close();
	}
    
}
