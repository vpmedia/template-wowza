package hu.vpmedia.media.wms.plugin.security;

public class SecureTokenDef
{

    public SecureTokenDef(String s, String s1)
    {
        name = "";
        sharedSecret = "";
        doKill = false;
        doNotify = false;
        name = s;
        sharedSecret = s1;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String s)
    {
        name = s;
    }

    public String getSharedSecret()
    {
        return sharedSecret;
    }

    public void setSharedSecret(String s)
    {
        sharedSecret = s;
    }

    public boolean isDoKill()
    {
        return doKill;
    }

    public void setDoKill(boolean flag)
    {
        doKill = flag;
    }

    public boolean isDoNotify()
    {
        return doNotify;
    }

    public void setDoNotify(boolean flag)
    {
        doNotify = flag;
    }

    private String name;
    private String sharedSecret;
    private boolean doKill;
    private boolean doNotify;
}
