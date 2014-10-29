package com.wowza.wms.plugin.collection.module;

import java.util.*;

import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.module.*;

public class ModuleLimitStreamViewers extends ModuleBase
{
	static final public int MAXVIEWERS = 200;
	
	private int maxStreamViewers = MAXVIEWERS;
	
	public void onAppStart(IApplicationInstance appInstance)
	{
		this.maxStreamViewers = appInstance.getProperties().getPropertyInt("maxStreamViewers", MAXVIEWERS);
	}

	public void play(IClient client, RequestFunction function, AMFDataList params)
	{
		String streamName = params.getString(PARAM1);
		IMediaStream stream = getStream(client, function);
		
		IApplicationInstance appInstance = client.getAppInstance();
		List streamList = appInstance.getPlayStreamsByName(streamName);
		int count = streamList==null?0:streamList.size();
		
		if (count < this.maxStreamViewers)
		{
			this.invokePrevious(client, function, params);
		}
		else
		{
			if (stream != null)
			{
				String code = "NetStream.Play.Failed";
				String description = "ModuleLimitViewers: Over viewer limit["+this.maxStreamViewers+"]";
				sendStreamOnStatusError(stream, code, description);
			}
		} 
	}
}
