package com.bottle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ServiceBluetooth extends IntentService {

	private BluetoothAdapter mBluetoothAdapter;
	public static BluetoothDevice mDevice;

	public static final String BT_DEVICE = "RepRap";
	public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
	public static final int STATE_CONNECTED = 3; // now connected to a remote
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	// public mInHangler mHandler = new mInHangler(this);
	// private static Handler mHandler = null;
	public static int mState = STATE_NONE;

	public ServiceBluetooth() {
		super("UfoBottleService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

		findDevice(); // enable BT, find device, connect

		return START_STICKY;
	}

	private synchronized void findDevice() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter != null) {

			if (!mBluetoothAdapter.isEnabled())
				mBluetoothAdapter.enable();

			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			for (BluetoothDevice device : pairedDevices)
				if (device.getName().equals("RepRap")) {
					mDevice = device;
					break;
				}

			if (mDevice != null)
				connectToDevice(mDevice);
			else {
				stopSelf();
				return;
			}
		}
	}

	private synchronized void connectToDevice(BluetoothDevice device) {
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	private synchronized void connected(BluetoothSocket mmSocket) {
		// Cancel the thread that completed the connection
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mConnectedThread = new ConnectedThread(mmSocket);
		mConnectedThread.start();

		setState(STATE_CONNECTED);
		Log.v("BT", "Connected");
		toast("Connected");

	}

	private void setState(int state) {
		ServiceBluetooth.mState = state;
		Log.v("BT", String.valueOf(state));		
		// if (mHandler != null) {
		// mHandler.obtainMessage(AbstractActivity.MESSAGE_STATE_CHANGE, state,
		// -1).sendToTarget();
		// }
	}
	private void toast(String text){
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();	
	}
	

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;

		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;
			try {
				tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
			} catch (IOException e) {
			}
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				Log.v("BT", "Try to connect");
				mmSocket.connect();
				
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
					Log.v("BT", "Socket closed");
				} catch (IOException closeException) {
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			connected(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer);
					// Send the obtained bytes to the UI activity
					Log.v("BT", String.valueOf(bytes));
					// mHandler.obtainMessage(MESSAGE_READ, bytes, -1,
					// buffer).sendToTarget();
				} catch (IOException e) {
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "service stoped", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		synchronized (this) {

		}
	}

}
