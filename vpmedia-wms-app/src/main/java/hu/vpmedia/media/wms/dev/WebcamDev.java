/*
 *=BEGIN CLOSED LICENSE
 *
 * Copyright(c) 2012 András Csizmadia.
 * http://www.vpmedia.eu
 *
 * For information about the licensing and copyright please 
 * contact András Csizmadia at andras@vpmedia.eu.
 *
 *=END CLOSED LICENSE
 */
 
package hu.vpmedia.media.wms.dev;

import java.util.*;

import com.wowza.wms.amf.*;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.server.*;
import com.wowza.wms.stream.*;
import hu.vpmedia.media.wms.plugin.bwcheck.BWCheck;
import hu.vpmedia.media.core.*;

import com.wowza.util.IOPerformanceCounter;

public class WebcamDev extends ModuleBase
{
	// Application Level API
	
	public void onAppStart( IApplicationInstance appInstance )
	{
		getLogger().info( "onAppStart" + " " + appInstance.getApplication().getName() + "/"+appInstance.getName() );
	}

	public void onAppStop(IApplicationInstance appInstance)
	{
		getLogger().info( "onAppStop" + " " + appInstance.getApplication().getName() + "/"+appInstance.getName() );
	}
    
  // Client Level API

	public void onConnect( IClient client, RequestFunction function, AMFDataList params )
	{
		getLogger().info( "onConnect" + " " + client.getClientId() );
	    getLogger().info( "flashVer: " + client.getFlashVer()
         + ", ip: " + client.getIp()
         + ", referrer: " + client.getReferrer()
         + ", connectTime: " + client.getConnectTime()
         + ", properties: " + client.getProperties() );
                
        AMFDataObj param1 = (AMFDataObj)getParam(params, PARAM1); 
        getLogger().info("Params: " + param1 );
        
        client.acceptConnection();
        
        // client.rejectConnection("ERROR_MESSAGE");
	    BWCheck.calculateClientBw( client );
	}

	public void onDisconnect( IClient client )
	{
		getLogger().info( "onDisconnect" + " " + client.getClientId() );
	}

  public void onConnectAccept( IClient client )
  {
    getLogger().info( "onConnectAccept" + " " + client.getClientId() );
    // login
    AMFDataObj loginResult = new AMFDataObj();
    loginResult.put( "userName", "user_" + client.getClientId() );
    loginResult.put( "userType", UserTypes.GUEST );        
    client.call( "onRoomLogin", null, loginResult );
    // user list
    AMFDataArray resultUsers = new AMFDataArray();        
    resultUsers.add( "user_" + client.getClientId() );        
    client.call( "onUserListChange", null, resultUsers );
    // stream list
    AMFDataArray resultStreams = new AMFDataArray();        
    resultStreams.add( "user_" + client.getClientId() );        
    client.call( "onStreamListChange", null, resultStreams );
  }

  public void onConnectReject( IClient client )
  {
    getLogger().info( "onConnectReject" + " " + client.getClientId() );
  }
  
  // Stream Level API
  
  public void onStreamCreate ( IMediaStream stream )
  {
    getLogger().info( "onStreamCreate: " + stream.getSrc() );
  }
  
  public void onStreamDestroy ( IMediaStream stream )
  {
    getLogger().info( "onStreamDestroy: " + stream.getSrc() );
  }
  
  // Flash Client API
  
  public void chatMessage( IClient client, RequestFunction function, AMFDataList params )
  {
    getLogger().info( "chatMessage" + " " + client.getClientId() );
    AMFDataItem param1 = (AMFDataItem)getParam(params, PARAM1);
    
    client.getAppInstance().broadcastMsg("onChatMessage", param1);
    //sendResult(client, params, "true");
  }
  
  public void logoutRoom( IClient client, RequestFunction function, AMFDataList params )
  {
    getLogger().info( "logoutRoom" + " " + client.getClientId() );
    client.getAppInstance().shutdownClient(client);
  }
  
  // Flash Media Encoder API
  
  public void FCPublish( IClient client, RequestFunction function, AMFDataList params )
  {
      getLogger().info( "FCPublish" + " " + client.getClientId() );
  }
  
  public void FCUnpublish( IClient client, RequestFunction function, AMFDataList params )
  {
      getLogger().info( "FCUnpublish" + " " + client.getClientId() );
  }

  public void releaseStream( IClient client, RequestFunction function, AMFDataList params )
  {
      getLogger().info( "FCUnpublish" + " " + client.getClientId() );
  }         

}
