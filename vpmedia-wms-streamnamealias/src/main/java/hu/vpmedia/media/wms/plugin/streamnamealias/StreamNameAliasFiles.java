package hu.vpmedia.media.wms.plugin.streamnamealias;

import java.util.*;
import java.io.*;

public class StreamNameAliasFiles
{
	static private StreamNameAliasFiles instance = null;
	static private Object lock = new Object();
	public static String nameDelimiter = "=";
	
	private Map<String, StreamNameAliasFile> aliasFiles = new HashMap<String, StreamNameAliasFile>();
	
	public static void setNameDelimeter(String nameDelimiter)
	{
		StreamNameAliasFiles.nameDelimiter = nameDelimiter;
	}
	
	public static String getNameDelimeter()
	{
		return StreamNameAliasFiles.nameDelimiter;
	}
	
	public static StreamNameAliasFiles getInstance()
	{
		synchronized(StreamNameAliasFiles.lock)
		{
			if (StreamNameAliasFiles.instance == null)
				StreamNameAliasFiles.instance = new StreamNameAliasFiles();
			return StreamNameAliasFiles.instance;
		}
	}
	
	public StreamNameAliasFile getMapFile(File file)
	{
		StreamNameAliasFile ret = null;
		
		synchronized(StreamNameAliasFiles.lock)
		{
			if (file.exists())
			{
				String fileKey = file.getAbsolutePath();
				ret = aliasFiles.get(fileKey);
				if (ret == null)
				{
					ret = new StreamNameAliasFile(file);
					aliasFiles.put(fileKey, ret);
				}
			}
		}
		
		return ret;
	}
}
