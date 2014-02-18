package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ConnexionHTTP extends Connexion {
	
	private String requeteCourte;

	public ConnexionHTTP(Socket socketClient, String ipWeb, int portWeb, byte[] requeteInitiale) throws Exception {
		super(socketClient, ipWeb, portWeb);
		String s = new String(requeteInitiale);
		this.requeteCourte = (s.substring(0, s.indexOf("User-Agent"))+"\n");
		System.out.println(requeteCourte);
	}
	
	public void lancer() throws Exception{
		Socket socketToWeb = new Socket(ipWeb, 80);
		BufferedOutputStream requeteServeur = new BufferedOutputStream(socketToWeb.getOutputStream(), Constantes.BUFFER_SIZE);
		BufferedInputStream reponseServeur = new BufferedInputStream(socketToWeb.getInputStream(), Constantes.BUFFER_SIZE);
		requeteServeur.write(requeteCourte.getBytes());
		requeteServeur.flush();
		
		Transfert threadClientVersServerWeb = new Transfert(entreeClient, requeteServeur);
		Transfert threadServeurWebVersClient = new Transfert(reponseServeur, sortieClient);
		threadClientVersServerWeb.start();
		threadServeurWebVersClient.start();
	}
}
