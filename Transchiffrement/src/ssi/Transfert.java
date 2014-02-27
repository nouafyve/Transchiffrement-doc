package ssi;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLException;

import ssi.paquets.JournalFichier;

public class Transfert extends Thread {

	private final InputStream entree;
	private final OutputStream sortie;
	private final String texteEntree;
	private final String texteSortie;
	private boolean is_running = false;
	private String current_file;
	
	private String textExtrait;
	private String textRestant;
	private static String nomFichier = "default";
	
	public Transfert(InputStream entree, OutputStream sortie,
			String texteEntree, String texteSortie) {
		this.entree = entree;
		this.sortie = sortie;
		this.texteEntree = texteEntree;
		this.texteSortie = texteSortie;
		this.is_running = true;
	}

	public void run() {
		textExtrait = "";
		byte[] buffer = new byte[Constantes.BUFFER_SIZE];
		while (is_running) {
			try {
				buffer = new byte[Constantes.BUFFER_SIZE];
				int byteRead = entree.read(buffer, 0, Constantes.BUFFER_SIZE);
				String line = new String(buffer);
				textRestant = line;
				
				textExtrait = recupererHeader();
				System.out.println(textExtrait);
				if(estUneRequete(textExtrait)){
					nomFichier = nomPageDemandee();
					System.out.println("+++++++++++++++++++++++++++++++++++++++++++"+nomFichier);
				}
				else{
					System.out.println("C'est du contenu du fichier : " + nomFichier);
					textExtrait = recupererContenu();
					//System.out.println(textExtrait);
					
					DateFormat dateFormatDetail = new SimpleDateFormat("yyyy_MM_dd_HH:mm");
					Date date = new Date();

					String directoryName = dateFormatDetail.format(date);
					File theDir = new File(directoryName);

					if (!theDir.exists()) {
						boolean creationOk = theDir.mkdir();

						if (!creationOk) {
							System.out.println("Exception à lever ici");
						}
					}

					String filename = theDir + "/" + nomFichier;
					ecrireBytes(filename, textExtrait.getBytes(), textExtrait.length());
				}
				

				
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
		}
	}
	
	
	private void ecrireBytes(String filename, byte[] buffer, int readBytes) {
		try {
			DataOutputStream flux = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(
							filename), true)));
			flux.write(buffer, 0, readBytes);
			flux.close();
		} catch (Exception e) {
		}
	}
	
	
	private boolean estUneReponse(String text){
		byte beginHeaderResponse[] = "HTTP".getBytes();
		return text.indexOf(new String(beginHeaderResponse)) == 0; 
	}
	
	private boolean estUneRequete(String text){
		byte beginHeaderRequest[] = "GET".getBytes();
		byte beginHeaderRequest2[] = "POST".getBytes();
		return (text.indexOf(new String(beginHeaderRequest)) == 0) ||(text.indexOf(new String(beginHeaderRequest2)) == 0);
	}
	
	private String nomPageDemandee(){
		Pattern httpGetPattern = Pattern.compile("^(.+)[ \\t]+/(.+) HTTP/1.1", Pattern.DOTALL);
		Matcher httpGetMatcher = httpGetPattern.matcher(textExtrait);
		String resultat = "";
		if (httpGetMatcher.find()) {
			resultat = httpGetMatcher.group(2);
		}
		if(resultat.equals("")){
			resultat = "index.html";
		}
		return resultat;
	}
	
	private String recupererHeader(){
		String endHeader = "\r\n\r\n";
		String resultat = "";
		if(textRestant.indexOf(endHeader) != -1){
			resultat = textRestant.substring(0, textRestant.indexOf(new String(endHeader)));
			textRestant = textRestant.substring(textRestant.indexOf(new String(endHeader)));
		}
		return resultat;
	}
	
	private String recupererContenu(){
		String beginHeaderRequest = "GET";
		String beginHeaderRequest2 = "POST";
		String resultat = "";
		int debutSuivant = -1;
		int debutSuivant1 = textRestant.indexOf(beginHeaderRequest);
		int debutSuivant2 = textRestant.indexOf(beginHeaderRequest2);
		if(debutSuivant1 != -1 && debutSuivant2 != -1){
			debutSuivant = Math.min(debutSuivant1, debutSuivant2);
		}
		if(debutSuivant1 == -1 && debutSuivant2 != -1){
			debutSuivant = debutSuivant2;
		}
		if(debutSuivant1 != -1 && debutSuivant2 == -1){
			debutSuivant = debutSuivant1;
		}
		if(debutSuivant != -1){
			resultat = textRestant.substring(0, debutSuivant);
			textRestant = textRestant.substring(0, debutSuivant);
		}
		else{
			resultat = textRestant;
			textRestant = "";
		}
		return resultat;
	}
}
