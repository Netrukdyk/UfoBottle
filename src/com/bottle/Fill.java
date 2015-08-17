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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

@SuppressLint("SdCardPath")
public class Fill extends Activity implements OnClickListener {
	TextView serverStatus, info;
	Button home, fill, back, errorDevTryAgain, errorDbTryAgain, successHome;
	String butKodas;
	int butLiko;
	UfoBottle app;
	Boolean klaida;
	VideoView videoView2;

	LinearLayout dev_error_layout, db_error_layout, success;
	RelativeLayout progress, settings;

	private Button wuMinus, wuPlus, wuSet, wuGet, settingsHome;
	private TextView wuValue, density;
	int old, newValue;

	ImageView[] steps = new ImageView[6];

	private BTConnection bt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fill);

		app = (UfoBottle) UfoBottle.getAppContext();
		
//		// Bluetooth
		bt = app.getBluetooth();
//		bt.sethandler(uiHandler);
//		bt.start();
		
		bt.sethandler(uiHandler);
		
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

		progress = (RelativeLayout) findViewById(R.id.progress);
		steps[0] = (ImageView) findViewById(R.id.step1);
		steps[1] = (ImageView) findViewById(R.id.step2);
		steps[2] = (ImageView) findViewById(R.id.step3);
		steps[3] = (ImageView) findViewById(R.id.step4);
		steps[4] = (ImageView) findViewById(R.id.step5);
		steps[5] = (ImageView) findViewById(R.id.step6);

		settings = (RelativeLayout) findViewById(R.id.settings);
		density = (TextView) findViewById(R.id.density);
		wuValue = (TextView) findViewById(R.id.wuValue);
		wuMinus = (Button) findViewById(R.id.wuMinus);
		wuPlus = (Button) findViewById(R.id.wuPlus);
		wuSet = (Button) findViewById(R.id.wuSet);
		wuGet = (Button) findViewById(R.id.wuGet);
		settingsHome = (Button) findViewById(R.id.settingsBack);

		bt.sendToServer(1, "GetWaterUnits");
		old = 390;
		newValue = old;
		
		density.append(getResources().getDisplayMetrics().density+"");
//		wuValue.setText(newValue + ""); // <-- Read from Device, just skip here
		wuMinus.setOnClickListener(this);
		wuPlus.setOnClickListener(this);
		wuSet.setOnClickListener(this);
		wuGet.setOnClickListener(this);
		settingsHome.setOnClickListener(this);

		butKodas = getIntent().getStringExtra(QR.EXTRA_MESSAGE);

		if (butKodas.equals("settings")) {
			setSettings(true);
		}

		butLiko = app.getData(butKodas);

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

				if(temp.length() == 3) {
					wuValue.setText(temp);
					old = Integer.parseInt(temp);
					newValue = old;
					Log.v("WaterUnits read from Deviec", temp);
				}
				
				if (temp.contains("Begin cycle"))
					setStep(true, 0);
				else if (temp.contains("start: BOTTLE_ATTACH"))
					setStep(true, 1);
				else if (temp.contains("complete: BOTTLE_ATTACH"))
					setStep(true, 2);
				else if (temp.contains("start: FILL_BOTTLE"))
					setStep(true, 3);
				else if (temp.contains("complete: FILL_BOTTLE"))
					setStep(true, 4);
				else if (temp.contains("start: BOTTLE_DETACH"))
					setStep(true, 5);
				else if (temp.contains("complete: BOTTLE_DETACH"))
					setStep(true, 6);

				// serverStatus.append(temp + "\n");
				// jei serveris atsiuntë klaidà
				if (temp.contains("error")) {
					klaida = true;
				}
				if (temp != null && temp.contains("complete: BOTTLE_DETACH") && klaida) {
					setDevError(true);
					fill.setEnabled(true);
					klaida = false;
					setStep(false, 0);
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
					setStep(false, 0);
					return;
				}
				break;
			case 2:
				// serverStatus.setText(msg.getData().getString("message") +
				// "\n");
				break;
			case 3:
				Log.v("FILL", "EXTRA 3 case" + msg.getData().getString("message"));
				fill.setEnabled(true);
				setStep(false, 0);
//				setDevError(true);
				break;
			}
		};
	};

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
			bt.sendToServer(1, "start");
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
		case R.id.wuMinus:
			wuValue.setText(--newValue + "");
			break;
		case R.id.wuPlus:
			wuValue.setText(++newValue + "");
			break;
		case R.id.wuSet:
			// Bluetooth command
			bt.sendToServer(4, "SetWaterUnits "+newValue);
			old = newValue;
			Toast.makeText(getApplicationContext(), "Set to " + newValue, Toast.LENGTH_SHORT).show();
			break;
		case R.id.wuGet:
			bt.sendToServer(4, "GetWaterUnits");			
			break;
		case R.id.settingsBack:
			goHome();
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

	private void setStep(Boolean enable, int step) {
		progress.setVisibility((enable) ? RelativeLayout.VISIBLE : RelativeLayout.GONE);
		for (int i = 1; i <= 6; i++) {
			if (i <= step) {
				steps[i - 1].setImageDrawable(getResources().getDrawable(R.drawable.step_green));
			} else {
				steps[i - 1].setImageDrawable(getResources().getDrawable(R.drawable.step));
			}
		}
	}

	private void setSettings(Boolean enable) {
		settings.setVisibility((enable) ? RelativeLayout.VISIBLE : RelativeLayout.GONE);
	}

	private void goHome() {
		if (this.timer != null)
			this.timer.cancel();

		if (videoView2 != null)
			videoView2.stopPlayback();
		//bt.sendToServer(1, "close");
		
		finish();
		startActivity(new Intent(Fill.this, Main.class));
	}

	private void goQR() {
		if (videoView2 != null)
			videoView2.stopPlayback();
		//bt.sendToServer(1, "close");
		finish();
		startActivity(new Intent(Fill.this, QR.class));
	}

}
