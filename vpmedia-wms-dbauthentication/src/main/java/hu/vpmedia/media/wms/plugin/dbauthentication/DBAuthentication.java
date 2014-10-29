package hu.vpmedia.media.wms.plugin.dbauthentication;

import java.sql.*;

import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;

public class DBAuthentication extends ModuleBase 
{

	static public void onConnect(IClient client, RequestFunction function,
			AMFDataList params) 
	{
		
		String userName = getParamString(params, PARAM1);
		String password = getParamString(params, PARAM2);
				
		IApplicationInstance appInstance = client.getAppInstance();
		
        String dbUser = appInstance.getProperties().getPropertyStr("dbuser");
        String dbPass = appInstance.getProperties().getPropertyStr("dbpass");
        String dbName = appInstance.getProperties().getPropertyStr("dbname");
        String dbHost = appInstance.getProperties().getPropertyStr("dbhost");

		// preload the driver class
		try 
		{
			Class.forName("com.mysql.jdbc.Driver").newInstance(); 
		} 
		catch (Exception e) 
		{ 
			getLogger().error("Error loading: com.mysql.jdbc.Driver: "+e.toString());
		} 
		
		Connection conn = null;
		try 
		{
			conn = DriverManager.getConnection("jdbc:mysql://" + dbHost + "/" + dbName + "?user=" + dbUser+"&password=" + dbPass);

			Statement stmt = null;
			ResultSet rs = null;

			try 
			{
				stmt = conn.createStatement();
				rs = stmt.executeQuery("SELECT id(*) as id FROM user_auth where name = '"+userName+"' and word = '"+password+"'");
				if (rs.next() == true)
				{
				    if (rs.getInt("id") > 0)
					{
						client.acceptConnection();
					}
				}

			} 
			catch (SQLException sqlEx) 
			{
				getLogger().error("sqlexecuteException: " + sqlEx.toString());
			} 
			finally 
			{
				// it is a good idea to release resources in a finally{} block
				// in reverse-order of their creation if they are no-longer needed

				if (rs != null) 
				{
					try 
					{
						rs.close();
					} 
					catch (SQLException sqlEx) 
					{

						rs = null;
					}
				}

				if (stmt != null) 
				{
					try 
					{
						stmt.close();
					} 
					catch (SQLException sqlEx) 
					{
						stmt = null;
					}
				}
			}

			conn.close();
		} 
		catch (SQLException ex) 
		{
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}

		getLogger().info("onConnect: " + client.getClientId());
	}

	static public void onConnectAccept(IClient client) 
	{
		getLogger().info("onConnectAccept: " + client.getClientId());
	}

	static public void onConnectReject(IClient client) 
	{
		getLogger().info("onConnectReject: " + client.getClientId());
	}

	static public void onDisconnect(IClient client) 
	{
		getLogger().info("onDisconnect: " + client.getClientId());
	}

}