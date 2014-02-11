import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
 
import javax.xml.bind.DatatypeConverter;
 
import org.bouncycastle.jce.provider.BouncyCastleProvider;
 
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
 
public class GenerateCertificate {
 
        public static X509Certificate getCertificateFromFile(String file) {
                FileInputStream fis = null;
                try {
                        fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                }
                BufferedInputStream bis = new BufferedInputStream(fis);
 
                CertificateFactory cf = null;
                try {
                        cf = CertificateFactory.getInstance("X.509");
                } catch (CertificateException e) {
                        e.printStackTrace();
                }
                X509Certificate cert = null;
                try {
                        while (bis.available() > 0) {
                                cert = (X509Certificate) cf.generateCertificate(bis);
                        }
                } catch (CertificateException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return cert;
        }
 
        public X509Certificate generateCertificate(X509Certificate serveur,
                        X509CertInfo goodInfo, String dn, Set<String> ext, KeyPair pair,
                        KeyPair autorite, int days, String algorithm)
                        throws GeneralSecurityException, IOException {
 
                PrivateKey privkey = autorite.getPrivate();
                X509CertInfo info = new X509CertInfo();
                Calendar cal = Calendar.getInstance();
                Date from = cal.getTime();
                Date to = new Date(from.getTime() + (days * 86400000));
                CertificateValidity interval = new CertificateValidity(from, to);
                BigInteger sn = new BigInteger(64, new SecureRandom());
                X500Name owner = new X500Name(dn);
                X500Name ca = new X500Name(
                                "EMAIL=ssi@univ-rouen.fr, CN=SSISign, OU=SSI, O=Transchiffrement, L=Rouen, ST=Normandie, C=FR");
 
                info.set(X509CertInfo.VALIDITY, interval);
                info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
                info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
                info.set(X509CertInfo.ISSUER, new CertificateIssuerName(ca));
                info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
                info.set(X509CertInfo.VERSION, new CertificateVersion(
                                CertificateVersion.V3));
                if (goodInfo.get(X509CertInfo.EXTENSIONS) != null){
                        info.set(X509CertInfo.EXTENSIONS, goodInfo.get(X509CertInfo.EXTENSIONS));
                }
                AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
                info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
                // Sign the cert to identify the algorithm that's used.
                X509CertImpl cert = new X509CertImpl(info);
                cert.sign(privkey, algorithm);
                // Update the algorithm, and resign.
                algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
                info.set(CertificateAlgorithmId.NAME + "."
                                + CertificateAlgorithmId.ALGORITHM, algo);
                cert = new X509CertImpl(info);
                cert.sign(privkey, algorithm);
                return cert;
        }
 
        public void ecrire(String nomFic, String texte) {
                String adressedufichier = System.getProperty("user.dir") + "/cles/"
                                + nomFic;
 
                try {
                       
                        FileWriter fw = new FileWriter(adressedufichier, false);
 
                        BufferedWriter output = new BufferedWriter(fw);
 
                        output.write(texte);
 
                        output.flush();
 
                        output.close();
 
                } catch (IOException ioe) {
                        System.out.print("Erreur : ");
                        ioe.printStackTrace();
                }
 
        }
 
        public KeyPair LoadKeyPair(String path, String algorithm)
                        throws IOException, NoSuchAlgorithmException,
                        InvalidKeySpecException {
                // Read Public Key.
                File filePublicKey = new File(path + "/pubkey.pem");
                FileInputStream fis = new FileInputStream(path + "/pubkey.pem");
                byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
                fis.read(encodedPublicKey);
                fis.close();
                java.security.Security.addProvider(
                         new BouncyCastleProvider()
                );
                // Read Private Key.
                File filePrivateKey = new File(path + "/privkey.der");
                fis = new FileInputStream(path + "/privkey.der");
                byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
                fis.read(encodedPrivateKey);
                fis.close();
                // Generate KeyPair.
                KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                                encodedPrivateKey);
                PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
                PublicKey publicKey = null;
                return new KeyPair(publicKey, privateKey);
        }
 
        public GenerateCertificate(X509Certificate serveur) throws Exception{
                byte[] encoded = null;
                encoded = serveur.getEncoded();
                X509CertImpl caCertImpl = null;
                caCertImpl = new X509CertImpl(encoded);
                X509CertInfo caCertInfo = null;
                caCertInfo = (X509CertInfo) caCertImpl.get(X509CertImpl.NAME + "."
                                        + X509CertImpl.INFO);
                KeyPairGenerator keyPairGen = null;
                X509Certificate certif = null;
                KeyPair autorite = null;
                autorite = this.LoadKeyPair("cles", "RSA");
                keyPairGen = KeyPairGenerator.getInstance("RSA");
                keyPairGen.initialize(1024, new SecureRandom());
                KeyPair cles = keyPairGen.generateKeyPair();
                certif = this.generateCertificate(serveur, caCertInfo, serveur
                                        .getSubjectDN().toString(), serveur
                                        .getCriticalExtensionOIDs(), cles, autorite, 10,
                                        "SHA1withRSA");
                String s = new String(DatatypeConverter.printBase64Binary(certif
                                        .getEncoded()));
                int i = 1;
                char[] tab = s.toCharArray();
                String affichage = new String();
                for (char partie : tab) {
                        affichage = (affichage + "" + partie);
                        if (i % 64 == 0) {
                                affichage = (affichage + "\n");
                        }
                        i++;
                }
                this.ecrire("fake-cert.pem", new String(
                                        "-----BEGIN CERTIFICATE-----\n" + affichage
                                                        + "\n-----END CERTIFICATE-----\n"));
        }
 
       
       
       
       
       
        public static void main(String args[]) throws Exception {
                //X509Certificate       test = GenerateCertificate.getCertificateFromFile("cles/*.univ-rouen.fr.pem");
                //GenerateCertificate gen = new GenerateCertificate(test);
        }
}