package com.wowza.wms.plugin.collection.module;

import java.io.*;
import java.util.*;

import com.wowza.util.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.authentication.*;
import com.wowza.wms.authentication.file.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.util.*;
import com.wowza.wms.vhost.*;

public class ModuleOnConnectAuthenticate2 extends ModuleBase
{ 
	public static final String AUTHPASSWORDFILEPATH = "${com.wowza.wms.context.VHostConfigHome}/conf/connect.password";
	private File passwordFile = null;
	private String usernamePasswordProviderClass = null;

	public void onAppStart(IApplicationInstance appInstance)
	{
		WMSProperties props = appInstance.getProperties();
				
		String passwordFileStr = props.getPropertyStr("rtmpAuthenticateFile", AUTHPASSWORDFILEPATH);
		this.usernamePasswordProviderClass = props.getPropertyStr("usernamePasswordProviderClass", this.usernamePasswordProviderClass);
		if (passwordFileStr != null)
		{
			Map<String, String> envMap = new HashMap<String, String>();
			
			IVHost vhost = appInstance.getVHost();
			envMap.put("com.wowza.wms.context.VHost", vhost.getName());
			envMap.put("com.wowza.wms.context.VHostConfigHome", vhost.getHomePath());
			envMap.put("com.wowza.wms.context.Application", appInstance.getApplication().getName());
			envMap.put("com.wowza.wms.context.ApplicationInstance", appInstance.getName());

			passwordFileStr = SystemUtils.expandEnvironmentVariables(passwordFileStr, envMap);
			passwordFile = new File(passwordFileStr);
		}
		
		if (passwordFile != null)
			getLogger().info("ModuleOnConnectAuthenticate: Authorization password file: "+passwordFile.getAbsolutePath());
		if (usernamePasswordProviderClass != null)
			getLogger().info("ModuleOnConnectAuthenticate: Authorization password class: "+usernamePasswordProviderClass);
	}

public void onConnect(IClient client, RequestFunction function, AMFDataList params)
{
	boolean isAuthenticated = false;
	
	String username = null;
	String password = null;
	
	try
	{
		while(true)
		{
			getLogger().info("size: " + params.size());
			
			AMFDataMixedArray auth = getParamMixedArray(params, PARAM1);
			
			username = auth.getString(0);
			password = auth.getString(1);
				
				if (username == null || password == null)
					break;
								
				IAuthenticateUsernamePasswordProvider filePtr = null;
				if (usernamePasswordProviderClass != null)
					filePtr = AuthenticationUtils.createUsernamePasswordProvider(usernamePasswordProviderClass);
				else if (passwordFile != null)
					filePtr = AuthenticationPasswordFiles.getInstance().getPasswordFile(passwordFile);
				
				if (filePtr == null)
					break;

				filePtr.setClient(client);

				String userPassword = filePtr.getPassword(username);
				if (userPassword == null)
					break;

				if (!userPassword.equals(password))
					break;
				
				isAuthenticated = true;
				break;
			}
		}
		catch(Exception e)
		{
			getLogger().error("ModuleOnConnectAuthenticate.onConnect: "+e.toString());
			isAuthenticated = false;
		}
		
		if (!isAuthenticated)
			client.rejectConnection("Authentication Failed["+client.getClientId()+"]: "+username);
		else
			client.acceptConnection();
	}
}
