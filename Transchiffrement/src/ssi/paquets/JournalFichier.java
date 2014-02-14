package ssi.paquets;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class JournalFichier implements Journal{
	
	private PrintWriter writer;
	
	public JournalFichier(String nomFichier) throws Exception{
		this.writer = new PrintWriter(new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(nomFichier),
						"US-ASCII")));
	}
	
	
	public void ecrire(String chaine){
		writer.println(chaine);
	}
	
	public void close(){
		writer.close();
	}	
}
