package hu.vpmedia.media.wms.plugin.security;

import com.wowza.util.XMLUtils;
import com.wowza.wms.client.IClient;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;
import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import org.w3c.dom.*;

public class SecureTokenManager
{

    public SecureTokenManager()
    {
        tokenLists = new HashMap();
        secureTokenDefs = new HashMap();
        clientMap = new HashMap();
    }

    public static SecureTokenManager getInstance(IVHost ivhost)
    {
        SecureTokenManager securetokenmanager = (SecureTokenManager)instances.get(ivhost);
        if(securetokenmanager == null)
        {
            securetokenmanager = new SecureTokenManager();
            securetokenmanager.loadConfig(ivhost);
            instances.put(ivhost, securetokenmanager);
        }
        return securetokenmanager;
    }

    public void loadConfig(IVHost ivhost)
    {
        String s = (new StringBuilder()).append(ivhost.getHomePath()).append("/conf/SecureTokens.xml").toString();
        File file = new File(s);
        if(file.exists())
        {
            //WMSLoggerFactory.getLogger(com/wowza/wms/plugin/security/HTTPSecureTokens).info((new StringBuilder()).append("HTTPSecureTokens: loading config: ").append(s).toString());
            loadConfigFile(ivhost, new File(s));
        }
    }

    private void loadConfigFile(IVHost ivhost, File file)
    {
        try
        {
            DocumentBuilderFactory documentbuilderfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentbuilder = documentbuilderfactory.newDocumentBuilder();
            Document document = documentbuilder.parse(file);
            XPathFactory xpathfactory = XMLUtils.newXPathFactory();
            XPath xpath = xpathfactory.newXPath();
            if(document != null)
            {
                Element element = document.getDocumentElement();
                String s = "/Root/SecureTokens/SecureToken";
                XPathExpression xpathexpression = xpath.compile(s);
                NodeList nodelist = (NodeList)xpathexpression.evaluate(element, XPathConstants.NODESET);
                if(nodelist != null)
                {
                    SecureTokenManager securetokenmanager = this;
                    Map map = securetokenmanager.getSecureTokenDefs();
                    for(int i = 0; i < nodelist.getLength(); i++)
                    {
                        org.w3c.dom.Node node = nodelist.item(i);
                        Element element1 = (Element)node;
                        String s1 = XMLUtils.getXMLPropertyStr(xpath, "Name", element1, null);
                        String s2 = XMLUtils.getXMLPropertyStr(xpath, "SharedSecret", element1, null);
                        if(s1 != null && s2 != null)
                        {
                            SecureTokenDef securetokendef = new SecureTokenDef(s1, s2);
                            String s3 = XMLUtils.getXMLPropertyStr(xpath, "ClientAction", element1, null);
                            if(s3 != null)
                            {
                                if(s3.equalsIgnoreCase("kill"))
                                    securetokendef.setDoKill(true);
                                if(s3.equalsIgnoreCase("notify"))
                                    securetokendef.setDoNotify(true);
                            }
                            map.put(s1, securetokendef);
                            //WMSLoggerFactory.getLogger(com/wowza/wms/plugin/security/HTTPSecureTokens).info((new StringBuilder()).append("HTTPSecureTokens: SecureToken: ").append(s1).toString());
                        } else
                        {
                            //WMSLoggerFactory.getLogger(com/wowza/wms/plugin/security/HTTPSecureTokens).error("HTTPSecureTokens: loadConfigFile: error parsing secure token file: Name and SharedSecret elements required.");
                        }
                    }

                }
            }
        }
        catch(Exception exception)
        {
            //WMSLoggerFactory.getLogger(com/wowza/wms/plugin/security/HTTPSecureTokens).error((new StringBuilder()).append("HTTPSecureTokens: loadConfigFile: error parsing secure token file: (").append(file).append(") error: ").append(exception.toString()).toString());
            exception.printStackTrace();
        }
    }

    public Map getTokenList()
    {
        return tokenLists;
    }

    public String newGUID()
    {
        return UUID.randomUUID().toString();
    }

    public Map getSecureTokenDefs()
    {
        return secureTokenDefs;
    }

    public void registerClient(IClient iclient, SecureToken securetoken)
    {
        synchronized(clientMap)
        {
            clientMap.put(iclient, securetoken);
        }
    }

    public SecureToken unregisterClient(IClient iclient)
    {
        SecureToken securetoken = null;
        synchronized(clientMap)
        {
            securetoken = (SecureToken)clientMap.remove(iclient);
        }
        return securetoken;
    }

    public SecureToken getSecureTokenByClient(IClient iclient)
    {
        SecureToken securetoken = null;
        synchronized(clientMap)
        {
            securetoken = (SecureToken)clientMap.get(iclient);
        }
        return securetoken;
    }

    private static Map instances = new HashMap();
    private Map tokenLists;
    private Map secureTokenDefs;
    private Map clientMap;

}
