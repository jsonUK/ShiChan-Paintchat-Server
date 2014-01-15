// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 6/1/2009 9:49:24 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TalkerLineWork.java

package paintchat_server;

import java.io.IOException;
import paintchat.*;
import syi.util.ByteStream;
import syi.util.ThreadPool;

// Referenced classes of package paintchat_server:
//            TalkerLine, ArchiveLine, Data, Talker, 
//            TalkerTextWork, TalkerText

public class TalkerLineWork
    implements Runnable
{

    public TalkerLineWork(Data data)
    {
        isLive = true;
        array = new TalkerLine[5];
        talkerCount = 0;
        workBuffer = new ByteStream();
        workAddLine = new ByteStream();
        dd = data;
        archive = new ArchiveLine(data.config);
        thread = ThreadPool.poolStartThread(this, "VectorLine");
    }

    public synchronized void add(TalkerLine talkerline)
    {
        if(talkerline.isDead())
            return;
        if(talkerCount >= array.length)
        {
            TalkerLine atalkerline[] = new TalkerLine[talkerCount + 5];
            System.arraycopy(array, 0, atalkerline, 0, array.length);
            array = atalkerline;
        }
        array[talkerCount++] = talkerline;
    }

    public void addLine(TalkerLine talkerline, Mg mg, ByteStream bytestream)
    {
        int i = 0;
        int k = bytestream.size();
        int l = talkerCount;
        byte abyte0[] = bytestream.getBuffer();
        byte byte0 = 10;
        while(i < k) 
        {
            i += ((abyte0[i] & 0xff) << 8 | abyte0[i + 1] & 0xff) + 2;
            if(i > k)
                return;
        }
        for(int j = 0; j < k;)
        {
            j += mg.set(abyte0, j);
            if(mg.iHint == byte0)
                dClear(((Talker) (talkerline)).ID);
            for(int i1 = 0; i1 < l; i1++)
            {
                TalkerLine talkerline1;
                if((talkerline1 = array[i1]) != null && talkerline1 != talkerline)
                    talkerline1.addLine(mg, workBuffer);
            }

            archive.add(mg);
        }

    }

    public synchronized void clear()
    {
        removeAll();
        archive.clear();
    }

    public synchronized void close()
    {
        removeAll();
        if(thread != null)
            thread.interrupt();
        archive.close();
    }

    private void dClear(int i)
    {
        String s = vectorText.getTalker(i).getUserName() + dd.res.get("erase_all", "canvas erased");
        dd.debug.log(s);
        vectorText.addText(i, new MgText((byte)6, s));
    }

    public synchronized void gc()
    {
        try
        {
            int i = talkerCount;
            int j = 0;
            for(int k = 0; k < i; k++)
            {
                TalkerLine talkerline;
                if((talkerline = array[k]) != null)
                {
                    j++;
                    if(talkerline.gc())
                    {
                        k = -1;
                        j = 0;
                        i = talkerCount;
                    }
                }
            }

            i = talkerCount;
            if(i - j > 0 || array.length - i > 10)
            {
                TalkerLine atalkerline[] = new TalkerLine[j + 1];
                int l = 0;
                for(int i1 = 0; i1 < i; i1++)
                {
                    TalkerLine talkerline1;
                    if((talkerline1 = array[i1]) != null)
                        atalkerline[l++] = talkerline1;
                }

                talkerCount = l;
                array = atalkerline;
            }
        }
        catch(RuntimeException _ex)
        {
            clear();
        }
    }

    public int getLogSize()
    {
        return archive.size();
    }

    public final synchronized TalkerLine getTalker(int i)
    {
        int j = talkerCount;
        for(int k = 0; k < j; k++)
        {
            TalkerLine talkerline;
            if((talkerline = array[k]) != null && ((Talker) (talkerline)).ID == i)
                return talkerline;
        }

        return null;
    }

    public synchronized void killTalker(int i)
    {
        TalkerLine talkerline = getTalker(i);
        if(talkerline != null)
            talkerline.kill();
    }

    public synchronized void remove(TalkerLine talkerline)
    {
        if(talkerline == null)
            return;
        for(int i = 0; i < talkerCount; i++)
        {
            if(array[i] != talkerline)
                continue;
            removeAt(i);
            break;
        }

    }

    public synchronized void removeAll()
    {
        for(int i = 0; i < talkerCount; i++)
            array[i] = null;

        talkerCount = 0;
    }

    private final synchronized void removeAt(int i)
    {
        int j = talkerCount - 1;
        if(i < 0 || i > j)
            return;
        if(j == i)
            array[i] = null;
        else
            System.arraycopy(array, i + 1, array, i, j - i);
        talkerCount = Math.max(j, 0);
    }

    public void run()
    {
        try
        {
            thread = Thread.currentThread();
            while(isLive) 
            {
                workRun();
                Thread.sleep(size() == 0 ? '\u1F40' : 4000);
            }
        }
        catch(Throwable _ex) { }
    }

    public void setWork(TalkerTextWork talkertextwork)
    {
        vectorText = talkertextwork;
    }

    public final int size()
    {
        return talkerCount;
    }

    private void workRun()
        throws IOException
    {
        synchronized(this)
        {
            int j = talkerCount;
            workBuffer.reset();
            for(int i = 0; i < j; i++)
            {
                TalkerLine talkerline;
                if((talkerline = array[i]) != null)
                    talkerline.getLog(archive, workAddLine);
            }

        }
    }
    
    public void saveLog(String dest) {
    	archive.saveTo(dest);
    }

    private boolean isLive;
    private Thread thread;
    private TalkerTextWork vectorText;
    private TalkerLine array[];
    private int talkerCount;
    private Data dd;
    private ArchiveLine archive;
    private ByteStream workBuffer;
    private ByteStream workAddLine;
}
