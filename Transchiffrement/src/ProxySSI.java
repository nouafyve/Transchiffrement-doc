import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.cert.X509Certificate;

public class ProxySSI {
	public static void main(String[] args) throws Exception {

		ServerSocket serverSocket = new ServerSocket(9999);
		Socket connectionSocket = serverSocket.accept();

		byte[] buf = new byte[40960];
		byte[] buffer2 = new byte[40960];
		int count = -1;
		int count2 = -1;
		String line;

		BufferedInputStream in = new BufferedInputStream(connectionSocket.getInputStream(), buf.length);
		BufferedOutputStream out = new BufferedOutputStream(connectionSocket.getOutputStream(), buf.length);
		
		while ((count = in.read(buf)) > 0) {
			line = new String(buf, 0, count, "UTF-8");
			System.out.println(line);

			// TODO regex ???
			Pattern m_httpsConnectPattern = Pattern.compile(
					"^CONNECT[ \\t]+([^:]+):(\\d+).*\r\n\r\n", Pattern.DOTALL);
			Matcher httpsConnectMatcher = m_httpsConnectPattern.matcher(line);
			
			if (httpsConnectMatcher.find()) {

				//TODO Flush ?
				//while (in.read(buf, 0, in.available()) > 0) {}
				
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
		        
				System.setProperty("https.protocols","TLSv1");
				String keyStoreFile = "/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/security/cacerts";
				char[] keyStorePassword = "changeit".toCharArray();
		        
		        /* Create keystore */
		        KeyStore keyStore = null; // = KeyStore.getInstance(KeyStore.getDefaultType());

	        	// Que fait le 2ème paramètre ?
	        	String keyStoreType = "jks";
	        	keyStore = KeyStore.getInstance(keyStoreType);
	        	keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
	        	System.out.println("On a ouvert un keystoreFile " +keyStore);

		        /* Get factory for the given keystore */
		        //TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		        //tmf.init(keyStore);		        
		        SSLContext ctx = SSLContext.getInstance("TLS");
		        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		        
		        KeyStore.Entry entry = keyStore.getEntry("mykey", new KeyStore.PasswordProtection("000000".toCharArray()));
		        
		        kmf.init(keyStore, "000000".toCharArray());
		        ctx.init(kmf.getKeyManagers(), null, null);
		        
		        SSLSocketFactory factory = ctx.getSocketFactory();

		        
		        
		        
		        
		        
		        
		        
		        
		        
		        
		        
		        /*
		    	PrivateKey pk = (PrivateKey) keyStore.getKey(JSSEConstants.DEFAULT_ALIAS,keyStorePassword);
		    	iaik.x509.X509Certificate newCert = SignCert.forgeCert(keyStore, keyStorePassword,JSSEConstants.DEFAULT_ALIAS,  remoteCN, remoteServerCert);
		    	KeyStore newKS = KeyStore.getInstance("jks");
		    	newKS.load(null, null);
		    	newKS.setKeyEntry(JSSEConstants.DEFAULT_ALIAS, pk, keyStorePassword, new Certificate[] {newCert});
		    	keyManagerFactory.init(newKS, keyStorePassword);
		    	m_sslContext.init(keyManagerFactory.getKeyManagers(),  new TrustManager[] { new TrustEveryone() }, null);
		    	m_serverSocketFactory = m_sslContext.getServerSocketFactory(); */
		        
		        
		        
		        
		        
		        
		        
		        
		        /*********************************************/
				
				try {
					// Client => Proxy					
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					// TODO
					String clientHost = connectionSocket.getInetAddress().toString();
					System.out.println("Test : "+clientHost);
					
					SSLSocket sslSocket = (SSLSocket) factory.createSocket(connectionSocket, "127.0.0.1", connectionSocket.getPort(), false);
					sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
									
					
					for( String i : sslSocket.getEnabledProtocols()){
						System.out.println("Julien : "+i);
					}
					
					sslSocket.setUseClientMode(false);
					
					// Proxy <=> Server
					SSLSocket sslsocketClient = (SSLSocket) sslsocketfactory.createSocket(remoteHost, remotePort);
					
					sslsocketClient.setEnabledCipherSuites(sslsocketClient.getSupportedCipherSuites());
				
					
					System.out.println(sslsocketClient.getSession().getPeerCertificates()[0]);
					
					OutputStream requeteAuServeurWeb = sslsocketClient.getOutputStream();
					InputStream reponseDuServeurWeb = sslsocketClient.getInputStream();
					
					requeteAuServeurWeb.write("GET / \n".getBytes());
					
					//ETAPE 4
					//SSLSOCKET = Client <-> Proxy
					OutputStream outputStreamClient = sslSocket.getOutputStream();
					
					while ((count2 = reponseDuServeurWeb.read(buffer2)) > 0) {
						System.out.println(new String(buffer2)); 
						outputStreamClient.write(buffer2);
						System.out.println("passe pas");
					}
					//outputStreamClient.flush();
					System.out.println("Bim le client.... ou pas :/");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		connectionSocket.close();
		serverSocket.close();
	}
}
