import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

class EnvoiMessage {
	public static void main(String[] arg) {
		int portDestinataire;
		InetAddress adresseIP;
		DatagramSocket socketUDP;
		DatagramPacket message;
		BufferedReader entree;
		String ligne;
		int longueur;
		byte[] tampon;

		try {
			socketUDP = new DatagramSocket();
			System.out.println("Port local : " + socketUDP.getLocalPort());
			adresseIP = InetAddress.getByName(arg[0]);
			portDestinataire = Integer.parseInt(arg[1]);
			entree = new BufferedReader(new InputStreamReader(System.in));

			while(true) {
				ligne = entree.readLine();
				tampon = ligne.getBytes();
				longueur = tampon.length;
				message = new DatagramPacket(tampon, longueur, adresseIP, portDestinataire);
				socketUDP.send(message);
			}
		}
		catch(ArrayIndexOutOfBoundsException exc) {
			System.out.println("Avez-vous donne le nom de la machine destinatrice et le numero de port du client ?");
		}
		catch(UnknownHostException exc) {
			System.out.println("Destinataire inconnu");
		}
		catch(SocketException exc) {
			System.out.println("Probleme d'ouverture de la socket");
		}
		catch(IOException exc) {
			System.out.println("Probleme sur la reception ou l'envoi du message");
		}
		catch(NumberFormatException exc) {
			System.out.println("Le second argument doit etre un entier");
		}
	}
}