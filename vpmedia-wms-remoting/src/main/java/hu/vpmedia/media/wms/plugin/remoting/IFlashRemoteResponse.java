package hu.vpmedia.media.wms.plugin.remoting;

import com.wowza.wms.amf.*;

/**
 * 
 * <p>IFlashRemoteResponse: Interface for FlashRemoteSession calls.</p>
 * @author Wowza Media Systems
 *
 */

public interface IFlashRemoteResponse
{
	/**
	 * Called if the Flash Remote call returns a value and the call was successful.
	 * @param handlerName handler name
	 * @param res message data
	 */
	public abstract void onResult(String handlerName, AMFData res);
	
	/**
	 * Called if the Flash Remote call failed.
	 * @param handlerName handler name
	 * @param statusCode response code (HTTP status code)
	 * @param responseMsg response message
	 */
	public abstract void onFailure(String handlerName, int statusCode, String responseMsg);
}
