package hu.vpmedia.media.wms.plugin.remoting;

import com.wowza.wms.amf.AMFData;

public class FlashRemoteMessage
{
	String handlerName = null;
	int responseId = -1;
	AMFData[] amfParams = null;
	IFlashRemoteResponse response = null;
}
