package com.wowza.wms.plugin.collection.module;
import java.io.*;

import com.wowza.util.*;
import com.wowza.wms.module.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.IClient;
import com.wowza.wms.stream.*;
import com.wowza.wms.vhost.*;
import com.wowza.wms.request.*;

public class ModuleCreateSnapshot extends ModuleBase
{
	Object lock = new Object(); 
	 
	public void createSnapshotLive(IClient client, RequestFunction function, AMFDataList params) 
	{
		String streamName = params.getString(PARAM1);
		 
		String fileName = "";
		IApplicationInstance appInstance = client.getAppInstance();
		MediaStreamMap streams = appInstance.getStreams();
		IMediaStream stream = streams.getStream(streamName);
		if (stream != null)
		{
			AMFPacket packet = stream.getLastKeyFrame();
			if (packet != null)
			{
				fileName = streamName + "_" + packet.getAbsTimecode() + ".flv";
				File newFile = stream.getStreamFileForWrite(streamName, null, null);
				
				String filePath = newFile.getPath().substring(0, newFile.getPath().length()-4) + "_" + packet.getAbsTimecode() + ".flv";
								
				try
				{
					synchronized(lock)
					{
						BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(filePath), false));
						FLVUtils.writeHeader(out, 0, null);
						
						AMFPacket codecConfig = stream.getVideoCodecConfigPacket(packet.getAbsTimecode());
						if (codecConfig != null)
							FLVUtils.writeChunk(out, codecConfig.getDataBuffer(), codecConfig.getSize(), 0, (byte)codecConfig.getType());
						
						FLVUtils.writeChunk(out, packet.getDataBuffer(), packet.getSize(), 0, (byte)packet.getType());
						out.close();
					}
					
					getLogger().info("snapshot created: "+filePath);
				}
				catch (Exception e)
				{
					getLogger().error("createSnapshot: "+e.toString());
				}
			}
		}
		
		sendResult(client, params, fileName);
	}
	
	public void createSnapshotVOD(IClient client, RequestFunction function, AMFDataList params) 
	{
		String streamName = params.getString(PARAM1);
		int timecode = params.getInt(PARAM2);
		
		String fileName = "";
		IApplicationInstance appInstance = client.getAppInstance();
		
		String flvFilePath = appInstance.getStreamStoragePath() + "/" + streamName + ".flv";
		File flvFile = new File(flvFilePath);
		
		if (flvFile.exists())
		{
			AMFPacket lastVideoKeyFrame = null;
			try
			{
				BufferedInputStream is = new BufferedInputStream(new FileInputStream(flvFile));
				FLVUtils.readHeader(is);
				AMFPacket amfPacket;
				while ((amfPacket = FLVUtils.readChunk(is)) != null)
				{
					if (lastVideoKeyFrame != null && amfPacket.getTimecode() > timecode)
						break;
					if (amfPacket.getType() != IVHost.CONTENTTYPE_VIDEO)
						continue;
					if (FLVUtils.isVideoKeyFrame(amfPacket)) //if (FLVUtils.getFrameType(amfPacket.getFirstByte()) == FLVUtils.FLV_KFRAME)
						lastVideoKeyFrame = amfPacket;
				}
				is.close();
			}
			catch (Exception e)
			{
				getLogger().error("Error: createSnapshotVOD: reading flv: "+e.toString());
			}
			
			if (lastVideoKeyFrame != null)
			{
				try
				{
					fileName = streamName + "_" + timecode;
					String filePath = appInstance.getStreamStoragePath() + "/" + fileName + ".flv";
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(filePath), false));
					FLVUtils.writeHeader(out, 0, null);
					FLVUtils.writeChunk(out, lastVideoKeyFrame.getDataBuffer(), lastVideoKeyFrame.getSize(), 0, (byte)lastVideoKeyFrame.getType());
					out.close();
					
					getLogger().info("snapshot created: "+filePath);
				}
				catch (Exception e)
				{
					getLogger().error("Error: createSnapshotVOD: writing flv: "+e.toString());
				}
			}
		}
		
		sendResult(client, params, fileName);
	}
}