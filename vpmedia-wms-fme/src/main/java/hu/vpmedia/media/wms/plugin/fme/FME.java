package hu.vpmedia.media.wms.plugin.fme;

import java.util.*;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.util.IOPerformanceCounter;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.logging.*;

public class FME extends ModuleBase 
{
        
    // Flash Media Encoder API
	
	public void FCPublish(IClient client, RequestFunction function, AMFDataList params)
	{
		getLogger().info("FCPublish");
	}
	
	public void FCUnpublish(IClient client, RequestFunction function, AMFDataList params)
	{
		getLogger().info("FCUnpublish");
	}

	public void releaseStream(IClient client, RequestFunction function, AMFDataList params)
	{
		getLogger().info("FCUnpublish");
	}
}