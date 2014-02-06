import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ProxySSI {
	public static void main(String[] args) throws Exception {

		String keyStoreFile = "/home/emile/keyStoreSSIAutoSigned";
		String keyStorePassword = "000000";
		int proxyPort = 9999;
		int bufferSize = 40960;
		
		
		System.setProperty("javax.net.ssl.keyStore", keyStoreFile);
		System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
		System.setProperty("javax.net.ssl.trustStore", keyStoreFile);
		System.setProperty("javax.net.ssl.trustStorePassword", keyStorePassword);
		System.setProperty("https.protocols","TLSv1");
		
		
		ServerSocket serverSocket = new ServerSocket(proxyPort);
		Socket connectionSocket = serverSocket.accept();

		byte[] buf = new byte[bufferSize];
		byte[] buffer2 = new byte[bufferSize];
		int count = -1;
		String line;

		BufferedInputStream in = new BufferedInputStream(connectionSocket.getInputStream(), buf.length);
		BufferedOutputStream out = new BufferedOutputStream(connectionSocket.getOutputStream(), buf.length);
		
		while ((count = in.read(buf)) > 0) {
			line = new String(buf, 0, count, "UTF-8");
			System.out.println(line);

			// TODO regex ???
			Pattern m_httpsConnectPattern = Pattern.compile("^CONNECT[ \\t]+([^:]+):(\\d+).*\r\n\r\n", Pattern.DOTALL);
			Matcher httpsConnectMatcher = m_httpsConnectPattern.matcher(line);
			
//			Pattern m_httpGETPattern = Pattern.compile("^GET[ \\t]+([^:]+):(\\d+).*\r\n\r\n", Pattern.DOTALL);
//			Matcher httpGETMatcher = m_httpGETPattern.matcher(line);
			
			
			if (httpsConnectMatcher.find()) {				
				String remoteHost = httpsConnectMatcher.group(1);
				int remotePort = Integer.parseInt(httpsConnectMatcher.group(2));
				System.out.println("HTTPS SSL/TLS : "+httpsConnectMatcher.group(1)+" "+httpsConnectMatcher.group(2));
							
				StringBuffer response = new StringBuffer();
			    response.append("HTTP/1.0 ").append("200 OK").append("\r\n");
			    response.append("Host: " + remoteHost + ":" + remotePort + "\r\n");
			    response.append("Proxy-agent: CS255-MITMProxy/1.0\r\n");
		    	response.append("\r\n");
		    	out.write(response.toString().getBytes());
		    	out.flush();
				System.out.println("réponse 200 ok");
								
				
				/***********************************/	

		        /* Load properties */
		        
				
				char[] keyStorePasswordChar = keyStorePassword.toCharArray();

	        	// Que fait le 2ème paramètre ?
	        	KeyStore keyStore = KeyStore.getInstance("jks");
	        	keyStore.load(new FileInputStream(keyStoreFile), keyStorePasswordChar);

		        /* Get factory for the given keystore */	        
		        SSLContext ctx = SSLContext.getInstance("TLS");
		        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		        
		        //KeyStore.Entry entry = keyStore.getEntry("mykey", new KeyStore.PasswordProtection("000000".toCharArray()));

		        kmf.init(keyStore, keyStorePasswordChar);
		        ctx.init(kmf.getKeyManagers(), null, null);
		        SSLSocketFactory factory = ctx.getSocketFactory();
		        
		        
		        /*********************************************/
				
				try {
									
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					
					// Proxy <=> Server
					SSLSocket sslsocketClient = (SSLSocket) sslsocketfactory.createSocket(remoteHost, remotePort);
					sslsocketClient.setEnabledCipherSuites(sslsocketClient.getSupportedCipherSuites());
					//Certificate serveurCert = sslsocketClient.getSession().getPeerCertificates()[0]; 
					OutputStream requeteAuServeurWeb = sslsocketClient.getOutputStream();
					InputStream reponseDuServeurWeb = sslsocketClient.getInputStream();
					
					requeteAuServeurWeb.write("GET / \n".getBytes());
					reponseDuServeurWeb.read(buffer2);
					System.out.println(new String(buffer2));
					
					//ETAPE 4
					//SSLSOCKET = Client <-> Proxy
					// Client => Proxy
					// Pour récupérer l'adresse du client
					//String clientHost = connectionSocket.getInetAddress().toString();					
					
			        //keyStore.setKeyEntry("monserv", "ch	ngeit".getBytes(), new Certificate[]{serveurCert});
					
			        /*
			    	PrivateKey pk = (PrivateKey) keyStore.getKey(JSSEConstants.DEFAULT_ALIAS,keyStorePassword);
			    	iaik.x509.X509Certificate newCert = SignCert.forgeCert(keyStore, keyStorePassword,JSSEConstants.DEFAULT_ALIAS,  remoteCN, remoteServerCert);
			    	KeyStore newKS = KeyStore.getInstance("jks");
			    	newKS.load(null, null);
			    	newKS.setKeyEntry(JSSEConstants.DEFAULT_ALIAS, pk, keyStorePassword, new Certificate[] {newCert});
			    	keyManagerFactory.init(newKS, keyStorePassword);
			    	m_sslContext.init(keyManagerFactory.getKeyManagers(),  new TrustManager[] { new TrustEveryone() }, null);
			    	m_serverSocketFactory = m_sslContext.getServerSocketFactory(); */
					
					SSLSocket sslSocket = (SSLSocket) factory.createSocket(connectionSocket, "127.0.0.1", connectionSocket.getPort(), false);
					sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
					sslSocket.setUseClientMode(false);
					OutputStream outputStreamClient = sslSocket.getOutputStream();
					
					// Réponse qu'on envoie au client
					outputStreamClient.write(buffer2);
					System.out.println("Sortie HTTPS");
					outputStreamClient.flush();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			//else if (httpGETMatcher.find()){
			else{
				System.out.println("Entrée en mode HTTP");
				Socket clientSocket = new Socket("192.168.56.101", 80);
			
				BufferedOutputStream requeteServeur = new BufferedOutputStream(clientSocket.getOutputStream(), buffer2.length);
				BufferedInputStream reponseServeur = new BufferedInputStream(clientSocket.getInputStream(), buffer2.length);
				requeteServeur.write(buf);
				
				int count2;
				while ((count2 = reponseServeur.read(buffer2)) > 0) {
					System.out.println("Boucle");
					out.write(buffer2);
					buffer2 = new byte[40960];
				}
				System.out.println("Sortie HTTP");
				clientSocket.close();
			}
		}
		connectionSocket.close();
		serverSocket.close();
	} 
}
