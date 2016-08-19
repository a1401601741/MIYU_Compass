package com.example.zhangchen.miyu_compass;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhangchen.miyu_compass.view.CompassView;
import com.umeng.analytics.MobclickAgent;

import java.util.Locale;

//import com.umeng.analytics.MobclickAgent;


public class CompassActivity extends Activity {
    private static final int EXIT_TIME = 2000;
    private static final float MAX_ROTATE_DEGREE = 1.0f;
    private SensorManager sensorManager;
    private Sensor orientSensor;
    private LocationManager locationManager;
    private String locationProvider;
    private float direction;
    private float targetDirection;
    private AccelerateInterpolator interpolator;
    protected final Handler handler = new Handler();
    private boolean stopDrawing;
    private boolean chinese;
    private long firstExitTime = 0L;

    CompassView pointer;

    private TextView tv_degree;
    private TextView tv_latitude;
    private TextView tv_longitude;
    private TextView tv_pressure;
    private TextView tv_altitude;

    protected Runnable compassViewUpdate = new Runnable() {
        @Override
        public void run() {
            if (pointer != null && !stopDrawing) {
                if (direction != targetDirection) {
                    float to = targetDirection;
                    if (to - direction > 180) {
                        to -= 360;
                    } else if (to - direction < -180) {
                        to += 360;
                    }

                    float distance = to - direction;
                    if (Math.abs(distance) > MAX_ROTATE_DEGREE) {
                        distance = distance > 0 ? MAX_ROTATE_DEGREE : (-1.0f * MAX_ROTATE_DEGREE);
                    }

                    direction = normalizeDegree(direction + ((to - direction) * interpolator
                            .getInterpolation(Math.abs(distance) > MAX_ROTATE_DEGREE ? 0.4f : 0.3f)));

                    pointer.updateDirection(direction);
                }
                updateDirection();
                handler.postDelayed(compassViewUpdate, 20);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        initResource();
        initService();
    }

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - firstExitTime < EXIT_TIME) {
            finish();
        } else {
            Toast.makeText(this, "连续按两次返回键退出应用", Toast.LENGTH_SHORT).show();
            firstExitTime = currentTime;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        if (locationProvider != null) {
            updateLocation(locationManager.getLastKnownLocation(locationProvider));
            locationManager.requestLocationUpdates(locationProvider, 2000, 10, locationListener);
        } else {
            tv_latitude.setText(R.string.cannot_get_location);
        }

        if (orientSensor != null) {
            sensorManager.registerListener(orientationSensorEventListener, orientSensor,
                    SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(this, "此设备没有相关传感器", Toast.LENGTH_SHORT).show();
        }
        stopDrawing = false;
        handler.postDelayed(compassViewUpdate, 20);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        stopDrawing = true;
        if (orientSensor != null) {
            sensorManager.unregisterListener(orientationSensorEventListener);
        }
        if (locationProvider != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void initResource() {
        direction = 0.0f;
        targetDirection = 0.0f;
        interpolator = new AccelerateInterpolator();
        stopDrawing = true;
        pointer = (CompassView) findViewById(R.id.view_compass);
        chinese = TextUtils.equals(Locale.getDefault().getLanguage(), "zn");
        tv_degree = (TextView) findViewById(R.id.tv_degree);
        tv_latitude = (TextView) findViewById(R.id.tv_latitude);
        tv_longitude = (TextView) findViewById(R.id.tv_longitude);
        tv_pressure = (TextView) findViewById(R.id.tv_pressure);
        tv_altitude = (TextView) findViewById(R.id.tv_altitude);
    }

    private void initService() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.ACCURACY_FINE);
        locationProvider = locationManager.getBestProvider(criteria, true);
    }

    private void updateDirection() {
        String east = "";
        String west = "";
        String south = "";
        String north = "";
        StringBuilder finalDirection = new StringBuilder();
        float direction = normalizeDegree(targetDirection * -1.0f);
        if (direction > 22.5f && direction < 157.5f) {
            east = "东";
        } else if (direction > 202.5f && direction < 337.5) {
            west = "西";
        }
        if (direction > 112.5f && direction < 247.5f) {
            south = "南";
        } else if (direction < 67.5f || direction > 292.5f) {
            north = "北";
        }

        if (east != "") {
            finalDirection.append(east);
        }
        if (west != "") {
            finalDirection.append(west);
        }
        if (north != "") {
            finalDirection.append(north);
        }
        if (south != "") {
            finalDirection.append(south);
        }
        finalDirection.append("     ");

        int finalDegree = (int) direction;
        boolean show = false;
        if (finalDegree > 100) {
            finalDirection.append(String.valueOf(finalDegree / 100));
            finalDegree %= 100;
            show = true;
        }
        if (finalDegree >= 10 || show) {
            finalDirection.append(String.valueOf(finalDegree / 10));
            finalDegree %= 10;
        }
        finalDirection.append(String.valueOf(finalDegree) + "°");

        tv_degree.setText(finalDirection.toString());
    }

    private void updateLocation(Location location) {
        if (location == null) {
            tv_latitude.setText("正在获取位置……");
        } else {
            StringBuilder sb_latitude = new StringBuilder();
            StringBuilder sb_longitude = new StringBuilder();
            StringBuilder sb_altitude = new StringBuilder();
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            double altitude = location.getAltitude();
            Log.i("海拔", String.valueOf(altitude));
            Log.i("纬度", String.valueOf(latitude));
            Log.i("经度", String.valueOf(longitude));
            if (latitude > 0.0f) {
                sb_latitude.append(getLocationString(latitude));
            } else {
                sb_latitude.append(
                        getLocationString(-1.0f * latitude));
            }

//            sb_latitude.append("     ");
            if (longitude > 0.0f) {
                sb_longitude.append(
                        getLocationString(longitude));
            } else {
                sb_longitude.append(
                        getLocationString(-1.0f * longitude));
            }
            tv_latitude.setText(sb_latitude.toString());
            tv_longitude.setText(sb_longitude.toString());
            tv_altitude.setText(String.valueOf(altitude));
        }
    }

    private String getLocationString(double input) {
        int du = (int) input;
        int fen = (int) ((input - du) * 3600) / 60;
        int miao = ((int) ((input - du) * 3600)) % 60;
        return String.valueOf(du) + "°" + String.valueOf(fen) + "'" + String.valueOf(miao) + "″";
    }

    private float normalizeDegree(float degree) {
        return (degree + 720) % 360;
    }

    private SensorEventListener orientationSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float direciton = event.values[0] * -1.0f;
            targetDirection = normalizeDegree(direciton);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status != LocationProvider.OUT_OF_SERVICE) {
                updateLocation(locationManager.getLastKnownLocation(locationProvider));
            } else {
                tv_latitude.setText(R.string.cannot_get_location);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
}
