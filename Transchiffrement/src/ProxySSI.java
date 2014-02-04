import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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
				// TODO Flush ?
				
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
				
				
				
				
				
				
				X509Certificate java_cert = null;
				SSLSocket remoteSocket = null;
				try {
					// Client => Proxy					
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
					// TODO
					String clientHost = connectionSocket.getInetAddress().toString();
					System.out.println("Test : "+clientHost);
					
					
					SSLSocket sslSocket = (SSLSocket) sslsocketfactory.createSocket(connectionSocket, "127.0.0.1", connectionSocket.getPort(), false);
					sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
					
					sslSocket.setUseClientMode(false);
					
					// Proxy => Server
					SSLSocket sslsocketClient = (SSLSocket) sslsocketfactory.createSocket(remoteHost, remotePort);
					
					
					sslsocketClient.setEnabledCipherSuites(sslsocketClient.getSupportedCipherSuites());

					//sslsocketClient.startHandshake();
					
					OutputStream requeteAuServeurWeb = sslsocketClient.getOutputStream();
					InputStream reponseDuServeurWeb = sslsocketClient.getInputStream();
					
					requeteAuServeurWeb.write("GET / \n".getBytes());
					
					//ETAPE 4
					InputStream inputStreamClient = sslSocket.getInputStream();
					OutputStream outputStreamClient = sslSocket.getOutputStream();
					
					
					while ((count2 = reponseDuServeurWeb.read(buffer2)) > 0) {
						System.out.println(new String(buffer2));
						System.out.println("passe");
						outputStreamClient.write(buffer2);
						System.out.println("passe pas");
					}
					//outputStreamClient.flush();
					System.out.println("Bim le client");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		connectionSocket.close();
		serverSocket.close();
	}
}
