package paintchat_server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Hashtable;
import paintchat.Config;
import paintchat.Debug;
import paintchat.MgText;
import paintchat.Res;
import syi.util.Io;
import syi.util.PProperties;
import syi.util.ThreadPool;

public class Server
  implements Runnable
{
  public static final String STR_VERSION = "(C) PaintChatServer v3.55b";
  public static final String FILE_CONFIG = "cnf/paintchat.cf";
  private Data dd;
  private TalkerInstance talkerInstance;
  private boolean live = true;
  private boolean isLiveThread = true;
  private Config config = null;
  public Debug debug = null;
  private Thread tLive = null;

  public Server(Config config, Debug debug)
  {
    this.config = config;
    this.debug = new Debug(debug, null);
  }

  public synchronized void exitServer()
  {
    if (!(this.live))
      return;
    this.live = false;
    try
    {
      this.dd.destroy();     
      Thread localThread = Thread.currentThread();      
      if ((this.tLive != null) && (this.tLive != localThread))
        try
        {
          this.tLive.interrupt();
          this.tLive = null;
        }
        catch (Exception localException3)
        {
        }
      this.debug.log("Terminating Paintchat Server..");
    }
    catch (Throwable localThrowable)
    {
    }
  }

  public void init()
  {
    Thread localThread;
    try
    {    	
      localThread = new Thread(this, "init");
      localThread.setDaemon(false);
      localThread.start();
    }
    catch (Exception localException)
    {
      this.debug.log("init_thread" + localException);
    }
  }

  public void rInit()
    throws Throwable
  {
    this.dd = new Data(this.debug, this.config);
    this.talkerInstance = new TalkerInstance(this, this.dd);    
    runServer();
    System.out.println("LOADED ! " + config.getString("SERVER_ID") );
  }

  public void run()
  {
    try
    {
      switch (Thread.currentThread().getName().charAt(0))
      {
      case 'g':
        runLive();
        break;
      case 'i':
        rInit();
      }
    }
    catch (Throwable localThrowable)
    {
      localThrowable.printStackTrace();
    }
  }

  public void connect(Socket localSocket)
  {    
      try
      {
        if (!(this.isLiveThread))
          synchronized (this.tLive)
          {
            this.tLive.notify();
          }
        this.talkerInstance.newTalker(localSocket);
      }
      catch (Throwable localThrowable1){
    	  localThrowable1.printStackTrace();
      }   
  }

  private void runLive()
  {
    TalkerTextWork localTalkerTextWork = this.dd.getTalkerTextWork();
    TalkerLineWork localTalkerLineWork = this.dd.getTalkerLineWork();
    long l1 = System.currentTimeMillis();
    long l2 = l1 + 86400000L;
    long l3 = l1 + 3600000L;
    while (this.live)
      try
      {
        Thread.sleep(60000L);
        if (!(this.live))
          break;
        localTalkerTextWork.gc();
        localTalkerLineWork.gc();
        if (ThreadPool.getCountOfSleeping() >= 8)
          ThreadPool.poolGcAll();
        if (this.debug.bool_debug)
        {
          int i = localTalkerTextWork.size();
          int j = localTalkerLineWork.size();
          this.debug.logSys("pchats_gc:connect t=" + i + " l=" + j);
          this.debug.logSys("ThreadSleep=" + ThreadPool.getCountOfSleeping() + " ThreadWork=" + ThreadPool.getCountOfWorking());
        }
        if (localTalkerTextWork.size() == 0)
        {
          synchronized (this.tLive)
          {
            this.debug.log("pchat_gc:suspend");
            ThreadPool.poolGcAll();
            this.isLiveThread = false;
            this.tLive.wait();
            this.isLiveThread = true;
          }
          this.debug.log("pchat_gc:resume");
        }
        l1 = System.currentTimeMillis();
        if (l1 >= l3)
        {
          l3 = l1 + 3600000L;
          this.debug.log(new Date().toString());
          ThreadPool.poolGcAll();
          System.gc();
          System.runFinalization();
        }
        if (l1 >= l2)
        {
          l2 += l1 + 86400000L;
          this.debug.newLogFile(Io.getDateString("pserv_", "log", this.config.getString("Server_Log_Server_Dir", "save_server")));
          this.dd.clearKillList();
        }
      }
      catch (Throwable localThrowable)
      {
        if (this.live)
          localThrowable.printStackTrace();
      }
    label354: exitServer();
  }
  
  public Data getData() {
	return dd;  
  }

  private void runServer()
  {    
    this.tLive = new Thread(this);
    this.tLive.setDaemon(false);
    this.tLive.setPriority(1);
    this.tLive.setName("gc");   
    this.tLive.start();
  }

  private void sendInfomation(Socket paramSocket, BufferedInputStream paramBufferedInputStream, BufferedOutputStream paramBufferedOutputStream)
  {
  }

  private void sendIntoScript(Socket paramSocket, BufferedInputStream paramBufferedInputStream, BufferedOutputStream paramBufferedOutputStream)
  {
  }

  private void sendOnProperties(Socket paramSocket, BufferedInputStream paramBufferedInputStream, BufferedOutputStream paramBufferedOutputStream)
  {
  }
}