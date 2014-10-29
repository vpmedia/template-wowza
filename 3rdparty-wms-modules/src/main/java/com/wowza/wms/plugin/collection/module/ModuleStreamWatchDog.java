package com.wowza.wms.plugin.collection.module;

import java.util.*;
import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.module.*;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify2;
import com.wowza.wms.stream.MediaStreamMap;
import java.util.Timer;
import java.util.List;

public class ModuleStreamWatchDog extends ModuleBase {
	
	public void onStreamCreate(IMediaStream stream) {
		getLogger().info("onStreamCreate by: " + stream.getClientId());
		IMediaStreamActionNotify2 actionNotify  = new StreamListener();
		
		WMSProperties props = stream.getProperties();
		synchronized(props)
		{
			props.put("streamActionNotifier", actionNotify);
		}
		stream.addClientListener(actionNotify);
	}
	public void onStreamDestroy(IMediaStream stream) {
		getLogger().info("onStreamDestroy by: " + stream.getClientId());
		
		IMediaStreamActionNotify2 actionNotify = null;
		WMSProperties props = stream.getProperties();		synchronized(props)
		{
			actionNotify = (IMediaStreamActionNotify2)stream.getProperties().get("streamActionNotifier");
		}
		if (actionNotify != null)
		{
			stream.removeClientListener(actionNotify);
			getLogger().info("removeClientListener: "+stream.getSrc());
		}
	}
	
	class StreamListener  implements IMediaStreamActionNotify2 
	{
		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) 
		{
			streamName = stream.getName();
			getLogger().info("Stream Name: " + streamName);
		}
		
		public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket) 
		{
			getLogger().info("onMetaData By: " + stream.getClientId());
		}
		
		public void onPauseRaw(IMediaStream stream, boolean isPause, double location) 
		{
			getLogger().info("onPauseRaw By: " + stream.getClientId());
		}

		public void onSeek(IMediaStream stream, double location)
		{
			getLogger().info("onSeek");
		}
		
		public void onStop(IMediaStream stream)
		{
			getLogger().info("onStop By: " + stream.getClientId());
		}
		
		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			getLogger().info("onUNPublish");
			StreamWatchDog watchdog = (StreamWatchDog)stream.getClient().getAppInstance().getProperties().getProperty(streamName);
			if (watchdog != null)
				watchdog.stop();		
		}

		public  void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			getLogger().info("onPublish - stream name: " + stream.getName());
			getLogger().info("query string: " + stream.getQueryStr());
			getLogger().info("video codec: " + stream.getPublishVideoCodecId());
			StreamWatchDog watchdog = new StreamWatchDog();
			try
			{
				watchdog.appInstance = stream.getClient().getAppInstance();
				watchdog.streamName = streamName;
				watchdog.start();
				stream.getClient().getAppInstance().getProperties().setProperty(streamName, watchdog);
			}
			catch(Exception ex)
			{
				
			}
		}
		public void onPause(IMediaStream stream, boolean isPause, double location)
		{
			getLogger().info("onPause");
		}
	}
	
	private class StreamWatchDog
	{
		public Timer mTimer;
		public TimerTask mTask;
		public String streamName;
		public IApplicationInstance appInstance;
		long streamLastSeq;
		boolean isMissing = false;
		long currSeq = 0;
		String msg;
		public StreamWatchDog(){
			mTask = new TimerTask()
			{
				public void run() 
				{
					getLogger().info("Run StreamWatchDog");
					MediaStreamMap mediamap = appInstance.getStreams();
                    IMediaStream stream = mediamap.getStream(streamName);
                	
                	List packets = stream.getPlayPackets();
                	if (packets.size() == 0)
                    {
                		msg = "Stream not started";
                    }
                    else
                    {
                  	  AMFPacket packet = (AMFPacket)packets.get(packets.size()-1);
                      currSeq = packet.getSeq();
                      if (currSeq != streamLastSeq)
                      {
                         streamLastSeq = currSeq;
                         msg = "Stream OK";
                      }
                      else
                      {
                         msg = "Stream Appears Stalled";
                      }
                    }
                    appInstance.broadcastMsg("streamStats", streamName,currSeq,msg,new Date());
				}
			};
		}
		
		public void start(){
			
			if (mTimer==null)
				mTimer = new Timer();
			mTimer.schedule(mTask, 10000, 10000);
			getLogger().info("Start StreamWatchDog");
		}
		
		public void stop(){
			if (mTimer != null){
				mTimer.cancel();
				mTimer=null;
				getLogger().info("Stop StreamWatchDog");				
			}
		}
	}
}
