package hu.vpmedia.media.wms.plugin.security;

import com.wowza.wms.client.IClient;

public class SecureToken
{

    public SecureToken(String s, SecureTokenDef securetokendef)
    {
        guid = null;
        usageCount = 0;
        client = null;
        secureTokenDef = null;
        guid = s;
        secureTokenDef = securetokendef;
    }

    public synchronized int incUsageCount()
    {
        usageCount++;
        return usageCount;
    }

    public synchronized int getUsageCount()
    {
        return usageCount;
    }

    public IClient getClient()
    {
        return client;
    }

    public void setClient(IClient iclient)
    {
        client = iclient;
    }

    public SecureTokenDef getSecureTokenDef()
    {
        return secureTokenDef;
    }

    public void setSecureTokenDef(SecureTokenDef securetokendef)
    {
        secureTokenDef = securetokendef;
    }

    private String guid;
    private int usageCount;
    private IClient client;
    private SecureTokenDef secureTokenDef;
}
