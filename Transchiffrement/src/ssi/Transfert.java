package ssi;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ssi.paquets.JournalFichier;

public class Transfert extends Thread {

	// public byte[] buffer = new byte[Constantes.BUFFER_SIZE];
	private final InputStream entree;
	private final OutputStream sortie;
	private final String texteEntree;
	private final String texteSortie;
	
	public Transfert(InputStream entree, OutputStream sortie, String texteEntree, String texteSortie) {
		this.entree = entree;
		this.sortie = sortie;
		this.texteEntree = texteEntree;
		this.texteSortie = texteSortie;
	}

	public void run() {
		byte[] buffer = new byte[Constantes.BUFFER_SIZE];
		try {
			while (true) {
				buffer = new byte[Constantes.BUFFER_SIZE];
				int byteRead = entree.read(buffer, 0, Constantes.BUFFER_SIZE);
				String line = new String(buffer, 0, buffer.length);

				if (byteRead == -1) {
					break;
				}

				if (byteRead > 0) {
					DateFormat dateFormat = new SimpleDateFormat(
							"yyyy_MM_dd_HH");
					DateFormat dateFormatDetail = new SimpleDateFormat("mm:ss");
					Date date = new Date();
					JournalFichier jf = new JournalFichier(
							dateFormat.format(date));
					//System.out.println(dateFormatDetail.format(date) + "   " + texteEntree + " => " + texteSortie +"\n"+ line);
					jf.ecrire(dateFormatDetail.format(date) + "   " + texteEntree + " => " + texteSortie +"\n"+ line);
					jf.close();
					sortie.write(buffer, 0, byteRead);
					sortie.flush();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}