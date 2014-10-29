package hu.vpmedia.media.wms.plugin.medialoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;

public class MediaLoader extends ModuleBase 
{
	public void load(IClient client, RequestFunction function, AMFDataList params) 
    {
		getLogger().info("load");
		
        String fileName = "";
		byte[] fileBytes = null;
		
		if (getParam(params, PARAM1).getType() == AMFData.DATA_TYPE_STRING)
			fileName  = params.getString(PARAM1);
		
        getLogger().info("file:"+fileName);
		
        try 
        {
			if (!fileName.isEmpty()) 
            {
				File mediaFile = new File(fileName);
				if (mediaFile.exists() && mediaFile.canRead() && mediaFile.isFile() && mediaFile.length() < Integer.MAX_VALUE) 
                {
                        getLogger().info("reading media..");
                        
						InputStream in = new FileInputStream(mediaFile);
						int length = (int) mediaFile.length();
						fileBytes = new byte[length];

						int offset = 0;
				        int numRead = 0;
				        while (offset < fileBytes.length && (numRead=in.read(fileBytes, offset, fileBytes.length-offset)) >= 0) 
                        {
				            offset += numRead;
				        }
				        
				        if (offset < fileBytes.length) 
                        {
				            throw new IOException("Could not completely read file "+mediaFile.getName());
				        }
				        
				        in.close();
				} else {
                    getLogger().info("file read error!");
                }
			}
		} 
        catch (FileNotFoundException e) 
        {
			e.printStackTrace();
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
		}
		
		sendResult(client, params, AMFDataByteArray.wrap(fileBytes));
	}

}