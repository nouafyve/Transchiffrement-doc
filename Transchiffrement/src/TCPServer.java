import java.io.*;
import java.net.*;

class TCPServer {
	public static void main(String argv[]) throws Exception {
		String clientSentence, htmlPage;
		ServerSocket serverSocket = new ServerSocket(3129);

		
		while (true) {
			System.out.println("Listening");
			Socket connectionSocket = serverSocket.accept();
			System.out.println("Seeing something");
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			clientSentence = inFromClient.readLine();
			System.out.println("Received: " + clientSentence);
			
			htmlPage = "<!DOCTYPE html><html><body><h1>It works</h1><p>My first paragraph.</p></body></html>";
			outToClient.writeBytes(htmlPage);
			connectionSocket.close();
		}
	}
}