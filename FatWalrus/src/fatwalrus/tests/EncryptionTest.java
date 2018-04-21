/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.tests;

import fatwalrus.encryption.Decryptor;
import fatwalrus.encryption.Encryptor;
import fatwalrus.encryption.KeyGenerator;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 *
 * @author Bob
 */
public class EncryptionTest {
 
        private void generateKeys() {
            
            KeyGenerator gk;

            try {
                gk = new KeyGenerator(2048);
                gk.createKeys();
                gk.writeToFile("KeyPair/publicKey", gk.getPublicKey().getEncoded());
                gk.writeToFile("KeyPair/privateKey", gk.getPrivateKey().getEncoded());
            }

            catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
                System.err.println(e.getMessage());
            }
            
        }
        
        private void testMessage() throws Exception {
            
            Encryptor  enc            = new Encryptor();
            Decryptor  dec            = new Decryptor();
            PrivateKey privateKey     = enc.getPrivate("KeyPair/privateKey");
            PublicKey  publicKey      = enc.getPublic("KeyPair/publicKey");
            String     msg            = "This is a message that will be encrypted and decrypted.";
            String     encrypted_msg  = enc.encryptText(msg, publicKey);
            String     decrypted_msg  = dec.decryptText(encrypted_msg, privateKey);
            
            System.out.println("Original Message: " + msg + 
                "\nEncrypted Message: " + encrypted_msg
                + "\nDecrypted Message: " + decrypted_msg);

            if (new File("KeyPair/text.txt").exists()) {
                enc.encryptFile(enc.getFileInBytes(new File("KeyPair/text.txt")), 
                        new File("KeyPair/text_encrypted.txt"), publicKey);
                dec.decryptFile(enc.getFileInBytes(new File("KeyPair/text_encrypted.txt")),
                        new File("KeyPair/text_decrypted.txt"), privateKey);
            } 
            
            else {
                System.out.println("Create a file text.txt under folder KeyPair");
            }
            
        }
    
    	public static void main(String[] args) throws Exception {
            
            EncryptionTest et = new EncryptionTest();
            et.generateKeys();
            et.testMessage();

	}
    
}
