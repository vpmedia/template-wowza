package hu.vpmedia.media.wms.plugin.mediareader;

import java.util.*;
import java.nio.*;

import com.wowza.wms.mediareader.flv.*;
import com.wowza.wms.amf.*;

public class MediaReaderFLVInjectMetadata extends MediaReaderFLV
{
	public List getMetadata()
	{
		List ret = super.getMetadata();
		
		while(true)
		{
			if (ret == null)
				break;
			if (ret.size() <= 0)
				break;
			
			ByteBuffer packet = (ByteBuffer)ret.get(0);
			
			AMFDataList myMetadata = new AMFDataList(packet);

			AMFDataMixedArray dataObj = (AMFDataMixedArray)myMetadata.get(1);
			dataObj.put("wowzafield", new AMFDataItem("wowzadata"));
			
			byte[] data = myMetadata.serialize();
			ByteBuffer newPacket = ByteBuffer.wrap(data);			
			ret.set(0, newPacket);
			break;
		}

		return ret;
	}

}