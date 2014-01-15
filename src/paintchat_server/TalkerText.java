// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 6/16/2009 7:58:20 PM
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   TalkerText.java

package paintchat_server;

import java.io.*;
import java.net.InetAddress;
import paintchat.Debug;
import paintchat.MgText;
import syi.util.*;

// Referenced classes of package paintchat_server:
//            Talker, Data, TalkerTextWork, Server

class TalkerText extends Talker
{

    public TalkerText(int i, Data data, Server server1)
        throws IOException
    {
        super(i, data);
        debug = null;
        server = null;
        isKill = false;
        userName = "";
        kaiwaOut = new Vector2(12);
        textUpdate = null;
        workBuffer = new ByteStream();
        isMgIn = false;
        isMgOut = false;
        debug = data.debug;
        server = server1;
        vectorText = super.dd.getTalkerTextWork();
    }

    public boolean addMg(MgText mgtext)
    {
        if(isDead())
            return false;
        if(isKill)
            return true;
        try
        {
            synchronized(kaiwaOut)
            {
                kaiwaOut.add(mgtext);
            }
        }
        catch(Throwable _ex)
        {
            debug.log(super.ID + " error add talker talk");
            return false;
        }
        return true;
    }

    public boolean gc()
    {
        if(!super.gc() && kaiwaOut.size() > 256)
        {
            destroy();
            return true;
        } else
        {
            return false;
        }
    }

    public void getLog()
    {
        if(textUpdate == null)
            textUpdate = new Vector2();
        vectorText.getUsers(textUpdate);
        vectorText.getLog(textUpdate);
        textUpdate.add(MG_EMPTY);
        if(debug.bool_debug)
            debug.log_d(String.valueOf(super.ID) + "update send");
    }

    public String getUserName()
    {
        return userName;
    }

    private void in(MgText mgtext)
    {
        if(isMgIn)
            return;
        isMgIn = true;
        String s = mgtext.toString();
        String s1 = vectorText.getUniqueUserName(s);
        if(s1.length() > 20 || s1.length() <= 0)
        {
            destroy();
            return;
        }
        if(s != s1)
            mgtext.setData((byte)1, s1);
        userName = s1;
        addMg(mgtext);
        vectorText.addText(super.ID, mgtext);
        debug.log(userName + '=' + getAddress().getHostName());
    }

    public void kill()
    {
        isKill = true;
        debug.log("kill talker at " + getAddress().getHostName());
        destroy();
    }

    public void mDestroy()
    {
        if(debug.bool_debug)
            debug.log_d(String.valueOf(super.ID) + " End talker");
        debug.log("OutText " + userName);
        out();
        vectorText.remove(this);
    }

    private void out()
    {
        if(userName.length() <= 0)
            return;
        if(userName.length() > 0)
        {
            vectorText.addText(super.ID, new MgText((byte)2, userName));
            userName = "";
        }
        destroy();
    }

    private void read()
        throws IOException
    {
        int i = Io.readUShort(super.In);
        workBuffer.reset();
        workBuffer.addSize(i);
        byte abyte0[] = workBuffer.getBuffer();
        Io.rFull(super.In, abyte0, 0, i);
        int k;
        for(int j = 0; j < i; j += k + 2)
        {
            k = (abyte0[j] & 0xff) << 8 | abyte0[j + 1] & 0xff;
            byte abyte1[] = new byte[k - 1];
            System.arraycopy(abyte0, j + 3, abyte1, 0, k - 1);
            MgText mgtext = new MgText(abyte0[j + 2], abyte1);
            if(debug.bool_debug)
                debug.log_d(String.valueOf(super.ID) + ' ' + mgtext);
            run_read_switch(mgtext);
        }

        timeUpdate();
    }

    protected void rRun()
    {
        try
        {
            sendUpdate();
            while(super.isRun) 
                if(super.In.available() > 0)
                    read();
                else
                if(kaiwaOut.size() > 0)
                    write();
                else
                if(isTimeOut())
                {
                    if(debug.bool_debug)
                        debug.log(super.ID + "T Time Out");
                    Io.wShort(super.Out, 0);
                    super.Out.flush();
                    timeUpdate();
                } else
                {
                    Thread.sleep(2000L);
                }
        }
        catch(InterruptedException _ex) { }
        catch(EOFException _ex) { }
        catch(Throwable throwable)
        {
            debug.log("TalkerText.rRun=" + throwable.getMessage());
        }
    }

    private void run_read_switch(MgText mgtext)
    {
        if(isKill && mgtext.head != 2)
            return;
        switch(mgtext.head)
        {
        default:
            break;

        case 1: // '\001'
            in(mgtext);
            break;

        case 2: // '\002'
            out();
            break;

        case 0: // '\0'
            if(userName.length() >= 0 && (mgtext.getValueSize() != 0 && mgtext.getValueSize() <= 768))
            {
                String s = userName + '>' + mgtext.toString();
                mgtext.setData((byte)0, s);
                vectorText.addText(super.ID, mgtext);
            }
            break;

        case 4: // '\004'
            mgtext.setData((byte)4, "\u3057\u3043\u3061\u3083\u3093(C) (C)\u3057\u3043\u3061\u3083\u3093 PaintChatServer v3.55b");
            addMg(mgtext);
            break;

        case 101: // 'e'
            if(mgtext.getValueSize() <= 0 || super.dd.isPasswoad(mgtext.toString()))
                addMg(mgtext);
            break;

        case 5: // '\005'
            try
            {
                if(getAddress().equals(InetAddress.getLocalHost()))
                    server.exitServer();
            }
            catch(Throwable _ex) { }
            break;

        case 102: // 'f'
            switchServerMg(mgtext);
            break;
        }
    }

    private void sendUpdate()
        throws IOException
    {
        if(textUpdate == null)
            return;
        boolean flag = false;
        for(; textUpdate.size() > 0; timeUpdate())
        {
            workBuffer.reset();
            int i = 0;
            for(int j = 0; j < textUpdate.size(); j++)
            {
                ((MgText)textUpdate.get(j)).getData(workBuffer);
                i++;
                if(workBuffer.size() > 8000)
                    break;
            }

            Io.wShort(super.Out, workBuffer.size());
            workBuffer.writeTo(super.Out);
            super.Out.flush();
            textUpdate.remove(i);
        }

        textUpdate = null;
    }

    private void switchServerMg(MgText mgtext)
    {
        try
        {
            String s = mgtext.toString();
            int i = s.indexOf('\n');
            String s2 = s.substring(0, i);
            if(!super.dd.isPasswoad(s2))
                return;
            i = s2.length() + 1;
            String s3 = s.substring(i, s.indexOf('\n', i));
            i = s2.length() + s3.length() + 2;
            String s4 = s.substring(i);
            if(s3.equals("text"))
            {
                mgtext.setData((byte)8, s4);
                int j = s4.indexOf("$js:");
                if(j >= 0)
                {
                    if(j == 0)
                        vectorText.sendText(super.ID, mgtext);
                } else
                {
                    vectorText.addText(super.ID, mgtext);
                }
                return;
            }
            if(s3.equals("info"))
            {
                mgtext.setData((byte)102, vectorText.getInfomation());
                addMg(mgtext);
                return;
            }
            if(s3.equals("kill"))
            {
                boolean flag = super.dd.killTalker(s4);
                String s1 = "kill name=" + s4 + " bool=" + flag;
                mgtext.setData((byte)6, s1);
                addMg(mgtext);
                vectorText.addText(super.ID, mgtext);
                debug.log("kill done");
                return;
            }
            if(s3.equals("address"))
            {
                TalkerText talkertext = vectorText.getTalker(s4);
                mgtext.setData((byte)102, "header=address\naddress=" + (talkertext != null ? talkertext.getAddress().getHostName() + " kill=" + talkertext.isKill : "NotFound"));
                addMg(mgtext);
                return;
            }
            if(s3.equals("thread_refresh"))
            {
                ThreadPool.poolGcAll();
                return;
            }
            if(s3.equals("text_clear"))
            {
                mgtext.setData((byte)8, "$clear;");
                addMg(mgtext);
                vectorText.addText(super.ID, mgtext);
                vectorText.clearLog();
            }
            s3.equals("canvas_clear");
        }
        catch(RuntimeException runtimeexception)
        {
            debug.log(runtimeexception.getMessage());
        }
    }

    private void write()
        throws IOException
    {
        int i = kaiwaOut.size();
        workBuffer.reset();
        for(int j = 0; j < i; j++)
        {
            MgText mgtext = (MgText)kaiwaOut.get(j);
            if(mgtext != null)
            {
                mgtext.getData(workBuffer);
                if(debug.bool_debug)
                    debug.log(mgtext.toString() + " \u3092\u9001\u4FE1");
            }
        }

        Io.wShort(super.Out, workBuffer.size());
        workBuffer.writeTo(super.Out);
        super.Out.flush();
        synchronized(kaiwaOut)
        {
            kaiwaOut.remove(i);
        }
        timeUpdate();
    }

    private static final String STR_EMPTY = "";
    private static MgText MG_EMPTY = new MgText();
    private Debug debug;
    private Server server;
    private TalkerTextWork vectorText;
    private boolean isKill;
    private String userName;
    private Vector2 kaiwaOut;
    private Vector2 textUpdate;
    private ByteStream workBuffer;
    boolean isMgIn;
    boolean isMgOut;

}
