package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ConnexionHTTPS extends Connexion {

	private String ipWeb;
	public ConnexionHTTPS(Socket socketClient, String ipWeb, int portWeb) throws Exception {
		super(socketClient, ipWeb, portWeb);
		this.ipWeb = ipWeb;
	}

	public void lancer() throws Exception {
		envoyerMessageClient("200 OK");
		System.out.println("HTTPS SSL/TLS : " + ipWeb + " " + portWeb);
		char[] keyStorePasswordChar = Constantes.KEYSTORE_PASSWORD.toCharArray();

		KeyStore keyStore = KeyStore.getInstance("jks");
		keyStore.load(new FileInputStream(Constantes.KEYSTORE_FILE), keyStorePasswordChar);
		SSLContext ctx = SSLContext.getInstance("TLS");
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		kmf.init(keyStore, keyStorePasswordChar);
		ctx.init(kmf.getKeyManagers(), null, null);
		SSLSocketFactory factory = ctx.getSocketFactory();

		SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

		// Proxy <=> Server
		SSLSocket sslsocketClient = (SSLSocket) sslsocketfactory.createSocket(ipWeb, portWeb);
		sslsocketClient.setEnabledCipherSuites(sslsocketClient.getSupportedCipherSuites());
		X509Certificate serveurCert = (X509Certificate) sslsocketClient.getSession().getPeerCertificates()[0];
		BufferedOutputStream requeteAuServeurWeb = new BufferedOutputStream(sslsocketClient.getOutputStream(), Constantes.BUFFER_SIZE);
		BufferedInputStream reponseDuServeurWeb = new BufferedInputStream(sslsocketClient.getInputStream(), Constantes.BUFFER_SIZE);

		// TODO récupérer la page souhaitée par l'utilisateur
		//requeteAuServeurWeb.write(("GET / \n").getBytes());
//		byte[] buffer2 = new byte[4096];
//		int count = entreeClient.read(buffer2);
//		System.out.println(new String(buffer2,0,count,"UTF-8"));
//		requeteAuServeurWeb.write(buffer2);
		//requeteAuServeurWeb.flush();
		
		
		
		
		 //Génération de fake-cert et importation dans le keystore du nouveau pkcs12.
        GenerationCertificat gen = new GenerationCertificat(serveurCert);
        //Chargement du nouveau keystore dans la variable Java.
        keyStore.load(new FileInputStream(Constantes.KEYSTORE_FILE), keyStorePasswordChar);
        //Réinitiation des objets utiles à la création du socket avec le bon keystore.
        kmf.init(keyStore, keyStorePasswordChar);
        ctx.init(kmf.getKeyManagers(), null, null);
        factory = ctx.getSocketFactory();
		
		SSLSocket sslSocket = (SSLSocket) factory.createSocket(socketClient, ipClient, portClient, false);
		sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
		sslSocket.setUseClientMode(false);
		BufferedOutputStream outputStreamClient = new BufferedOutputStream(sslSocket.getOutputStream(),Constantes.BUFFER_SIZE);
		
		BufferedInputStream in = new BufferedInputStream(sslSocket.getInputStream(), Constantes.BUFFER_SIZE);
		
		
		byte[] buffer = new byte[4096];
		
		int count = in.read(buffer);
		String s = new String(buffer, 0, count, "UTF-8");
		count = in.read(buffer);
		s += new String(buffer, 0, count, "UTF-8");
		String s2 = (s.substring(0, s.indexOf("HTTP"))+"\n");
		System.out.println(s2);
		//requeteAuServeurWeb.write("GET / \n".getBytes());
		requeteAuServeurWeb.write(s2.getBytes());
		requeteAuServeurWeb.flush();
		
		Thread myThread = new Thread(new Transfert(reponseDuServeurWeb, outputStreamClient));
        myThread.start();
        myThread.join();
		/*Transfert threadServeurWebVersClient = new Transfert(reponseDuServeurWeb, outputStreamClient);
		threadServeurWebVersClient.start();*/
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
