// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 6/1/2009 9:47:01 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   ArchiveLine.java

package paintchat_server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import paintchat.Config;
import paintchat.Mg;
import paintchat_emu.EmuConfig;
import syi.util.ByteStream;
import syi.util.VectorBin;

// Referenced classes of package paintchat_server:
//            TempLine

public class ArchiveLine
{

    public ArchiveLine(Config config1)
    {
        lineBlock = new VectorBin();
        lines = new ByteStream();
        work = new ByteStream();
        workBuffer = new byte[10240];
        mgline = new Mg();
        cashLength = 0;
        config = config1;
        inflater = new Inflater(false);
        deflater = new Deflater(9, false);
        init();
    }

    public void add(Mg mg)
    {
        if(mg.iHint == Mg.H_CLEAR)
        {
            saveTo(config.getString(EmuConfig.SERVER_ARCHIVE_PATH) + getTimestampFileString() + ".tmp");
            tmp.delete();
            clear();
            return;
        }
        if(!isCash)
            return;
        mg.get(lines, work, lines.size() != 0 ? mgline : null);
        mgline.set(mg);
        if(lines.size() >= sizeBlock)
            flush();
    }
    
    public synchronized boolean saveTo(String destination) {
    	flush();
    	tmp.save();
    	
    	File srcFile = new File(config.getString(Config.CF_LOG_LINE_DIR), "cash.tmp");
    	File destFile = new File(destination);
    	
    	if(!srcFile.exists()) {
    		return false;
    	}
    	try {
    		copyTo(srcFile, destFile);
    	}
    	catch(Exception e) {
    		System.err.println("Could not save line file");
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }
    
    /**
     * Note: duplicate method found in EmuServer
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
     * NOTE: duplicate method found in EmuServer
     * @return
     */
    private static String getTimestampFileString() {
    	SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
    	return df.format(new Date());
    }

    public synchronized void clear()
    {
        flush();
        lines.reset();
        lineBlock.removeAll();
        cashLength = 0;
    }

    public synchronized void close()
    {
        flush();
        tmp.save();
        clear();
    }

    public void deflate(ByteStream bytestream, byte abyte0[], int i, int j)
    {
        deflater.reset();
        deflater.setInput(abyte0, i, j);
        deflater.finish();
        int k;
        while(!deflater.finished()) 
            if((k = deflater.deflate(workBuffer, 0, workBuffer.length)) > 0)
                bytestream.write(workBuffer, 0, k);
    }

    private void flush()
    {
        int i = lines.size();
        if(i == 0)
            return;
        work.reset();
        deflate(work, lines.getBuffer(), 0, i);
        for(cashLength += work.size(); cashLength >= cashMax && lineBlock.size() > 0; lineBlock.remove(1))
            cashLength -= lineBlock.get(0).length;

        lineBlock.add(work.toByteArray());
        lines.reset();
        tmp.write(work);
    }

    public void getLog(VectorBin vectorbin, ByteStream bytestream)
        throws IOException
    {
        lineBlock.copy(vectorbin);
        lines.writeTo(bytestream);
    }

    public void inflate(ByteStream bytestream, byte abyte0[], int i, int j)
    {
        inflater.reset();
        inflater.setInput(abyte0, i, j);
        try
        {
            while(!inflater.needsInput()) 
            {
                int k = inflater.inflate(workBuffer, 0, workBuffer.length);
                if(k > 0)
                    bytestream.write(workBuffer, 0, k);
            }
        }
        catch(DataFormatException _ex) { }
    }

    private void init()
    {
        Config config1 = config;
        isCash = config1.getBool("Server_Cash_Line", true);
        cashMax = config1.getInt("Server_Cash_Line_Size", 0x7d000);
        sizeBlock = 20000;
        tmp = new TempLine(config1);
        tmp.load(lineBlock);
    }

    public int size()
    {
        return lineBlock.getSizeBytes() + lines.size();
    }

    private Config config;
    private VectorBin lineBlock;
    private ByteStream lines;
    private TempLine tmp;
    private ByteStream work;
    private byte workBuffer[];
    private Mg mgline;
    private Deflater deflater;
    private Inflater inflater;
    private boolean isCash;
    private int sizeBlock;
    private int cashMax;
    private int cashLength;
}
