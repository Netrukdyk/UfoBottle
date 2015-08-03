package com.bottle;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

@SuppressLint("SdCardPath")
public class Main extends Activity implements OnClickListener {
	Button start, lang_en, lang_lt;
	VideoView videoView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        					 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loadLanguage(UfoBottle.lang);
        setContentView(R.layout.activity_main);
        
        start = (Button) findViewById(R.id.start);
        lang_en = (Button) findViewById(R.id.lang_en);
        lang_lt = (Button) findViewById(R.id.lang_lt);
        start.setOnClickListener(this);
        lang_en.setOnClickListener(this);
        lang_lt.setOnClickListener(this);              
        
        playVideo();
    }
   
	
	@Override
	protected void onStart() {
		super.onStart();
//		videoView.start();
	}
	
	@Override
	public void onDestroy(){
		stopService(new Intent(this, ServiceBluetooth.class));
		super.onDestroy();
	}
    
    private void playVideo() {
    	videoView = (VideoView) findViewById(R.id.video);
    	
    	videoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                videoView.start();
            }
        });
    	videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                    //mp.reset();
                    //videoView.setVideoPath("/sdcard/media/out.mp4");
                    videoView.resume();
            }
        });
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setKeepScreenOn(true);
        videoView.setVideoPath("/sdcard/media/out.mp4");
        videoView.start();
    }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
	        case R.id.start:
	        	videoView.stopPlayback();
	            startActivity(new Intent(Main.this, QR.class));
	        	break;
	        case R.id.lang_en:
	        	setLanguage("en");
	        	break;
	        case R.id.lang_lt:
	        	setLanguage("lt");
	        	break;	        	
		}
	}
	
	private void setLanguage(String lang){
		UfoBottle.lang = lang;
		Log.v("Language",lang);
		Locale myLocale = new Locale(lang);
	    Resources res = getResources(); 
	    DisplayMetrics dm = res.getDisplayMetrics();
	    Configuration conf = res.getConfiguration(); 
	    conf.locale = myLocale; 
	    res.updateConfiguration(conf, dm); 
	    Intent refresh = new Intent(this, Main.class); 
	    startActivity(refresh); 
	}
	
	private void loadLanguage(String lang){
		Locale locale = new Locale(lang); 
	    Locale.setDefault(locale);
	    Configuration config = new Configuration();
	    config.locale = locale;
	    getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());
	}
		
} //End of Main