// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 6/16/2009 8:42:06 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   Talker.java

package paintchat_server;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import paintchat.Debug;
import syi.util.Io;
import syi.util.ThreadPool;

// Referenced classes of package paintchat_server:
//            Data

public abstract class Talker
    implements Runnable
{

    public Talker(int i, Data data)
    {
        isRun = false;
        isLive = true;
        timer = System.currentTimeMillis();
        ID = 0;
        thread = null;
        strDef = "talker";
        dd = data;
        ID = i;
    }

    public void destroy()
    {
        synchronized(this)
        {
            if(!isLive)
                return;
            isLive = false;
        }
        stop();
        mDestroy();
    }

    protected void finalize()
        throws Throwable
    {
        if(!isDead())
            destroy();
    }

    public boolean gc()
    {
        try
        {
            if(isDead())
            {
                dd.debug.log_d(String.valueOf(ID) + " garbage Talker");
                destroy();
                return true;
            } else
            {
                return false;
            }
        }
        catch(RuntimeException runtimeexception)
        {
            dd.debug.log("gc:" + runtimeexception);
        }
        destroy();
        return true;
    }

    public InetAddress getAddress()
    {
        try
        {
            return socket.getInetAddress();
        }
        catch(RuntimeException _ex)
        {
            return null;
        }
    }

    protected final boolean isDead()
    {
        return !isRun && isTimeOut() || !isLive;
    }

    protected final boolean isStop()
    {
        return !isRun && System.currentTimeMillis() - timer > 3000L;
    }

    protected final boolean isTimeOut()
    {
        return System.currentTimeMillis() - timer > 60000L;
    }

    public abstract void mDestroy();

    protected abstract void rRun();

    public void run()
    {
        try
        {
            thread = Thread.currentThread();
            rRun();
        }
        catch(Throwable _ex) { }
        stop();
    }

    public boolean start(Socket socket1, InputStream inputstream, OutputStream outputstream)
        throws IOException
    {
        if(isRun || isDead())
        {
            return false;
        } else
        {
            Io.wShort(outputstream, ID);
            outputstream.flush();
            timeUpdate();
            isRun = true;
            In = inputstream;
            Out = outputstream;
            socket = socket1;
            ThreadPool.poolStartThread(this, strDef);
            return true;
        }
    }

    public void stop()
    {
        try
        {
            Socket socket1;
            InputStream inputstream;
            OutputStream outputstream;
            synchronized(this)
            {
                if(!isRun)
                    return;
                timeUpdate();
                isRun = false;
                socket1 = socket;
                inputstream = In;
                outputstream = Out;
                socket = null;
                In = null;
                Out = null;
                Thread thread1 = Thread.currentThread();
                if(thread != null && thread1 != thread)
                {
                    thread.interrupt();
                    thread = null;
                }
            }
            try
            {
                if(socket1 != null)
                    socket1.close();
                if(inputstream != null)
                    inputstream.close();
                if(outputstream != null)
                    outputstream.close();
            }
            catch(IOException _ex) { }
        }
        catch(Throwable throwable)
        {
            throwable.printStackTrace();
        }
    }

    protected final void timeUpdate()
    {
        timer = System.currentTimeMillis();
    }

    protected boolean isRun;
    private boolean isLive;
    private long timer;
    public int ID;
    private Socket socket;
    protected InputStream In;
    protected OutputStream Out;
    protected Data dd;
    protected Thread thread;
    private String strDef;
}
