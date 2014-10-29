package hu.vpmedia.media.wms.plugin.keepalive;

import java.util.*;

import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.vhost.*;

public class KeepAlive extends ModuleBase {

	static public void onAppStart(IApplicationInstance appInstance) {
		WMSProperties prop = appInstance.getProperties();
		prop.setProperty("done", false);
		appTimer(appInstance);
	}
	
	static public void onAppStop(IApplicationInstance appInstance) {
		WMSProperties prop = appInstance.getProperties();
		prop.setProperty("done", true);
	}
	
	static public void onDisconnect(IClient client) {
		WMSProperties prop = client.getAppInstance().getProperties();
		Object clientId = client.getClientId();
		if(prop.containsKey(clientId)) {
			prop.remove(clientId);
		}
	}
	
	static private void appTimer(final IApplicationInstance appInstance) {
		class MyTimer implements Runnable
		{
			private IApplicationInstance appInstance;
			private WMSProperties prop;
			public MyTimer(IApplicationInstance appInstance)
			{
				this.appInstance = appInstance;
				this.prop = this.appInstance.getProperties();
			}

			public void run()
			{
				while (true)
				{
					//		 do action here and set done to true to exit
					if (this.prop.getPropertyBoolean("done", false)) {
						break;
					} else {
						pollClients(appInstance);
					}
					try
					{
						//		 interval in milliseconds
						Thread.currentThread().sleep(5000);
					}
					catch (Exception e)
					{
					}
				}
			}
		}
		appInstance.getVHost().getThreadPool().execute(new MyTimer(appInstance));
	}
	
	static private void pollClients(IApplicationInstance appInstance) {
		int count = appInstance.getClientCount();
		WMSProperties prop = appInstance.getProperties();
		Map clientMap = new HashMap();
		class ResultObj implements IModuleCallResult {
			private IClient client = null;
			private WMSProperties prop;
			public ResultObj(IClient p_client){
				this.client = p_client;
				this.prop = this.client.getAppInstance().getProperties();
			}
			
			public void onResult(IClient client, RequestFunction function, AMFDataList params) {
				Object clientId = client.getClientId();
				if(this.prop.containsKey(clientId)) {
					this.prop.remove(clientId);
				}
			}
		}

		for (int i = 0; i < count; i++) {
			IClient client = appInstance.getClient(i);
			int clientId = client.getClientId();
			Long now = new Long(System.currentTimeMillis());
			if(!prop.containsKey(clientId)) {
				prop.put(clientId, now);
				ResultObj resultObj = new ResultObj(client);
				client.call("isAlive", resultObj, "");
			}
		}
		
		clientMap.putAll(prop);
		int mapSize = clientMap.size();
		Long old = new Long(System.currentTimeMillis() - 15000);
		Iterator keyValuePairs = clientMap.entrySet().iterator();
		for (int j = 0; j < mapSize; j++)
		{
			Map.Entry entry = (Map.Entry) keyValuePairs.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			try {
				if(Long.parseLong(value.toString()) < old) {
					for(int k = 0; k < appInstance.getClientCount(); k++) {
						IClient client = appInstance.getClient(k);
						try {
							if(client.getClientId() == Long.parseLong(key.toString())) {
								getLogger().info("Non-responsive client: "+client.getClientId()+ " ShutDown");
								//appInstance.getVHost().killClient(client.getClientId());
								if(Thread.currentThread() instanceof VHostWorkerThread)
                                ((VHostWorkerThread)Thread.currentThread()).setClient((Client)client);
                                appInstance.getVHost().killClient(client.getClientId());
                                if(Thread.currentThread() instanceof VHostWorkerThread)
                                ((VHostWorkerThread)Thread.currentThread()).setClient(null);								
							}
						}
						catch(Exception e){
						}
					}
				}
			}
			catch (Exception e){
			}
		}
		clientMap.clear();
	}
}