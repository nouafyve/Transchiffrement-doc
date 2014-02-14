package ssi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import ssi.paquets.JournalFichier;

public class Transfert extends Thread {

	private byte[] buffer = new byte[Constantes.BUFFER_SIZE];
	private BufferedInputStream entree;
	private BufferedOutputStream sortie;

	public Transfert(BufferedInputStream entree, BufferedOutputStream sortie) {
		this.entree = entree;
		this.sortie = sortie;
	}

	public void run() {
		try {
			while (entree.read(buffer) > 0) {
				String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp);
				System.out.println("Passage dans la boucle du thread");
				System.out.println("SALUT LES ZOUZOUS ! "+new String(buffer, "US-ASCII"));
				System.out.println("Ouverture du ficheir");
				JournalFichier logger = new JournalFichier(Constantes.DOSSIER_LOG + "log" + timeStamp + ".txt");
				
				logger.ecrire(new String(buffer));
				logger.close();
				System.out.println("Fermeture du ficheir");
				sortie.write(buffer);
				sortie.flush();
				Thread.sleep(10);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}