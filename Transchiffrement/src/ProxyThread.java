import java.net.*;
import java.io.*;

/*
 Java Transparent Proxy
 Copyright (C) 1999 by Didier Frick (http://www.dfr.ch/)
 This software is provided under the GNU general public license (http://www.gnu.org/copyleft/gpl.html).
 */

public class ProxyThread extends Thread {

	protected class StreamCopyThread extends Thread {
		private Socket inSock;
		private Socket outSock;
		private boolean done = false;
		private StreamCopyThread peer;

		public StreamCopyThread(Socket inSock, Socket outSock) {
			this.inSock = inSock;
			this.outSock = outSock;
		}

		public void run() {
			byte[] buf = new byte[bufSize];
			int count = -1;
			try {
				InputStream in = inSock.getInputStream();
				OutputStream out = outSock.getOutputStream();
				try {
					while (((count = in.read(buf)) > 0) && !isInterrupted()) {
						System.out.println(new String(buf));
						out.write(buf, 0, count);
					}
				} catch (Exception xc) {
					if (debug) {
						err.println(header + ":" + xc);
						xc.printStackTrace();
					}
				} finally {
					// The input and output streams will be closed when the
					// sockets themselves
					// are closed.
					out.flush();
				}
			} catch (Exception xc) {
				err.println(header + ":" + xc);
				xc.printStackTrace();
			}
			synchronized (lock) {
				done = true;
				try {
					if ((peer == null) || peer.isDone()) {
						// Cleanup if there is only one peer OR
						// if _both_ peers are done
						inSock.close();
						outSock.close();
					} else
						// Signal the peer (if any) that we're done on this side
						// of the connection
						peer.interrupt();
				} catch (Exception xc) {
					err.println(header + ":" + xc);
					xc.printStackTrace();
				} finally {
					connections.removeElement(this);
				}
			}
		}

		public boolean isDone() {
			return done;
		}

		public void setPeer(StreamCopyThread peer) {
			this.peer = peer;
		}

	}

	// Holds all the currently active StreamCopyThreads
	private java.util.Vector connections = new java.util.Vector();
	// Used to synchronize the connection-handling threads with this thread
	private Object lock = new Object();
	// The address to forward connections to
	private InetAddress dstAddr;
	// The port to forward connections to
	private int dstPort;
	// Backlog parameter used when creating the ServerSocket
	protected static final int backLog = 100;
	// Timeout waiting for a StreamCopyThread to finish
	public static final int threadTimeout = 2000; // ms
	public static final int lingerTime = 180; // seconds (?)
	public static final int bufSize = 2048;
	private String header;
	private ServerSocket srvSock;
	private boolean debug = false;

	// Log streams for output and error messages
	private PrintStream out;
	private PrintStream err;

	public ProxyThread(int srcPort, InetAddress dstAddr, int dstPort, PrintStream out, PrintStream err) throws IOException {
		this.out = out;
		this.err = err;
		this.srvSock = new ServerSocket(srcPort, backLog);
		this.dstAddr = dstAddr;
		this.dstPort = dstPort;
		this.header = ":" + srcPort + " <-> " + dstAddr + ":" + dstPort;
		start();
	}

	public void run() {
		out.println(header + " : starting");
		try {
			while (!isInterrupted()) {
				Socket serverSocket = srvSock.accept();
				try {
					serverSocket.setSoLinger(true, lingerTime);
					Socket clientSocket = new Socket(dstAddr, dstPort);
					clientSocket.setSoLinger(true, lingerTime);
					StreamCopyThread sToC = new StreamCopyThread(serverSocket, clientSocket);
					StreamCopyThread cToS = new StreamCopyThread(clientSocket, serverSocket);
					sToC.setPeer(cToS);
					cToS.setPeer(sToC);
					synchronized (lock) {
						connections.addElement(cToS);
						connections.addElement(sToC);
						sToC.start();
						cToS.start();
					}
				} catch (Exception xc) {
					err.println(header + ":" + xc);
					if (debug)
						xc.printStackTrace();
				}
			}
			srvSock.close();
		} catch (IOException xc) {
			err.println(header + ":" + xc);
			if (debug)
				xc.printStackTrace();
		} finally {
			cleanup();
			out.println(header + " : stopped");
		}
	}

	private void cleanup() {
		synchronized (lock) {
			try {
				while (connections.size() > 0) {
					StreamCopyThread sct = (StreamCopyThread) connections
							.elementAt(0);
					sct.interrupt();
					sct.join(threadTimeout);
				}
			} 
			catch (InterruptedException xc) {}
		}
	}

	private static void parseConfigFile(String fileName,
			PrintStream out, PrintStream err) throws FileNotFoundException,
			IOException, UnknownHostException {
		FileInputStream in = new FileInputStream(fileName);
		java.util.Properties props = new java.util.Properties();
		props.load(in);
		in.close();
		int srcPort = Integer.parseInt(props.getProperty("proxy.0.sourcePort"));
		InetAddress dstAddr = InetAddress.getByName(props.getProperty("proxy.0.destAddr"));
		int dstPort = Integer.parseInt(props.getProperty("proxy.0.destPort"));
		if (dstPort == 0) {
			throw new IllegalArgumentException("Missing configuration parameter for proxy ");
		}
		new ProxyThread(srcPort, dstAddr, dstPort, out, err);
	}

	public void start(String configFile) throws Exception{
		try {
			parseConfigFile(configFile, System.out, System.err);
		} catch (IllegalArgumentException xc) {
			System.err.println(xc.getMessage());
			System.exit(1);
		}
	}
	
	public ProxyThread(){}
	
	public static void main(String[] argv) throws Exception {
		ProxyThread proxy = new ProxyThread();
		proxy.start(argv[0]);
	}
}
