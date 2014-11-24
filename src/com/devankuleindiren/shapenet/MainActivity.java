package com.devankuleindiren.shapenet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
	Button resetButton;
	Button useNetButton;
	Button trainButton;
	TextView accelerationNo;
	TextView outputDisplay;
	
	static Thread thread;
	
	//ACCELERATION
	Sensor accelerometer;
	SensorManager accelerometerManager;
	double XAcceleration;
	double ZAcceleration;
	private static int noOfRecords = 10;
	double[] XAccelerationSaved = new double[noOfRecords];
	double[] ZAccelerationSaved = new double[noOfRecords];
	
	//TRACKERS
	static int accelerationCount = 0;
	static int nextRecognitionCount = 0;
	static boolean usingNet = false;
	
	//SAVING
	private static String preferenceKey = "com.devankuleindiren.shapenet.netPref";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//INITIALISE WEIGHTS
		DeepNet.initWeights();
		/*BufferedReader reader = null;
		try {
		    reader = new BufferedReader(new InputStreamReader(getAssets().open("trainedWeights.txt")));
		    int i = 0;
		    for (String line = reader.readLine(); line != null; line = reader.readLine(), i++) {
		    	String[] inputs = line.split("\\,");
		    	if (i < DeepNet.inputNodesNo) {
		    		for (int j = 0; j < DeepNet.hiddenNeuronNo; j++) DeepNet.weights1[i][j] = Double.parseDouble(inputs[j]);
		    	} else {
		    		for (int j = 0; j < DeepNet.outputNeuronNo; j++) DeepNet.weights2[i-DeepNet.inputNodesNo][j] = Double.parseDouble(inputs[j]);
		    	}
		    }
		} catch (IOException e) {
		    //log the exception
		} finally {
		    if (reader != null) {
		         try {
		             reader.close();
		         } catch (IOException e) {
		             //log the exception
		         }
		    }
		}*/
		//***
		
		//LOAD NET
		Context context = MainActivity.this;
		SharedPreferences sharedPref = context.getSharedPreferences(
		        preferenceKey, Context.MODE_PRIVATE);
		Log.d("QX", "*1*");
		for (int i = 0; i < DeepNet.inputNodesNo; i++) {
			String tempString = "";
			for (int j = 0; j < DeepNet.hiddenNeuronNo; j++) {
				DeepNet.weights1[i][j] = sharedPref.getFloat("weights1_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.weights1[i][j]);
				tempString = tempString + Double.toString(DeepNet.weights1[i][j]) + ",";
			}
			tempString = tempString.substring(0, tempString.length() - 1);
			Log.d("QX", tempString);
		}
		Log.d("QX", "*2*");
		for (int i = 0; i < (DeepNet.hiddenNeuronNo + 1); i++) {
			String tempString = "";
			for (int j = 0; j < DeepNet.outputNeuronNo; j++) {
				DeepNet.weights2[i][j] = sharedPref.getFloat("weights2_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.weights2[i][j]);
				tempString = tempString + Double.toString(DeepNet.weights2[i][j]) + ",";
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
					
					
					if (usingNet && nextRecognitionCount == 0) {
						double[][] currentOutput = new double[1][2];
						double[][] currentInput = new double[1][DeepNet.inputNodesNo];
						for (int j = 0; j < noOfRecords; j++) {
							currentInput[0][j] = ZAccelerationSaved[j];
						}
						for (int j = 0; j < noOfRecords; j++) {
							currentInput[0][j+noOfRecords] = XAccelerationSaved[j];
						}
						currentInput[0][noOfRecords*2] = -1;
						currentOutput = DeepNet.useNet(currentInput, 1.0);
						currentOutput = DeepNet.rectifyActivations(currentOutput);
						
						if (currentOutput[0][0] == 1.0) {
							outputDisplay.post(new Runnable() {
							    public void run() {
							    	if (outputDisplay.getText() != "NOTHING") {
							    		outputDisplay.setTextColor(Color.parseColor("#0000FF")); 
							    		outputDisplay.setText("NOTHING");
									    nextRecognitionCount = 4;
							    	}
							    } 
							});
						} else if (currentOutput[0][1] == 1.0) {
							outputDisplay.post(new Runnable() {
							    public void run() {
							    	if (outputDisplay.getText() != "CIRCLE") {
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
		XAcceleration = event.values[0];
		ZAcceleration = event.values[2];
	}
}
