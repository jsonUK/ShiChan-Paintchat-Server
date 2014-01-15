package paintchat_emu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import nanoxml.XMLElement;
import paintchat_server.EmuServer;

public class CmdParser {
	
	private static final String CMD_GET_USERS = "userlist";
	private static final String CMD_RESET_SERVER = "reset";
	private static final String CMD_SHUTDOWN = "shutdown";
	private static final String CMD_KICKUSER = "kickuser";
	private static final String CMD_RESET_SERVER_DELETE_LOG = "reset_delete";
	private static final String CMD_SEND_MSG = "sendmsg";
	private static final String CMD_LOAD_LOG = "loadlog";
	private static final String CMD_SAVE_LOG = "savelog";
	
	private static Vector<XMLElement> cached_users = new Vector<XMLElement>();
	private static long last_time_requested_users;
	
	public static void handleCmd(ServerManager mgr, BufferedReader reader, PrintWriter writer) {						
		try {
			String cmd = reader.readLine();
			System.out.println("Processing cmd: " + cmd);					
			
			// setup XML response format
			XMLElement root = new XMLElement();
			root.setName("response");					
						
			try {
				if(cmd.equals(CMD_GET_USERS)) {
					doCmdGetUsers(mgr,  root);					
				}
				
				else if(cmd.equals(CMD_RESET_SERVER)) {
					doCmdResetServer(mgr, root, reader);
				}
				
				else if(cmd.equals(CMD_SHUTDOWN)) {
					doCmdShutdown(mgr);
				}
				
				else if(cmd.equals(CMD_RESET_SERVER_DELETE_LOG)) {
					doCmdResetServerDeleteLog(mgr, root, reader);
				}
				
				else if(cmd.equals(CMD_KICKUSER)) {
					doCmdKickUser(mgr, root, reader);
				}
				else if(cmd.equals(CMD_SEND_MSG)) {
					doCmdSendMsg(mgr, root, reader);
				}
				else if(cmd.equals(CMD_LOAD_LOG)) {
					doCmdLoadLog(mgr, root, reader);
				}
				else if(cmd.equals(CMD_SAVE_LOG)) {
					doCmdSaveLog(mgr, root, reader);
				}
				else {
					throw new Exception("Cmd not Found");
				}
				
				// success
				root.setAttribute("exitcode", "0");
			}
			// if any errors caught, change exit code so requester knows about them
			catch(Exception e) {
				root.setAttribute("exitcode", "-1");
				root.setAttribute("error", e.getMessage());
				e.printStackTrace();
			}
			
			root.write(writer);			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void doCmdSendMsg(ServerManager mgr, XMLElement root, BufferedReader reader) throws Exception {
		String serverID = reader.readLine();
		String sender = reader.readLine();
		String msg = reader.readLine();
		
		// check to see if we selected the server
		if(! serverID.equals("ALL")) {
			EmuServer server = mgr.getServer(serverID);
			if(server == null) {
				throw new Exception("server ID: [" + serverID + "] unknown to server list");
			}
			server.sendMsg(msg, sender);
		}
		// global
		else {
			for(EmuServer server : mgr.getServers()) {
				server.sendMsg(msg, sender);
			}
		}
	}
	
	private static void doCmdLoadLog(ServerManager mgr, XMLElement root, BufferedReader reader) throws Exception {
		String serverID = reader.readLine();
		String filename = reader.readLine();
		
		// check to see if we selected the server		
		EmuServer server = mgr.getServer(serverID);
		if(server == null) {
			throw new Exception("server ID: [" + serverID + "] unknown to server list");
		}							
		
		server.loadLog(filename);
	}
	
	private static void doCmdSaveLog(ServerManager mgr, XMLElement root, BufferedReader reader) throws Exception {
		String serverID = reader.readLine();
		String filename = reader.readLine();
		
		// check to see if we selected the server		
		EmuServer server = mgr.getServer(serverID);
		if(server == null) {
			throw new Exception("server ID: [" + serverID + "] unknown to server list");
		}					
		
		server.saveLog(filename);
				
		root.setAttribute("logsize" , server.getLogSize());
	}
	
	private static void doCmdKickUser(ServerManager mgr, XMLElement root, BufferedReader reader) throws Exception {
		String serverID = reader.readLine();
		
		EmuServer server = mgr.getServer(serverID);
		if(server == null) {
			throw new Exception("server ID: [" + serverID + "] unknown to server list");
		}
		
		String userName = reader.readLine();
		if( ! server.kickUser(userName) ) {
			throw new Exception("Kick failed: User does not exist: " + userName);
		}
	}
	
	private static void doCmdResetServer(ServerManager mgr, XMLElement root, BufferedReader reader) throws Exception { 
		String serverID = reader.readLine();
		
		EmuServer server = mgr.getServer(serverID);
		if(server == null) {
			throw new Exception("server ID: [" + serverID + "] unknown to server list");
		}
		
		server.stopServer();
		server.startServer();
	}
	
	private static void doCmdResetServerDeleteLog(ServerManager mgr, XMLElement root, BufferedReader reader) throws Exception {
		String serverID = reader.readLine();
		
		EmuServer server = mgr.getServer(serverID);
		if(server == null) {
			throw new Exception("server ID: [" + serverID + "] unknown to server list");
		}
		
		server.resetDeleteLog();
	}
	
	public static void doCmdShutdown(ServerManager mgr) throws Exception {
		mgr.shutdownServers();
	}
	
	
	private static synchronized void doCmdGetUsers(ServerManager mgr, XMLElement root) throws IOException {
		// if we have requested this twice w/in last 5 seconds, return the cache 
		if((System.currentTimeMillis() - last_time_requested_users) < 1500 && cached_users != null) {
			for(XMLElement e : cached_users) {
				root.addChild(e);
			}
			return;
		}
		// clear cache
		cached_users.clear();
		
		// build XML output
		for(EmuServer server : mgr.getServers()) {
			XMLElement serverXML = new XMLElement();
			serverXML.setName("server");
			serverXML.setAttribute("id", server.getName());
			serverXML.setAttribute("size", server.getLogSize());
			
			// now add all users
			for(String userName : server.getUsers()) {
				XMLElement userXML = new XMLElement();
				userXML.setName("user");
				userXML.setAttribute("name", userName);
				serverXML.addChild(userXML);
			}
			
			root.addChild(serverXML);
			cached_users.add(serverXML);
			last_time_requested_users = System.currentTimeMillis();			
		}
		
	}
}
