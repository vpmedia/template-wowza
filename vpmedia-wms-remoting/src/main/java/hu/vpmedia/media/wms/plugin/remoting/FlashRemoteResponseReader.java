package hu.vpmedia.media.wms.plugin.remoting;

import java.nio.ByteBuffer;

import com.wowza.util.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.logging.*;

public class FlashRemoteResponseReader
{
	byte[] buffer = null;
	int offset = 0;
	int len = 0;
	
	public FlashRemoteResponseReader(byte[] buffer, int len)
	{
		this.buffer = buffer;
		this.len = len;
	}
	
	public int remaining()
	{
		return this.len-this.offset;
	}
	
	public void inc(int inc)
	{
		this.offset += inc;
	}
	
	public void skip(int inc)
	{
		inc(inc);
	}
	
	public int position()
	{
		return this.offset;
	}
	
	public void position(int offset)
	{
		this.offset = offset;
	}
	
	public AMFData getAMF(long len)
	{
		AMFData ret = null;
		try
		{
			//System.out.println("getAMF: "+this.buffer+":"+this.offset+":"+len);
			
			if (len >= 0x0ffffffffL)
			{
				len = this.remaining();
				ByteBuffer dataBuffer = ByteBuffer.wrap(this.buffer, this.offset, (int)len);
				long startPos = dataBuffer.position();
				ret = AMFData.deserializeInnerObject(dataBuffer, AMFData.createContextDeserialize());
				len = dataBuffer.position()-startPos;
			}
			else
			{
				ByteBuffer dataBuffer = ByteBuffer.wrap(this.buffer, this.offset, (int)len);
				ret = AMFData.deserializeInnerObject(dataBuffer, AMFData.createContextDeserialize());
			}
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(null).error("HTTPUtils.getString: "+e.toString());
			e.printStackTrace();
		}
		this.inc((int)len);

		return ret;
	}
	
	public int getByte()
	{
		int ret = 0;
		try
		{
			ret = BufferUtils.byteArrayToInt(this.buffer, this.offset, 1);
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(null).error("HTTPUtils.getString: "+e.toString());
			e.printStackTrace();
		}
		this.inc(1);

		return ret;
	}
	
	public int getShort()
	{
		int ret = 0;
		try
		{
			ret = BufferUtils.byteArrayToInt(this.buffer, this.offset, 2);
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(null).error("HTTPUtils.getString: "+e.toString());
			e.printStackTrace();
		}
		this.inc(2);

		return ret;
	}

	public long getInt()
	{
		long ret = 0;
		try
		{
			ret = BufferUtils.byteArrayToLong(this.buffer, this.offset, 4);
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(null).error("HTTPUtils.getString: "+e.toString());
			e.printStackTrace();
		}
		this.inc(4);

		return ret;
	}
	
	public String getString()
	{
		String ret = null;
		
		int len = 0;
		try
		{
			len = BufferUtils.byteArrayToInt(this.buffer, this.offset, 2);
			this.inc(2);
			
			if (len > 0)
				ret = BufferUtils.byteArrayToString(this.buffer, this.offset, len);
			else
				ret = "";
		}
		catch(Exception e)
		{
			WMSLoggerFactory.getLogger(null).error("HTTPUtils.getString: "+e.toString());
			e.printStackTrace();
		}
		this.inc(len);
		
		return ret;
	}

}
