package com.wowza.wms.plugin.collection.module;
 
import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.*;	
import com.wowza.wms.util.*;
import com.wowza.wms.module.*;

public class ModuleCalculateBitRate extends ModuleBase
{
	public void getStreamBitrate(IClient client, RequestFunction function, AMFDataList params)
	{ 
		double ret = 0;
		String streamName = getParamString(params, PARAM1);
		String streamExt = MediaStream.BASE_STREAM_EXT;
		String queryStr = ""; 
		
		String[] streamDecode = ModuleUtils.decodeStreamExtension(streamName, streamExt);
		streamName = streamDecode[0];
		streamExt = streamDecode[1];
		
		boolean isStreamNameURL = streamName.indexOf("://") >= 0;
		int streamQueryIdx = streamName.indexOf("?");
		if (!isStreamNameURL && streamQueryIdx >= 0)
		{
			queryStr = streamName.substring(streamQueryIdx+1);
			streamName = streamName.substring(0, streamQueryIdx);
		}
		 
		IApplicationInstance appInstance = client.getAppInstance();
		String oldStreamName = null;

		IMediaReader mediaReader = MediaReaderFactory.getInstance(appInstance, client.getVHost().getMediaReaders(), streamExt);
		if (mediaReader != null)
		{
			MediaStreamMap streams = appInstance.getStreams();
			IMediaStream stream = new MediaStreamDisconnected();
			stream.init(streams, 0, new WMSProperties());
			stream.setName(streamName, oldStreamName, streamExt, queryStr, 0, -1, 1);
			stream.setClient(client);
			
			String basePath = client.getAppInstance().getStreamStoragePath();
			mediaReader.init(client.getAppInstance(), stream, streamExt, basePath, streamName);
			long lastTC = mediaReader.getDuration();
			double duration = (double)lastTC/1000.0;
			long length = mediaReader.getLength();
			if (duration != 0)
				ret = (((double)length*8.0)/duration);
			mediaReader.close();
			
			getLogger().debug("calculateBitrate duration:"+duration+" length:"+length+" ret:"+ret);
		}
		else
			getLogger().warn("calculateBitrate: Could not create MediaReader for stream: "+streamName);
						
		getLogger().info("calculateBitrate ["+streamName+"] bitrate:"+ret);
		sendResult(client, params, ret);
	}
}