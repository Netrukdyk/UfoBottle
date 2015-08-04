package com.bottle;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

@SuppressLint("SdCardPath")
public class Fill extends Activity implements OnClickListener {
	TextView serverStatus, info;
	Button home, fill, back, errorDevTryAgain, errorDbTryAgain, successHome;
	Handler serverHandler;
	String butKodas;
	int butLiko;
	UfoBottle app;
	Boolean klaida;
	VideoView videoView2;

	LinearLayout dev_error_layout, db_error_layout, success;

	private BTConnection bt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fill);

		bt = new BTConnection();
		bt.sethandler(uiHandler);
		serverHandler = bt.getHandler();
		bt.start();

		serverStatus = (TextView) findViewById(R.id.serverStatus); // debug
		info = (TextView) findViewById(R.id.info);

		fill = (Button) findViewById(R.id.fill);
		back = (Button) findViewById(R.id.back);
		home = (Button) findViewById(R.id.home);
		errorDevTryAgain = (Button) findViewById(R.id.try_again_dev);
		errorDbTryAgain = (Button) findViewById(R.id.try_again_db);
		successHome = (Button) findViewById(R.id.home_success);

		fill.setOnClickListener(this);
		back.setOnClickListener(this);
		home.setOnClickListener(this);
		errorDevTryAgain.setOnClickListener(this);
		errorDbTryAgain.setOnClickListener(this);
		successHome.setOnClickListener(this);

		dev_error_layout = (LinearLayout) findViewById(R.id.device_error);
		db_error_layout = (LinearLayout) findViewById(R.id.database_error);
		success = (LinearLayout) findViewById(R.id.success);

		butKodas = getIntent().getStringExtra(QR.EXTRA_MESSAGE);
		app = (UfoBottle) UfoBottle.getAppContext();
		butLiko = app.getData(butKodas);
		
		if(butKodas.equals("godmode")){
			goGodMode();			
		}
		
		Log.v("FillLiko", String.valueOf(butLiko));
		if (butLiko <= 0) {
			if (butLiko == -2) { // -2 = reset
				Log.v("Fill", "Reset OK");
				goHome();
				Toast.makeText(getApplicationContext(), "Reset OK", Toast.LENGTH_SHORT).show();
			} else if (butLiko == -3) { // -3 = exit
				Log.v("Fill", "Exit OK");
				// android.os.Process.killProcess(android.os.Process.myPid());
				Toast.makeText(getApplicationContext(), "Exit OK", Toast.LENGTH_SHORT).show();
				finish();
				Intent startMain = new Intent(Intent.ACTION_MAIN);
				startMain.addCategory(Intent.CATEGORY_HOME);
				startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(startMain);
			} else { // -1 = nëra duomenø bazëje
				Log.v("Fill", butKodas + " - nëra duomenø bazëje");
				setDbError(true);
			}
		} else {
			info.setText(getString(R.string.bottle) + ": " + butKodas + "\n" + getString(R.string.leftfills) + ": " + butLiko);
		}

		playVideo2();
	}

	@Override
	protected void onStart() {
		super.onStart();
		videoView2.start();
	}

	private void playVideo2() {
		videoView2 = (VideoView) findViewById(R.id.video2);

		videoView2.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.setLooping(true);
			}
		});
		videoView2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				mp.reset();
				videoView2.setVideoPath("/sdcard/media/out2.mp4");
				videoView2.start();
			}
		});
		MediaController mediaController = new MediaController(this);
		mediaController.setAnchorView(videoView2);
		videoView2.setKeepScreenOn(true);
		videoView2.setVideoPath("/sdcard/media/out2.mp4");
		videoView2.start();
	}

	// Apdoroja þinutes, gautas ið serverio
	@SuppressLint("HandlerLeak")
	public Handler uiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				String temp = msg.getData().getString("message");
				// serverStatus.append(temp + "\n");
				// jei serveris atsiuntë klaidà
				if (temp.contains("error")) {
					klaida = true;
				}
				
				if (temp != null && temp.contains("complete: BOTTLE_DETACH") && klaida) {
					setDevError(true);
					fill.setEnabled(true);
					klaida = false;
					return;
				}
				// jei buteliukas pripiltas (-1)
				if (temp != null && temp.contains("complete: BOTTLE_DETACH") && !klaida) {
					Log.v("Fill", "Success");
					app.setData(butKodas, butLiko - 1);
					// sendToServer(1, "close");
					klaida = false;					
					setSuccess(true);
					fill.setEnabled(true);
					return;
				}
				break;
			case 2:
				// serverStatus.setText(msg.getData().getString("message") + "\n");
				break;
			case 3:
				fill.setEnabled(true);
				Log.v("FILL", "EXTRA 3 case" + msg.getData().getString("message"));
				break;
			}
		};
	};

	// suformuoja praneðimà ir iðsiunèia serveriui
	private void sendToServer(int what, String msgText) {
		Bundle b = new Bundle();
		b.putString("command", msgText);
		Message msg = new Message();
		msg.what = what;
		msg.setData(b);
		serverHandler.sendMessage(msg);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.home:
			goHome();
			break;
		case R.id.back:
			goQR();
			break;
		case R.id.fill:
			klaida = false;
			fill.setEnabled(false);// Disable "Pilti" mygtukà
			sendToServer(1, "start");
			break;
		case R.id.try_again_dev:
			setDevError(false);
			break;
		case R.id.try_again_db:
			goQR();
			break;
		case R.id.home_success:			
			goHome();
			break;
		}
	}

	private void setDevError(Boolean enable) {
		dev_error_layout.setVisibility((enable) ? LinearLayout.VISIBLE : LinearLayout.GONE);
	}

	private void setDbError(Boolean enable) {
		db_error_layout.setVisibility((enable) ? LinearLayout.VISIBLE : LinearLayout.GONE);
	}
	Timer timer;
	
	private void setSuccess(Boolean enable) {
		success.setVisibility((enable) ? LinearLayout.VISIBLE : LinearLayout.GONE);
		if (enable) {
			this.timer = new Timer();
			this.timer.schedule(new TimerTask() {
				public void run() {
					runOnUiThread(new Runnable() {
					    public void run() {
					    	goHome();
					    }
					});
				}
			}, 15000 /* ms */);
		}
	}
	private void goHome() {
		
		if(this.timer != null) this.timer.cancel();
		
		if(videoView2 != null) videoView2.stopPlayback();
		sendToServer(1, "close");
		finish();
		startActivity(new Intent(Fill.this, Main.class));
	}

	private void goQR() {
		if(videoView2 != null) videoView2.stopPlayback();
		sendToServer(1, "close");
		finish();
		startActivity(new Intent(Fill.this, QR.class));
	}
	private void goGodMode() {
		if(videoView2 != null) videoView2.stopPlayback();
		sendToServer(1, "close");
		finish();
		startActivity(new Intent(Fill.this, God.class));
	}	
}
