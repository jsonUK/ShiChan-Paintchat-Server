package paintchat_emu;

import java.util.Hashtable;

public class EmuConvert {
	
	
	private static Hashtable <String,String> clzConvertTable;
	
	static {
		clzConvertTable = new Hashtable<String, String>();
		
		clzConvertTable.put("CONNECTION_PORT_PAINTCHAT", "Connection_Port_PaintChat");
		clzConvertTable.put("CONNECTION_MAX", "Connection_Max");
		clzConvertTable.put("CONNECTION_TIMEOUT", "Connection_Timeout");
		clzConvertTable.put("CONNECTION_HOST", "Connection_Host");
		
		clzConvertTable.put("ADMIN_PASSWORD", "Admin_Password");
		clzConvertTable.put("SERVER_USER_MAX", "Server_User_Max");
		clzConvertTable.put("SERVER_USER_GUEST", "Server_User_Guest");
		clzConvertTable.put("SERVER_INFORMATION", "Server_Infomation");
		clzConvertTable.put("SERVER_LOG_LINE", "Server_Log_Line");
		clzConvertTable.put("SERVER_LOG_SERVER", "Server_Log_Server");
		clzConvertTable.put("SERVER_DEBUG", "Server_Debug");
		clzConvertTable.put("SERVER_LOG_TEXT", "Server_Log_Text");
		clzConvertTable.put("SERVER_LOAD_LINE", "Server_Load_Line");
		clzConvertTable.put("SERVER_CASH_TEXT", "Server_Cash_Text");
		clzConvertTable.put("SERVER_CASH_LINE", "Server_Cash_Line");
		clzConvertTable.put("SERVER_CASH_LINE_SIZE", "Server_Cash_Line_Size");
		clzConvertTable.put("SERVER_CASH_TEXT_SIZE", "Server_Cash_Text_Size");
		clzConvertTable.put("SERVER_LOG_LINE_DIR", "Server_Log_Line_Dir");
		clzConvertTable.put("SERVER_LOG_TEXT_DIR", "Server_Log_Text_Dir");
		
		clzConvertTable.put("CLIENT_IMAGE_WIDTH", "Client_Image_Width");
		clzConvertTable.put("CLIENT_IMAGE_HEIGHT", "Client_Image_Height");
		clzConvertTable.put("CLIENT_SOUND", "Client_Sound");
		
		clzConvertTable.put("COMMENTSTRING", "commentString");
		clzConvertTable.put("APP_ISCONSOLE", "App_IsConsole");
		clzConvertTable.put("APP_VERSION", "App_Version");
		clzConvertTable.put("FILE_PAINTCHAT_INFOMATION", "File_PaintChat_Infomation");
		clzConvertTable.put("APP_SHOWSTARTHELP", "App_ShowStartHelp");		  
		clzConvertTable.put("ADMINISTRATORNAME", "administratorName");
	}
	
	/**
	 * CLean the XML tag that is automatically converted to upper case to its proper casing to be used
	 * @param dirty
	 * @return
	 */
	public static String clean(String dirty) {
		String clean = clzConvertTable.get(dirty);
		if(clean == null) {
			return dirty;
		}
		return clean;
	}
	  
}
