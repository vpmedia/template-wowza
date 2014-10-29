package hu.vpmedia.media.wms.plugin.security;

import com.wowza.wms.amf.AMFDataItem;
import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.*;
import com.wowza.wms.client.Client;
import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.ModuleBase;
import hu.vpmedia.media.wms.plugin.security.encryption.TEA;
import com.wowza.wms.request.RequestFunction;
import java.io.File;
import java.net.URLDecoder;
import java.util.*;

public class SecureURLParams extends ModuleBase
{

    public SecureURLParams()
    {
        denyallConnections = false;
    }

    public void onAppStart(IApplicationInstance iapplicationinstance)
    {
        IApplication iapplication = iapplicationinstance.getApplication();
        String s = (new StringBuilder()).append(iapplication.getApplicationPath()).append(File.separatorChar).append("secureurlparams").append(File.separatorChar).append(iapplicationinstance.getName()).toString();
        File file = new File(s);
        if(!file.exists())
        {
            boolean flag = false;
            String as[] = {
                "connect", "play", "publish"
            };
            int i = 0;
            do
            {
                if(i >= as.length)
                    break;
                String s1 = iapplicationinstance.getProperties().getPropertyStr((new StringBuilder()).append("secureurlparams.").append(as[i]).toString());
                if(s1 != null)
                {
                    flag = true;
                    break;
                }
                i++;
            } while(true);
            if(!flag)
                denyallConnections = true;
        }
        getLogger().info((new StringBuilder()).append("SecureURLParams.onAppStart: ").append(iapplication.getName()).append("/").append(iapplicationinstance.getName()).toString());
        if(denyallConnections)
            getLogger().info((new StringBuilder()).append("SecureURLParams.onAppStart: All connections to this application instance will be denied. The \"secureurlparams\" folder for this application instance is missing and there are no \"secureurlparams\" properties defined in [install-dir]/conf/").append(iapplication.getName()).append("/Application.xml.").toString());
    }

    public void onAppStop(IApplicationInstance iapplicationinstance)
    {
        IApplication iapplication = iapplicationinstance.getApplication();
        getLogger().info((new StringBuilder()).append("SecureURLParams.onAppStop: ").append(iapplication.getName()).append("/").append(iapplicationinstance.getName()).toString());
    }

    String doURLDecodeStr(String s)
    {
        String s1 = s;
        try
        {
            s1 = URLDecoder.decode(s, "UTF-8");
        }
        catch(Exception exception)
        {
            getLogger().error((new StringBuilder()).append("URLDecoder: ").append(exception.toString()).toString());
        }
        return s1;
    }

    boolean checkFolder(String s, IClient iclient)
    {
        IApplicationInstance iapplicationinstance = getAppInstance(iclient);
        IApplication iapplication = getApplication(iclient);
        boolean flag = false;
        if(!flag)
        {
            String s1 = iapplication.getApplicationPath();
            s1 = (new StringBuilder()).append(s1).append(File.separatorChar).append("secureurlparams").append(File.separatorChar).append(iapplicationinstance.getName()).toString();
            File file = new File((new StringBuilder()).append(s1).append(File.separatorChar).append(s).toString());
            flag = file.exists();
        }
        if(!flag)
            flag = iapplicationinstance.getProperties().getPropertyStr((new StringBuilder()).append("secureurlparams.").append(s).toString()) != null;
        return flag;
    }

    List parseQueryStr(String s)
    {
        ArrayList arraylist = new ArrayList();
        if(s != null && s.length() > 0)
        {
            String as[] = s.split("[&]");
            for(int i = 0; i < as.length; i++)
            {
                String as1[] = as[i].split("[=]");
                if(as1.length == 2)
                    arraylist.add(as1);
            }

        }
        return arraylist;
    }

    boolean checkParams(String s, IClient iclient)
    {
        Boolean boolean1;
label0:
        {
            boolean1 = Boolean.valueOf(false);
            IApplicationInstance iapplicationinstance = getAppInstance(iclient);
            IApplication iapplication = getApplication(iclient);
            String s1 = iapplication.getApplicationPath();
            s1 = (new StringBuilder()).append(s1).append(File.separatorChar).append("secureurlparams").append(File.separatorChar).append(iapplicationinstance.getName()).toString();
            WMSLogger wmslogger = null;
            if(WMSLoggerFactory.getLogger(null).isDebugEnabled())
                wmslogger = WMSLoggerFactory.getLogger(null);
            File file = new File((new StringBuilder()).append(s1).append(File.separatorChar).append(s).toString());
            if(file.exists())
            {
                String s2 = iclient.getQueryStr();
                List list = parseQueryStr(s2);
                if(list.size() > 0)
                {
                    Iterator iterator = list.iterator();
                    boolean flag;
                    do
                    {
                        if(!iterator.hasNext())
                            break label0;
                        String as1[] = (String[])iterator.next();
                        File file1 = new File((new StringBuilder()).append(s1).append(File.separatorChar).append(s).append(File.separatorChar).append(doURLDecodeStr(as1[1])).append(".").append(doURLDecodeStr(as1[0])).toString());
                        flag = file1.exists();
                        if(wmslogger != null)
                            wmslogger.debug((new StringBuilder()).append("SecureURLParams: ").append(s).append(": ").append(flag ? "SUCCEED" : "FAIL").append(" (file: ").append(file1.getPath()).append(")").toString());
                    } while(!flag);
                    boolean1 = Boolean.valueOf(true);
                } else
                if(wmslogger != null)
                    wmslogger.debug((new StringBuilder()).append("SecureURLParams: ").append(s).append(": FAIL (no query parameters)").toString());
                break label0;
            }
            String s3 = iapplicationinstance.getProperties().getPropertyStr((new StringBuilder()).append("secureurlparams.").append(s).toString());
            if(s3 == null)
            {
                boolean1 = Boolean.valueOf(true);
                if(wmslogger != null)
                    wmslogger.debug((new StringBuilder()).append("SecureURLParams: ").append(s).append(": SUCCEED (no folder found [").append(s1).append("], no property found [").append("secureurlparams.").append(s).append("])").toString());
                break label0;
            }
            s3 = s3.trim();
            if(s3.length() == 0)
            {
                boolean1 = Boolean.valueOf(false);
                if(wmslogger != null)
                    wmslogger.debug((new StringBuilder()).append("SecureURLParams: ").append(s).append(": FAIL (property is empty  [").append("secureurlparams.").append(s).append("])").toString());
                break label0;
            }
            ArrayList arraylist = new ArrayList();
            String as[] = s3.split("[,]");
            for(int i = 0; i < as.length; i++)
            {
                String s5 = as[i].trim();
                arraylist.add(s5);
            }

            String s4 = iclient.getQueryStr();
            List list1 = parseQueryStr(s4);
            if(list1.size() > 0)
            {
                Iterator iterator1 = list1.iterator();
                boolean flag1;
                do
                {
                    if(!iterator1.hasNext())
                        break label0;
                    String as2[] = (String[])iterator1.next();
                    String s6 = (new StringBuilder()).append(doURLDecodeStr(as2[1])).append(".").append(doURLDecodeStr(as2[0])).toString();
                    flag1 = arraylist.contains(s6);
                    if(wmslogger != null)
                        wmslogger.debug((new StringBuilder()).append("SecureURLParams: ").append(s).append(": ").append(flag1 ? "SUCCEED" : "FAIL").append(" (property: ").append(s6).append(")").toString());
                } while(!flag1);
                boolean1 = Boolean.valueOf(true);
            } else
            if(wmslogger != null)
                wmslogger.debug((new StringBuilder()).append("SecureURLParams: ").append(s).append(": FAIL (no query parameters)").toString());
        }
        return boolean1.booleanValue();
    }

    private void killClient(IClient iclient)
    {
        ((Client)iclient).setShutdownClient(true);
        ((Client)iclient).doIdle();
    }

    public void secureTokenResponse(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        String s = amfdatalist.getString(3);
        WMSProperties wmsproperties = iclient.getProperties();
        String s1 = null;
        synchronized(wmsproperties)
        {
            s1 = (String)wmsproperties.get("secureToken");
        }
        boolean flag = false;
        if(s1 == null)
        {
            getLogger().error("Error: SecureToken: Challenge not found: kill connection");
            killClient(iclient);
        } else
        if(s == null)
        {
            getLogger().error("Error: SecureToken: Response not found: kill connection");
            killClient(iclient);
        } else
        if(!s.equals(s1))
        {
            getLogger().error("Error: SecureToken: Challenge does not equal response: kill connection");
            killClient(iclient);
        } else
        {
            getLogger().debug("SecureToken: Challenge matches response.");
            synchronized(wmsproperties)
            {
                wmsproperties.put("secureTokenOK", new Boolean(true));
            }
            flag = true;
        }
        sendResult(iclient, amfdatalist, new AMFDataItem(flag));
    }

    public void onConnect(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        if(denyallConnections || !checkParams("connect", iclient))
        {
            getLogger().info("SecureURLParams: onConnect: rejected");
            iclient.rejectConnection("SecureURLParams: reject connection");
        } else
        {
            String s = (String)iclient.getAppInstance().getProperties().get("secureTokenSharedSecret");
            if(s != null)
            {
                SecureTokenManager securetokenmanager = SecureTokenManager.getInstance(iclient.getVHost());
                String s1 = securetokenmanager.newGUID();
                iclient.getProperties().put("secureToken", s1);
                iclient.getProperties().put("secureTokenOK", new Boolean(false));
                iclient.addAcceptConnectionAttribute("secureToken", TEA.encrypt(s1, s));
                getLogger().debug("SecureURLParams: generate SecureToken GUID");
            }
        }
    }

    private boolean checkSecureToken(IClient iclient)
    {
        boolean flag = true;
        if(iclient != null)
        {
            WMSProperties wmsproperties = iclient.getProperties();
            synchronized(wmsproperties)
            {
                Boolean boolean1 = (Boolean)wmsproperties.get("secureTokenOK");
                getLogger().debug((new StringBuilder()).append("SecureURLParams: Check SecureToken: ").append(boolean1.booleanValue()).toString());
                if(!boolean1.booleanValue())
                    flag = false;
            }
        }
        return flag;
    }

    public void releaseStream(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        boolean flag = true;
        if(checkFolder("publish", iclient))
            flag = checkParams("publish", iclient);
        else
        if(iclient.getProperties().get("secureTokenOK") != null)
            flag = checkSecureToken(iclient);
        if(flag)
        {
            invokePrevious(this, iclient, requestfunction, amfdatalist);
        } else
        {
            getLogger().info("SecureURLParams: releaseStream: rejected");
            killClient(iclient);
        }
    }

    public void publish(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        boolean flag = true;
        if(checkFolder("publish", iclient))
            flag = checkParams("publish", iclient);
        else
        if(iclient.getProperties().get("secureTokenOK") != null)
            flag = checkSecureToken(iclient);
        if(flag)
        {
            invokePrevious(this, iclient, requestfunction, amfdatalist);
        } else
        {
            getLogger().info("SecureURLParams: publish: rejected");
            killClient(iclient);
        }
    }

    public void play(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        boolean flag = true;
        if(checkFolder("play", iclient))
            flag = checkParams("play", iclient);
        else
        if(iclient.getProperties().get("secureTokenOK") != null)
            flag = checkSecureToken(iclient);
        if(flag)
        {
            invokePrevious(this, iclient, requestfunction, amfdatalist);
        } else
        {
            getLogger().info("SecureURLParams: play: rejected");
            killClient(iclient);
        }
    }

    private boolean denyallConnections;
}
