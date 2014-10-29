package hu.vpmedia.media.wms.plugin.connectionlimiter;

import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.request.*;
import com.wowza.wms.module.*;

public class ConnectionLimiter extends ModuleBase
{
	static final public int MAXCONNECTIONS = 200;
	
	private int maxApplicationConnections = MAXCONNECTIONS;
	
	public void onAppStart(IApplicationInstance appInstance)
	{
		this.maxApplicationConnections = appInstance.getProperties().getPropertyInt("maxApplicationConnections", maxApplicationConnections);
	}
	
	public void onConnect(IClient client, RequestFunction function, AMFDataList params)
	{
		IApplicationInstance appInstance = client.getAppInstance();
		IApplication app = appInstance.getApplication();
		long count = app.getConnectionCounter().getCurrent();
		if ((count+1) > this.maxApplicationConnections)
		{
			client.rejectConnection("Over application connection limit ["+app.getName()+"/"+appInstance.getName()+"]: Limit is: "+this.maxApplicationConnections);
		}
	}
}