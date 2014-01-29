import java.net.*;
import java.io.*;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.packet.*;

public class Traceroute {
	/* variables */
	JpcapCaptor captor;
	NetworkInterface[] list;
	String str, info;
	int x, choice;

	public static void main(String args[]) throws Exception{
		new Traceroute();
	}

	public Traceroute() throws IOException {

		NetworkInterface[] devices = JpcapCaptor.getDeviceList();

		int i = 0;
		System.out.println(i + ": " + devices[i].name + "("	+ devices[i].description + ")");

		// print out its datalink name and description
		System.out.println(" datalink: " + devices[i].datalink_name + "(" + devices[i].datalink_description + ")");

		// print out its MAC address
		System.out.print(" MAC address:");
		for (byte b : devices[i].mac_address)
			System.out.print(Integer.toHexString(b & 0xff) + ":");
		System.out.println();

		// print out its IP address, subnet mask and broadcast address
		for (NetworkInterfaceAddress a : devices[i].addresses)
			System.out.println(" address:" + a.address + " " + a.subnet + " " + a.broadcast);
		int index = 0; // set index of the interface that you want to open.

		// Open an interface with openDevice(NetworkInterface intrface, int
		// snaplen, boolean promics, int to_ms)
		try {
			JpcapCaptor captor = JpcapCaptor.openDevice(devices[index], 65535, false, 20);
			captor.setFilter("ip and tcp", true);
			captor.loopPacket(10, new PacketPrinter());
			captor.close();
		} catch (Exception e) {
			System.out.println("Exception");
		}
		
		//open a network interface to send a packet to
		JpcapSender sender=JpcapSender.openDevice(devices[index]);

		//create a TCP packet with specified port numbers, flags, and other parameters
		TCPPacket p=new TCPPacket(12,34,56,78,false,false,false,false,true,true,true,true,10,10);

		//specify IPv4 header parameters
		p.setIPv4Parameter(0,false,false,false,0,false,false,false,0,1010101,100,IPPacket.IPPROTO_TCP,
		  InetAddress.getByName("www.microsoft.com"),InetAddress.getByName("www.google.com"));

		//set the data field of the packet
		p.data=("data").getBytes();

		//create an Ethernet packet (frame)
		EthernetPacket ether=new EthernetPacket();
		//set frame type as IP
		ether.frametype=EthernetPacket.ETHERTYPE_IP;
		//set source and destination MAC addresses
		ether.src_mac=new byte[]{(byte)0,(byte)1,(byte)2,(byte)3,(byte)4,(byte)5};
		ether.dst_mac=new byte[]{(byte)0,(byte)6,(byte)7,(byte)8,(byte)9,(byte)10};

		//set the datalink frame of the packet p as ether
		p.datalink=ether;

		//send the packet p
		sender.sendPacket(p);
		System.out.println("Sent !");
		sender.close();
		
		/*TCPPacket p = (TCPPacket)packet;

		// Get the tcp src and dest ports
		int destPort = p.dst_port;
		int srcPort = p.src_port;

		// Get the src and dest IP addresses from the IP layer
		InetAddress destIp = p.dst_ip;
		InetAddress srcIp = p.src_ip;*/
		// }
	}
}
