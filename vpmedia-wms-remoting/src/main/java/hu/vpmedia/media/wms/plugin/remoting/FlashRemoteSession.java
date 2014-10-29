package hu.vpmedia.media.wms.plugin.remoting;

import java.io.*;
import java.net.*;
import java.util.*;

import com.wowza.util.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.vhost.*;
import com.wowza.wms.logging.*;

/**
 * 
 * <p>FlashRemoteSession: Main interface into Flash Remoting API. 
 * The remote calls can either be make synchronously (the default) or asynchronously (by call FlashRemoteSession.setAsynchronous(true)).
 * In synchronous mode FlashRemoteSession.flush() must be called to send the request to the remote server. In asynchronous mode requests
 * are sent immediately but responses are delivered asynchronously using the virtual host handler thread pool.</p>
 * 
 * <p>Here is an example of a synchronous session:</p>
 * 
<code><pre>
String url = "http://localhost/amfphp/gateway.php";
FlashRemoteSession remoteSession = FlashRemoteSession.createInstance(appInstance.getVHost(), url);

class MyResponse implements IFlashRemoteResponse
{
	public void onFailure(String handlerName, int statusCode, String responseMsg)
	{
		System.out.println("MyResponse.onFailure["+handlerName+"]: "+statusCode+":"+responseMsg);
	}

	public void onResult(String handlerName, AMFData res)
	{
		System.out.println("MyResponse.onResult["+handlerName+"]: "+(res==null?"null":res.toString()));
	}
}

IFlashRemoteResponse responseHandler = new MyResponse();

remoteSession.call("HelloWorld.say", responseHandler, "My First Hello World Message");
remoteSession.call("Counter.increment", responseHandler);
remoteSession.call("DataGrid.getDataSet", responseHandler);
remoteSession.flush();

remoteSession.call("HelloWorld.say", responseHandler, "My Second Hello World Message");
remoteSession.call("Counter.increment", responseHandler);
remoteSession.flush();

remoteSession.close();
</pre></code>
 * <p>An asynchronous session looks like this:</p>
 * 
<code><pre>
String url = "http://localhost/amfphp/gateway.php";
FlashRemoteSession remoteSession = FlashRemoteSession.createInstance(appInstance.getVHost(), url);

class MyResponse implements IFlashRemoteResponse
{
	public void onFailure(String handlerName, int statusCode, String responseMsg)
	{
		System.out.println("MyResponse.onFailure["+handlerName+"]: "+statusCode+":"+responseMsg);
	}

	public void onResult(String handlerName, AMFData res)
	{
		System.out.println("MyResponse.onResult["+handlerName+"]: "+(res==null?"null":res.toString()));
	}
}

IFlashRemoteResponse responseHandler = new MyResponse();

remoteSession.call("HelloWorld.say", responseHandler, "My First Hello World Message");
remoteSession.call("Counter.increment", responseHandler);
remoteSession.call("DataGrid.getDataSet", responseHandler);
remoteSession.call("HelloWorld.say", responseHandler, "My Second Hello World Message");
remoteSession.call("Counter.increment", responseHandler);
</pre></code>
 * 
 * @author Wowza Media Systems
 *
 */

public class FlashRemoteSession implements Runnable
{
	public final static String APPENDTOURL = "AppendToGatewayUrl";
	public final static int CRLFTEST = 0x0d0a;
	
	private IVHost vhost = null;
	private String url = null;
	private String queryStr = "";
	private Map<Integer, FlashRemoteMessage> messageMap = new HashMap<Integer, FlashRemoteMessage>();
	private List<FlashRemoteMessage> messageList = new ArrayList<FlashRemoteMessage>();
	private boolean running = false;
	private Object lock = new Object();
	private int nextResponseId = 0;
	private boolean asynchronous = false;
	private int connectionTimeout = 4000;
	private Map<String, String> httpHeaderPairs = new HashMap<String, String>();
	private List<IFlashRemotingNotify> listeners = new ArrayList<IFlashRemotingNotify>();
	
	/**
	 * Creates a new Flash Remote session that uses the sources of vhost and connects to the server specified by url.
	 * @param vhost virutal host
	 * @param url remote url
	 * @return new FlashRemoteSession
	 */
	public static FlashRemoteSession createInstance(IVHost vhost, String url)
	{
		FlashRemoteSession ret = new FlashRemoteSession();
		ret.init(vhost, url);
		return ret;
	}
	
	private void init(IVHost vhost, String url)
	{
		this.vhost = vhost;
		
		String queryStr = "";
		int qloc = url.indexOf("?");
		if (qloc >= 0)
		{
			queryStr = url.substring(qloc+1);
			url = url.substring(0, qloc);
		}
		
		this.url = url;
		this.queryStr = queryStr;
		
		this.addHTTPHeader("Content-Type", "application/x-amf");
	}
	
	/**
	 * Close this session
	 */
	public void close()
	{
	}
	
	/**
	 * Add an HTTP header to all future HTTP requests. Might be used for authentication.
	 * @param key header name
	 * @param value header value
	 */
	public void addHTTPHeader(String key, String value)
	{
		synchronized(lock)
		{
			httpHeaderPairs.put(key, value);
		}
	}
	
	/**
	 * Get a list of the HTTP header names
	 * @return list of the header names
	 */
	public Set<String> addHTTPHeaderNames()
	{
		Set<String> ret = new HashSet<String>();
		synchronized(lock)
		{
			ret.addAll(httpHeaderPairs.keySet());
		}
		return ret;
	}
	
	/**
	 * Get an HTTP header value
	 * @param key header name
	 * @return header value
	 */
	public String getHTTPHeader(String key)
	{
		String ret = null;
		synchronized(lock)
		{
			ret = httpHeaderPairs.get(key);
		}
		return ret;
	}
	
	/**
	 * Set the session to be either synchronous or asynchronous (default is false)
	 * @param asynchronous is asynchronous
	 */
	public void setAsynchronous(boolean asynchronous)
	{
		synchronized(lock)
		{
			this.asynchronous = asynchronous;
		}
	}
	
	/**
	 * Return true is session is asynchronous
	 * @return true is session is asynchronous
	 */
	public boolean isAsynchronous()
	{
		synchronized(lock)
		{
			return this.asynchronous;
		}
	}
	
	/**
	 * Flush the current calls. If synchronous wait for responses.
	 */
	public void flush()
	{
		if (this.asynchronous)
		{
			synchronized(lock)
			{
				if (messageList.size() > 0 && !running)
				{
					running = true;
					vhost.getHandlerThreadPool().execute(this);
				}
			}
		}
		else
			run();
	}
	
	/**
	 * Call a remote method
	 * @param handlerName method name
	 * @param params parameters
	 */
	public void call(String handlerName, Object ... params)
	{
		call(handlerName, (IFlashRemoteResponse)null, params);
	}
	
	/**
	 * Call a remote method
	 * @param handlerName method name
	 * @param response response handler
	 * @param params parameters
	 */
	public void call(String handlerName, IFlashRemoteResponse response, Object ... params)
	{
		AMFData[] amfParams = AMFUtils.convertParams(params);
		
		FlashRemoteMessage message = new FlashRemoteMessage();
		
		message.handlerName = handlerName;
		message.amfParams = amfParams;
		message.response = response;
		
		synchronized(lock)
		{
			nextResponseId++;
			message.responseId = nextResponseId;
			messageList.add(message);
			messageMap.put(new Integer(message.responseId), message);
			
			if (this.asynchronous)
			{
				if (!running)
				{
					running = true;
					vhost.getHandlerThreadPool().execute(this);
				}
			}
		}
	}
	
	/**
	 * Internal for asynchronous operation
	 */
	public void run()
	{
		while(true)
		{
			List<FlashRemoteMessage> toSend = new ArrayList<FlashRemoteMessage>();
			try
			{
				synchronized(lock)
				{
					if (messageList.size() > 0)
					{
						toSend.addAll(messageList);
						messageList.clear();
					}
					else
						running = false;
				}
				
				if (toSend.size() > 0)
				{
					sendMessages(toSend);
				}
				else
					break;
			}
			catch (Exception e)
			{
				WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("FlashRemoteSession.run: "+e.toString());
			}
		}
	}
	
	
	private byte[] messagesToBytes(List<FlashRemoteMessage> toSend)
	{
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(byteOut);
		try
		{

			dataOut.write(0); // header1
			dataOut.write(0); // header2
			
			dataOut.writeShort(0); // header length
			
			dataOut.writeShort(toSend.size()); // body length

			Iterator<FlashRemoteMessage> iter = toSend.iterator();
			while(iter.hasNext())
			{
				FlashRemoteMessage message = iter.next();
				
				dataOut.writeUTF(message.handlerName);
				dataOut.writeUTF(message.responseId+"");
				dataOut.writeShort(0);
				dataOut.writeShort(0);
				
				AMFDataArray params = new AMFDataArray();
				if (message.amfParams != null)
				{
					for(int p=0;p<message.amfParams.length;p++)
						params.add(message.amfParams[p]);
				}
				byte[] paramsBytes = params.serialize();
				dataOut.write(paramsBytes);
			}
		}
		catch (Exception e)
		{
			WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("FlashRemoteSession.messagesToBytes: "+e.toString());
		}
		
		return byteOut.toByteArray();
	}
	
	// Strange method but it will add/replace a query string with a new value and
	// maintain the order - may be overkill but I think it is needed
	private String addQueryStrParam(String queryStr, String name, String value)
	{
		String newQueryStr = "";
		try
		{
			if (queryStr.length() <= 0)
				return name+"="+value;
			else
			{
				String[] parts = queryStr.split("[&]");
				
				List<String> keys = new ArrayList<String>();
				Map<String, String> map = new HashMap<String, String>();
				for(int i=0;i<parts.length;i++)
				{
					String key = parts[i];
					String val = null;
					
					int eloc = key.indexOf("=");
					if (eloc >= 0)
					{
						val = key.substring(eloc+1);
						key = key.substring(0, eloc);
					}
					
					if (!keys.contains(key))
						keys.add(key);
					map.put(key, val);
				}
				
				if (!keys.contains(name))
					keys.add(name);
				map.put(name, value);
				
				Iterator<String> iter = keys.iterator();
				while(iter.hasNext())
				{
					String key = iter.next();
					String val = map.get(key);
					
					if (newQueryStr.length() > 0)
						newQueryStr += "&";
					newQueryStr = key+"="+val;
				}
			}
		}
		catch (Exception e)
		{
			WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("HTTPUtils.addQueryStrParam: "+e.toString());
		}
		
		return newQueryStr;
	}
	
	private void handleResponse(List<FlashRemoteMessage> toSend, byte[] responseBytes, int responseCode, String responseMsg)
	{
		try
		{
			if (responseBytes.length > 0)
			{
				if (responseBytes != null)
				{
					//System.out.println("responseBytes: "+responseBytes+":"+responseBytes.length);
					//System.out.println(DebugUtils.formatBytes(responseBytes));
				}
				
				FlashRemoteResponseReader bufferPtr = new FlashRemoteResponseReader(responseBytes, responseBytes.length);
				
				// skip starting CRLF
				while(true)
				{
					int savePos = bufferPtr.position();
					int crlfCheck = bufferPtr.getShort();
					//System.out.println("crlfCheck: "+Integer.toHexString(0x0ffff&crlfCheck)+":"+Integer.toHexString(0x0ffff&CRLFTEST));
					if (crlfCheck != CRLFTEST)
					{
						bufferPtr.position(savePos);
						break;
					}
				}
				//System.out.println("position: "+bufferPtr.position());
	
				int version = bufferPtr.getShort();
				int headerCount = bufferPtr.getShort();
				//System.out.println("headerCount: "+headerCount);
				for(int i=0;i<headerCount;i++)
				{
					String headerVal = bufferPtr.getString();
					bufferPtr.getByte(); // skip byte
					long len = bufferPtr.getInt();
					//System.out.println("len: "+len);
					AMFData headerObj = bufferPtr.getAMF(len);
					//System.out.println("header["+i+":"+headerVal+"]: \n"+(headerObj==null?"null":headerObj.toString()));
					
					handleHeader(headerVal, headerObj);
				}
				
				int bodyCount = bufferPtr.getShort();
				//System.out.println("bodyCount: "+bodyCount);
				for(int i=0;i<bodyCount;i++)
				{
					String responseURI = bufferPtr.getString();
					String responseTarget = bufferPtr.getString();
					long len = bufferPtr.getInt();
					//System.out.println("len: "+len);
					AMFData bodyObj = bufferPtr.getAMF(len);
					//System.out.println("body["+i+":"+responseURI+","+responseTarget+"]: \n"+(bodyObj==null?"null":bodyObj.toString()));

					handleMessage(responseURI, responseTarget, bodyObj);
				}
				
				Iterator<FlashRemoteMessage> iter = toSend.iterator();
				while(iter.hasNext())
				{
					FlashRemoteMessage message = iter.next();
					int responseId = message.responseId;
					if (responseId > 0)
					{
						synchronized(lock)
						{
							messageMap.remove(new Integer(responseId));
						}
					}
				}
			}
			else
			{
				handleRequestFailure(responseCode, responseMsg);

				Iterator<FlashRemoteMessage> iter = toSend.iterator();
				while(iter.hasNext())
				{
					FlashRemoteMessage message = iter.next();
					
					int responseId = message.responseId;
					if (responseId > 0)
					{
						synchronized(lock)
						{
							messageMap.remove(new Integer(responseId));
						}
					}
					
					if (message.response != null)
					{
						try
						{
							message.response.onFailure(message.handlerName, responseCode, responseMsg);
						}
						catch(Exception e)
						{
							WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("FlashRemoteSession.handleResponse["+message.handlerName+":"+responseId+"]: "+e.toString());
							e.printStackTrace();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("FlashRemoteSession.handleResponse: "+e.toString());
			e.printStackTrace();
		}
	}
	
	private void handleRequestFailure(int responseCode, String responseMsg)
	{
		notifyRequestFailure(responseCode, responseMsg);
	}

	private void handleHeader(String handlerName, AMFData data)
	{
		notifyOnHeader(handlerName, data);
		
		//header[0:AppendToGatewayUrl]:
		//?PHPSESSID=8jumkk5cla6lv1thiclf25vf45
		if (handlerName.equalsIgnoreCase(APPENDTOURL))
		{
			if (data instanceof AMFDataItem)
			{
				String newQueryVal = ((AMFDataItem)data).toString();
				if (newQueryVal.startsWith("?"))
					newQueryVal = newQueryVal.substring(1);
				String key = newQueryVal;
				String value = null;
				int eloc = key.indexOf("=");
				if (eloc >= 0)
				{
					value = key.substring(eloc+1);
					key = key.substring(0, eloc);
				}
				
				this.queryStr = this.addQueryStrParam(this.queryStr, key, value);
			}
		}
	}
	
	private void handleMessage(String responseURI, String responseTarget, AMFData data)
	{
		notifyOnMessage(responseURI, responseTarget, data);
		
		while(true)
		{
			int sloc = responseURI.indexOf("/");
			if (sloc < 0)
				break;
			
			String responseIndexStr = responseURI.substring(0, sloc);
			if (responseIndexStr.length() <= 0)
				break;
			
			int responseIndex = -1;
			try
			{
				responseIndex = Integer.parseInt(responseIndexStr);
			}
			catch (Exception e)
			{
			}
			if (responseIndex <= 0)
				break;
			
			FlashRemoteMessage message = messageMap.remove(new Integer(responseIndex));
			if (message == null)
				break;
			
			try
			{
				if (message.response != null)
					message.response.onResult(message.handlerName, data);
			}
			catch(Exception e)
			{
				WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("FlashRemoteSession.handleResponse["+message.handlerName+":"+responseIndex+"]: "+e.toString());
				e.printStackTrace();
			}
			break;
		}		
		//body[0:1/onResult,null]:
		//You said |param1|param2| after	
	}
	
	private void sendMessages(List<FlashRemoteMessage> toSend)
	{
		byte[] inData = messagesToBytes(toSend);
		if (inData == null)
			return;
		
		try
		{
			URL url;
			HttpURLConnection urlConn;
			DataOutputStream printout = null;
			DataInputStream input = null;
			String fullUrl = this.url+(queryStr.length()>0?"?"+queryStr:"");

			url = new URL(fullUrl);
			urlConn = (HttpURLConnection)url.openConnection();
			urlConn.setRequestMethod("POST");
			urlConn.setConnectTimeout(this.connectionTimeout);
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			
			synchronized(lock)
			{
				Iterator<String> iter = httpHeaderPairs.keySet().iterator();
				while(iter.hasNext())
				{
					String key = iter.next();
					urlConn.setRequestProperty(key, httpHeaderPairs.get(key));
				}
			}
			urlConn.setRequestProperty("Content-Length", ""+inData.length);
			printout = new DataOutputStream(urlConn.getOutputStream());
			printout.write(inData);
			printout.flush();
			printout.close();
			printout = null;

			input = new DataInputStream(urlConn.getInputStream());
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(byteOut);

			int rChunk = 64 * 1024;
			byte[] myData = new byte[rChunk];

			while (true)
			{
				int bytesRead = input.read(myData, 0, rChunk);
				
				//System.out.println("bytesRead: "+bytesRead);
				
				if (bytesRead == -1)
					break;
				else
				{
					dataOut.write(myData, 0, bytesRead);
					Thread.sleep(1);
				}
			}
			
			//if (urlConn instanceof HttpURLConnection)
			//{
			//	HttpURLConnection httpUrlConn = (HttpURLConnection)urlConn;
			//	System.out.println("fields: "+httpUrlConn.getHeaderFields());
			//}

			input.close();
			input = null;
			
			int responseCode = urlConn.getResponseCode();
			String responseMsg = urlConn.getResponseMessage();
			byte[] responseBytes = byteOut.toByteArray();
			
			handleResponse(toSend, responseBytes, responseCode, responseMsg);
		}
		catch (Exception e)
		{
			WMSLoggerFactory.getLogger(FlashRemoteSession.class).error("FlashRemoteSession.sendMessages: "+e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Get the HTTP connection timeout (milliseconds)
	 * @return HTTP connection timeout (milliseconds)
	 */
	public int getConnectionTimeout()
	{
		return connectionTimeout;
	}

	/**
	 * Set the HTTP connection timeout (milliseconds)
	 * @param connectionTimeout HTTP connection timeout (milliseconds)
	 */
	public void setConnectionTimeout(int connectionTimeout)
	{
		this.connectionTimeout = connectionTimeout;
	}
	
	/**
	 * Add a listener to this session (see IFlashRemotingNotify)
	 * @param listener listener
	 */
	public void addListener(IFlashRemotingNotify listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}
	
	/**
	 * Remove a listener to this session (see IFlashRemotingNotify)
	 * @param listener listener
	 */
	public void removeListener(IFlashRemotingNotify listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}
	
	private void notifyOnHeader(String headerName, AMFData data)
	{
		List<IFlashRemotingNotify> tmpList = new ArrayList<IFlashRemotingNotify>();
		synchronized(listeners)
		{
			tmpList.addAll(listeners);
		}
		
		Iterator<IFlashRemotingNotify> iter = tmpList.iterator();
		while(iter.hasNext())
		{
			IFlashRemotingNotify listener = iter.next();
			listener.onHeader(headerName, data);
		}
	}
	
	private void notifyOnMessage(String responseURI, String responseTarget, AMFData data)
	{
		List<IFlashRemotingNotify> tmpList = new ArrayList<IFlashRemotingNotify>();
		synchronized(listeners)
		{
			tmpList.addAll(listeners);
		}
		
		Iterator<IFlashRemotingNotify> iter = tmpList.iterator();
		while(iter.hasNext())
		{
			IFlashRemotingNotify listener = iter.next();
			listener.onMessage(responseURI, responseTarget, data);
		}
	}
	
	private void notifyRequestFailure(int responseCode, String responseMsg)
	{
		List<IFlashRemotingNotify> tmpList = new ArrayList<IFlashRemotingNotify>();
		synchronized(listeners)
		{
			tmpList.addAll(listeners);
		}
		
		Iterator<IFlashRemotingNotify> iter = tmpList.iterator();
		while(iter.hasNext())
		{
			IFlashRemotingNotify listener = iter.next();
			listener.onRequestFailure(responseCode, responseMsg);
		}
	}
}
