package ssi;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;

public abstract class Connexion {
	protected Socket socketClient;
	protected String ipClient;
	protected int portClient;
	protected String ipWeb;
	protected int portWeb;
	protected BufferedOutputStream sortieClient;
	protected BufferedInputStream entreeClient;
	
	public Connexion(Socket socketClient, String ipWeb, int portWeb) throws Exception {
		this.socketClient = socketClient;
		this.ipWeb = ipWeb;
		this.portWeb = portWeb;
		this.entreeClient =  new BufferedInputStream(socketClient.getInputStream(), Constantes.BUFFER_SIZE);
		this.sortieClient =  new BufferedOutputStream(socketClient.getOutputStream(), Constantes.BUFFER_SIZE);
		this.ipClient = socketClient.getRemoteSocketAddress().toString();
		this.portClient = socketClient.getPort();
	}
}
