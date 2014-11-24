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
	
	Button circleButton;
	Button nothingButton;
	Button saveButton;
	TextView accelerationNo;
	public static TextView errorDisplay;
	
	Thread thread2;
	
	//ACCELERATION
	Sensor accelerometer;
	SensorManager accelerometerManager;
	double XAcceleration;
	double ZAcceleration;
	private static int noOfRecords = 10;
	double[] XAccelerationSaved = new double[noOfRecords];
	double[] ZAccelerationSaved = new double[noOfRecords];
	
	static int accelerationCount = 0;
	static int iVCount = 0;
	
	static int inputNo = 21;
	static int iVNo = 100;
	static int outputNo = 2;
	static double betaValue = 1;
	static double lR = 0.01;
	
	double[][] inputVectors = new double[iVNo][inputNo];
	double[][] targets = new double[iVNo][outputNo];
	
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
		
		circleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				targets[iVCount][1] = 1;
				addVector();
			}
		});
		
		nothingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				targets[iVCount][0] = 1;
				addVector();
			}
		});
		
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Context context = Train.this;
				SharedPreferences sharedPref = context.getSharedPreferences(
				        preferenceKey, Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPref.edit();
				for (int i = 0; i < DeepNet.inputNodesNo; i++) {
					for (int j = 0; j < DeepNet.hiddenNeuronNo; j++) {
						editor.putFloat("weights1_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.weights1[i][j]);
					}
				}
				for (int i = 0; i < (DeepNet.hiddenNeuronNo + 1); i++) {
					for (int j = 0; j < DeepNet.outputNeuronNo; j++) {
						editor.putFloat("weights2_" + Integer.toString(i) + Integer.toString(j), (float)DeepNet.weights2[i][j]);
					}
				}
			    editor.commit();
			    Toast.makeText(Train.this, "Net Saved.", Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public void addVector() {
		for (int i = 0; i < noOfRecords; i++) {
			inputVectors[iVCount][i] = ZAccelerationSaved[i];
		}
		for (int i = 0; i < noOfRecords; i++) {
			inputVectors[iVCount][i+noOfRecords] = XAccelerationSaved[i];
		}
		inputVectors[iVCount][noOfRecords*2] = -1;
		
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
	
	public void resetTargets() {
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
		XAcceleration = event.values[0];
		ZAcceleration = event.values[2];
		
	}
}
