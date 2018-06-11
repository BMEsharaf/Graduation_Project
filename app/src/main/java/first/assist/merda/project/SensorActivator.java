package first.assist.merda.project;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;

class SensorActivator implements SensorEventListener {
    private static final int THRESHOLD = 10;
    private static final float ALPHA = 0.8f;
    private SensorManager sensorManager;
    private float gravity[] = new float[3];
    private OnActivateSuccessListener mListener;
    private boolean activated = false;

    SensorActivator(Context context, OnActivateSuccessListener mListener) {
        this.mListener = mListener;
        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
    }

    public void unregisterAccelerometer() {
        activated = false;
        sensorManager.unregisterListener(this);
    }

    public void registerAccelerometer() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void deActivate() {
        activated = false;
        Log.i("TAG", "Sensor is deactivated");
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float values[] = event.values.clone();
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            values = highPass(values[0],
                    values[1],
                    values[2]);
            Log.i("values", "x  value : " + values[0]);

            double sumOfSquares = (values[0] * values[0])
                    + (values[2] * values[2]);
            double acceleration = Math.sqrt(sumOfSquares);
            Log.i("values", "Total acceleration value : " + acceleration);
            if (acceleration > THRESHOLD) {
                Log.i("test", "Movement detected");
                mListener.onActivatedService();
                deActivate();
            }
        }
    }

    private float[] highPass(float x, float y, float z) {
        float[] filteredValues = new float[3];
        //Get low pass data by weighting algorithm
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;
        //Subtract low pass filtered data from data to get high pass data
        filteredValues[0] = x - gravity[0];
        filteredValues[1] = y - gravity[1];
        filteredValues[2] = z - gravity[2];
        return filteredValues;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            Log.i("Accuracy", "High accuracy");
        } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            Log.i("Accuracy", "Low accuracy");
        } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
            Log.i("Accuracy", "Medium accuracy");
        }
    }

    public boolean isActivated() {
        return activated;
    }
}