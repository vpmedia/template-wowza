package com.wowza.wms.plugin.collection.module;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.wowza.util.IOPerformanceCounter;
import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.module.*;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify2;

public class ModuleLimitPublishedStreamBandwidth extends ModuleBase {
	
	int MAXBITRATE = 800; // 0 = no limit
	int INTERVAL = 5000;
	
	public void onAppStart(IApplicationInstance appInstance)
	{
		MAXBITRATE = appInstance.getApplication().getProperties().getPropertyInt("MaxBitrate",MAXBITRATE);
	}
	
	public void onStreamCreate(IMediaStream stream) {
		getLogger().info("StreamCreate Name: " + stream.getName());
		
		IMediaStreamActionNotify2 actionNotify  = new StreamListener();
		WMSProperties props = stream.getProperties();
		synchronized(props)
		{
			props.put("streamActionNotifier", actionNotify);
		}
		stream.addClientListener(actionNotify);
	}
	public void onStreamDestroy(IMediaStream stream) {
		getLogger().info("StreamDestroy: " + stream.getName());
		
		IMediaStreamActionNotify2 actionNotify = null;
		WMSProperties props = stream.getProperties();
		synchronized(props)
		{
			actionNotify = (IMediaStreamActionNotify2)stream.getProperties().get("streamActionNotifier");
		}
		if (actionNotify != null)
		{
			stream.removeClientListener(actionNotify);
			getLogger().info("removeClientListener: "+stream.getSrc());
		}
		stream.removeClientListener(actionNotify);
	}
	
	class StreamListener  implements IMediaStreamActionNotify2 
	{
		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) 
		{
			getLogger().info("Play Stream: " + streamName);
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
			getLogger().info("onSeek, stream name: " + stream.getName());
		}
		
		public void onStop(IMediaStream stream)
		{
			getLogger().info("onStop By: " + stream.getClientId());
		}
		
		public void onPause(IMediaStream stream, boolean isPause, double location)
		{
			getLogger().info("onPause");
		}
		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			getLogger().info("unPublish Stream: " + stream.getName());
			WMSProperties props = stream.getProperties();
			
			MonitorStream monitor;
			
			synchronized(props)
			{
				monitor = (MonitorStream)props.get("monitor");
			}
			if (monitor!=null)
				monitor.stop();
		}

		public  void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			MonitorStream monitor = new MonitorStream(stream);
			WMSProperties props = stream.getProperties();
			synchronized(props)
			{
				props.put("monitor", monitor);
			}
			monitor.start();
		}
	}
	
	private class MonitorStream {
		public Timer mTimer;
		public TimerTask mTask;
		public IMediaStream stream;
		public MonitorStream(IMediaStream s){
			stream = s;
			mTask = new TimerTask(){
				public void run() {
					
					if (stream==null)
						stop();
					
					getLogger().info("");
					IOPerformanceCounter perf = stream.getMediaIOPerformance();
					Double bitrate = perf.getMessagesInBytesRate() * 8 * .001;
					getLogger().info("Stream '" + stream.getName() + "' BitRate: " + Math.round(Math.floor(bitrate)) + "kbs");
					if (bitrate > MAXBITRATE && MAXBITRATE > 0)
					{
						getLogger().info("Sent NetStream.Publish.Rejected to " + stream.getClientId() + " stream name: " + stream.getName());
						sendStreamOnStatusError(stream, "NetStream.Publish.Rejected", "bitrate too high");
						
						stream.getClient().setShutdownClient(true);
					}
					}
			};
			mTimer = new Timer();		
		}
		
		public void start(){			
			if (mTimer==null)
				mTimer = new Timer();
			mTimer.scheduleAtFixedRate(mTask, new Date(),INTERVAL);
		}
		
		public void stop(){
			if (mTimer != null){
				mTimer.cancel();
				mTimer=null;
			}
		}
	}
}