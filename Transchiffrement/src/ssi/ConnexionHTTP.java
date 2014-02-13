package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnexionHTTP extends Connexion {
	
	private byte[] requeteInitiale;

	public ConnexionHTTP(Socket socketClient, String ipWeb, int portWeb, byte[] requeteInitiale) throws Exception {
		super(socketClient, ipWeb, portWeb);
		this.requeteInitiale = requeteInitiale;
	}
	
	public void lancer() throws Exception{
		Socket socketToWeb = new Socket(ipWeb, 80);
		BufferedOutputStream requeteServeur = new BufferedOutputStream(socketToWeb.getOutputStream(), Constantes.BUFFER_SIZE);
		BufferedInputStream reponseServeur = new BufferedInputStream(socketToWeb.getInputStream(), Constantes.BUFFER_SIZE);
		requeteServeur.write(requeteInitiale);
		
		Transfert threadClientVersServerWeb = new Transfert(entreeClient, requeteServeur);
		Transfert threadServeurWebVersClient = new Transfert(reponseServeur, sortieClient);
		threadClientVersServerWeb.start();
		threadServeurWebVersClient.start();
	}
}
