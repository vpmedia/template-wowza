package com.wowza.wms.plugin.collection.serverlistener;

import com.wowza.wms.amf.AMFDataObj;
import com.wowza.wms.application.*;
import com.wowza.wms.server.*;
import com.wowza.wms.vhost.*;

import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.publish.*;
import com.wowza.wms.logging.*;
import java.io.File;
import java.text.SimpleDateFormat; 
import java.util.*; 

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element; 
import org.w3c.dom.Node; 
import org.w3c.dom.NodeList;
 
public class ServerListenerStreamPublisher implements IServerNotify { 
	
	WMSLogger log = WMSLoggerFactory.getLogger(null); 
	
	Map<String, Stream> streamMap = new HashMap<String, Stream>();
	Map<String, Playlist> playlistMap = new HashMap<String, Playlist>();
	
	public void onServerInit(IServer server)
	{
		try
		{
			IVHost vhost = VHostSingleton.getInstance(server.getProperties().getPropertyStr("PublishToVHost", VHost.VHOST_DEFAULT));
			IApplication app = vhost.getApplication(server.getProperties().getPropertyStr("PublishToApplication", "live"));
			Boolean passThruMetaData = server.getProperties().getPropertyBoolean("PassthruMetaData", true);
			
			String storageDir = app.getAppInstance("_definst_").getStreamStorageDir();
 
			String smilLoc = storageDir + "/streamschedule.smil";
			File playlistxml = new File(smilLoc);
				
			if (playlistxml.exists() == false){
				log.info("Could not find playlist file: " + smilLoc);
				return; 
			}
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse("file:///" + smilLoc);
			document.getDocumentElement().normalize();
			
			NodeList streams = document.getElementsByTagName("stream");
			for (int i = 0; i < streams.getLength(); i++)
			{
				Node streamItem = streams.item(i);
				if (streamItem.getNodeType() == Node.ELEMENT_NODE)
				{ 
					Element e = (Element) streamItem;
					String streamName = e.getAttribute("name");
					Stream stream = Stream.createInstance(vhost, app.getName(), streamName);
					streamMap.put(streamName, stream);
					app.getAppInstance("_definst_").getProperties().setProperty(streamName, stream);
				}
			}
			
			NodeList playList = document.getElementsByTagName("playlist");
			if (playList.getLength() == 0){
				log.info("No playlists defined in smil file");
				return;
			} 
			for (int i = 0; i < playList.getLength(); i++)
			{
				Node scheduledPlayList = playList.item(i);
				
				if (scheduledPlayList.getNodeType() == Node.ELEMENT_NODE)
				{
					Element e = (Element) scheduledPlayList;	
					
					NodeList videos = e.getElementsByTagName("video");
					if (videos.getLength() == 0){
				 		log.info("No videos defined in stream");
						return;
					}
					
					String streamName = e.getAttribute("playOnStream");
					if (streamName.length()==0)
						continue;
					
					Playlist playlist = new Playlist(streamName);
					playlist.setRepeat((e.getAttribute("repeat").equals("false"))?false:true);
					
					playlistMap.put(e.getAttribute("name"), playlist);
					
					for (int j = 0; j < videos.getLength(); j++)
					{
						Node video = videos.item(j);				
						if (video.getNodeType() == Node.ELEMENT_NODE)
						{
							Element e2 = (Element) video;
							String src = e2.getAttribute("src");
							Integer start = Integer.parseInt(e2.getAttribute("start"));
							Integer length = Integer.parseInt(e2.getAttribute("length"));
							playlist.addItem(src, start, length);
						}
					}
					String scheduled = e.getAttribute("scheduled");
					SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date startTime = parser.parse(scheduled);
					Stream stream = streamMap.get(streamName);
					stream.setSendOnMetadata(passThruMetaData);
					ScheduledItem item = new ScheduledItem(startTime, playlist, stream);
					item.start();
					IStreamActionNotify actionNotify  = new StreamListener(app.getAppInstance("_definst_"));
					stream.addListener(actionNotify);
					log.info("Scheduled: " + stream.getName() + " for: " + scheduled);
				}	
			}
		}
		catch(Exception ex)
		{
			log.info(ex.getMessage());
		}
	}
	
private class ScheduledItem {
		public Timer mTimer;
		public TimerTask mTask;
		public Date mStart;
		public Playlist mPL;
		public Stream mStream;
		public ScheduledItem(Date d, Playlist pl, Stream s){
			mStart = d;
			mPL = pl;
			mStream = s;
			mTask = new TimerTask(){
				public void run() {
					//synchronized(mStream.getLock())
					//{
						mPL.open(mStream);
					//}
					log.info("Scheduled stream is now live: " + mStream.getName());
				}
			};
			mTimer = new Timer();
		}
		
		public void start(){ 
			
			if (mTimer==null)
				mTimer = new Timer();
			mTimer.schedule(mTask, mStart);
			log.info("scheduled playlist: "+mPL.getName()+
						" on stream: "+mStream.getName()+
						" for:"+mStart.toString());
		}
		
		public void stop(){
			if (mTimer != null){
				mTimer.cancel();
				mTimer=null;
				log.info("cancelled playlist: "+mPL.getName()+
						" on stream: "+mStream.getName()+
						" for:"+mStart.toString());
			}
		}
	}
	public void onServerCreate(IServer server)
	{
	}
	public void onServerShutdownComplete(IServer server)
	{
		log.info("Shutdown server start");
		for (Map.Entry<String, Stream> entry : streamMap.entrySet())
		{
			try
			{
			Stream stream = entry.getValue();
			stream.close();
			stream = null;
			log.info("Closed Stream: " + entry.getKey());
			}
			catch(Exception ex)
			{
				log.error(ex.getMessage());
			}
		}
		for (Map.Entry<String, Playlist> entry : playlistMap.entrySet())
		{
			try
			{
			Playlist pl = entry.getValue();
			pl = null;
			}
			catch(Exception ex)
			{
				log.error(ex.getMessage());
			}
		}
	}

	public void onServerShutdownStart(IServer server)
	{
		
	}
	
	class StreamListener implements IStreamActionNotify
	{
		StreamListener(IApplicationInstance appInstance)
		{
		}	
		public void onPlaylistItemStop(Stream stream, PlaylistItem item)
		{
		}
		public void onPlaylistItemStart(Stream stream, PlaylistItem item) 
		{
			IMediaStream theStream = stream.getPublisher().getStream();
			AMFDataObj metaData = new AMFDataObj();
			metaData.put("duration", "");
			//theStream.send("onMetaData", metaData);
		}
	}
}












