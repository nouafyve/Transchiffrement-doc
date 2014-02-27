package ssi;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.SSLException;

import ssi.paquets.JournalFichier;

public class Transfert extends Thread {

	private final InputStream entree;
	private final OutputStream sortie;
	private final String texteEntree;
	private final String texteSortie;
	private boolean is_running = false;
	
	public Transfert(InputStream entree, OutputStream sortie,
			String texteEntree, String texteSortie) {
		this.entree = entree;
		this.sortie = sortie;
		this.texteEntree = texteEntree;
		this.texteSortie = texteSortie;
		this.is_running = true;
	}

	public void run() {
		byte[] buffer = new byte[Constantes.BUFFER_SIZE];
		int i = 0;
		while (is_running) {
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
						JournalFichier jf = new JournalFichier(Constantes.PROJECT_FOLDER + "logs/"+dateFormat.format(date)+".txt");
						jf.ecrire(dateFormatDetail.format(date) + "   "
								+ texteEntree + " => " + texteSortie + "\n" + line +"\n");
						jf.close();
					}
					sortie.write(buffer, 0, byteRead);
					sortie.flush();
				}
			} catch (SSLException e) {
				String message_cause = e.getMessage();
				if(message_cause.contains("Received fatal alert: unknown_ca")){
					System.out.println("Erreur unknown ca");					
					is_running = false;
				}
				else if(message_cause.contains("Received fatal alert: bad_certificate")){
					System.out.println("Erreur bad cert");			
					is_running = false;
				}
				else{
					e.printStackTrace();
				}
			}
			catch(Exception e){
				String message_cause = e.getMessage();
				if(message_cause.contains("read timed out")){
					System.out.println("Read timed out");			
					is_running = false;
				}
			}
			i++;
		}
	}
}