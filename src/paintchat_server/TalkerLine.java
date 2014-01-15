// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 6/16/2009 8:40:34 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TalkerLine.java

package paintchat_server;

import java.io.*;
import java.net.InetAddress;
import paintchat.Debug;
import paintchat.Mg;
import syi.util.*;

// Referenced classes of package paintchat_server:
//            Talker, Data, ArchiveLine, TalkerLineWork

class TalkerLine extends Talker
{

    public TalkerLine(int i, Data data)
        throws IOException
    {
        super(i, data);
        debug = null;
        mgIn = new Mg();
        mgOut = null;
        isKill = false;
        isLogGet = false;
        lines = new VectorBin();
        bufferIn = new ByteStream();
        bufferOut = new ByteStream();
        bIn = new byte[1024];
        beforeHeader = -1;
        vectorLine = data.getTalkerLineWork();
        debug = super.dd.debug;
    }

    public final boolean addLine(Mg mg, ByteStream bytestream)
    {
        if(isDead())
            return false;
        if(!isLogGet)
            return true;
        synchronized(bufferOut)
        {
            mg.get(bufferOut, bytestream, mgOut);
            if(mgOut == null)
                mgOut = new Mg();
            mgOut.set(mg);
        }
        return true;
    }

    public boolean gc()
    {
        if(!super.gc() && bufferOut.size() >= 0x493e0)
        {
            destroy();
            super.dd.debug.log_d(String.valueOf(super.ID) + " garbage TalkerLine");
            return true;
        } else
        {
            return false;
        }
    }

    public void getLog(ArchiveLine archiveline, ByteStream bytestream)
        throws IOException
    {
        if(!isLogGet)
        {
            isLogGet = true;
            if(lines == null)
                lines = new VectorBin();
            archiveline.getLog(lines, bufferOut);
            Io.wShort(bufferOut, 0);
            resetMg();
        }
        if(bufferIn.size() == 0)
            return;
        bytestream.reset();
        synchronized(bufferIn)
        {
            bufferIn.writeTo(bytestream);
            bufferIn.reset();
        }
        vectorLine.addLine(this, mgIn, bytestream);
    }

    public void kill()
    {
        try
        {
            debug.log("kill talker line " + getAddress().getHostName());
            isKill = true;
            destroy();
        }
        catch(RuntimeException _ex) { }
    }

    public void mDestroy()
    {
        try
        {
            vectorLine.remove(this);
            if(debug.bool_debug)
                debug.log_d(String.valueOf(super.ID) + " destroy talker line");
            lines = null;
            debug.log("OutLineC:" + String.valueOf(super.ID));
        }
        catch(Throwable throwable)
        {
            debug.log("Talker_Line m_destroy:" + throwable.toString());
        }
    }

    private final void readLine()
        throws EOFException, IOException
    {
        int i = Io.readUShort(super.In);
        if(debug.bool_debug)
            debug.log(super.ID + " Line in " + i);
        if(i == 0)
        {
            Io.wShort(super.Out, 0);
            super.Out.flush();
            timeUpdate();
            return;
        }
        if(i >= 32767 || isKill)
        {
            int k;
            for(int j = 0; j < i; j += k)
            {
                k = super.In.read(bIn, 0, Math.min(i - j, bIn.length));
                if(k == -1)
                    throw new EOFException();
            }

        } else
        {
            if(bIn.length < i)
                bIn = new byte[i + 1024];
            Io.rFull(super.In, bIn, 0, i);
            synchronized(bufferIn)
            {
                bufferIn.write(bIn, 0, i);
            }
            timeUpdate();
        }
    }

    public void resetMg()
    {
        synchronized(bufferOut)
        {
            mgOut = null;
        }
    }

    protected void rRun()
    {
        boolean flag = false;
        try
        {
            sendUpdate();
            while(super.isRun) 
            {
                int i = super.In.available();
                if(i >= 2)
                    readLine();
                else
                if(bufferOut.size() >= 2)
                    writeBuffer();
                else
                if(isTimeOut())
                {
                    if(debug.bool_debug)
                        debug.log(super.ID + "L Time Out");
                    Io.wShort(super.Out, 0);
                    super.Out.flush();
                    timeUpdate();
                } else
                if(i == 0)
                    Thread.sleep(3000L);
            }
        }
        catch(EOFException _ex) { }
        catch(InterruptedException _ex) { }
        catch(Throwable throwable)
        {
            debug.log(super.ID + "L " + throwable.getMessage());
        }
    }

    private void sendUpdate()
        throws IOException, InterruptedException
    {
        if(lines == null)
            return;
        Thread thread = Thread.currentThread();
        while(!isLogGet && super.isRun) 
            Thread.sleep(3000L);
        int i = lines.size();
        for(int j = 0; j < i; j++)
        {
            if(!super.isRun)
                return;
            byte abyte0[] = lines.get(0);
            Io.wShort(super.Out, 65535);
            Io.wShort(super.Out, abyte0.length);
            super.Out.write(abyte0);
            super.Out.flush();
            lines.remove(1);
            if(debug.bool_debug)
                debug.log(super.ID + "Line out " + (abyte0.length + 4));
            timeUpdate();
        }

        lines = null;
    }

    private boolean writeBuffer()
        throws EOFException, IOException
    {
        int i;
        synchronized(bufferOut)
        {
            i = bufferOut.size();
        }
        if(debug.bool_debug)
            debug.log(super.ID + "Line out " + i);
        Io.wShort(super.Out, i);
        super.Out.write(bufferOut.getBuffer(), 0, i);
        super.Out.flush();
        synchronized(bufferOut)
        {
            bufferOut.reset(i);
        }
        timeUpdate();
        return true;
    }

    private TalkerLineWork vectorLine;
    private Debug debug;
    private Mg mgIn;
    private Mg mgOut;
    private boolean isKill;
    private boolean isLogGet;
    private VectorBin lines;
    public ByteStream bufferIn;
    public ByteStream bufferOut;
    private byte bIn[];
    private int beforeHeader;
}
