package com.wowza.wms.plugin.collection.module;

// com.wowza.wms.plugin.collection.module.ModuleNoDuplicatePublishStreamname

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.IMediaStream;

public class ModuleNoDuplicatePublishStreamname extends ModuleBase {
	
	public void publish(IClient client, RequestFunction function, AMFDataList params)
	{
		boolean bAuthorized = false;

		try {
			bAuthorized = checkPermissions(client, function, params);
		}
		catch (Exception ex) {
			
		}
			
		if (bAuthorized != true)
		{
			sendClientOnStatusError(client, "NetStream.Publish.Denied", "Invalid credentials supplied");
		}
		else
		{
			invokePrevious(client, function, params);
		}
	}

	
	public void releaseStream(IClient client, RequestFunction function, AMFDataList params)
	{
		boolean bAuthorized = false;

		try {
			bAuthorized = checkPermissions(client, function, params);
		}
		catch (Exception ex) {
			// some error
		}
			
		if (bAuthorized != true)
		{
			sendClientOnStatusError(client, "NetStream.Publish.Denied", "Invalid credentials supplied");
			client.setShutdownClient(true);
		}
		else
		{
			invokePrevious(client, function, params);
		}
	}
	public boolean checkPermissions(IClient client, RequestFunction function, AMFDataList params)
	{
		boolean doesStreamExist = false;
		boolean authorized = false;
		
		String streamName;
		try
		{
		streamName = params.getString(PARAM1).split("\\?")[0];
		}
		catch(Exception ex)	
		{
			return false;
		}
		
		IMediaStream stream = client.getAppInstance().getStreams().getStream(streamName);
		
		getLogger().info("Checking stream Name: " + streamName);
		
		doesStreamExist = (stream != null);
		
		if (doesStreamExist)
		{
			authorized = false;
		}
		else
		{
			authorized = true;
		}
		
		// add other permission checking
		
		getLogger().info("Authorized: " + authorized);
		
		return authorized;
	}

}