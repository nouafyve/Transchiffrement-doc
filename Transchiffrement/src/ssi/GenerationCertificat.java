package ssi;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;

public class GenerationCertificat {

	public GenerationCertificat(X509Certificate serveur, String ip) throws Exception {
			// Création du bi-clé RSA pour fake-cert.
			Process p = Runtime.getRuntime().exec(
					"openssl genpkey -algorithm rsa -out "+Constantes.PROJECT_FOLDER +"cles/fakeCle.pem");
			p.waitFor();
			// Création de la requête de signature avec le bi-clé généré en
			// utilisant les informations du certificat serveur.
			System.out.println(serveur.getSubjectDN().toString().replace(", ", "/"));
			p = Runtime.getRuntime().exec(
					"openssl req -new -key "+Constantes.PROJECT_FOLDER +"cles/fakeCle.pem -out "+ Constantes.PROJECT_FOLDER +"cles/fake-cert.csr -subj /"
							+ serveur.getSubjectDN().toString().replace(", ", "/").replace(" ", ""));
			p.waitFor();
			//CN=*.facebook.com/O="Facebook/Inc."/L=Palo Alto/ST=California/C=US
			// Signature de fake-cert avec SSISign.
			p = Runtime
					.getRuntime()
					.exec("openssl x509 -req -days 365 -in " + Constantes.PROJECT_FOLDER + "cles/fake-cert.csr -CA " + Constantes.PROJECT_FOLDER + "cles/SSISign.crt -CAcreateserial -CAkey " + Constantes.PROJECT_FOLDER + "cles/cles_SSISign.pem -out " + Constantes.PROJECT_FOLDER + "cles/fake-cert.crt");
			p.waitFor();
			
			// Récupération du contenu de fake-cert pour la concaténation.
			File fileIn = new File(Constantes.PROJECT_FOLDER +"cles/fake-cert.crt");
			FileInputStream fis = new FileInputStream(fileIn);
			byte[] buffer = new byte[fis.available()];
			fis.read(buffer);
			String stringIn = new String(buffer);
			fis.close(); //
			// Récupération du contenu du fichier du bi-clé pour la concaténation.
			fileIn = new File(Constantes.PROJECT_FOLDER +"cles/fakeCle.pem");
			fis = new FileInputStream(fileIn);
			buffer = new byte[fis.available()];
			fis.read(buffer);
			stringIn += new String(buffer);
			fis.close();
			// Récupération du contenu de SSISign pour la concaténation.
			fileIn = new File(Constantes.PROJECT_FOLDER +"cles/SSISign.crt");
			fis = new FileInputStream(fileIn);
			buffer = new byte[fis.available()];
			fis.read(buffer);
			stringIn += new String(buffer);
			fis.close();
			// Création du fichier de concaténation pour créer le pkcs12.
			File fileOut = new File(Constantes.PROJECT_FOLDER + "cles/concat.txt");
			FileOutputStream fos = new FileOutputStream(fileOut);
			fos.write(stringIn.getBytes());
			fos.close();
			// Création d'un fichier pkcs12 à partir du fichier de concaténation
			// précédent.
			p = Runtime
					.getRuntime()
					.exec("openssl pkcs12 -export -in " + Constantes.PROJECT_FOLDER + "cles/concat.txt -out " + Constantes.PROJECT_FOLDER + "cles/fakestore.p12 -password pass:000000");
			p.waitFor();
			// Suppression de l'ancien pkcs12 présent dans le keystore.
	//		p = Runtime
	//				.getRuntime()
	//				.exec("keytool -delete -alias 1 -keystore keystore_ok.jks -storepass 000000");
	//		p.waitFor();
			// Importation du nouveau pkcs12 dans le keystore utilisé pour les
			// échanges de certificats.
			p = Runtime
					.getRuntime()
					.exec("cp "+Constantes.KEYSTORE_PATH +"/vierge.jks "+Constantes.KEYSTORE_PATH +ip+".jks");
			p.waitFor();
			p = Runtime
					.getRuntime()
					.exec("keytool -importkeystore -srckeystore " + Constantes.PROJECT_FOLDER + "cles/fakestore.p12 -srcstoretype pkcs12 -srcstorepass 000000 -destkeystore "+Constantes.KEYSTORE_PATH +ip+".jks -deststoretype jks -deststorepass 000000 -srcalias 1 -destalias 1");
			p.waitFor();
		}

}