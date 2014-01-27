import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


/** 
 * Cette classe propose des méthodes permettant de chiffrer et déchiffrer des 
 * messages avec l'algorithme RSA. Le message doit cependant être plus petit
 * que KEY_SIZE.
 */
public class MyRSA {
  public final static int KEY_SIZE = 2048;  // [512..2048]

  private RSAPublicKey publicKey;
  private RSAPrivateKey privateKey;
  
  public MyRSA() {
  }

 
 
  
  public RSAPublicKey getPublicKey() {
	    return publicKey;
	  }
	  
	  
	  public byte[] getPublicKeyInBytes() {
	    return publicKey.getEncoded();
	  }
	  
	  
	  public RSAPrivateKey getPrivateKey() {
	    return privateKey;
	  }
	  

	  public byte[] getPrivateKeyInBytes() {
	    return privateKey.getEncoded();
	  }  
	  
	  
	  public void setPublicKey(RSAPublicKey publicKey) {
	    this.publicKey = publicKey;
	  }
	  
	  
	  public void setPublicKey(byte[] publicKeyData) {
	    try {
	      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyData);
	      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	      publicKey = (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
	    }
	    catch (Exception e) {System.out.println(e);} 
	  }
	  
	  
	  public void setPrivateKey(RSAPrivateKey privateKey) {
	    this.privateKey = privateKey;
	  }
	  
	  
	  public void setPrivateKey(byte[] privateKeyData) {
	    try {
	      PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyData);
	      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	      privateKey = (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
	    }
	    catch (Exception e) {System.out.println(e);} 
	  }    
    
  
 

  public void generateKeyPair() {
	    try {
	      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
	      keyPairGen.initialize(KEY_SIZE, new SecureRandom());
	      KeyPair kp = keyPairGen.generateKeyPair();
	      publicKey = (RSAPublicKey)kp.getPublic();
	      privateKey = (RSAPrivateKey)kp.getPrivate();
	    }
	    catch (Exception e) {System.out.println(e);} 
  }

  
  
  public byte[] crypt(byte[] plaintext) {
	    return crypt(new BigInteger(addOneByte(plaintext))).toByteArray();
	  }
	  
	    
	  public byte[] crypt(String plaintext) {
	    return crypt(plaintext.getBytes());
	  }
	 
	 
	  public byte[] decryptInBytes(byte[] ciphertext) {
	    return removeOneByte(decrypt(new BigInteger(ciphertext)).toByteArray());
	  }    
	  
	  
	  public String decryptInString(byte[] ciphertext) {
	    return new String(decryptInBytes(ciphertext));
	  }


  /**
   * Cette méthode permet de tester le bon fonctionnement des autres.
   */
  public static void main(String[] args) {
    String plaintext = "Test 1,2,3 Soleil !";
    System.out.println("Texte envoyé par le client = " + plaintext);
    MyRSA rsaProxy = new MyRSA();
    rsaProxy.generateKeyPair();
    byte[] publicKeyProxy = rsaProxy.getPublicKeyInBytes();
    byte[] privateKeyProxy = rsaProxy.getPrivateKeyInBytes();
    byte[] ciphertext = rsaProxy.crypt(plaintext);   
    System.out.println("Texte chiffré transitant entre le client et le proxy = " + new BigInteger(ciphertext));
    rsaProxy.setPublicKey(publicKeyProxy);
    rsaProxy.setPrivateKey(privateKeyProxy);    
    String plaintext2 = rsaProxy.decryptInString(ciphertext);
    System.out.println("Texte déchiffré par le proxy = " + plaintext2);
    if (!plaintext2.equals(plaintext)) System.out.println("Error: plaintext2 != plaintext");
        
    MyRSA rsa = new MyRSA();
    rsa.generateKeyPair();
    byte[] publicKey = rsa.getPublicKeyInBytes();
    byte[] privateKey = rsa.getPrivateKeyInBytes();
    byte[] ciphertext2 = rsa.crypt(plaintext2);   
    System.out.println("Texte chiffré transitant entre le proxy et le  = " + new BigInteger(ciphertext2));
    rsa.setPublicKey(publicKey);
    rsa.setPrivateKey(privateKey);    
    String plaintext3 = rsa.decryptInString(ciphertext2);
    System.out.println("Texte déchiffré par le  = " + plaintext3);
    if (!plaintext3.equals(plaintext2)) System.out.println("Error: plaintext3 != plaintext2");
    
  }
  

 
  
  private BigInteger crypt(BigInteger plaintext) {
	    return plaintext.modPow(publicKey.getPublicExponent(), publicKey.getModulus());
	  }
	  
	  
	  private BigInteger decrypt(BigInteger ciphertext) {
	    return ciphertext.modPow(privateKey.getPrivateExponent(), privateKey.getModulus());
	  } 
  

  /**
   * Ajoute un byte de valeur 1 au début du message afin d'éviter que ce dernier
   * ne corresponde pas à un nombre négatif lorsqu'il sera transformé en
   * BigInteger.
   */
  private static byte[] addOneByte(byte[] input) {
    byte[] result = new byte[input.length+1];
    result[0] = 1;
    for (int i = 0; i < input.length; i++) {
      result[i+1] = input[i];
    }
    return result;
  }
  
  
  /**
   * Retire le byte ajouté par la méthode addOneByte.
   */
  private static byte[] removeOneByte(byte[] input) {
    byte[] result = new byte[input.length-1];
    for (int i = 0; i < result.length; i++) {
      result[i] = input[i+1];
    }
    return result;
  }
}