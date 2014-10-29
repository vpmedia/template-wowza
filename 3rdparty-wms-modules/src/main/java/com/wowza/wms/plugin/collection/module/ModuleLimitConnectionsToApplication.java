package com.wowza.wms.plugin.collection.module;

import com.wowza.util.IOPerformanceCounter;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.*;
import com.wowza.wms.httpstreamer.smoothstreaming.httpstreamer.*;
import com.wowza.wms.request.*;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.module.*;

public class ModuleLimitConnectionsToApplication extends ModuleBase
{
	static final public int MAXCONNECTIONS = 200;
	
	private int maxApplicationConnections = MAXCONNECTIONS;
	
	public void onAppStart(IApplicationInstance appInstance)
	{
		this.maxApplicationConnections = appInstance.getProperties().getPropertyInt("maxApplicationConnections", maxApplicationConnections);
	}
	
	public void changeLimit(IClient client, RequestFunction function,
			AMFDataList params) {
		client.getAppInstance().broadcastMsg("handlerName");
		Integer newLimit = params.getInt(PARAM1);
		this.maxApplicationConnections = newLimit;
	}
	
	public void onConnect(IClient client, RequestFunction function, AMFDataList params)
	{
		IApplicationInstance appInstance = client.getAppInstance();
		IApplication app = appInstance.getApplication();
		long count = app.getConnectionCounter().getCurrent(); 
		
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
		
		if ((count+1) > this.maxApplicationConnections && !isPublisher)
		{
			client.rejectConnection("Over application connection limit ["+app.getName()+"/"+appInstance.getName()+"]: Limit is: "+this.maxApplicationConnections);
		}
	}
	
	public void onHTTPSmoothStreamingSessionCreate(HTTPStreamerSessionSmoothStreamer httpSmoothStreamingSession)
	{
		IApplicationInstance appInstance = httpSmoothStreamingSession.getAppInstance();
		IApplication app = appInstance.getApplication();
		long count = app.getConnectionCounter().getCurrent();
		
		if ((count+1) > this.maxApplicationConnections)
		{
			httpSmoothStreamingSession.rejectSession();
		}		
	}

	public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpCupertinoStreamingSession)
	{

		IApplicationInstance appInstance = httpCupertinoStreamingSession.getAppInstance();
		IApplication app = appInstance.getApplication();
		long count = app.getConnectionCounter().getCurrent();

		if ((count+1) > this.maxApplicationConnections)
		{
			httpCupertinoStreamingSession.rejectSession();
		}
	}
	public void onRTPSessionCreate(RTPSession rtpSession)
	{
		IApplicationInstance appInstance = rtpSession.getAppInstance();
		IApplication app = appInstance.getApplication();
		long count = app.getConnectionCounter().getCurrent();

		if ((count+1) > this.maxApplicationConnections)
		{
			rtpSession.rejectSession();
		}
	}
	
	public void onHTTPSmoothStreamingSessionDestroy(HTTPStreamerSessionSmoothStreamer session)
	{
	}
	
	public void onHTTPCupertinoStreamingSessionDestroy(HTTPStreamerSessionCupertino session)
	{
	}
	public void onRTPSessionDestroy(RTPSession session)
	{
	}
	
}
