package com.wowza.wms.plugin.collection.module;

import java.util.*;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.server.*;
import com.wowza.wms.application.*;
import com.wowza.wms.stream.*;

public class ModuleGetConnectionCount extends ModuleBase
{
    public void getServerConnectionCount(IClient client, RequestFunction function, AMFDataList params)
    {
		Server server = Server.getInstance();
		int count = (int)server.getConnectionCounter().getCurrent();
		sendResult(client, params, count);
    }
   
    public void getApplicationConnectionCount(IClient client, RequestFunction function, AMFDataList params)
    {
		IApplication application = client.getApplication();
		int count = (int)application.getConnectionCounter().getCurrent();
		sendResult(client, params, count);
    }
    
    public void getApplicationInstanceConnectionCount(IClient client, RequestFunction function, AMFDataList params)
    {
		IApplicationInstance applicationInstance = client.getAppInstance();
		int count = (int)applicationInstance.getConnectionCounter().getCurrent();
		sendResult(client, params, count);
    }
    
    public void getStreamConnectionCount(IClient client, RequestFunction function, AMFDataList params)
    {
    	String streamName = params.getString(PARAM1);
    	int count = 0;
		IApplicationInstance applicationInstance = client.getAppInstance();
    	List<IMediaStream> streamList = applicationInstance.getPlayStreamsByName(streamName);
    	if (streamList != null)
    		count = streamList.size();
    	sendResult(client, params, count);
    }
    
    public void getStreamClientIds(IClient client, RequestFunction function, AMFDataList params)
    {
    	AMFDataArray clientList = new AMFDataArray();
    	String streamName = params.getString(PARAM1);
 		IApplicationInstance applicationInstance = client.getAppInstance();
    	List<IMediaStream> streamList = applicationInstance.getPlayStreamsByName(streamName);
    	if (streamList != null)
    	{
    		Iterator<IMediaStream> iter = streamList.iterator();
    		while (iter.hasNext())
    		{
    			IMediaStream stream = iter.next();
    			if (stream == null)
    				continue;
    			IClient sclient = stream.getClient();
    			if (sclient == null)
    				continue;
    			clientList.add(new AMFDataItem(sclient.getClientId()));
    		}
    	}
    	sendResult(client, params, clientList);
    }
}
