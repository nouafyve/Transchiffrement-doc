import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

class ReceptionMessage {
	public static void main(String[] arg) {
		DatagramSocket socketUDP;
		DatagramPacket message;
		byte[] tampon;
		int portLocal;
		String texte ;

		try {
			portLocal = Integer.parseInt(arg[0]);
			socketUDP = new DatagramSocket(portLocal);
			while(true) {
				tampon = new byte[256];
				message = new DatagramPacket(tampon, tampon.length);
				socketUDP.receive(message);
				InetAddress adresseIP = message.getAddress();
				int portDistant = message.getPort();
				texte = new String(tampon) ;
				texte = texte.substring(0, message.getLength());
				System.out.println("Reception du port " + portDistant + " de la machine " + adresseIP.getHostName() + " : " + texte);
			}
		}

		catch(ArrayIndexOutOfBoundsException exc) {
			System.out.println("Avez-vous donne le numero de port sur lequel vous attendez le message ?");
		}
		catch(SocketException exc) {
			System.out.println("Probleme d'ouverture du socket");
		}
		catch(IOException exc) {
			System.out.println("Probleme sur la reception ou l'envoi du message");
		}
	}
}