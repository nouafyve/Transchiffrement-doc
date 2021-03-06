package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ConnexionHTTPS extends Connexion {

	private String ipWeb;

	public ConnexionHTTPS(Socket socketClient, String ipWeb, int portWeb)
			throws Exception {
		super(socketClient, ipWeb, portWeb);
		this.ipWeb = ipWeb;
	}

	public void lancer() throws Exception {
		envoyerMessageClient("200 OK");

		char[] keyStorePasswordChar = Constantes.KEYSTORE_PASSWORD
				.toCharArray();

		KeyStore keyStore = KeyStore.getInstance("jks");
		keyStore.load(new FileInputStream(Constantes.KEYSTORE_FILE),
				keyStorePasswordChar);
		SSLContext ctx = SSLContext.getInstance("TLS");
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
				.getDefaultAlgorithm());

		kmf.init(keyStore, keyStorePasswordChar);
		ctx.init(kmf.getKeyManagers(), null, null);
		SSLSocketFactory factory = ctx.getSocketFactory();

		SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
				.getDefault();

		// Proxy <=> Server
		try {
			SSLSocket sslsocketClient = (SSLSocket) sslsocketfactory
					.createSocket(ipWeb, portWeb);
			sslsocketClient.setEnabledCipherSuites(sslsocketClient
					.getSupportedCipherSuites());
			sslsocketClient.setSoTimeout(50000);
			X509Certificate serveurCert = (X509Certificate) sslsocketClient
					.getSession().getPeerCertificates()[0];
			BufferedOutputStream requeteAuServeurWeb = new BufferedOutputStream(
					sslsocketClient.getOutputStream(), Constantes.BUFFER_SIZE);
			BufferedInputStream reponseDuServeurWeb = new BufferedInputStream(
					sslsocketClient.getInputStream(), Constantes.BUFFER_SIZE);

			// Génération de fake-cert et importation dans le keystore du
			// nouveau
			// pkcs12.
			File test = new File(Constantes.KEYSTORE_PATH+ipWeb+".jks"); 
			if( !test.exists() ) {
			//if(!keyStore.containsAlias(ipWeb)){
//			Process p = Runtime.getRuntime().exec(
//					"keytool -list -v -keystore keystore_ok.jks -storepass 000000 -alias "+ipWeb);
//			p.waitFor();
//			if(p.exitValue() == 0){
//				System.out.println("Certificat déjà stocké");
//			}
//			else{
				GenerationCertificat gen = new GenerationCertificat(serveurCert, ipWeb);
			}
				// Chargement du nouveau keystore dans la variable Java.
				keyStore.load(new FileInputStream(Constantes.KEYSTORE_PATH+ipWeb+".jks"),
						keyStorePasswordChar);
				// Réinitiation des objets utiles à la création du socket avec le
				// bon
				// keystore.
				kmf.init(keyStore, keyStorePasswordChar);
				ctx.init(kmf.getKeyManagers(), null, null);
				factory = ctx.getSocketFactory();
			

			SSLSocket sslSocket = (SSLSocket) factory.createSocket(
					socketClient, ipClient, portClient, false);
			sslSocket.setEnabledCipherSuites(sslSocket
					.getSupportedCipherSuites());
			sslSocket.setUseClientMode(false);
			sslsocketClient.setSoTimeout(50000);
			BufferedOutputStream outputStreamClient = new BufferedOutputStream(
					sslSocket.getOutputStream(), Constantes.BUFFER_SIZE);

			BufferedInputStream in = new BufferedInputStream(
					sslSocket.getInputStream(), Constantes.BUFFER_SIZE);

			byte[] buffer = new byte[4096];

			Thread firstThread = new Thread(new Transfert(
					sslSocket.getInputStream(), requeteAuServeurWeb, sslSocket
							.getRemoteSocketAddress().toString(),
					sslsocketClient.getRemoteSocketAddress().toString()));
			firstThread.start();
			Thread myThread = new Thread(new Transfert(reponseDuServeurWeb,
					outputStreamClient, sslsocketClient
							.getRemoteSocketAddress().toString(), sslSocket
							.getRemoteSocketAddress().toString()));
			myThread.start();
		} catch (SocketException e) {
			System.out.println("Réseau inaccessible");
		}
	}

	public void envoyerMessageClient(String message) throws Exception {
		StringBuffer response = new StringBuffer();
		response.append("HTTP/1.0 ").append(message).append("\r\n");
		response.append("Host: " + ipClient + ":" + portClient + "\r\n");
		response.append("Proxy-agent: ssi_transchiffrement/1.0\r\n");
		response.append("\r\n");
		sortieClient.write(response.toString().getBytes());
		sortieClient.flush();
	}
}
