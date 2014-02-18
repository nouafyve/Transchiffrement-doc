package ssi.paquets;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

public class JournalFichier implements Journal {

	private PrintWriter writer;

	public JournalFichier(String nomFichier) throws Exception {
		this.writer = new PrintWriter(new BufferedWriter(new FileWriter(nomFichier, true)));
	}

	public void ecrire(String chaine) {
		writer.println(chaine);
	}

	public void close() {
		writer.close();
	}
}
