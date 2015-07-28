package com.bottle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ErrorDevice extends Activity implements OnClickListener{
	private Button home, back;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        					 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_error_device);
           
        back = (Button)findViewById(R.id.backerror);
        home = (Button)findViewById(R.id.homeerror);
        back.setOnClickListener(this);
        home.setOnClickListener(this);

	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
	        case R.id.homeerror:
	            startActivity(new Intent(ErrorDevice.this, Main.class));
	        	break;
	        case R.id.backerror:
	        	finish();
	        	break;
        	
		}
	}
}
