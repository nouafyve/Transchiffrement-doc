package ssi;

import java.io.BufferedInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Transchiffrement {

	public static void main(String[] args) throws Exception {
		Transchiffrement proxy = new Transchiffrement();
		proxy.initialisationKeyStore();
		proxy.attendreRequete();
	}

	private void attendreRequete() throws Exception {
		int count = 0;
		byte[] buf = new byte[Constantes.BUFFER_SIZE];
		ServerSocket serverSocket;
		Socket connectionSocket;
		serverSocket = new ServerSocket(Constantes.PROXY_PORT);
		while (true) {
			connectionSocket = serverSocket.accept();
			BufferedInputStream in = new BufferedInputStream(connectionSocket.getInputStream(), buf.length);
			String line;
				count = in.read(buf);
				if(count !=-1){
				line = new String(buf, 0, count, "UTF-8");
				// System.out.println("Début de line" +line);
				Pattern httpsConnectPattern = Pattern.compile(
						"^CONNECT[ \\t]+([^:]+):(\\d+).*\r\n\r\n",
						Pattern.DOTALL);
				Pattern httpGetPattern = Pattern
						.compile(
								"^(.+)[ \\t]+(http://.+) HTTP/1.1\r\nHost:[ \\t]+(.+)\r\n.*\r\n\r\n",
								Pattern.DOTALL);

				Matcher httpsConnectMatcher = httpsConnectPattern.matcher(line);
				Matcher httpGetMatcher = httpGetPattern.matcher(line);

				if (httpsConnectMatcher.find()) {
					//System.out.println("Entrée HTTPS");
					String ipWeb = httpsConnectMatcher.group(1);
					
					//System.out.println("IP web : "+ipWeb);
					if(!validateIPPattern(ipWeb)){
						InetAddress address = InetAddress.getByName(ipWeb); 
						ipWeb = address.getHostAddress();
					}
					//System.out.println("IP web : "+ipWeb);	
					int portWeb = Integer.parseInt(httpsConnectMatcher.group(2));

					
					ConnexionHTTPS connexionHTTPS = new ConnexionHTTPS(
							connectionSocket, ipWeb, portWeb);
					connexionHTTPS.lancer();
					//System.out.println("Sortie HTTPS");
				} else if (httpGetMatcher.find()) {
					//System.out.println("Entrée HTTP");
					String requete = httpGetMatcher.group(1) + " "
							+ httpGetMatcher.group(2);
					// System.out.println(requete);
					String ipWeb = httpGetMatcher.group(3);
					ipWeb = ipWeb.substring(0, ipWeb.indexOf("\n")-1);
					//System.out.println("IP web : "+ipWeb);
					if(!validateIPPattern(ipWeb)){
						InetAddress address = InetAddress.getByName(ipWeb); 
						ipWeb = address.getHostAddress();
					}
					//System.out.println("IP web : "+ipWeb);
					ConnexionHTTP connexionHTTP = new ConnexionHTTP(connectionSocket, ipWeb, 80, buf);
					connexionHTTP.lancer();
					//System.out.println("Sortie HTTP");
				}
				buf = new byte[Constantes.BUFFER_SIZE];
				}
		}
	}

	private final String PATTERN = 
	        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	private boolean validateIPPattern(final String ip){          

	      Pattern pattern = Pattern.compile(PATTERN);
	      Matcher matcher = pattern.matcher(ip);
	      return matcher.matches();             
	}
	
	private void initialisationKeyStore() {
		System.setProperty("javax.net.ssl.keyStore", Constantes.KEYSTORE_FILE);
		System.setProperty("javax.net.ssl.keyStorePassword",
				Constantes.KEYSTORE_PASSWORD);
		System.setProperty("javax.net.ssl.trustStore", Constantes.KEYSTORE_FILE);
		System.setProperty("javax.net.ssl.trustStorePassword",
				Constantes.KEYSTORE_PASSWORD);
		System.setProperty("https.protocols", "TLSv1");
	}
}
