package com.devankuleindiren.shapenet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
	
	//UI
	private Button resetButton;
	private Button useNetButton;
	private Button trainButton;
	private TextView accelerationNo;
	private TextView outputDisplay;

	private static Thread thread;
	
	//ACCELERATION
	private Sensor accelerometer;
	private SensorManager accelerometerManager;
	private double XAcceleration;
	private double ZAcceleration;
	private static int noOfRecords = 10;
	private double[] XAccelerationSaved = new double[noOfRecords];
	private double[] ZAccelerationSaved = new double[noOfRecords];
	
	//TRACKERS
	private static int accelerationCount = 0;
	private static int nextRecognitionCount = 0;
	private static boolean usingNet = false;
	
	//SAVING
	private static String preferenceKey = "com.devankuleindiren.shapenet.netPref";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//INITIALISE WEIGHTS
		DeepNet.initWeights();
		
		//LOAD THE WEIGHTS OF THE LAST SAVED NEURAL NET
		Context context = MainActivity.this;
		SharedPreferences sharedPref = context.getSharedPreferences(
		        preferenceKey, Context.MODE_PRIVATE);
		Log.d("QX", "* 1 *");
		//LOAD THE WEIGHTS BETWEEN THE INPUTS AND HIDDEN LAYER
		for (int i = 0; i < DeepNet.getInputNodesNo(); i++) {
			String tempString = "";
			for (int j = 0; j < DeepNet.getHiddenNeuronNo(); j++) {
				DeepNet.setWeight1(i, j, sharedPref.getFloat("weights1_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.getWeight1(i, j)));
				tempString = tempString + Double.toString(DeepNet.getWeight1(i, j)) + ",";
			}
			tempString = tempString.substring(0, tempString.length() - 1);
			Log.d("QX", tempString);
		}
		Log.d("QX", "* 2 *");
		//LOAD THE WEIGHTS BETWEEN THE INPUTS AND HIDDEN LAYER
		for (int i = 0; i < (DeepNet.getHiddenNeuronNo() + 1); i++) {
			String tempString = "";
			for (int j = 0; j < DeepNet.getOutputNeuronNo(); j++) {
				DeepNet.setWeight2(i, j, sharedPref.getFloat("weights2_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.getWeight2(i, j)));
				tempString = tempString + Double.toString(DeepNet.getWeight2(i, j)) + ",";
			}
			tempString = tempString.substring(0, tempString.length() - 1);
			Log.d("QX", tempString);
		}
		
	    Toast.makeText(MainActivity.this, "Net Loaded.", Toast.LENGTH_SHORT).show();
		
		//TRAIN BUTTON
		trainButton = (Button) findViewById(R.id.trainButton);
		trainButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), Train.class);
				startActivity(intent);
			}
		});
		
		//USENET BUTTON
		useNetButton = (Button) findViewById(R.id.useNet);
		useNetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (usingNet) {
					useNetButton.setText("Use Net");
				} else {
					useNetButton.setText("Stop");
				}
				usingNet = !usingNet;
			}
		});
		
		//RESET BUTTON
		resetButton = (Button) findViewById(R.id.resetButton);
		resetButton.setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View v) {
				accelerationCount = 0;
			}
		});
		
		//OUTPUT DISPLAY
		outputDisplay = (TextView) findViewById(R.id.outputDisplay);
		
		//ACCELERATION
		accelerometerManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = accelerometerManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		accelerometerManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		//ACCELERATION TEXT DISPLAY
		accelerationNo = (TextView) findViewById(R.id.recordingsDisplay);
		
		//ACCELERATION UPDATE AND PROCESSING
		thread = new Thread() {
			public void run() {
				for (accelerationCount = 1; accelerationCount < 10000000; accelerationCount++) {
					try {
						thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
					}
					
					double XTemp = XAcceleration;
					double ZTemp = ZAcceleration;
					
					//ADD ACCELERATION TO STORAGE
					for (int j = (noOfRecords-1); j >= 1; j--) {
						XAccelerationSaved[j] = XAccelerationSaved[j-1];
						ZAccelerationSaved[j] = ZAccelerationSaved[j-1];
					}
					XAccelerationSaved[0] = XTemp;
					ZAccelerationSaved[0] = ZTemp;
					
					//UPDATE RECORDINGS
					accelerationNo.post(new Runnable() {
					    public void run() {
					        accelerationNo.setText("Recordings: " + Integer.toString(accelerationCount));
					    } 
					});
					
					//FEED THE ACCELERATION DATA THROUGH THE NET AT REGULAR INTERVALS (IF IT'S IN USE)
					if (usingNet && nextRecognitionCount == 0) {
						
						//CREATE AN INPUT VECTOR FROM THE ACCELERATION DATA
						double[][] currentOutput = new double[1][2];
						double[][] currentInput = new double[1][DeepNet.getInputNodesNo()];
						for (int j = 0; j < noOfRecords; j++) {
							currentInput[0][j] = ZAccelerationSaved[j];
						}
						for (int j = 0; j < noOfRecords; j++) {
							currentInput[0][j+noOfRecords] = XAccelerationSaved[j];
						}
						currentInput[0][noOfRecords*2] = -1;
						
						//FEED THIS INPUT VECTOR THROUGH THE NET
						currentOutput = DeepNet.useNet(currentInput, 1.0);
						currentOutput = DeepNet.rectifyActivations(currentOutput);
						
						//DISPLAYS 'NOTHING' IF THE FIRST OUTPUT NODE HAS THE GREATEST ACTIVATION
						if (currentOutput[0][0] == 1.0) {
							outputDisplay.post(new Runnable() {
							    public void run() {
							    	if (!(outputDisplay.getText().equals("NOTHING"))) {
							    		outputDisplay.setTextColor(Color.parseColor("#0000FF")); 
							    		outputDisplay.setText("NOTHING");
									    nextRecognitionCount = 4;
							    	}
							    } 
							});
						}
						//DISPLAYS 'CIRCLE' IF THE SECOND OUTPUT NODE HAS THE GREATEST ACTIVATION
						else if (currentOutput[0][1] == 1.0) {
							outputDisplay.post(new Runnable() {
							    public void run() {
							    	if (!(outputDisplay.getText().equals("CIRCLE"))) {
							    		outputDisplay.setTextColor(Color.parseColor("#FF0000"));
							    		outputDisplay.setText("CIRCLE");
								        nextRecognitionCount = 4;
							    	}
							    } 
							});
						}
					}
					
					if (nextRecognitionCount > 0) nextRecognitionCount--;
				}	
			}
		};
		thread.start();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		//UPDATES THE ACCELERATION IN THE X AND Z AXES
		//Y AXIS NOT USED SINCE APP IS PRIMARILY DESIGNED FOR CIRCLE DETECTION IN THE X-Z PLANE
		XAcceleration = event.values[0];
		ZAcceleration = event.values[2];
	}
}
