	package com.wowza.wms.plugin.collection.module;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.request.*;
import com.wowza.wms.response.*;
import com.wowza.wms.module.*;
import com.wowza.wms.stream.*;

public class ModuleFCSubscribe extends ModuleBase
{
	public void FCSubscribe(IClient client, RequestFunction function, AMFDataList params)
	{
		String streamName = params.getString(PARAM1);
		
		IMediaStream stream = null;
		if (streamName != null)
			stream = client.getAppInstance().getStreams().getStream(streamName);
		
		String responseCode = (stream == null)?"NetStream.Play.StreamNotFound":"NetStream.Play.Start";
  
		AMFDataObj data = null;
		ResponseFunction resp = null;
		ResponseFunctions respFunctions = client.getRespFunctions();
		double clientID = client.getClientId();

		resp = new ResponseFunction(client);
		resp.createDefaultMessage("onFCSubscribe", 0.0);
		
		data = new AMFDataObj();
		data.put("level", new AMFDataItem("status"));
		data.put("code", new AMFDataItem(responseCode));
		data.put("clientid", new AMFDataItem((double)clientID));
		resp.addBody(data);	
 
		respFunctions.add(resp);
	}
}