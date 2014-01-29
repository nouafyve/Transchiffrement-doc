import jpcap.PacketReceiver;
import jpcap.packet.Packet;

class PacketPrinter implements PacketReceiver {
  //this method is called every time Jpcap captures a packet
  public void receivePacket(Packet packet) {
    //just print out a captured packet
	  System.out.println("Paquet : "+packet);
    //System.out.println("Paquet : "+new String(packet.header));
  }
}