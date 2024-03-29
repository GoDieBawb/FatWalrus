/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.encryption;

/**
 *
 * @author Bob
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyGenerator {

	private KeyPairGenerator keyGen;
	private KeyPair          pair;
	private PrivateKey       privateKey;
	private PublicKey        publicKey;

	public KeyGenerator(int keylength) throws NoSuchAlgorithmException, NoSuchProviderException {
            this.keyGen = KeyPairGenerator.getInstance("RSA");
            this.keyGen.initialize(keylength);
	}

	public void createKeys() {
            this.pair = this.keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
	}

	public PrivateKey getPrivateKey() {
            return this.privateKey;
	}

	public PublicKey getPublicKey() {
            return this.publicKey;
	}

	public void writeToFile(String path, byte[] key) throws IOException {
            File f = new File(path);
            f.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(key);
            fos.flush();
            fos.close();
	}

        private void testKeyPair() throws Exception{
            //Test Encrypt Decrypt
            Encryptor enc       = new Encryptor();
            Decryptor dec       = new Decryptor();
            String    sentence  = "Decrypt This Please";

            String encSent = enc.encryptText(sentence, getPublicKey());
            System.out.println("Encrypted: " + " " + encSent);

            String decSent = dec.decryptText(encSent, getPrivateKey());
            System.out.println("Decrypted: " + " " + decSent);    
        }        
        
}
