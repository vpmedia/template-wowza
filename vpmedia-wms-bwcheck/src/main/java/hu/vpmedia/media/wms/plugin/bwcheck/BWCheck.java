package hu.vpmedia.media.wms.plugin.bwcheck;

import java.util.*;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.util.IOPerformanceCounter;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.logging.*;

public class BWCheck extends ModuleBase {
        
    // BW Checker Encoder API
	
	public void checkBandwidth(IClient p_client, RequestFunction function, AMFDataList params) {
        getLogger().info("checkBandwidth");
        calculateClientBw(p_client);
    }
    
    static public void calculateClientBw(IClient p_client) {
        getLogger().info("calculateClientBw");
        AMFDataArray payload = new AMFDataArray();
        for (int i=0;i<1200;i++) {
            payload.add(new AMFDataItem((double)Math.random()));
        }
        p_client.getProperties().put("payload", payload); 
        AMFDataArray payload_1 = new AMFDataArray();
        for (int i=0;i<12000;i++) {
            payload_1.add(new AMFDataItem((double)Math.random()));
        }
        p_client.getProperties().put("payload_1", payload_1);
        AMFDataArray payload_2 = new AMFDataArray();
        for (int i=0;i<120000;i++) {
            payload_2.add(new AMFDataItem((double)Math.random()));
        }
        p_client.getProperties().put("payload_2", payload_2);
        
        List<Long> beginningValues = null;
        final IOPerformanceCounter beginningStats = p_client.getTotalIOPerformanceCounter();
        final Long start = new Long(System.nanoTime()/1000000); //new Long(System.currentTimeMillis());
        
        class ResultObj implements IModuleCallResult {
            IClient client = null;
            double latency = 0;
            double cumLatency = 1;
            int count = 0;
            int sent = 0;
            double kbitDown = 0;
            double deltaDown = 0;
            double deltaTime = 0;
            
        
            List<Long> pakSent = new ArrayList<Long>();
            List<Long> pakRecv = new ArrayList<Long>();
            
            
            List<Long> beginningValues = null;
            
            public ResultObj(IClient p_client) {
                this.client = p_client;
                
                beginningValues = new ArrayList<Long>();
                beginningValues.add(0, beginningStats.getMessagesOutBytes());
                beginningValues.add(1, beginningStats.getMessagesInBytes());
                beginningValues.add(2, start);
            }
            
            public void onResult(IClient client, RequestFunction function, AMFDataList params) {
                Long now1 = new Long(System.nanoTime()/1000000); //new Long(System.currentTimeMillis());
                pakRecv.add(now1);
                Long timePassed = (now1 - beginningValues.get(2));
                count++;
                
                if (count == 1) {
                    latency = Math.min(timePassed, 800);
                    latency = Math.max(latency, 10);
                    
                    //WMSLoggerFactory.getLogger(null).info("count: "+count+ " sent: "+sent+" timePassed: "+timePassed+" latency: "+latency);
                    
                    // We now have a latency figure so can start sending test data.
                    // Second call.  1st packet sent
                    pakSent.add(now1);
                    sent++;
                    this.client.call("onBWCheck", this, this.client.getProperties().get("payload"));
                }
                
                //To run a very quick test, uncomment the following if statement and comment out the next 3 if statements.
                
                /*
                else if (count == 2 && (timePassed < 2000)) {
                    pakSent.add(now1);
                    sent++;
                    cumLatency++;
                    this.client.call("onBWCheck", this, this.client.getProperties().get("payload"));
                }
                */
                
                //The following will progressivly increase the size of the packets been sent until 1 second has elapsed.
                else if ((count > 1 && count < 3) && (timePassed < 1000)) {
                    pakSent.add(now1);
                    sent++;
                    cumLatency++;
                    this.client.call("onBWCheck", this, this.client.getProperties().get("payload"));
                }
                else if ((count >=3 && count < 6) && (timePassed < 1000)) {
                    pakSent.add(now1);
                    sent++;
                    cumLatency++;
                    this.client.call("onBWCheck", this, this.client.getProperties().get("payload_1"));
                }
                else if (count >= 6 && (timePassed < 1000)) {
                    pakSent.add(now1);
                    sent++;
                    cumLatency++;
                    this.client.call("onBWCheck", this, this.client.getProperties().get("payload_2"));
                }
                //Time elapsed now do the calcs
                else if (sent == count) {
                    // see if we need to normalize latency
                    if (latency >= 100) {
                        //make sure satelite and modem is detected properly
                        if (pakRecv.get(1) - pakRecv.get(0) > 1000) {
                           latency = 100; 
                        }
                    }
                    
                    this.client.getProperties().remove("payload");
                    this.client.getProperties().remove("payload_1");
                    this.client.getProperties().remove("payload_2");
                    // tidy up
                    // and compute bandwidth
                    IOPerformanceCounter endStats = this.client.getTotalIOPerformanceCounter();
                    deltaDown = (endStats.getMessagesOutBytes() - beginningValues.get(0)) * 8 / 1000; // bytes to kbits
                    deltaTime = ((now1 - beginningValues.get(2)) - (latency * cumLatency)) / 1000; // total dl time - latency for each packet sent in secs
                    if (deltaTime <= 0) {
                        deltaTime = (now1 - beginningValues.get(2)) / 1000;
                    }
                    kbitDown = Math.round(deltaDown / deltaTime); // kbits / sec
                                                            
                    this.client.call("onBWDone", null, this.kbitDown, this.deltaDown, this.deltaTime, this.latency);
                }
            }
        }
        getLogger().info("ResultObj");
        ResultObj res = new ResultObj(p_client);
        
        res.pakSent.add(start);
        res.sent++;
        p_client.call("onBWCheck", res, ""); // 1st call sends empty packet to get latency
    }
}