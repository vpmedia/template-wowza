package hu.vpmedia.media.wms.plugin.remoting;

import com.wowza.wms.amf.*;

/**
 * 
 * <p>IFlashRemotingNotify: FlashRemoteSession listener interface. Implement this interface and call FlashRemoteSession.addListener()
 * to add a raw listener to the session.</p>
 * @author Wowza Media Systems
 *
 */

public interface IFlashRemotingNotify
{
	/**
	 * Called for each header message
	 * @param headerName header name
	 * @param data header data
	 */
	public abstract void onHeader(String headerName, AMFData data);
	
	/**
	 * Called for each message
	 * @param responseURI response URI
	 * @param responseTarget response target
	 * @param data message data
	 */
	public abstract void onMessage(String responseURI, String responseTarget, AMFData data);
	
	/**
	 * Called if the FlashRemote call fails
	 * @param responseCode response code (HTTP status code)
	 * @param responseMsg response message
	 */
	public abstract void onRequestFailure(int responseCode, String responseMsg);
}	

