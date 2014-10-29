package hu.vpmedia.media.wms.plugin.security;

import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.http.*;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import hu.vpmedia.media.wms.plugin.security.encryption.TEA;
import com.wowza.wms.vhost.HostPort;
import com.wowza.wms.vhost.IVHost;
import java.io.OutputStream;
import java.util.Map;

public class HTTPSecureTokens
    implements IHTTPProvider
{

    public HTTPSecureTokens()
    {
        properties = null;
    }

    public void onBind(IVHost ivhost, HostPort hostport)
    {
    }

    public void onUnbind(IVHost ivhost, HostPort hostport)
    {
    }

    public void setProperties(WMSProperties wmsproperties)
    {
        properties = wmsproperties;
    }

    public void onHTTPRequest(IVHost ivhost, IHTTPRequest ihttprequest, IHTTPResponse ihttpresponse)
    {
        String s = ihttprequest.getParameter("secureTokenName");
        if(s == null)
            s = "default";
        if(s.length() == 0)
            s = "default";
        SecureTokenManager securetokenmanager = SecureTokenManager.getInstance(ivhost);
        Map map = securetokenmanager.getTokenList();
        String s1 = securetokenmanager.newGUID();
        Map map1 = securetokenmanager.getSecureTokenDefs();
        SecureTokenDef securetokendef = (SecureTokenDef)map1.get(s);
        String s2 = "";
        if(securetokendef != null)
        {
            SecureToken securetoken = new SecureToken(s1, securetokendef);
            synchronized(map)
            {
                map.put(s1, securetoken);
            }
            s2 = TEA.encrypt(s1, securetokendef.getSharedSecret());
        } else
        {
           // WMSLoggerFactory.getLogger(com/webcam/media/wms/plugin/security/HTTPSecureTokens).error((new StringBuilder()).append("Secure token definition missing for (").append(s).append(").").toString());
        }
        String s3 = (new StringBuilder()).append("connectToken=").append(s2).toString();
        try
        {
            OutputStream outputstream = ihttpresponse.getOutputStream();
            byte abyte0[] = s3.toString().getBytes();
            outputstream.write(abyte0);
        }
        catch(Exception exception)
        {
           // WMSLoggerFactory.getLogger(com/webcam/media/wms/plugin/security/HTTPSecureTokens).error((new StringBuilder()).append("HTMLServerVersion: ").append(exception.toString()).toString());
        }
    }

    private WMSProperties properties;
}
