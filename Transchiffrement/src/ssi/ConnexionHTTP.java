package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ssi.paquets.JournalFichier;

public class ConnexionHTTP extends Connexion {

	private String requeteCourte;

	public ConnexionHTTP(Socket socketClient, String ipWeb, int portWeb,
			byte[] requeteInitiale) throws Exception {
		super(socketClient, ipWeb, portWeb);
		String s = new String(requeteInitiale);
		this.requeteCourte = (s.substring(0, s.indexOf("User-Agent")) + "\n");
		//System.out.println(requeteCourte);
	}

	public void lancer() throws Exception {
		Socket socketToWeb = new Socket(ipWeb, 80);
		BufferedOutputStream requeteServeur = new BufferedOutputStream(
				socketToWeb.getOutputStream(), Constantes.BUFFER_SIZE);
		BufferedInputStream reponseServeur = new BufferedInputStream(
				socketToWeb.getInputStream(), Constantes.BUFFER_SIZE);
		DateFormat dateFormat = new SimpleDateFormat(
				"yyyy_MM_dd_HH");
		DateFormat dateFormatDetail = new SimpleDateFormat("mm:ss");
		Date date = new Date();
		JournalFichier jf = new JournalFichier(
				dateFormat.format(date));
		//System.out.println(dateFormatDetail.format(date) + "   " + texteEntree + " => " + texteSortie +"\n"+ line);
		jf.ecrire(dateFormatDetail.format(date) + "   " + socketClient.getRemoteSocketAddress().toString() + " => " + "/"+ipWeb+":"+80 +"\n"+ requeteCourte);
		jf.close();
		requeteServeur.write(requeteCourte.getBytes());
		requeteServeur.flush();
		Transfert threadClientVersServerWeb = new Transfert(entreeClient,
				requeteServeur,socketClient.getRemoteSocketAddress().toString() ,"/"+ipWeb+":"+80);
		Transfert threadServeurWebVersClient = new Transfert(reponseServeur,
				sortieClient, "/"+ipWeb+":"+80, socketClient.getRemoteSocketAddress().toString());
		threadClientVersServerWeb.start();
		threadServeurWebVersClient.start();
	}
}
