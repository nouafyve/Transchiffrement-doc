package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ConnexionHTTPS extends Connexion {

	public ConnexionHTTPS(Socket socketClient, String ipWeb, int portWeb) throws Exception {
		super(socketClient, ipWeb, portWeb);
	}

	public void lancer() throws Exception {
		envoyerMessageClient("200 OK");
		System.out.println("HTTPS SSL/TLS : " + ipWeb + " " + portWeb);
		char[] keyStorePasswordChar = Constantes.KEYSTORE_PASSWORD.toCharArray();

		KeyStore keyStore = KeyStore.getInstance("jks");
		keyStore.load(new FileInputStream(Constantes.KEYSTORE_FILE), keyStorePasswordChar);
		SSLContext ctx = SSLContext.getInstance("TLS");
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		// KeyStore.Entry entry = keyStore.getEntry("mykey", new KeyStore.PasswordProtection("000000".toCharArray()));

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

		GenerationCertificat gen = new GenerationCertificat(serveurCert);
		X509Certificate truc = GenerationCertificat.getCertificateFromFile("cles/fake-cert.pem");
		// PrivateKey pk = (PrivateKey)
		// keyStore.getKey("mykey",keyStorePassword);
		// keyStore.setKeyEntry(new String("mykey"), pk, keyStorePassword, new
		// Certificate[] {truc});

		// TODO récupérer la page souhaitée par l'utilisateur
		requeteAuServeurWeb.write("GET / \n".getBytes());

		// ETAPE 4
		// SSLSOCKET = Client <-> Proxy
		// Client => Proxy
		// Pour récupérer l'adresse du client
		// String clientHost = connectionSocket.getInetAddress().toString();

		// keyStore.setKeyEntry("monserv", "ch	ngeit".getBytes(), new
		// Certificate[]{serveurCert});

		/*
		 * PrivateKey pk = (PrivateKey)
		 * keyStore.getKey(JSSEConstants.DEFAULT_ALIAS,keyStorePassword);
		 * iaik.x509.X509Certificate newCert = SignCert.forgeCert(keyStore,
		 * keyStorePassword,JSSEConstants.DEFAULT_ALIAS, remoteCN,
		 * remoteServerCert); KeyStore newKS = KeyStore.getInstance("jks");
		 * newKS.load(null, null);
		 * newKS.setKeyEntry(JSSEConstants.DEFAULT_ALIAS, pk, keyStorePassword,
		 * new Certificate[] {newCert}); keyManagerFactory.init(newKS,
		 * keyStorePassword);
		 * m_sslContext.init(keyManagerFactory.getKeyManagers(), new
		 * TrustManager[] { new TrustEveryone() }, null); m_serverSocketFactory
		 * = m_sslContext.getServerSocketFactory();
		 */

		SSLSocket sslSocket = (SSLSocket) factory.createSocket(socketClient, ipClient, portClient, false);
		sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
		System.out.println(sslSocket.getSupportedCipherSuites());
		sslSocket.setUseClientMode(false);
		BufferedOutputStream outputStreamClient = new BufferedOutputStream(sslSocket.getOutputStream(),Constantes.BUFFER_SIZE);
		Transfert threadServeurWebVersClient = new Transfert(reponseDuServeurWeb, outputStreamClient);
		threadServeurWebVersClient.start();
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
