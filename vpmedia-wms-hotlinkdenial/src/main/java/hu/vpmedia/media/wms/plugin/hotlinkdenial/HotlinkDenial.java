package hu.vpmedia.media.wms.plugin.hotlinkdenial;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;

public class HotlinkDenial extends ModuleBase 
{

	public void onConnect(IClient client, RequestFunction function,
			AMFDataList params) {
		getLogger().info("onConnect: " + client.getClientId());
		boolean reject = true;
		try
		{
		String pageUrl = client.getProperties().getPropertyStr("connectpageUrl").toLowerCase();
		String[] domainLock = client.getAppInstance().getProperties().getPropertyStr("domainLock").toLowerCase().split(",");
		getLogger().info("domainLock: " + client.getAppInstance().getProperties().getPropertyStr("domainLock").toLowerCase());
		getLogger().info("pageUrl: " + pageUrl);
		for (int i=0; i<domainLock.length; i++)
		{
			if (pageUrl.startsWith(domainLock[i]))
			{
				reject = false;
			}
		}
		}
		catch(Exception ex)
		{
			reject = true;
		}
		if (reject)
		{
			getLogger().info("Client Rejected. IP: " + client.getIp());
			client.rejectConnection();
		}
	}
}