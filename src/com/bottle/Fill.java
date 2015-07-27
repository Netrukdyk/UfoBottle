package com.bottle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

@SuppressLint("SdCardPath")
public class Fill extends Activity implements OnClickListener {
	TextView serverStatus, info;
	Button home, fill, back;
	private BTConnection bt;
	Handler serverHandler;
	String butKodas;
	int butLiko;
	UfoBottle app;
	Boolean klaida;
	VideoView videoView2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fill);

		serverStatus = (TextView) findViewById(R.id.serverStatus); // rodo
																	// praneðimus
																	// ið
																	// serverio
		info = (TextView) findViewById(R.id.info);

		fill = (Button) findViewById(R.id.fill);
		back = (Button) findViewById(R.id.back);
		home = (Button) findViewById(R.id.home);

		fill.setOnClickListener(this);
		back.setOnClickListener(this);
		home.setOnClickListener(this);

		butKodas = getIntent().getStringExtra(QR.EXTRA_MESSAGE);
		app = (UfoBottle) UfoBottle.getAppContext();
		butLiko = app.getData(butKodas);
		Log.v("FillLiko",String.valueOf(butLiko));
		if (butLiko <= 0) {
			if(butLiko == -2){ // -2 = reset
				Log.v("Fill", "Reset OK");
				startActivity(new Intent(Fill.this, Main.class));
			} else { // -1 = nëra duomenø bazëje
				// klaida nëra duomenø bazëje
				Log.v("Fill", butKodas + " - nëra duomenø bazëje");
				startActivity(new Intent(Fill.this, ErrorDatabase.class));
			}
		} else {
			info.setText(getString(R.string.bottle)+": "+ butKodas + "\n"
							+ getString(R.string.leftfills) + ": " + butLiko);
		}

		playVideo2();

		bt = new BTConnection(uiHandler); // sukuria bluetooth serverá
		bt.start();
		serverHandler = bt.getHandler(); // gaunam serverio handlerá, per kurá
											// siøsim þinutes serveriui
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
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
				case 0 :
					String temp = msg.getData().getString("message");
					serverStatus.append(temp + "\n");
					// jei serveris atsiuntë klaidà
					if (temp.contains("error")) {
						// sendToServer(1, "close");
						klaida = true;
						fill.setEnabled(true);
						startActivity(new Intent(Fill.this, ErrorDevice.class));
						return;
					}
					// jei buteliukas pripiltas (-1)
					if (temp != null && temp.contains("complete: BOTTLE_DETACH") && !klaida) {
						Log.v("Fill", "Success");
						app.setData(butKodas, butLiko - 1);
						sendToServer(1, "close");
						klaida = false;
						fill.setEnabled(true);
						startActivity(new Intent(Fill.this, Success.class));
						return;
					}
					break;
				case 2 :
					serverStatus.setText(msg.getData().getString("message") + "\n");
					klaida = true;
					fill.setEnabled(true);
					startActivity(new Intent(Fill.this, ErrorDevice.class));
					break;
				case 3 :
					if (msg.getData().getString("message") == "Enable BT") {
						Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(enableBluetooth, 0);
					}
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
			case R.id.home :
				videoView2.stopPlayback();
				sendToServer(1, "close");
				startActivity(new Intent(Fill.this, Main.class));
				break;
			case R.id.back :
				sendToServer(1, "close");
				startActivity(new Intent(Fill.this, QR.class));
				break;
			case R.id.fill :
				klaida = false;
				fill.setEnabled(false);// Disable "Pilti" mygtukà
				sendToServer(1, "start");
				break;
		}
	}
}
