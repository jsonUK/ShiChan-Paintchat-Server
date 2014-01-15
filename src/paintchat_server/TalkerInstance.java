// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 6/16/2009 7:17:03 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TalkerInstance.java

package paintchat_server;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import paintchat.Debug;
import syi.util.Io;
import syi.util.ThreadPool;

// Referenced classes of package paintchat_server:
//            Data, TalkerTextWork, TalkerLineWork, Talker, 
//            TalkerLine, TalkerText, Server

public class TalkerInstance
    implements Runnable
{

    public TalkerInstance()
    {
        sockets = new Socket[5];
        lock = new Object();
    }

    public TalkerInstance(Server server1, Data data)
    {
        sockets = new Socket[5];
        lock = new Object();
        dd = data;
        server = server1;
    }

    public void newTalker(Socket socket)
    {
        int i = 0;
        synchronized(lock)
        {
            int j = sockets.length;
            for(i = 0; i < j; i++)
                if(sockets[i] == null)
                    break;

            if(i >= j)
                if(j >= 30)
                {
                    for(int k = 0; k < j; k++)
                        sockets[k] = null;

                    i = 0;
                } else
                {
                    Socket asocket[] = new Socket[Math.min((int)((double)j * 1.5D), 30)];
                    System.arraycopy(sockets, 0, asocket, 0, j);
                    sockets = asocket;
                }
            sockets[i] = socket;
        }
        ThreadPool.poolStartThread(this, String.valueOf(i));
    }

    private void newTalkerLine(Socket socket, InputStream inputstream, OutputStream outputstream)
        throws IOException
    {
        int i = Io.readUShort(inputstream);
        if(i < 0)
            return;
        boolean flag = Io.r(inputstream) != 0;
        TalkerLineWork talkerlinework = dd.getTalkerLineWork();
        if(dd.getTalkerTextWork().getTalker(i) == null)
            throw new IOException("Wrong ID");
        TalkerLine talkerline;
        synchronized(talkerlinework)
        {
            if(!flag)
            {
                talkerline = talkerlinework.getTalker(i);
                if(talkerline == null)
                    throw new IOException("Not found line ID");
                if(!talkerline.isStop())
                    throw new IOException("still working line");
                talkerline.resetMg();
                talkerline.start(socket, inputstream, outputstream);
            } else
            {
                talkerline = new TalkerLine(i, dd);
                talkerlinework.add(talkerline);
            }
        }
        talkerline.start(socket, inputstream, outputstream);
        dd.debug.log("JoinLine " + i + ' ' + socket.getInetAddress().getHostName());
    }

    private void newTalkerText(Socket socket, InputStream inputstream, OutputStream outputstream)
        throws IOException
    {
        int i = Io.readUShort(inputstream);
        if(i < 0)
            return;
        boolean oldID = i == 0;
        boolean flag1 = Io.r(inputstream) != 0;
        TalkerTextWork talkertextwork = dd.getTalkerTextWork();
        TalkerText talkertext;
        synchronized(talkertextwork)
        {
            if(!oldID)
            {
                talkertext = talkertextwork.getTalker(i);
                if(talkertext.isDead())
                    throw new IOException();
                if(!talkertext.isStop())
                    throw new IOException("still work Text");
                if(flag1)
                    talkertext.getLog();
            } else
            {
                int j = talkertextwork.getUniqueID();
                talkertext = new TalkerText(j, dd, server);
                talkertext.getLog();
                talkertextwork.add(talkertext);
                dd.debug.log("JoinText " + j + ' ' + socket.getInetAddress().getHostName());
            }
        }
        try
        {
            talkertext.start(socket, inputstream, outputstream);
        }
        catch(Throwable throwable)
        {
            throwable.printStackTrace();
            if(oldID)
                talkertextwork.remove(talkertext);
        }
    }

    public void run()
    {
        Socket socket = null;
        try
        {
            synchronized(lock)
            {
                int i = Integer.parseInt(Thread.currentThread().getName());
                socket = sockets[i];
                sockets[i] = null;
            }
            if(socket == null)
                return;
            InputStream inputstream = socket.getInputStream();
            OutputStream outputstream = socket.getOutputStream();
            switch(Io.r(inputstream))
            {
            case 116: // 't'
                newTalkerText(socket, inputstream, outputstream);
                break;

            case 108: // 'l'
                newTalkerLine(socket, inputstream, outputstream);
                break;

            default:
                throw new Throwable();
            }
            return;
        }
        catch(Throwable throwable)
        {
            dd.debug.log(throwable.getMessage());
        }
        try
        {
            socket.close();
        }
        catch(Throwable _ex) { }
    }

    private Data dd;
    private Server server;
    private Socket sockets[];
    private Object lock;
}
