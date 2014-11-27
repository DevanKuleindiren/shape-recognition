package com.devankuleindiren.shapenet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

public class Train extends Activity implements SensorEventListener {
	
	//UI
	private Button circleButton;
	private Button nothingButton;
	private Button saveButton;
	private TextView accelerationNo;
	private static TextView errorDisplay;
	
	private Thread thread2;
	
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
	private static int iVCount = 0;
	
	//NUMBER OF INPUTS, INPUT VECTORS & OUTPUTS
	private static int inputNo = DeepNet.getInputNodesNo();
	private static int iVNo = 100;
	private static int outputNo = 2;
	
	//BETA VALUE AND LEARNING RATE FOR NEURAL NET
	private static double betaValue = 1;
	private static double lR = 0.01;
	
	//ARRAYS TO STORE INPUT VECTORS AND ASSOCIATED TARGETS
	private double[][] inputVectors = new double[iVNo][inputNo];
	private double[][] targets = new double[iVNo][outputNo];
	
	//SAVING
	private static String preferenceKey = "com.devankuleindiren.shapenet.netPref";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_train);
		
		//INITIALISE TARGETS
		resetTargets();
		
		//ACCELERATION
		accelerometerManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = accelerometerManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		accelerometerManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			
		accelerationNo = (TextView) findViewById(R.id.recordingsDisplay);
				
		//ACCELERATION UPDATE AND PROCESSING
		thread2 = new Thread() {
			public void run() {
				for (accelerationCount = 1; accelerationCount < 10000000; accelerationCount++) {
					try {
						thread2.sleep(200);
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
				}	
			}
		};
		thread2.start();
		
		//ERROR DISPLAY
		errorDisplay = (TextView) findViewById(R.id.errorDisplay);
		
		//BUTTONS
		circleButton = (Button) findViewById(R.id.circleButton);
		nothingButton = (Button) findViewById(R.id.nothingButton);
		saveButton = (Button) findViewById(R.id.saveButton);
		
		//ADD INPUT VECTOR ASSOCIATED WITH 'CIRCLE'
		circleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				targets[iVCount][1] = 1;
				addVector();
			}
		});
		
		//ADD INPUT VECTOR ASSOCIATED WITH 'NOTHING'
		nothingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				targets[iVCount][0] = 1;
				addVector();
			}
		});
		
		//SAVE THE WEIGHTS OF THE CURRENT NEURAL NETWORK
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Context context = Train.this;
				SharedPreferences sharedPref = context.getSharedPreferences(
				        preferenceKey, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				for (int i = 0; i < DeepNet.getInputNodesNo(); i++) {
					for (int j = 0; j < DeepNet.getHiddenNeuronNo(); j++) {
						editor.putFloat("weights1_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.getWeight1(i, j));
					}
				}
				for (int i = 0; i < (DeepNet.getHiddenNeuronNo() + 1); i++) {
					for (int j = 0; j < DeepNet.getOutputNeuronNo(); j++) {
						editor.putFloat("weights2_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.getWeight2(i, j));
					}
				}
			    editor.commit();
			    Toast.makeText(Train.this, "Net Saved.", Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	//ADD THE CURRENT INPUT VECTOR TO THE BATCH OF INPUT VECTORS STORED
	private void addVector() {
		
		//CREATE INPUT VECTOR
		for (int i = 0; i < noOfRecords; i++) {
			inputVectors[iVCount][i] = ZAccelerationSaved[i];
		}
		for (int i = 0; i < noOfRecords; i++) {
			inputVectors[iVCount][i+noOfRecords] = XAccelerationSaved[i];
		}
		inputVectors[iVCount][noOfRecords*2] = -1;
		
		//IF THE BATCH OF INPUT VECTORS IS FULL, TRAIN THE NEURAL NETWORK ON THIS BATCH
		if (iVCount < (iVNo - 1)) {
			iVCount++;
		} else {
			iVCount = 0;
			String tempString = "";
			for (int i = 0; i < iVNo; i++) {
				for (int j = 0; j < inputNo; j++) {
					tempString = tempString + Double.toString(inputVectors[i][j]) + ", ";
				}
				Log.d("QX", tempString);
				tempString = "";
			}
			double error = DeepNet.trainNet(inputVectors, targets, lR, betaValue, 1500);
			errorDisplay.setText("Error: " + Double.toString(error));
			resetTargets();
		}
	}
	
	//RESETS TARGETS ARRAY
	private void resetTargets() {
		for (int i = 0; i < targets.length; i++) {
			for (int j = 0; j < targets[0].length; j++) {
				targets[i][j] = 0;
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		//UPDATES THE ACCELERATION IN THE X AND Z AXES
		//Y AXIS NOT USED SINCE APP IS PRIMARILY DESIGNED FOR CIRCLE DETECTION IN THE X-Z PLANE
		XAcceleration = event.values[0];
		ZAcceleration = event.values[2];
		
	}
}
