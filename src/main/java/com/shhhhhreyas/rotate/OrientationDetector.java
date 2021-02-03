package com.shhhhhreyas.rotate;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.OrientationEventListener;

import androidx.annotation.Nullable;

public class OrientationDetector extends Service {

    OrientationEventListener mOrientationListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mOrientationListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int i) {
                //Log.d("OrientationDetector", "onOrientationChanged: "+i);
                if(i>=-5 && i<=5)
                    sendBroadcast(new Intent("rotated").putExtra("deg",0));
                if(i>=88 && i<=92)
                    sendBroadcast(new Intent("rotated").putExtra("deg",90));
                if(i>=175 && i<=185)
                    sendBroadcast(new Intent("rotated").putExtra("deg",180));
                if(i>=268 && i<=272)
                    sendBroadcast(new Intent("rotated").putExtra("deg",270));
            }
        };
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }
}
