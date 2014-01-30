import java.io.IOException;
import java.math.BigInteger;

import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.binary.Hex;

class PacketPrinter implements PacketReceiver {

	private NetworkInterface outputInterface;

	public PacketPrinter(NetworkInterface outputInterface) {
		this.outputInterface = outputInterface;
	}

	// this method is called every time Jpcap captures a packet
	public void receivePacket(Packet packet) {
		// just print out a captured packet
		System.out.println("Paquet : " + packet);
		// System.out.println("Paquet : "+new String(packet.data));

		/*
		 * Pour convertir paquet en TCP
		 * 
		 * TCPPacket p = (TCPPacket)packet;
		 * 
		 * // Get the tcp src and dest ports int destPort = p.dst_port; int
		 * srcPort = p.src_port;
		 * 
		 * // Get the src and dest IP addresses from the IP layer InetAddress
		 * destIp = p.dst_ip; InetAddress srcIp = p.src_ip;
		 */

		String contentType = Hex.encodeHexString(packet.data).substring(0, 2);
		String handshakeType = Hex.encodeHexString(packet.data).substring(4, 6);
		switch (contentType) {
		case "16":
			System.out.println("22 Handshake : " + contentType);
			break;
		case "17":
			System.out.println("23 Application Data : " + contentType);
			break;
		default:
			System.out.println("Autre : " + contentType);
		}
		
		switch (handshakeType) {
		case "01":
			System.out.println("1 Client Hello : "+handshakeType);
			break;
		case "02":
			System.out.println("2 Server Hello : "+handshakeType);
			break;
		case "10":
			System.out.println("16 Client Key Exchange : "+handshakeType);
			break;
		case "0b":
			System.out.println("11 Certificate : "+handshakeType);
			break;
		case "14":
			System.out.println("20 Change Cypher Spec : "+handshakeType);
			break;
		default:
			System.out.println("Autre : " + handshakeType);
		}
		
		/*JpcapSender sender;
		try {
			sender = JpcapSender.openDevice(outputInterface);
			sender.sendPacket(packet);
			System.out.println("Sent !");
			sender.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}
}