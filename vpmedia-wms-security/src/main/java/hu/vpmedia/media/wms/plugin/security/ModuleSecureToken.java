package hu.vpmedia.media.wms.plugin.security;

import com.wowza.wms.amf.AMFDataItem;
import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.*;
import com.wowza.wms.client.Client;
import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.module.*;
import hu.vpmedia.media.wms.plugin.security.encryption.TEA;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify;

public class ModuleSecureToken extends ModuleBase
    implements IModuleOnConnect, IModuleOnStream
{
    class StreamActions
        implements IMediaStreamActionNotify
    {

        public void onPause(IMediaStream imediastream, boolean flag, double d)
        {
        }

        public void onPlay(IMediaStream imediastream, String s, double d, double d1, int i)
        {
            if(doPlay)
                checkSecureToken(imediastream.getClient());
        }

        public void onPublish(IMediaStream imediastream, String s, boolean flag, boolean flag1)
        {
            if(doPublish)
                checkSecureToken(imediastream.getClient());
        }

        public void onSeek(IMediaStream imediastream, double d)
        {
        }

        public void onStop(IMediaStream imediastream)
        {
        }

        public void onUnPublish(IMediaStream imediastream, String s, boolean flag, boolean flag1)
        {
        }

        final ModuleSecureToken this$0;

        StreamActions()
        {            
            super();
            this$0 = ModuleSecureToken.this;
        }
    }


    public ModuleSecureToken()
    {
        doPlay = false;
        doPublish = false;
        doCreate = true;
        streamActions = new StreamActions();
    }

    public void onConnect(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        SecureTokenManager securetokenmanager = SecureTokenManager.getInstance(iclient.getVHost());
        String s = securetokenmanager.newGUID();
        iclient.getProperties().put("secureToken", s);
        iclient.getProperties().put("secureTokenOK", new Boolean(false));
        String s1 = (String)iclient.getAppInstance().getProperties().get("secureTokenSharedSecret");
        if(s1 != null)
        {
            iclient.addAcceptConnectionAttribute("secureToken", TEA.encrypt(s, s1));
        } else
        {
            String s2 = (new StringBuilder()).append("Error: SecureToken: secureTokenSharedSecret property not defined in: ").append(iclient.getApplication().getConfigPath()).toString();
            iclient.addAcceptConnectionAttribute("secureTokenError", s2);
            getLogger().error(s2);
        }
        String s3 = (String)iclient.getAppInstance().getProperties().get("secureTokenTarget");
        if(s3 != null)
        {
            doPlay = false;
            doPublish = false;
            doCreate = false;
            String as[] = s3.split("[,]");
            for(int i = 0; i < as.length; i++)
            {
                String s4 = as[i].trim().toLowerCase();
                if(s4.equals("create"))
                {
                    doCreate = true;
                    continue;
                }
                if(s4.equals("publish"))
                {
                    doPublish = true;
                    continue;
                }
                if(s4.equals("play"))
                    doPlay = true;
            }

        }
        getLogger().info((new StringBuilder()).append("SecureTokenTarget: create:").append(doCreate).append(" play:").append(doPlay).append(" publish:").append(doPublish).toString());
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

    public void onConnectAccept(IClient iclient)
    {
    }

    public void onConnectReject(IClient iclient)
    {
    }

    public void onDisconnect(IClient iclient)
    {
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
                if(!boolean1.booleanValue())
                {
                    getLogger().error("Error: SecureToken: Action before response received: kill connection");
                    killClient(iclient);
                    flag = false;
                }
            }
        }
        return flag;
    }

    public void onStreamCreate(IMediaStream imediastream)
    {
        boolean flag = true;
        if(doCreate)
            flag = checkSecureToken(imediastream.getClient());
        if(flag && (doPlay || doPublish))
            imediastream.addClientListener(streamActions);
    }

    public void onStreamDestroy(IMediaStream imediastream)
    {
    }

    private boolean doPlay;
    private boolean doPublish;
    private boolean doCreate;
    private StreamActions streamActions;



}
