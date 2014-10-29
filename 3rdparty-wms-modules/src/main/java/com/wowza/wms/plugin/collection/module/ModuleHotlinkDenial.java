package com.wowza.wms.plugin.collection.module;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;

public class ModuleHotlinkDenial extends ModuleBase {

	public void onConnect(IClient client, RequestFunction function,
			AMFDataList params) {
		
		getLogger().info("onConnect HotlinkDenial Module: " + client.getClientId());
  
		String flashver = client.getFlashVer();
		getLogger().info("Flashver: " + flashver);
		
		Boolean isPublisher=false;
		String allowedEncoder="";
		
		try
		{
		allowedEncoder = client.getAppInstance().getProperties().getPropertyStr("AllowEncoder");
		isPublisher = flashver.startsWith(allowedEncoder); 
		
		if (isPublisher) 
		{
			client.acceptConnection();
			return;
		}
		}
		catch(Exception ex)
		{	
		}
		boolean reject = true;
		String[] domainLocks = null;
		String[] domainUrl = null;;
			
		try 
		{ 
		domainLocks = client.getAppInstance().getProperties().getPropertyStr("domainLock").toLowerCase().split(",");
		String pageUrl = client.getProperties().getPropertyStr("connectpageUrl").toLowerCase();
		domainUrl = pageUrl.split("/");
		getLogger().info("domainLock: " + client.getAppInstance().getProperties().getPropertyStr("domainLock").toLowerCase());
		getLogger().info("pageUrl: " + pageUrl);
		for (int i = 0; i < domainLocks.length; i++)
		{ 
			if (domainLocks[i].trim().startsWith("*"))
			{
				String lock = domainLocks[i].trim().substring(1);
				if (domainUrl[2].endsWith(lock))
				{
					reject = false;
				}
			} 
			else if (domainUrl[2].equalsIgnoreCase(domainLocks[i].trim()))
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