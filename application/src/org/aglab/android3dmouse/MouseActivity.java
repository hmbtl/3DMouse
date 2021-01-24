package org.aglab.android3dmouse;

import java.math.RoundingMode;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPort;
import com.illposed.osc.OSCPortOut;


public class MouseActivity extends Activity implements SensorEventListener {

	private Button mSendButton;
	private OSCPortOut sender;
	
	private Button mMiddleButton;
	
	private int select = 2;
	
	private int middleState = 0;
	private SensorManager mSensorManager = null;
	
    // angular speeds from gyro
    private float[] gyro = new float[3];
 
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
 
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
 
    // magnetic field vector
    private float[] magnet = new float[3];
 
    // accelerometer vector
    private float[] accel = new float[3];
 
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
 
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
 
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
	private float timestamp;
	private boolean initState = true;
    
	public static final int TIME_CONSTANT = 30;
	public static final float FILTER_COEFFICIENT = 0.98f;
	private Timer fuseTimer = new Timer();
	
	// The following members are only for displaying the sensor output.
	public Handler mHandler;
	DecimalFormat d = new DecimalFormat("#.##");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mouse_2d);
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		
				
		   mMiddleButton = (Button) findViewById(R.id.middleButton);
		   mMiddleButton.setOnClickListener(new View.OnClickListener() {


			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				OSCPortOut sender;
				Object[] args = new Object[1];
				args[0] = middleState; /* key down */
				if (middleState == 0) middleState = 1;
				else middleState = 0;
				OSCMessage msg = new OSCMessage("/middleButton", args);
				try {
					sender = new OSCPortOut(InetAddress.getByName(MouseSettings.ip), OSCPort
							.defaultSCOSCPort());
					sender.send(msg);
				} catch (Exception ex) {
					Log.d("3DMouse", ex.toString());
				}
			}
	 	    });
		
			mSendButton = (Button) findViewById(R.id.sendButton);
			mSendButton.setOnClickListener(new View.OnClickListener() {


				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					new sendMessages().execute();
				}
			});
			
			gyroOrientation[0] = 0.0f;
	        gyroOrientation[1] = 0.0f;
	        gyroOrientation[2] = 0.0f;
	 
	        // initialise gyroMatrix with identity matrix
	        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
	        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
	        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
	 
	        // get sensorManager and initialise sensor listeners
	        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
	        initListeners();
	        
	        // wait for one second until gyroscope and magnetometer/accelerometer
	        // data is initialised then scedule the complementary filter task
	        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
	                                      1000, TIME_CONSTANT);
	        
	        // GUI stuff
	        mHandler = new Handler();
	        d.setRoundingMode(RoundingMode.HALF_UP);
	        d.setMaximumFractionDigits(3);
	        d.setMinimumFractionDigits(3);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.general_menu, menu);
		return true;
	}
	
	@Override
	public void onPause() {
		
		super.onPause();
		mSensorManager.unregisterListener(this);
		this.sender.close();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		initListeners();
		
	}
	
	public void onStop() {
		super.onStop();
		mSensorManager.unregisterListener(this);
		this.sender.close();
	}

	public void onDestroy() {
		super.onDestroy();
		mSensorManager.unregisterListener(this);
		this.sender.close();
	}
	
	
	 public void initListeners(){
	        mSensorManager.registerListener(this,
	            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
	            SensorManager.SENSOR_DELAY_FASTEST);
	     
	        mSensorManager.registerListener(this,
	            mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
	            SensorManager.SENSOR_DELAY_FASTEST);
	     
	        mSensorManager.registerListener(this,
	            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
	            SensorManager.SENSOR_DELAY_FASTEST);
	    }
	

	
	private void sendSensorDataAcce(Object[] args){
		OSCPortOut sender;
		OSCMessage msg = new OSCMessage("/sensorAcce", args);
		try {
			sender = new OSCPortOut(InetAddress.getByName(MouseSettings.ip), OSCPort
					.defaultSCOSCPort());
			sender.send(msg);
		} catch (Exception ex) {
			Log.d("3DMouse", ex.toString());
		}
	}
	
	private void sendSensorDataGyro(Object[] args){
		OSCPortOut sender;
		OSCMessage msg = new OSCMessage("/sensorGyro", args);
		try {
			sender = new OSCPortOut(InetAddress.getByName(MouseSettings.ip), OSCPort
					.defaultSCOSCPort());
			sender.send(msg);
		} catch (Exception ex) {
			Log.d("3DMouse", ex.toString());
		}
	}
	
	private void sendSensorDataFused(Object[] args){
		OSCPortOut sender;
		OSCMessage msg = new OSCMessage("/sensorFused", args);
		try {
			sender = new OSCPortOut(InetAddress.getByName(MouseSettings.ip), OSCPort
					.defaultSCOSCPort());
			sender.send(msg);
		} catch (Exception ex) {
			Log.d("3DMouse", ex.toString());
		}
	}
	
	
	private class sendMessages extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			OSCPortOut sender;
			Object[] args = new Object[3];
			args[0] = 0; /* key down */
			args[1] = "keycode";
			args[2] = "String";
			OSCMessage msg = new OSCMessage("/keyboard", args);
			try {
				sender = new OSCPortOut(InetAddress.getByName(MouseSettings.ip), OSCPort
						.defaultSCOSCPort());
				sender.send(msg);
			} catch (Exception ex) {
				Log.d("3DMouse", ex.toString());
			}
			return null;
		}

	     protected void onProgressUpdate(Integer... progress) {
	     }

	     protected void onPostExecute(Long result) {
	    	 sender.close();
	     }


	 }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		/*float azimuth = event.values[0];
		float pitch = event.values[1];
		float roll = event.values[2];
		Object[] args = new Object[3];
		args[0] = azimuth;
		args[1] = pitch;
		args[2] = roll;
		Sensor sensor = event.sensor;
		System.out.println(" s"+args);*/
		switch(event.sensor.getType()) {
	    case Sensor.TYPE_ACCELEROMETER:
	        // copy new accelerometer data into accel array and calculate orientation
	        System.arraycopy(event.values, 0, accel, 0, 3);
	        calculateAccMagOrientation();
	        break;
	 
	    case Sensor.TYPE_GYROSCOPE:
	        // process gyro data
	        gyroFunction(event);
	        break;
	 
	    case Sensor.TYPE_MAGNETIC_FIELD:
	        // copy new magnetometer data into magnet array
	        System.arraycopy(event.values, 0, magnet, 0, 3);
	        break;
	    }
		
		
	}
	
	public void calculateAccMagOrientation() {
	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
	        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
	    }
	}
	
	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
            float[] deltaRotationVector,
            float timeFactor)
	{
		float[] normValues = new float[3];
		
		// Calculate the angular speed of the sample
		float omegaMagnitude =
		(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
		gyroValues[1] * gyroValues[1] +
		gyroValues[2] * gyroValues[2]);
		
		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > EPSILON) {
		normValues[0] = gyroValues[0] / omegaMagnitude;
		normValues[1] = gyroValues[1] / omegaMagnitude;
		normValues[2] = gyroValues[2] / omegaMagnitude;
		}
		
		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
	
    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;
     
        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }
     
        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
        System.arraycopy(event.values, 0, gyro, 0, 3);
        getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }
     
        // measurement done, save current time for next interval
        timestamp = event.timestamp;
     
        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
     
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
     
        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }
    
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
     
        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);
     
        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
     
        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
     
        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
     
        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
    
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            
            /*
             * Fix for 179° <--> -179° transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
             */
            
            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }
            
            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }
            
            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }
     
            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
            
            
            // update sensor output in GUI
            mHandler.post(updateOreintationDisplayTask);
        }
    }
    
    
     public void updateOreintationDisplay() {
    	 
    	 Object[] args = new Object[3];
    	 select = 2;
	switch(select) {
	case 0:
		args[0] = accMagOrientation[0] * 180/Math.PI ;
		args[1] = accMagOrientation[1] * 180/Math.PI ;
		args[2] = accMagOrientation[2] * 180/Math.PI ;
		sendSensorDataAcce(args);
		break;
	case 1:
		args[0] = gyroOrientation[0] * 180/Math.PI;
		args[1] = gyroOrientation[1] * 180/Math.PI;
		args[2] = gyroOrientation[2] * 180/Math.PI;
		sendSensorDataGyro(args);
		break;
	case 2:
		args[2] = Float.parseFloat(d.format(fusedOrientation[0] * 180/Math.PI));
		args[0] = Float.parseFloat(d.format(fusedOrientation[1] * 180/Math.PI));
		args[1] = Float.parseFloat(d.format(fusedOrientation[2] * 180/Math.PI));
		Log.d("Fusion", args[0] + ":" + args[1] + ":" + args[2]);
		sendSensorDataFused(args);
		
		break;
	}
}
    
    private Runnable updateOreintationDisplayTask = new Runnable() {
		public void run() {
			updateOreintationDisplay();
		}
	};

}
