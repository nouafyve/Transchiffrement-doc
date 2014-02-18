package ssi;

import java.io.BufferedInputStream;
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

	public void attendreRequete() throws Exception {
		// Faire une boucle pour tourner en boucle
		int count = -1;
		byte[] buf = new byte[Constantes.BUFFER_SIZE];
		boolean https_mode = false;
		ServerSocket serverSocket;
		Socket connectionSocket;
		serverSocket = new ServerSocket(Constantes.PROXY_PORT);
		while (true) {
			connectionSocket = serverSocket.accept();
			BufferedInputStream in = new BufferedInputStream(
					connectionSocket.getInputStream(), buf.length);

			String line;

			while ((count = in.read(buf)) > 0) {
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

				if (httpsConnectMatcher.find() || https_mode) {
					// https_mode= true;
					System.out.println("Entrée HTTPS");
					String ipWeb = httpsConnectMatcher.group(1);
					int portWeb = Integer
							.parseInt(httpsConnectMatcher.group(2));
					ConnexionHTTPS connexionHTTPS = new ConnexionHTTPS(
							connectionSocket, ipWeb, portWeb);
					connexionHTTPS.lancer();
					System.out.println("Sortie HTTPS");
					connectionSocket.shutdownInput();
					connectionSocket.shutdownOutput();
				} else if (httpGetMatcher.find()) {
					System.out.println("Entrée HTTP");
					String requete = httpGetMatcher.group(1) + " "
							+ httpGetMatcher.group(2);
					// System.out.println(requete);
					String ipWeb = httpGetMatcher.group(3);
					ConnexionHTTP connexionHTTP = new ConnexionHTTP(
							connectionSocket, ipWeb, 80, buf);
					connexionHTTP.lancer();
					System.out.println("Sortie HTTP");
				}
			}
			buf = new byte[Constantes.BUFFER_SIZE];
		}
	}

	public void initialisationKeyStore() {
		System.setProperty("javax.net.ssl.keyStore", Constantes.KEYSTORE_FILE);
		System.setProperty("javax.net.ssl.keyStorePassword",
				Constantes.KEYSTORE_PASSWORD);
		System.setProperty("javax.net.ssl.trustStore", Constantes.KEYSTORE_FILE);
		System.setProperty("javax.net.ssl.trustStorePassword",
				Constantes.KEYSTORE_PASSWORD);
		System.setProperty("https.protocols", "TLSv1");
	}
}
