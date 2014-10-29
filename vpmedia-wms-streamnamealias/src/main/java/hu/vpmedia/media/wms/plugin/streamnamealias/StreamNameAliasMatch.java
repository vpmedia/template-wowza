package hu.vpmedia.media.wms.plugin.streamnamealias;

import java.util.*;

public class StreamNameAliasMatch
{
	String pattern = null;
	String alias = null;
	List<String> wildcardMatches = null;
	
	public String toString()
	{
		String ret = "{pattern: \""+pattern+"\" alias:\""+alias+"\" wildcardMatches:";
		if (wildcardMatches == null)
			ret += "null";
		else
		{
			ret += "{";
			int index = 0;
			Iterator<String> iter = wildcardMatches.iterator();
			while(iter.hasNext())
			{
				String value = iter.next();
				if (index > 0)
					ret += ", ";
				ret += "["+index+"]: \""+value+"\"";
				index++;
			}
			ret += "}";
		}
			
		ret += "}";
		return ret;
	}
}
