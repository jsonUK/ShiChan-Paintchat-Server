package paintchat_emu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import paintchat_server.EmuServer;

public class NetInterface implements Runnable {

	private ServerSocket metaSocket;
	private ServerSocket pchatSocket;
	private ServerManager manager;
	private EmuConfig config;
	
	private Thread metaThread;
	private Thread pchatThread;
	private boolean running;
	
	
	public NetInterface(ServerManager manager, EmuConfig config) throws Exception {
		this.manager = manager;
		this.config = config;
		
		// only 50 connectios at once (still a bit overkill), since only our localhost should be making these connections
		this.metaSocket = new ServerSocket(config.getInt(EmuConfig.NET_META_PORT), 50);
		this.pchatSocket = new ServerSocket(config.getInt(EmuConfig.NET_PCHAT_PORT), 255);
		
		metaThread = new Thread(this, "meta");
		pchatThread = new Thread(this, "pchat");
		pchatThread.setDaemon(true);
		pchatThread.setPriority(1);		
	}
	
	public synchronized void Start() {
		if(!running) {
			running = true;
			metaThread.start();
			System.out.println("Listening on port: " + pchatSocket.getLocalPort());
			pchatThread.start();
		}		
	}
	
	public synchronized void Stop() {
		if(running) {
			metaThread.interrupt();
			pchatThread.interrupt();
			running = false;
		}
	}


	@Override
	public void run() {
		try {
			switch (Thread.currentThread().getName().charAt(0))
			{
			case 'm':
				runMetaConnection();
			    break;		
			case 'p':
				runPChatConnection();
				break;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void runMetaConnection() {
		while(running) {
			try {
				Socket client = this.metaSocket.accept();
				client.setSoTimeout(60000);
				new SocketHandler(client).start();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void runPChatConnection() {
		while(running) {
			try {
				Socket client = this.pchatSocket.accept();
				client.setSoTimeout(600000);
				
				connectClient(client);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void connectClient(Socket client) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));		
		OutputStreamWriter writer = new OutputStreamWriter(client.getOutputStream());
		
		// get destination server
		reader.readLine();	// clear buffer
		String serverID = reader.readLine();
		System.out.println(serverID);
		
		EmuServer server = manager.getServer(serverID);		
		// if failed, close and return
		if(server == null) {
			writer.write(1);
			writer.flush();
			writer.close();
			reader.close();				
			client.close();
			return;
		}
		
		// send OK signal back
		writer.write(0);
		writer.flush();
		
		// forward connection to server
		server.addConnection(client);
	}
	
	class SocketHandler implements Runnable {

		private Socket client;
		private boolean running;			
		
		public SocketHandler(Socket s) {
			client = s;
			running = false;
		}
		
		public synchronized void start() {
			if(!running) {
				running = true;
				new Thread(this).start();
			}
		}
		
		@Override
		public void run() {			
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));		
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
				// process command
				CmdParser.handleCmd(manager, reader, writer);
				
				writer.flush();
				writer.close();
				reader.close();				
				client.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}			
		}				
	}
}
