package com.wowza.wms.plugin.collection.module;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;

public class ModuleRequireSecureConnection extends ModuleBase
{
	public void onConnect(IClient client, RequestFunction function, AMFDataList params)
	{
		
		String flashver = client.getFlashVer();
		getLogger().info("Flashver: " + flashver);
		
		Boolean isPublisher = false;
		try
		{
		isPublisher = flashver.startsWith(client.getAppInstance().getProperties().getPropertyStr("AllowEncoder"));
		}
		catch(Exception ex)
		{
		}
		
		if (!client.isSecure() && !isPublisher)
		{
			client.rejectConnection("Secure connection required.");
			getLogger().info("ModuleRequireSecureConnection.onConnect: rejectConnection: clientId:"+client.getClientId());
		}
	}
} 