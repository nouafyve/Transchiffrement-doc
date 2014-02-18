package ssi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
 
public class Transfert extends Thread {
 
        //public byte[] buffer = new byte[Constantes.BUFFER_SIZE];
        private final InputStream entree;
        private final OutputStream sortie;
 
        public Transfert(InputStream entree, OutputStream sortie) {
                this.entree = entree;
                this.sortie = sortie;
        }
 
        public void run() {
                byte[] buffer = new byte[Constantes.BUFFER_SIZE];
                try {
                        while (true) {
                        		System.out.println("ENTREE");
                                int byteRead = entree.read(buffer, 0, Constantes.BUFFER_SIZE);
                                //System.out.println("Transfert.buffer : " + new String(buffer));
                                String line = new String(buffer, 0, buffer.length);
                               
                                if (byteRead == -1) {
                                		System.out.println("PAS BON");
                                        break;
                                }
                               
                                if (byteRead > 0) {
                                		System.out.println("BON");
                                		System.out.println(line);
                                        sortie.write(buffer, 0, byteRead);
                                        sortie.flush();
                                }
                        }
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
//                       
//              System.out.println("Sortie boucle while");
//              try {
//                      entree.close();
//              } catch (IOException e) {
//                      // TODO Auto-generated catch block
//                      e.printStackTrace();
//                }
             
        }
}