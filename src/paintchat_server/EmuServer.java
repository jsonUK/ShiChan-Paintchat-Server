package paintchat_server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import paintchat.Config;
import paintchat.Debug;
import paintchat.MgText;
import paintchat_emu.EmuConfig;
import syi.util.Vector2;

public class EmuServer {


	private Server server;
	private Config config;
	private EmuConfig emuConfig;
	private String name;
	private Debug debug;
	private boolean running;
	private static final Vector<String> EMPTY_USERS = new Vector<String>();
	
	public EmuServer(String name, Config c, EmuConfig ec, Debug d) {
		this.name = name;
		this.config = c; 
		this.debug = d;
		this.emuConfig = ec;
	}
	
	public String getName() {
		return name;
	}
	
	public synchronized void startServer() {
		if(!running) {
			running = true;
			if(server == null) {
				server = new Server(config, debug);
			}
			server.init();
		}
	}
	
	public synchronized void stopServer() {
		if(running) {
			running = false;
			server.exitServer();
			server = null;
		}
	}
	
	public int getLogSize() {
		if(!running) {
			return -1;
		}
		return server.getData().getTalkerLineWork().getLogSize();
	}
	
	public synchronized void saveLog(String destFileName) throws Exception {
		server.getData().getTalkerLineWork().saveLog(emuConfig.getString(EmuConfig.USER_LOG_SAVE_DIR) + File.separator + destFileName + ".tmp");
	}
	
	public synchronized void loadLog(String srcFileName) throws IOException {
		stopServer();
		
		// remove cash line file
		String lineLogDir = config.getString(Config.CF_LOG_LINE_DIR);		
				
		File destLog = new File(lineLogDir, "cash.tmp");
		File srcFile = new File(emuConfig.getString(EmuConfig.USER_LOG_SAVE_DIR) + File.separator + srcFileName + ".tmp");
		
		if(!srcFile.exists()){
			throw new IOException("Log file does not exist to load");
		}
				
		copyTo(srcFile, destLog);
		
		startServer();
	}
	
	public Vector<String> getUsers() {
		if(!running) {
			return EMPTY_USERS;
		}
		
		Vector<String> users = new Vector<String>();
		Vector2 mgUsers = new Vector2();
		server.getData().getTalkerTextWork().getUsers(mgUsers);
		
		int size = mgUsers.size();
		for(int i=0; i < size; i++) {
			users.add(((MgText) mgUsers.get(i)).toString());
		}
	
		return users;
	}
	
	/**
	 * Add a new client to the server
	 * @param client
	 */
	public void addConnection(Socket client) {
		server.connect(client);
	}
	
	public void sendMsg(String msg) {
		sendMsg(msg, "SYSTEM");
	}
	
	public void sendMsg(String msg, String sender) {
		if(!running){
			return;
		}
				
		MgText mgtext = new MgText((byte) 12, "ADMIN[" + sender + "]> " + msg);
		server.getData().getTalkerTextWork().sendText(-1, mgtext);
	}
	
	/**
	 * Attempt to remove this user
	 * @param username
	 * @return true, if successful
	 */
	public boolean kickUser(String username) {
		if(!running) {
			return false;
		}
	
		try {			
			return server.getData().getTalkerTextWork().killTalker(username, new Vector());			
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}		
	}
	
	/**
	 * Reset server and clear graphics log file
	 * @return
	 */
	public synchronized void resetDeleteLog() throws Exception {
		server.getData().getTalkerLineWork().saveLog(config.getString(EmuConfig.SERVER_ARCHIVE_PATH) + getTimestampFileString() + ".tmp");
		
		stopServer();		
		
		// remove cash line file
		String lineLogDir = config.getString(Config.CF_LOG_LINE_DIR);
		String textLogDir = config.getString(Config.CF_LOG_TEXT_DIR);			
		
		File lineLog = new File(lineLogDir, "cash.tmp");
		if(lineLog.exists()) {
			lineLog.delete();
		}
		
		File textLog = new File(textLogDir, "cash_txt.tmp");
		if(textLog.exists()) {		
			textLog.delete();
		}
		
		startServer();
	}
	
	/**
	 * Note: duplicate method found in arhive line
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	private static final void copyTo(File src, File dest) throws IOException {
		InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dest);

	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0){
	    	out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();		
	}	
	
	/**
     * get timestamp in a nice way that can be sorted easily by filename
     * NOTE: duplicate method found in ArchiveLine
     * @return
     */
    private static String getTimestampFileString() {
    	SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
    	return df.format(new Date());
    }
}
