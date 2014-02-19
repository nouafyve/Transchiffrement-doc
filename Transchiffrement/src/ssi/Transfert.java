
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

		while (true) {
			try {
				buffer = new byte[Constantes.BUFFER_SIZE];
				int byteRead = entree.read(buffer, 0, Constantes.BUFFER_SIZE);
				String line = new String(buffer);

				if (byteRead != -1) {
					DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH");
					DateFormat dateFormatDetail = new SimpleDateFormat("HH:mm:ss");
					Date date = new Date();
					JournalFichier jf = new JournalFichier(dateFormat.format(date));
//					if (line.contains("Accept-Encoding")){
//						//line = line.replaceAll("Accept-Encoding: \".*\"", "");
//						String sDebut = (line.substring(0, line.indexOf("Accept-Encoding")));
//						String sFin = (line.substring(line.indexOf("Connection"), line.length()));
//						line = sDebut + sFin;
//						System.out.println(line);
//						line = line.replace("gzip, ", "");
//					}
//					if(line.contains("If-Modified-Since")){
//						String sDebut = (line.substring(0, line.indexOf("If-Modified-Since")));
//						String sFin = (line.substring(line.indexOf("If-None-Match"), line.length()));
//						line = sDebut + sFin;
//					}
//					if(line.contains("If-None-Match")){
//						line = line.replaceAll("If-None-Match: \".*\"", "");
//						System.out.println(line);
//					}
					// System.out.println(dateFormatDetail.format(date) + "   "
					// + texteEntree + " => " + texteSortie +"\n"+ line);
					jf.ecrire(dateFormatDetail.format(date) + "   "
							+ texteEntree + " => " + texteSortie + "\n" + line);
					jf.close();
					sortie.write(buffer,0, byteRead);
					sortie.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}