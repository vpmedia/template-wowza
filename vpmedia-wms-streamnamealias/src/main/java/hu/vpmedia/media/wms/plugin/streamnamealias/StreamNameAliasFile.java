package hu.vpmedia.media.wms.plugin.streamnamealias;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.wowza.wms.logging.*;

public class StreamNameAliasFile
{
	class StreamAlias
	{
		String pattern = null;
		String alias = null;
		
		public StreamAlias(String pattern, String alias)
		{
			this.pattern = pattern;
			this.alias = alias;
		}
		
		public String toString()
		{
			return "{pattern: \""+pattern+"\" alias: \""+alias+"\"}";
		}
	}

	private long lastModDate = -1;
	private long lastSize = -1;
	private File file = null;
	private List<StreamAlias> aliasVals = new ArrayList<StreamAlias>();
	
	
	public StreamNameAliasFile(File file)
	{
		this.file = file;
		load();
	}
	
	private void load()
	{
		WMSLoggerFactory.getLogger(StreamNameAliasFile.class).debug("StreamNameAliasFile.load: "+file.getAbsolutePath());
		aliasVals.clear();
		
		String delimter = StreamNameAliasFiles.getNameDelimeter();
		
		try
		{
			BufferedReader inf = new BufferedReader(new FileReader(file));
			String line;
			while ((line = inf.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#"))
					continue;
				if (line.length() == 0)
					continue;
				
				String pattern = null;
				String alias = null;
				int pos = line.indexOf(delimter);
				if (pos >= 0)
				{
					pattern = line.substring(0, pos).trim();
					alias = line.substring(pos+1).trim();
				}
				if (pattern != null && alias != null)
					aliasVals.add(new StreamAlias(pattern, alias));
			}
			inf.close();
		}
		catch (Exception e)
		{
			WMSLoggerFactory.getLogger(StreamNameAliasFile.class).error("StreamNameAliasFile.load: "+e.toString());
		}
		
		lastModDate = file.lastModified();
		lastSize = file.length();
	}
	
	private void checkReload()
	{
		if (lastModDate != file.lastModified() || lastSize != file.length())
			load();
	}
		
	public synchronized StreamNameAliasMatch findMap(String streamName)
	{
		checkReload();
		
		StreamNameAliasMatch ret = null;
		Iterator<StreamAlias> iter = aliasVals.iterator();
		List<String> wildcardMatches = null;
		
		while(iter.hasNext())
		{
			StreamAlias streamAlias = iter.next();
			String pattern = streamAlias.pattern;
			boolean isMatch = false;
						
			if (pattern.indexOf("*") >= 0)
			{
				String regPattern = pattern;
				regPattern = regPattern.replace(".", "\\.");
				regPattern = regPattern.replace("*", "(.*)");
				regPattern = "^"+regPattern+"$";
				Pattern p = Pattern.compile(regPattern);
				Matcher m = p.matcher(streamName);
				if (m.find())
				{
					isMatch = (m.start() == 0 && m.end() == streamName.length());
					if (isMatch && m.groupCount() > 0)
					{
						wildcardMatches = new ArrayList<String>();
						for(int i=0;i<m.groupCount();i++)
							wildcardMatches.add(m.group(i+1));
					}
				}
			}
			else
				isMatch = pattern.equals(streamName);
			
			if (isMatch)
			{
				ret = new StreamNameAliasMatch();
				ret.pattern = streamAlias.pattern;
				ret.alias = streamAlias.alias;
				ret.wildcardMatches = wildcardMatches;
				break;
			}
		}
		
		return ret;
	}
}
