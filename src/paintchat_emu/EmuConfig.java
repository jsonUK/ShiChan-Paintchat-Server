package paintchat_emu;

import java.util.Hashtable;

import syi.util.PProperties;

public class EmuConfig extends PProperties {

	public static final String NET_META_PORT = "PHP_PORT";
	public static final String NET_PCHAT_PORT = "PCHAT_PORT";
	public static final String USER_LOG_SAVE_DIR = "USER_LOG_SAVE_DIR";
	public static final String ARCHIVE_LOG_SAVE_DIR = "ARCHIVE_LOG_SAVE_DIR";
	
	//does not need a default, this is generated per server, this should be like the "ARCHIVE_LOG_SAVE_DIR + \<server_id>_"
	public static final String SERVER_ARCHIVE_PATH = "ARCHIVE_PATH";
	
	public EmuConfig(Hashtable props) {		
		putAll(props);
		putDefaults();
	}
	
	
	
	private void putDefaults() {
		putDef(NET_META_PORT, "31234");
		putDef(NET_PCHAT_PORT, "31235");
		putDef(USER_LOG_SAVE_DIR, "custom_saves");
		putDef(ARCHIVE_LOG_SAVE_DIR, "archive_saves"); 		
	}
	
	private void putDef(String key, String defValue) {
		put(key, getString(key, defValue));
	}
}
