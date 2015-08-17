/* Handlerių naudojami pranešimų kodai: 
 * 0 - Valdomo įrenginio pranešimai
 * 1 - Valdymo komandos iš UI
 * 2 - Bluetooth ryšio pranešimai
 * 3 - Kiti pranešimai
 */
package com.bottle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class BTConnection extends Thread {

	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;

	int server_state = 0;
	private Handler uiHandler;

	public void sethandler(Handler uiHandler) {
		this.uiHandler = uiHandler;		
	}

	// Serverio Handleris, apdoroja žinutes iš UI
	@SuppressLint("HandlerLeak")
	private Handler serverHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				// Log.v("Server", msg.getData().getString("command"));
				try {
					switch (msg.getData().getString("command")) {
					case "start":
						sendData();
						break;
					case "close":
						closeBT();
						break;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if(msg.what == 4){
				try {		
					String temp = msg.getData().getString("command");
					Log.v("BT 4 command",temp);
					if(temp.contains("GetWaterUnits")){
						sendGetWawterUnits();
					}
					else if(temp.contains("SetWaterUnits"))
						changeUnits(temp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
	};

	private void sendData() throws IOException {
		final Runnable r = new Runnable() {
			public void run() {
				String msg = "start\n";
				try {
					while (server_state != 1) {
						sleep(200);
					}

					if (mmOutputStream != null) {
						mmOutputStream.write(msg.getBytes());
						Log.v("Command", "start");
						sendToUI(2, "Data Sent");
					} else {
						Log.v("Bluetooth", "No output stream");
						sendToUI(3, "Bluetooth error");
					}
				} catch (IOException | InterruptedException e) {
					sendToUI(2, "Įvyko klaida. IOException | Interupted");	
					reconnect();
					e.printStackTrace();
				}
			}
		};
		serverHandler.post(r);
	}
	
	private void sendGetWawterUnits() throws IOException {
		final Runnable r = new Runnable() {
			public void run() {
				try {
					while (server_state != 1) {
						sleep(200);
					}
					String msg = "GetWaterUnits\n";
					if (mmOutputStream != null) {
						Log.v("Bluetooth", "Sending...");
						mmOutputStream.write(msg.getBytes());
						mmOutputStream.flush();
						Log.v("Bluetooth", "Sent Get water units");
						sendToUI(2, "Data Sent");						
					} else {
						Log.v("Bluetooth", "No output stream");
						sendToUI(3, "Bluetooth error");
					}
				} catch (IOException | InterruptedException e) {
					sendToUI(2, "Įvyko klaida. IOException | Interupted");
					e.printStackTrace();
				}
			}
		};
		serverHandler.post(r);
	}
	
	private void changeUnits(final String msg) throws IOException {
		final Runnable r = new Runnable() {
			public void run() {
				try {
					while (server_state != 1) {
						sleep(200);
					}
					if (mmOutputStream != null) {
						mmOutputStream.write(msg.getBytes());
						Log.v("Command", "start");
						sendToUI(2, "Data Sent");
					} else {
						Log.v("Bluetooth", "No output stream");
						sendToUI(3, "Bluetooth error");
					}
				} catch (IOException | InterruptedException e) {
					sendToUI(2, "Ávyko klaida. IOException | Interupted");
					e.printStackTrace();
				}
			}
		};
		serverHandler.post(r);
	}
	private void closeBT() throws IOException {
		stopWorker = true;
		if (mmOutputStream != null)
			mmOutputStream.close();
		if (mmInputStream != null)
			mmInputStream.close();
		if (mmSocket != null)
			mmSocket.close();
		
		
		sendToUI(2, "Bluetooth Closed");
		server_state = 0;
		Log.v("Bluetooth", "Closed");
	}

	// suformuoja pranešimą ir išsiunčia serveriui
	public void sendToServer(int what, String msgText) {
		Bundle b = new Bundle();
		b.putString("command", msgText);
		Message msg = new Message();
		msg.what = what;
		msg.setData(b);
		serverHandler.sendMessage(msg);
	}

	private void findBT() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Log.v("Bluetooth", "BT adapter not found");
			sendToUI(2, "No bluetooth adapter available");
			return;
		}

		setBluetooth(true);

		Log.v("Bluetooth", "Looking for paired device...");
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				if (device.getName().equals("RepRap")) {
					mmDevice = device;
					mBluetoothAdapter.cancelDiscovery();
					return;
				}
			}
		}
	}

	private void connect() {
		server_state = 0;
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

		if (mmDevice != null & mmDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
			try {
				if (mmSocket != null) {
					mmSocket.close();
					mmSocket = null;
				}
				mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
				// mmSocket = (BluetoothSocket)
				// mmDevice.getClass().getMethod("createRfcommSocket", new
				// Class[] {int.class}).invoke(mmDevice,1);

			} catch (IOException e1) {
				Log.d("Bluetooth", "socket not created");
				e1.printStackTrace();
				return;
			}

			try {
				// Log.v("BT", mmSocket.isConnected()+"");
				mmSocket.connect();
				mmOutputStream = mmSocket.getOutputStream();
				mmInputStream = mmSocket.getInputStream();
			} catch (IOException e) {
				try {
					Log.d("Bluetooth", "Socket cannot connect");
					//sendToUI(3, "Bluetooth error8888");
					closeBT();
					sleep(2000);
				} catch (IOException | InterruptedException e1) {
					Log.d("Bluetooth", "Socket not closed");
					e1.printStackTrace();
				}
				e.printStackTrace();
				return; // kai įvyksta klaida reikia nutraukti metodą, nes
						// vėliau
						// nustatoma teigiama serverio būsena
			}
			server_state = 1;
			Log.v("Bluetooth", "Connected");
			sendToUI(2, "Bluetooth connected");

		} else {
			Log.v("Bluetooth", "No bounded device");
		}
	}
	
	private void reconnect(){
		try {
			sendToUI(3, "Bluetooth error");
			closeBT();
			connect();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		Looper.prepare();

		findBT();

		do
			connect();
		while (server_state != 1);

		final Handler handler = new Handler();
		final byte delimiter = 10; // This is the ASCII newline character

		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		workerThread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = mmInputStream.available();
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);
							for (int i = 0; i < bytesAvailable; i++) {
								byte b = packetBytes[i];
//								Log.v("TEST", b+"");
								if (b == delimiter) {
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
									final String data = new String(encodedBytes, "US-ASCII");
									readBufferPosition = 0;

									handler.post(new Runnable() {
										public void run() {
											Log.v("Water Station", data);
											sendToUI(0, data);
										}
									});
								} else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} catch (IOException ex) {
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
		Looper.loop();
	}

	// Metodas tam, kad gautume serverio handlerį
	public Handler getHandler() {
		return serverHandler;
	}

	// Suformuoja žinutę ir išsiunčia UI
	private void sendToUI(int what, String msgText) {
		Bundle b = new Bundle();
		b.putString("message", msgText);
		Message msg = new Message();
		msg.what = what;
		msg.setData(b);
		if(uiHandler != null)
			uiHandler.sendMessage(msg);
	}

	private boolean setBluetooth(boolean enable) {
		boolean isEnabled = mBluetoothAdapter.isEnabled();
		if (enable && !isEnabled) {
			return mBluetoothAdapter.enable();
		} else if (!enable && isEnabled) {
			return mBluetoothAdapter.disable();
		}
		// No need to change bluetooth state
		return true;
	}


} // End of BTConnection