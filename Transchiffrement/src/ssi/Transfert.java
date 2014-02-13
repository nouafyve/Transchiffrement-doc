package ssi;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

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
				System.out.println("Passage dans la boucle du thread");
				System.out.println(new String(buffer));
				sortie.write(buffer);
				sortie.flush();
				Thread.sleep(10);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}