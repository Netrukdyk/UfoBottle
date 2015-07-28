package com.bottle;

/* Handleriø naudojami praneðimø kodai: 
 * 0 - Valdomo árenginio praneðimai
 * 1 - Valdymo komandos ið UI
 * 2 - Bluetooth ryðio praneðimai
 * 3 - Kiti praneðimai
 */

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.bottle.R;

@SuppressWarnings("deprecation")
public class QR extends Activity implements OnClickListener {
    public final static String EXTRA_MESSAGE = "message";

	private Button home;
	
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    ImageScanner scanner;

    private boolean previewing = true;
    
    static {
        System.loadLibrary("iconv");
    } 
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        					 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_qr);

        home = (Button) findViewById(R.id.home);
        home.setOnClickListener(this); 
        
        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();
        
        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    // Paspaudus mygtukà, suformuojama þinutë, kuri iðsiunèiama á serverá
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
	        case R.id.home:
	        	finish();
	        	break;
		}
	}
	
	  public void onPause() {
	        super.onPause();
	        releaseCamera();
	    }

	    /** A safe way to get an instance of the Camera object. */
	    public static Camera getCameraInstance(){
	        Camera c = null;
	        try {
	            c = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
	        } catch (Exception e){
	        }
	        return c;
	    }

	    private void releaseCamera() {
	        if (mCamera != null) {
	            previewing = false;
	            mCamera.setPreviewCallback(null);
	            mCamera.release();
	            mCamera = null;
	        }
	    }

	    private Runnable doAutoFocus = new Runnable() {
	            public void run() {
	                if (previewing)
	                    mCamera.autoFocus(autoFocusCB);
	            }
	        };

	    PreviewCallback previewCb = new PreviewCallback() {
	            public void onPreviewFrame(byte[] data, Camera camera) {
	                Camera.Parameters parameters = camera.getParameters();
	                Size size = parameters.getPreviewSize();

	                Image barcode = new Image(size.width, size.height, "Y800");
	                barcode.setData(data);

	                int result = scanner.scanImage(barcode);
	                
	                if (result != 0) {
	                    previewing = false;
	                    mCamera.setPreviewCallback(null);
	                    mCamera.stopPreview();
	                    
	                    SymbolSet syms = scanner.getResults();
	                    for (Symbol sym : syms) {
	        	        	Intent newIntent = new Intent(QR.this, Fill.class);     	        	
	        	        	newIntent.putExtra(EXTRA_MESSAGE, sym.getData());
	        	            startActivity(newIntent);
	                    }
	                }
	            }
	        };

	    // Mimic continuous auto-focusing
	    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
	            public void onAutoFocus(boolean success, Camera camera) {
	                autoFocusHandler.postDelayed(doAutoFocus, 1000);
	            }
	        };
	
}