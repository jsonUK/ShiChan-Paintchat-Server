package paintchat_emu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import nanoxml.XMLElement;
import paintchat.Config;
import paintchat.Debug;
import paintchat_server.EmuServer;

public class ServerManager {

	/**
	 * Hasht able to map server names to their configs
	 */
	private static final String SERVER_ID = "SERVER_ID";
	private Hashtable<String,EmuServer> serverTable;
	private NetInterface netInterface;
	private Debug debug;
	private EmuConfig emuConfig;
	
	public static void main(String [] argz) {
		String configFile = null;
		if(argz.length > 0) {
			configFile = argz[0];
		}
		else {
			configFile = "cnf/servers.xml";
		}
		
		ServerManager st = new ServerManager(configFile);
		st.startServers();		
		
		// test threads
		String quitMsg = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String line;			
			while( (line = in.readLine()) != null )  {
				if(line.startsWith("quit")) {
					String [] quitline = line.split(" ", 2);
					quitMsg = quitline.length > 1? quitline[1] : null;
					break;
				}
				else if(line.startsWith("test")) {
					st.test();
				}
			}
		}
		catch(Exception e) {
			//e.printStackTrace();
		}		
		finally {
			try {
				st.shutdownServers(quitMsg == null? "Servers Shutting down!" : quitMsg);
			}
			catch(Exception e){}
		}
		System.exit(0);
	}
	
	/**
	 * 
	 * @param configFile
	 */
	protected ServerManager(String configFile) {
		serverTable = new Hashtable<String, EmuServer>();
		// redirect debug output here
		debug = new Debug(null);		
		debug.write_file = new BufferedWriter(new OutputStreamWriter(System.out));
		debug.bool_debug = true;
		
		// builds serverTable and emuConfig from xml file				
		loadConfig(configFile);		
	}
	
	private void test() throws Exception {
		for(EmuServer s : serverTable.values()) {
			s.sendMsg("TESTING");
			System.out.println("test complete");
		}
	}
	
	/**
	 * Start servers and all interfaces attached to servers
	 */
	public void startServers() {
		try {
			// start each pchat server
			for(EmuServer s : serverTable.values()) {
				s.startServer();
			}
			// intersocket connector for cmds
			if(netInterface == null) {
				netInterface = new NetInterface(this, emuConfig);
			}
			netInterface.Start();									
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void shutdownServers() throws Exception {
		shutdownServers(null);
	}	
	
	public void shutdownServers(final String msg) throws Exception{	
		// start each pchat server		
		for(final EmuServer s : serverTable.values()) {
			// create the thread
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						if(msg != null) {
							s.sendMsg("**********************************");
							s.sendMsg("* " + msg);
							s.sendMsg("**********************************");
							Thread.sleep(5000);	// wait for msg to propogate to user before shutting down
						}
						
						s.stopServer();						
					}
					catch (Exception e) {
						
					}
				}
			});			
			// launch it
			t.start();
		}			
		
		netInterface.Stop();
		if(msg != null) {
			try {
				Thread.sleep(8000);	// sleep 8 secs to wait for each thread to finish 5 secs of work
			}
			catch(Exception e) {
			
			}
		}
	}
	
	/**
	 * Required to be unique values, but not critical to assume server has control over this so we control this  
	 * @param serverTable
	 */
	private void putServerDefaults(Hashtable<String,String> serverTable, EmuConfig config) {
		String id = serverTable.get(SERVER_ID);
		serverTable.put(Config.CF_LOG_LINE_DIR, "logs/server_" + id);
		serverTable.put(Config.CF_LOG_TEXT_DIR, "logs/server_" + id);
		serverTable.put("Server_Log_Text", "true");
		serverTable.put("Server_Load_Line", "true");
		serverTable.put("Server_Cash_Line_Size", "18048000");
		serverTable.put("Server_Cash_Text_Size", "200");
		serverTable.put(EmuConfig.SERVER_ARCHIVE_PATH, config.getString(EmuConfig.ARCHIVE_LOG_SAVE_DIR) + File.separator + id + "_");
	}

	/**
	 * Create config files for each server that will be loaded
	 * @param configFile
	 */
	private void loadConfig(String configFile) {
		// keep a separate table for default settings as we will refer back to those to build config files 
		Hashtable<String,String> defaultTable = new Hashtable<String, String>();
		Vector<Hashtable<String,String>> serverList = new Vector<Hashtable<String,String>>();
		Hashtable<String,String> emuConfigTable = new Hashtable<String, String>();
		
		
		try {
			XMLElement e = new XMLElement();
			e.parseFromReader(new FileReader(configFile));		
			
			Vector<XMLElement> children = e.getChildren();
			for(XMLElement child : children) {
				// parse a server config info
				if(child.getName().equalsIgnoreCase("server")) {				
					serverList.add(parseServer(child, new Hashtable<String,String>()));
				}
				// parse the default server info (this will set all the default values of server config unless overridden
				else if(child.getName().equalsIgnoreCase("server-default")) {
					defaultTable = parseServer(child, defaultTable);
				}
				// load the config that is not server specific
				else if(child.getName().equalsIgnoreCase("config")) {
					emuConfigTable.putAll(parseConfig(child));
				}
				else {
					System.err.println("Invalid XML tag: " + child.getName());
				}
			}
			
			// build emuconfig
			emuConfig = new EmuConfig(emuConfigTable);
			
			// sanity check to make sure our configs our in proper format for paintchat
			configSanityCheck(serverList);
						
			// build configs for each server
			for(Hashtable<String,String> serverTable : serverList) {
				Hashtable<String,String> table = new Hashtable<String, String>();			
				table.putAll(defaultTable);
				table.putAll(serverTable);
				putServerDefaults(table, emuConfig);
				// put them in server wrappers for individual obejcts to map to all properties for that server
				EmuServer wrapper = new EmuServer(table.get(SERVER_ID), new Config(table), emuConfig, debug);
				this.serverTable.put(wrapper.getName(), wrapper);
			}
			
			// build log for save files
			(new File(emuConfig.getString(EmuConfig.USER_LOG_SAVE_DIR))).mkdir();		
			(new File(emuConfig.getString(EmuConfig.ARCHIVE_LOG_SAVE_DIR))).mkdir();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * If there are any issues, throw an exception 
	 * @param servers
	 * @throws Exception
	 */
	private void configSanityCheck(Vector<Hashtable<String,String>> serverList) throws Exception {
		HashSet<String> serverIDs = new HashSet<String>();		
		
		for(Hashtable<String,String> tempTable : serverList) {
			String serverID = tempTable.get("SERVER_ID");
						
			if(serverID == null) {
				throwSanity("server_id", true);
			}						
			 
			if(serverIDs.contains(serverID)) {
				throwSanity("server_id", false);
			}						
			
			serverIDs.add(serverID);
		}
	}
	
	/**
	 * Originally a method to throwing an exception baased on parameters because it was common in the sanity
	 * check to throw many exceptions
	 * @param attribute
	 * @param isNullError
	 * @throws Exception
	 */
	private void throwSanity(String attribute, boolean isNullError) throws Exception {
		if(isNullError) {
			throw new Exception("Invalid <server> tag, it is required to have a " + attribute + "=\"val\" attribute");
		}
		else {
			throw new Exception("ERROR: Multiple " + attribute + "s found of the same value. Each one must be unique.");
		}			
	}
	
	/**
	 * Parse an XML Server node and return the hashtable
	 * @param e
	 * @return
	 */
	private Hashtable<String,String> parseServer(XMLElement e, Hashtable<String,String> table) {		 
		Enumeration<String> attrNames = e.enumerateAttributeNames();
		while(attrNames.hasMoreElements()) {
			String attribute = attrNames.nextElement();
			table.put(EmuConvert.clean(attribute), e.getStringAttribute(attribute));
		}		
		return table;
	}
	
	/**
	 * Parse the XMLElement which belongs to the server config
	 * the server config puts elements into EmuConfig, which should by, 
	 * development, all be uppercase to reflect the XML parsing attribute name
	 * 
	 * @param e config element
	 * @return the raw hashtable that can be used with the EmuConfig constructor
	 */
	private Hashtable<String,String> parseConfig(XMLElement e) {
		Hashtable<String,String> table = new Hashtable<String,String>();
		Enumeration<String> attrNames = e.enumerateAttributeNames();
		while(attrNames.hasMoreElements()) {
			String attribute = attrNames.nextElement();
			table.put(attribute, e.getStringAttribute(attribute));
		}		
		return table;
	}
	
	/**
	 * Get servers
	 * @return
	 */
	public Collection<EmuServer> getServers() {
		return serverTable.values();
	}
	
	/**
	 * Get server emu of this server id
	 * @param serverID
	 * @return
	 */
	public EmuServer getServer(String serverID) {
		return serverTable.get(serverID);
	}
	
	class TestThread implements Runnable {
		
		
		@Override
		public void run() {
			
			while(true) {
			
				try {				
					Thread.sleep(60000);
				}catch(Exception e) {}
				
				for(EmuServer sw : ServerManager.this.serverTable.values()) {
					System.out.println(sw.getName() + ": "+ sw.getUsers());
				}				
			}
		}
		
	}
}
