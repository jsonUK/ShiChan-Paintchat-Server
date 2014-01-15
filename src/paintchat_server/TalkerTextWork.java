package paintchat_server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;
import paintchat.MgText;
import syi.util.ThreadPool;
import syi.util.Vector2;

public class TalkerTextWork
{
  private Data dd;
  private TalkerLineWork vectorLine;
  private int talkerCount = 0;
  private TalkerText[] array = new TalkerText[5];
  private ArchiveText archive;

  public TalkerTextWork(Data paramData)
  {
    this.dd = paramData;
    this.archive = new ArchiveText(paramData.config);
  }

  public synchronized void add(TalkerText paramTalkerText)
  {
    if (paramTalkerText.isDead())
      return;
    if (this.talkerCount >= this.array.length)
    {
      TalkerText[] arrayOfTalkerText = new TalkerText[this.talkerCount + 5];
      System.arraycopy(this.array, 0, arrayOfTalkerText, 0, this.array.length);
      this.array = arrayOfTalkerText;
    }
    this.array[(this.talkerCount++)] = paramTalkerText;
  }

  public synchronized void addText(int paramInt, MgText paramMgText)
  {
    this.archive.add(paramMgText);
    paramMgText.toBin(true);
    textSends(paramInt, paramMgText);
  }

  public ArchiveText ArchiveText()
  {
    return this.archive;
  }

  public synchronized void clear()
  {
    for (int i = 0; i < this.talkerCount; ++i)
      this.array[i] = null;
    this.talkerCount = 0;
    this.archive.clear();
  }

  public synchronized void clearLog()
  {
    this.archive.clear();
  }

  public synchronized void close()
  {
    clear();
    this.archive.close();
  }

  public synchronized void gc()
  {
    int i;
    try
    {
      TalkerText localTalkerText;
      i = this.talkerCount;
      int j = 0;
      for (int k = 0; k < i; ++k)
      {
        if ((localTalkerText = this.array[k]) == null)
          continue;
        ++j;
        if (localTalkerText.gc())
        {
          k = -1;
          j = 0;
          i = this.talkerCount;
        }
      }
      i = this.talkerCount;
      if ((i - j > 0) || (this.array.length - i > 10))
      {
        TalkerText[] arrayOfTalkerText = new TalkerText[j + 1];
        int l = 0;
        for (int i1 = 0; i1 < i; ++i1)
        {
          if ((localTalkerText = this.array[i1]) == null)
            continue;
          arrayOfTalkerText[(l++)] = localTalkerText;
        }
        this.talkerCount = l;
        this.array = arrayOfTalkerText;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      clear();
    }
  }

  public String getInfomation()
  {
    int i = this.vectorLine.getLogSize();
    int j = this.archive.size();
    StringBuffer localStringBuffer = new StringBuffer();
    localStringBuffer.append("header=infomation\nlog_line_size=");
    localStringBuffer.append((i / 1024) + "KByte(" + i + "Byte)\nlog_text_size=");
    localStringBuffer.append(j);
    localStringBuffer.append("\nconnection_line=" + this.vectorLine.size());
    localStringBuffer.append("\nconnection_text=" + size());
    localStringBuffer.append("\nthread_work=" + ThreadPool.getCountOfWorking());
    localStringBuffer.append("\nthread_sleep=" + ThreadPool.getCountOfSleeping());
    return localStringBuffer.toString();
  }

  public synchronized void getLog(Vector2 paramVector2)
  {
    this.archive.getLog(paramVector2);
  }

  public final synchronized TalkerText getTalker(int paramInt)
  {
    int i = this.talkerCount;
    for (int j = 0; j < i; ++j)
    {
      TalkerText localTalkerText;
      if ((localTalkerText = this.array[j]) == null)
        continue;
      if (localTalkerText.ID == paramInt)
        return localTalkerText;
    }
    return null;
  }

  public final synchronized TalkerText getTalker(String paramString)
  {
    if ((paramString == null) || (paramString.length() <= 0))
      return null;
    int i = this.talkerCount;
    for (int j = 0; j < i; ++j)
    {
      TalkerText localTalkerText;
      if ((localTalkerText = this.array[j]) == null)
        continue;
      if (paramString.equals(localTalkerText.getUserName()))
        return localTalkerText;
    }
    return null;
  }

  public synchronized int getUniqueID()
  {
    int i = this.talkerCount;
    int j = 0;
    for (int k = 0; k < 100000; ++k)
    {
      while (j == 0)
        j = (int)(Math.random() * 65535.0D);
      int l = 1;
      try
      {
        for (int i1 = 0; i1 < i; ++i1)
        {
          TalkerText localTalkerText;
          if ((localTalkerText = this.array[i1]) == null)
            continue;
          if (localTalkerText.ID == j)
          {
            l = 0;
            break;
          }
        }
      }
      catch (RuntimeException localRuntimeException)
      {
      }
      if (l != 0)
        break;
      j = 0;
    }
    if (j == 0)
      throw new RuntimeException("Can't maked ID");
    return j;
  }

  public synchronized String getUniqueUserName(String origName)
  {
    int i = 1;
    int j = this.talkerCount;
    String name = origName;
    for (int l = 0; l < 1024; ++l)
    {
      int k = 0;
      for (int i1 = 0; i1 < j; ++i1)
      {
        TalkerText localTalkerText;
        if ((localTalkerText = this.array[i1]) == null)
          continue;
        if (localTalkerText.getUserName().equals(name))
        {
        	// HACK just kick old user and add new user
        	killTalker(name, new Vector());
        	return name;
          //k = 1;
          //break;
        }
      }
      if (k != 0)
      {
        ++i;
        name = origName + i;
      }
      else
      {
        break;
      }
    }
    return name;
  }

  public void getUsers(Vector2 paramVector2)
  {
    int i;
    try
    {
      i = this.talkerCount;
      for (int j = 0; j < i; ++j)
      {
        TalkerText localTalkerText;
        if ((localTalkerText = this.array[j]) == null)
          continue;
        String str = localTalkerText.getUserName();
        if ((str == null) || (str.length() == 0))
          continue;
        paramVector2.add(new MgText((byte)1, str));
      }
    }
    catch (Exception localRuntimeException)
    {
      localRuntimeException.printStackTrace();
    }
  }

  public synchronized boolean killTalker(String username, Vector paramVector)
  {
    int i = this.talkerCount;
    boolean j = false;
    try
    {
      for (int k = 0; k < i; ++k)
      {
        TalkerText localTalkerText;
        if ((localTalkerText = this.array[k]) == null)
          continue;
        if (username.equals(localTalkerText.getUserName()))
        {
          try
          {
            InetAddress localInetAddress = localTalkerText.getAddress();
            if ((localInetAddress != null) && (!(InetAddress.getLocalHost().equals(localInetAddress))))
              paramVector.addElement(localInetAddress);
          }
          catch (IOException localIOException)
          {
          }
          localTalkerText.kill();
          j = true;
          this.vectorLine.killTalker(localTalkerText.ID);
        }
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      j = false;
    }
    return j;
  }

  public synchronized void remove(TalkerText paramTalkerText)
  {
    if (paramTalkerText == null)
      return;
    for (int i = 0; i < this.talkerCount; ++i)
      if (this.array[i] == paramTalkerText)
      {
        removeAt(i);
        return;
      }
  }

  public final synchronized void removeAt(int paramInt)
  {
    int i = this.talkerCount - 1;
    if ((paramInt < 0) || (paramInt > i))
      return;
    TalkerText localTalkerText = this.array[paramInt];
    if (localTalkerText == null)
      return;
    this.vectorLine.remove(this.vectorLine.getTalker(localTalkerText.ID));
    if (i == paramInt)
      this.array[paramInt] = null;
    else
      System.arraycopy(this.array, paramInt + 1, this.array, paramInt, i - paramInt);
    this.talkerCount = Math.max(i, 0);
  }

  public synchronized void sendText(int paramInt, MgText paramMgText)
  {
    paramMgText.toBin(true);
    textSends(paramInt, paramMgText);
  }

  public void setWork(TalkerLineWork paramTalkerLineWork)
  {
    this.vectorLine = paramTalkerLineWork;
  }

  public final int size()
  {
    return this.talkerCount;
  }

  private synchronized void textSends(int paramInt, MgText paramMgText)
  {
    int i = this.talkerCount;
    int destroyIndex = -1;	// simple hack, just do 1 per, no need to arrange memory to flusht hem all
    for (int j = 0; j < i; ++j)
    {
      TalkerText localTalkerText;
      if (((localTalkerText = this.array[j]) == null) || (localTalkerText.ID == paramInt))
        continue;
      if (!(localTalkerText.addMg(paramMgText)))
      {        
        destroyIndex = j;
      }           
    }
    
    // now destroy
    if(destroyIndex > -1) {
    	TalkerText localTalkerText = this.array[destroyIndex];
    	if( localTalkerText != null) {
    		localTalkerText.destroy();
    	}   
    }
  }
}