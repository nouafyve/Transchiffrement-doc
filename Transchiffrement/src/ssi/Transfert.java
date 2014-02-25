package ssi;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import ssi.paquets.JournalFichier;

public class Transfert extends Thread {

	private final InputStream entree;
	private final OutputStream sortie;
	private final String texteEntree;
	private final String texteSortie;

	public Transfert(InputStream entree, OutputStream sortie,
			String texteEntree, String texteSortie) {
		this.entree = entree;
		this.sortie = sortie;
		this.texteEntree = texteEntree;
		this.texteSortie = texteSortie;
	}

	public void run() {
		byte[] buffer = new byte[Constantes.BUFFER_SIZE];
		int i = 0;
		while (true) {
			try {
				buffer = new byte[Constantes.BUFFER_SIZE];
				int byteRead = entree.read(buffer, 0, Constantes.BUFFER_SIZE);
				String line = new String(buffer);

				if (byteRead != -1) {
					DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH");
					DateFormat dateFormatDetail = new SimpleDateFormat("HH:mm:ss");
					Date date = new Date();
					if(line.contains("gzip, ")){
						line = line.replace("gzip, " , "");
						buffer = line.getBytes();
						byteRead-=6;
					}
					if(!line.contains("image/") && !line.contains("text/css") && !line.contains("application/javascript")){
						JournalFichier jf = new JournalFichier("logs/"+dateFormat.format(date));
						jf.ecrire(dateFormatDetail.format(date) + "   "
								+ texteEntree + " => " + texteSortie + "\n" + line +"\n");
						jf.close();
					}
					sortie.write(buffer, 0, byteRead);
					sortie.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			i++;
		}
	}

}