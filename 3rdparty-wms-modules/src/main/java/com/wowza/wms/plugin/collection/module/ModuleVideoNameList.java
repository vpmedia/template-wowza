package com.wowza.wms.plugin.collection.module;

import com.wowza.wms.amf.*;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import java.io.File;
import com.wowza.util.FileUtils;
import com.wowza.util.IFileProcess;

public class ModuleVideoNameList extends ModuleBase {

	AMFDataMixedArray recordedMovies = new AMFDataMixedArray();
	String storageDir;
	
	class PutFile implements IFileProcess
	{
		public void onFile(File file)
		{
			String sizeSuffix="";
			String s = storageDir;
			if (file.length()<1024000)
			{
				sizeSuffix = "["+ Math.round((file.length() * .001)) + " kb]";
			}
			else
			{
				sizeSuffix = "["+ Math.round((file.length() * .000001)) + " MB]";
			}
			
			String fileName = file.getName();
			
			String _abpath = file.getAbsolutePath().replace("\\", "/");
			String _parent = file.getParent().replace("\\", "/");
			String _path = file.getPath().replace("\\", "/");
			
			
			
			fileName = fileName.replace(storageDir, "");
			String fn = file.getName().toLowerCase();
			if (fn.indexOf(".m4v")>-1 || fn.indexOf(".mov")>-1 || fn.indexOf(".mp4")>-1 || fn.indexOf(".f4v")>-1)
			{
				fileName="mp4:" + fileName;
			}
			
			if (file.length() > 0 && fileName.indexOf(".")>-1)
			{
				recordedMovies.put(fileName, new AMFDataItem(fileName.replace(".flv", "") + " " + sizeSuffix));
				getLogger().info("fileName: " + fileName);
			}
		}
	}
	
	public void getVideoNames(IClient client, RequestFunction function,
			AMFDataList params) {
		getLogger().info("getFiles");
		storageDir = client.getAppInstance().getStreamStoragePath();
		recordedMovies = new AMFDataMixedArray();
		
		IApplicationInstance app = client.getAppInstance();
		
		PutFile putfile = new PutFile();
		FileUtils.traverseDirectory(new File(app.getStreamStoragePath().replace("_definst_", app.getName())), putfile);
		sendResult(client, params, recordedMovies);
	}
}

