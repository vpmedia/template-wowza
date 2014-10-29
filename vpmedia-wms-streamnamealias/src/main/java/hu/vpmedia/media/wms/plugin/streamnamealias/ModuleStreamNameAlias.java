package hu.vpmedia.media.wms.plugin.streamnamealias;

import java.util.*;
import java.io.*;

import com.wowza.util.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.stream.*;

public class ModuleStreamNameAlias extends ModuleBase
{	
	public static final String STREAMALIASFILE_PROPERTY = "streamNameAliasFile";
	public static final String STREAMALIASDEBUG_PROPERTY = "streamNameAliasDebug";
	public static final String STREAMALIASPATHDELIMITER_PROPERTY = "streamNameAliasPathDelimiter";
	public static final String STREAMALIASNAMEDELIMITER_PROPERTY = "streamNameAliasNameDelimiter";
	
	private Map<String, String> envMap = new HashMap<String, String>();
	private File aliasFilePtr = null;
	private boolean debug = false;
	private String pathDelimiter = "/";
	private String nameDelimiter = "=";

	public void onAppStart(IApplicationInstance appInstance)
	{
		WMSProperties props = appInstance.getProperties();
		debug = props.getPropertyBoolean(STREAMALIASDEBUG_PROPERTY, debug);
		pathDelimiter = props.getPropertyStr(STREAMALIASPATHDELIMITER_PROPERTY, pathDelimiter);
		nameDelimiter = props.getPropertyStr(STREAMALIASNAMEDELIMITER_PROPERTY, nameDelimiter);
		
		StreamNameAliasFiles.setNameDelimeter(nameDelimiter);

		while(true)
		{
			String aliasPath = props.getPropertyStr(STREAMALIASFILE_PROPERTY);
			if (aliasPath == null)
			{
				getLogger().warn("ModuleStreamNameAlias.onAppStart: Property "+STREAMALIASFILE_PROPERTY+" is missing. Can't find alias file.");
				break;
			}
			
			Map envMap = new HashMap();
			
			envMap.put("com.wowza.wms.context.VHost", appInstance.getVHost().getName());
			envMap.put("com.wowza.wms.context.VHostConfigHome", appInstance.getVHost().getHomePath());
			envMap.put("com.wowza.wms.context.Application", appInstance.getApplication().getName());
			envMap.put("com.wowza.wms.context.ApplicationInstance", appInstance.getName());
			
			aliasPath =  SystemUtils.expandEnvironmentVariables(aliasPath, envMap);
			this.aliasFilePtr = new File(aliasPath);
			
			if (debug)
				getLogger().info("ModuleStreamNameAlias.onAppStart: "+STREAMALIASFILE_PROPERTY+": "+aliasFilePtr.getAbsolutePath());
			break;
		}
		
		envMap.put("VHost.Name", appInstance.getVHost().getName());
		envMap.put("Application.Name", appInstance.getApplication().getName());
		envMap.put("ApplicationInstance.Name", appInstance.getName());
		envMap.put("AppInstance.Name", appInstance.getName());
	}
	
	public void onAppStop(IApplicationInstance appInstance)
	{
		getLogger().info("ModuleStreamNameAlias.onAppStop: "+appInstance.getApplication().getName()+"/"+appInstance.getName());
	}
	
	private String streamNameToAlias(String streamName)
	{
		String ret = null;

		while(true)
		{
			if (aliasFilePtr == null)
			{
				if (debug)
					getLogger().info("ModuleStreamNameAlias.streamNameToAlias: aliasFilePtr is null");
				break;
			}
			
			StreamNameAliasFiles aliasFiles = StreamNameAliasFiles.getInstance();
			if (aliasFiles == null)
			{
				if (debug)
					getLogger().info("ModuleStreamNameAlias.streamNameToAlias: aliasFiles is null");
				break;
			}
			
			StreamNameAliasFile aliasFile = aliasFiles.getMapFile(aliasFilePtr);
			if (aliasFile == null)
			{
				if (debug)
					getLogger().info("ModuleStreamNameAlias.streamNameToAlias: aliasFile missing: "+aliasFilePtr.getAbsolutePath());
				break;
			}
			
			StreamNameAliasMatch aliasMatch = aliasFile.findMap(streamName);
			if (aliasMatch == null)
			{
				if (debug)
					getLogger().info("ModuleStreamNameAlias.streamNameToAlias: Can't find alias entry for: "+streamName);
				break;
			}
			
			Map<String, String> sendMap = new HashMap<String, String>();
			sendMap.putAll(envMap);
			
			sendMap.put("Stream.Name", streamName);
			if (streamName.indexOf(pathDelimiter) >= 0)
			{
				String[] parts = streamName.split("["+pathDelimiter+"]");
				for(int i=0;i<parts.length;i++)
					sendMap.put("Stream.Name.Part"+(i+1), parts[i]);
			}
			else
				sendMap.put("Stream.Name.Part1", streamName);
			
			if (aliasMatch.wildcardMatches != null)
			{
				int gindex = 0;
				Iterator<String> iter = aliasMatch.wildcardMatches.iterator();
				while(iter.hasNext())
				{
					String value = iter.next();
					sendMap.put("Wildcard.Match"+(gindex+1), value);
					gindex++;
				}
			}
			
			ret =  SystemUtils.expandEnvironmentVariables(aliasMatch.alias, sendMap);
			if (debug)
				getLogger().info("ModuleStreamNameAlias.streamNameToAlias: streamName:"+streamName+" alias:"+aliasMatch+" result:"+ret);
			break;
		}

		return ret;
	}
	
	public void play(IClient client, RequestFunction function, AMFDataList params)
	{
		boolean doNext = true;
		
		if (params.get(PARAM1).getType() == AMFData.DATA_TYPE_STRING)
		{
			String streamName = params.getString(PARAM1);
			String newName = streamNameToAlias(streamName);
			IMediaStream stream = getStream(client, function);
			
			if (newName == null)
			{
				String code = "NetStream.Play.Failed";
				String description = "ModuleStreamNameAlias: No match for stream name "+streamName+".";
				sendStreamOnStatusError(stream, code, description);
				doNext = false;
				if (debug)
					getLogger().warn("ModuleStreamNameAlias.play: No match: "+streamName);
			}
			else
			{
				if (debug)
					getLogger().info("ModuleStreamNameAlias.play: "+streamName+"="+newName);
				params.set(PARAM1, new AMFDataItem(newName));
			}
		}
		
		if (doNext)
			this.invokePrevious(client, function, params);
	}

}
