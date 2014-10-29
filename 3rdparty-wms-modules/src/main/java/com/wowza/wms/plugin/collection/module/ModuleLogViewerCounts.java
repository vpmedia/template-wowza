package com.wowza.wms.plugin.collection.module;

import java.util.*;

import com.wowza.wms.logging.*;
import com.wowza.wms.module.*;
import com.wowza.wms.application.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.amf.*;

public class ModuleLogViewerCounts extends ModuleBase
{
	class MyLogger extends Thread
	{
		private boolean running = true;
		private boolean quit = false;
		private int logTime = 10000;
		private long lastLogTime = -1;
		private IApplicationInstance appInstance = null;
		
		public MyLogger(IApplicationInstance appInstance)
		{
			this.appInstance = appInstance;
		}
		
		public synchronized void quit()
		{
			this.quit = true;
		}
		
		private Map<String, String> getMetadataInfo(IMediaStream stream)
		{
			Map<String, String> ret = new HashMap<String, String>();			
			try
			{
				IMediaStreamMetaDataProvider metaDataProvider = stream.getMetaDataProvider();
				
				while (true)
				{
					if (metaDataProvider == null)
						break;
					
					List metaDataList = new ArrayList();
					long firstTimecode = 0;
					AMFPacket packet = stream.getLastPacket();
					firstTimecode = packet==null?0:packet.getAbsTimecode();
					metaDataProvider.onStreamStart(metaDataList, firstTimecode);
	
					if (metaDataList.size() <= 0)
						break;
					
					for(int i=0;i<metaDataList.size();i++)
					{
					
						AMFPacket metaPacket = (AMFPacket)metaDataList.get(i);
						AMFDataList dataList = new AMFDataList(metaPacket.getData());
						
						if (dataList.size() < 2)
							break;
										
						if (dataList.get(1).getType() == AMFData.DATA_TYPE_MIXED_ARRAY)
						{
							AMFDataMixedArray arr = (AMFDataMixedArray)dataList.get(1);
							Iterator<String> iter = arr.getKeys().iterator();
							while(iter.hasNext())
							{
								String key = iter.next();
								String value = arr.getString(key);
								if (value == null)
									continue;
								ret.put(key, value);
							}
						}
						else if (dataList.get(1).getType() == AMFData.DATA_TYPE_OBJECT)
						{
							AMFDataObj obj = (AMFDataObj)dataList.get(1);
							Iterator<String> iter = obj.getKeys().iterator();
							while(iter.hasNext())
							{
								String key = iter.next();
								String value = obj.getString(key);
								if (value == null)
									continue;
								ret.put(key, value);
							}
						}
					}
									
					break;
				}
			}
			catch (Exception e)
			{
				
			}
						
			return ret;
		}
		
		public void run()
		{
			while(true)
			{
				try
				{
					long currTime = System.currentTimeMillis();
					if (lastLogTime == -1)
						lastLogTime = currTime;
					
					if ((currTime - lastLogTime) > logTime)
					{
						MediaStreamMap streams = appInstance.getStreams();
						List<String> streamNames = streams.getPublishStreamNames();
						
						Iterator<String> iter = streamNames.iterator();
						while(iter.hasNext())
						{
							String streamName = iter.next();
							
							
							List<IMediaStream> listeners = appInstance.getPlayStreamsByName(streamName);
							if (listeners == null)
								continue;

							//System.out.println("streamName: "+listeners.size()+":"+streamName);

							IMediaStream stream = streams.getStream(streamName);
							if (stream == null)
								continue;
							
							IMediaStreamMetaDataProvider metaDataProvider = stream.getMetaDataProvider();
							
							List<AMFPacket> metaData = new ArrayList<AMFPacket>();
							metaDataProvider.onStreamStart(metaData, 0);
							
							String metaDataStr = "";
							int count = listeners.size();
							metaDataStr += "viewers"+": \""+count+"\"";
							Map<String, String> metaList = getMetadataInfo(stream);
							Iterator<String> iter2 = metaList.keySet().iterator();
							while(iter2.hasNext())
							{
								String key = iter2.next();
								String value = metaList.get(key);
								if (metaDataStr.length() > 0)
									metaDataStr += ", ";
								metaDataStr += key+": \""+value+"\"";
							}
							
							WMSLoggerFactory.getLogger(ModuleCore.class).info("{"+metaDataStr+"}", stream, WMSLoggerIDs.CAT_stream, "listeners", WMSLoggerIDs.STAT_general_successful, streamName);
						}
						
						lastLogTime = currTime;
					}

					synchronized(this)
					{
						Thread.currentThread().sleep(100);
						if (quit)
						{
							running = false;
							break;
						}
					}
				}
				catch (Exception e)
				{
					
				}
			}
		}
	}
	
	private MyLogger logger = null;
	
	public void onAppStart(IApplicationInstance appInstance)
	{
		Integer maxRate = appInstance.getProperties().getPropertyInt("maxBitRate", 500000);
		this.logger = new MyLogger(appInstance);
		this.logger.setDaemon(true);
		this.logger.start();
	}
	
	public void onAppStop(IApplicationInstance appInstance)
	{
		if (this.logger != null)
			this.logger.quit();
		this.logger = null;
	}
}
