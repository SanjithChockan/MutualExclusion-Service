import com.sun.nio.sctp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SCTPServer implements Runnable {
	// Port number to open server for clients to connect
	// Client should connect to same port number that server opens
	int PORT;

	// Size of ByteBuffer to accept incoming messages
	int MAX_MSG_SIZE = 4096;
	MEService mutex;

	public SCTPServer(int PORT, MEService mutex) {
		this.mutex = mutex;
		this.PORT = PORT;
	}

	public void start() throws Exception {
		InetSocketAddress addr = new InetSocketAddress(PORT); // Get address from port number
		SctpServerChannel ssc = SctpServerChannel.open();// Open server channel
		ssc.bind(addr);// Bind server channel to address
		System.out.println("Server started...");
		// Loop to allow all clients to connect
		while (true) {
			SctpChannel sc = ssc.accept(); // Wait for incoming connection from client
			System.out.println("Client connected");
			new Thread(new ClientHandler(sc)).start();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			this.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class ClientHandler implements Runnable {

		SctpChannel sc;
		// Terminal text colors
		String RESET = "\u001B[0m";
		String RED = "\u001B[31m";
		String GREEN = "\u001B[32m";
		String YELLOW = "\u001B[33m";
		String ANSI_WHITE = "\u001B[37m";
		String BLUE = "\u001B[34m";
		String PURPLE_BOLD = "\033[1;35m";
		String CYAN_UNDERLINED = "\033[4;36m";

		public ClientHandler(SctpChannel sc) {
			this.sc = sc;
		}

		@Override
		public void run() {
			try {
				while (true) {
					ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
					sc.receive(buf, null, null);
					Message rcvMsg = Message.fromByteBuffer(buf);
					// System.out.println(rcvMsg.message);

					// System.out.println("Before handling Requests and replies");
					// receive REQUESTS for key from another node
					synchronized (mutex) {

						if (rcvMsg.msgType == MessageType.REQUEST) {
							// mutex.handleRequest(rcvMsg);
							System.out.println(YELLOW + "Received key request from " + rcvMsg.NodeID + RESET);
							mutex.handleRequest(rcvMsg);
							// mutex.accessSharedKeys(false, true, false, false, false, false, rcvMsg);
							System.out.println(YELLOW + "Handled request for " + rcvMsg.NodeID + RESET);
						}
						// receive PERMISSION (shared key) from another node
						else if (rcvMsg.msgType == MessageType.REPLY) {
							// mutex.handleReply(rcvMsg);
							System.out.println(YELLOW + "Received permission from " + rcvMsg.NodeID + RESET);
							mutex.handleReply(rcvMsg);
							// mutex.accessSharedKeys(false, false, true, false, false, false, rcvMsg);
							System.out.println(YELLOW + "updated permission for " + rcvMsg.NodeID + RESET);
						}
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
