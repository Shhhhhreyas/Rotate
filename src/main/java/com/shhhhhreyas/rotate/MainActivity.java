package com.shhhhhreyas.rotate;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {

    static BroadcastReceiver receiver;
    LinearLayout orientationChanger;
    Boolean locked = false;
    NotificationManager notificationManager;
    NotificationChannel mChannel;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkDrawOverlayPermission();
        notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        addNotification();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("MainActivity", "onReceive: received");
                setOrientation(intent.getIntExtra("deg",0));
            }
        };
        IntentFilter filter = new IntentFilter("orientation");
        registerReceiver(receiver,filter);
    }

    private void startService(){
        startService(new Intent(this,ShowOverlay.class));
        //startService(new Intent(this,OrientationDetector.class));
        finish();
    }

    public void setOrientation(int rotation){
        orientationChanger = new LinearLayout(this);
        orientationChanger.setClickable(false);
        orientationChanger.setFocusable(false);
        orientationChanger.setFocusableInTouchMode(false);
        orientationChanger.setLongClickable(false);
        // Using TYPE_SYSTEM_OVERLAY is crucial to make your window appear on top
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        WindowManager.LayoutParams orientationLayout = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.RGBA_8888);
        Log.d("MainActivity", "setOrientation: outside "+rotation);
        if(!locked) {
            switch (rotation){
                case 0:
                    orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    Log.d("mainActivity", "setOrientation: Portrait");
                    break;
                case 90:
                    orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    Log.d("mainActivity", "setOrientation: Landscape");
                    break;
                case 270:
                    orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    Log.d("mainActivity", "setOrientation: Reverse Landscape");
                    break;
                case 180:
                    orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    Log.d("mainActivity", "setOrientation: Reverse Portrait");
                    break;
            }

            ShowOverlay.setButtonText("Unlock");
            locked=true;
        }
        else{
            Log.d("mainActivity", "setOrientation: released");
            orientationLayout.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
            ShowOverlay.setButtonText("Lock");
            locked=false;
        }
        WindowManager wm = (WindowManager) this.getSystemService(Service.WINDOW_SERVICE);
        orientationChanger.setVisibility(View.VISIBLE);
        wm.addView(orientationChanger, orientationLayout);
    }

    public final static int REQUEST_CODE = 101;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkDrawOverlayPermission() {
        Log.v("App", "Package Name: " + getApplicationContext().getPackageName());

        // check if we already  have permission to draw over other apps
        if (!Settings.canDrawOverlays(this)) {
            Log.v("App", "Requesting Permission" + Settings.canDrawOverlays(this));
            // if not construct intent to request permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getApplicationContext().getPackageName()));
    // request permission via start activity for result
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            Log.v("App", "We already have permission for it.");
            startService();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_CODE ) {

                Toast.makeText(MainActivity.this,"Please provide the permission",Toast.LENGTH_LONG).show();
                checkDrawOverlayPermission();

            }
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, ex.toString(),
                    Toast.LENGTH_LONG).show();
        }

    }

    private void addNotification() {
        // create the notification
        Notification.Builder m_notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText("Orientation Manager")
                .setSmallIcon(R.drawable.notification)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel("1", "Orientation manager", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(mChannel);
            m_notificationBuilder.setChannelId("1");
        }
        // create the pending intent and add to the notification
        Intent intent = new Intent(this, ShowOverlay.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        m_notificationBuilder.setContentIntent(pendingIntent);

        // send the notification
        notificationManager.notify(1, m_notificationBuilder.build());
    }
}