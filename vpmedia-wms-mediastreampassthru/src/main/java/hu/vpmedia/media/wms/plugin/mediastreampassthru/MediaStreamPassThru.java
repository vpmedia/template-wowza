package hu.vpmedia.media.wms.plugin.mediastreampassthru;

import java.nio.*;

import com.wowza.wms.application.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.live.*;
import com.wowza.wms.logging.*;

public class MediaStreamPassThru extends MediaStreamLive
{
	private int audioBytes = 0;
	private int videoBytes = 0;
	private int dataBytes = 0;
	private ByteBuffer audioBuffer = null;
	private ByteBuffer dataBuffer = null;
	private ByteBuffer videoBuffer = null;

    public MediaStreamPassThru()
	{
    	super();
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: create");
	}

	public void init(MediaStreamMap parent, int src, WMSProperties properties)
	{
		super.init(parent, src, properties);
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: init");
	}
	
	public void startPublishing()
	{
    	super.startPublishing();
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: startPublishing");
	}

	public void stopPublishing()
	{
    	super.stopPublishing();
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: stopPublishing");
	}

	public void publish()
	{
    	super.publish();
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: publish");
	}
			
	public void trim()
	{
    	super.trim();
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: trim");
	}
	
	public int getAudioMissing()
	{
 		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: getAudioMissing: "+(this.getAudioSize()-this.audioBytes));
		return this.getAudioSize()-this.audioBytes;
	}
	
	public int getVideoMissing()
	{
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: getVideoMissing: "+(this.getVideoSize()-this.videoBytes));
		return this.getVideoSize()-this.videoBytes;
	}

	public int getDataMissing()
	{
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: getDataMissing: "+(this.getDataSize()-this.dataBytes));
		return this.getDataSize()-this.dataBytes;
	}

	public void addVideoData(byte[] data, int offset, int size)
	{
    	super.addVideoData(data, offset, size);
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: addVideoData: offset:"+offset+" size:"+size);
		if (this.videoBuffer == null)
			this.videoBuffer = ByteBuffer.allocate(this.getVideoSize());
		
		this.videoBytes += size;
		this.videoBuffer.put(data, offset, size);
		if (this.videoBytes == this.getVideoSize())
		{
			this.videoBytes = 0;

			// this packets data is complete process
			WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: videoPacketComplete: timecode:"+this.getVideoTC()+" size:"+this.getVideoSize());
			
			this.videoBuffer = null;
		}
	}
	
	public void addDataData(byte[] data, int offset, int size)
	{
    	super.addDataData(data, offset, size);
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: addDataData: offset:"+offset+" size:"+size);
		if (this.dataBuffer == null)
			this.dataBuffer = ByteBuffer.allocate(this.getDataSize());

		this.dataBytes += size;
		this.dataBuffer.put(data, offset, size);
		if (this.dataBytes == this.getDataSize())
		{
			this.dataBytes = 0;
			
			// this packets data is complete process
			WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: dataPacketComplete: timecode:"+this.getDataTC()+" size:"+this.getDataSize());

			this.dataBuffer = null;
		}
	}
	
	public void addAudioData(byte[] data, int offset, int size)
	{
    	super.addAudioData(data, offset, size);
		WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: addAudioData: offset:"+offset+" size:"+size);
		if (this.audioBuffer == null)
			this.audioBuffer = ByteBuffer.allocate(this.getAudioSize());

		this.audioBytes += size;
		this.audioBuffer.put(data, offset, size);
		if (this.audioBytes == this.getAudioSize())
		{
			this.audioBytes = 0;
			
			// this packets data is complete process
			WMSLoggerFactory.getLogger(null).info("MediaStreamPassThru: audioPacketComplete: timecode:"+this.getAudioTC()+" size:"+this.getAudioSize());

			this.audioBuffer = null;
		}
	}
}
