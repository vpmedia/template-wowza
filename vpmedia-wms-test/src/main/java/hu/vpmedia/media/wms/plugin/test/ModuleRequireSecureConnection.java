package hu.vpmedia.media.wms.plugin.test;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;

public class ModuleRequireSecureConnection extends ModuleBase
{
	public void onConnect(IClient client, RequestFunction function, AMFDataList params)
	{
		if (!client.isSecure())
		{
			client.rejectConnection("Secure connection required.");
			getLogger().info("ModuleRequireSecureConnection.onConnect: rejectConnection: clientId:"+client.getClientId());
		}
	}
}