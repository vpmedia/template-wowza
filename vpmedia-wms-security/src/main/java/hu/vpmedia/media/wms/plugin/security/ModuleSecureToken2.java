package hu.vpmedia.media.wms.plugin.security;

import com.wowza.util.URLUtils;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.IModuleOnConnect;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.vhost.IVHost;
import java.util.Date;
import java.util.Map;

public class ModuleSecureToken2 extends ModuleBase
    implements IModuleOnConnect
{

    public ModuleSecureToken2()
    {
    }

    public void onConnect(IClient iclient, RequestFunction requestfunction, AMFDataList amfdatalist)
    {
        String s = iclient.getQueryStr();
        String s1 = "Unknown reason";
        String s2 = null;
        boolean flag = false;
        if(s == null)
            s1 = "Query string null";
        else
        if(s.length() == 0)
        {
            s1 = "Query string empty";
        } else
        {
            Map map = URLUtils.parseQueryStr(s, true);
            s2 = URLUtils.getParamValue(map, "secureToken");
            if(s2 == null)
                s1 = (new StringBuilder()).append("Query string mising secureToken (").append(s).append(")").toString();
        }
        if(s2 == null && amfdatalist.size() > 3)
        {
            AMFData amfdata = amfdatalist.get(amfdatalist.size() - 1);
            if(amfdata.getType() == 2)
            {
                s2 = amfdatalist.getString(amfdatalist.size() - 1);
                s1 = "Unknown reason";
            }
        }
        if(s2 != null)
        {
            SecureTokenManager securetokenmanager = SecureTokenManager.getInstance(iclient.getVHost());
            Map map1 = securetokenmanager.getTokenList();
            SecureToken securetoken = (SecureToken)map1.get(s2);
            if(securetoken == null)
            {
                s1 = (new StringBuilder()).append("SecureToken not found (").append(s2).append(")").toString();
            } else
            {
                int i = securetoken.incUsageCount();
                if(i == 1)
                {
                    securetoken.setClient(iclient);
                    securetokenmanager.registerClient(iclient, securetoken);
                    iclient.acceptConnection("SecureToken valid");
                    flag = true;
                } else
                {
                    IClient iclient1 = securetoken.getClient();
                    if(iclient1 != null)
                    {
                        s1 = (new StringBuilder()).append("SecureToken already used by client (").append(iclient1.getClientId()).append(")").toString();
                        SecureTokenDef securetokendef = securetoken.getSecureTokenDef();
                        if(securetokendef != null)
                        {
                            if(securetokendef.isDoKill())
                                iclient1.getVHost().removeClient(iclient1.getClientId());
                            if(securetokendef.isDoNotify())
                            {
                                AMFDataObj amfdataobj = new AMFDataObj();
                                amfdataobj.put("status", new AMFDataItem("error"));
                                amfdataobj.put("reason", new AMFDataItem(s1));
                                amfdataobj.put("clientId", new AMFDataItem(iclient.getClientId()));
                                amfdataobj.put("ipAddress", new AMFDataItem(iclient.getIp()));
                                amfdataobj.put("date", new AMFDataItem(new Date()));
                                iclient1.call("onSecureToken", null, new Object[] {
                                    amfdataobj
                                });
                            }
                        }
                    } else
                    {
                        s1 = (new StringBuilder()).append("SecureToken usage count is wrong (").append(securetoken.getUsageCount()).append(")").toString();
                    }
                }
            }
        }
        if(!flag)
        {
            iclient.rejectConnection(s1);
            //WMSLoggerFactory.getLogger(com/wowza/wms/plugin/security/ModuleSecureToken2).info((new StringBuilder()).append("SecureToken: Connection refused (").append(iclient.getClientId()).append("): ").append(s1).toString());
        }
    }

    public void onConnectAccept(IClient iclient)
    {
    }

    public void onConnectReject(IClient iclient)
    {
    }

    public void onDisconnect(IClient iclient)
    {
        SecureTokenManager securetokenmanager = SecureTokenManager.getInstance(iclient.getVHost());
        securetokenmanager.unregisterClient(iclient);
    }
}
