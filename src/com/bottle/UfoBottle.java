package com.bottle;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class UfoBottle extends Application {
	
	private static Context context;
    private Map<String, Integer> data;
    
    public static String lang = "en"; // default language
	
    private BTConnection bt;
	
    public UfoBottle(){
		
    	bt = new BTConnection();
    	prepareCodes(30000);
    }
    
    @Override
    public void onCreate(){
        super.onCreate();
        UfoBottle.context = getApplicationContext();
        //startService(new Intent(this, ServiceBluetooth.class));

		// Bluetooth
		bt.start();
		
    }
    
    public static Context getAppContext() {
        return UfoBottle.context;
    }
    
    public BTConnection getBluetooth() {
        return bt;
    }
    
    public int getData(String key) {
    	Log.v("getData",key);
    	if(key.equals("reset")) {    		
    		prepareCodes(30000);
    		return -2;
    	}
    	if(key.equals("exit")) {
    		Log.v("getData","KILL");
    		//android.os.Process.killProcess(android.os.Process.myPid());
    		return -3;
    	}    	
		return (data.get(key) != null) ? data.get(key) : -1;
    }
    
    public void setData(String key, int value) {
        data.put(key, value);
    }
    
    private void prepareCodes(int num){
    	data = null;
        String[] kodai = new String[]{
        		"10821","10822","10823","10824","10825","10826","10827",
        		"10828","10829","10830","10831","10832","10833","10834",
        		"10835","10836","10837","10838","10839","10840","10841",
        		"10842","10843","10844","10845","10846","10847","10848" };
        
        data = new HashMap<String, Integer>();
        for (int i=0; i<kodai.length; i++){
        	data.put(kodai[i], num);
        }
    }
    
}
