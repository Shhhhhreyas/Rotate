package com.shhhhhreyas.rotate;


import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.Toast;

public class ShowOverlay extends Service implements OnTouchListener, OnClickListener {
    private View topLeftView;

    private static Button overlayedButton;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;
    private WindowManager wm;
    private int rotation=0;
    private static boolean locked=false;
    NotificationManager notificationManager;
    NotificationChannel mChannel;


    BroadcastReceiver receiver;
    Runnable r;
    Handler handler = new Handler();

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
        startService(new Intent( this,OrientationDetector.class));
        notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        //addNotification();

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("Kill")
                .setMessage("Are you sure you want to kill the lock?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onDestroy();
                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        overlayedButton = new Button(this);
        float factor = this.getResources().getDisplayMetrics().density;
        GradientDrawable gd =  new GradientDrawable();
        gd.setCornerRadius(18*factor);
        gd.setColor(Color.BLACK);
        gd.setAlpha(120);
        overlayedButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock,0,0,0);
        int padding = (int)(6*factor);
        overlayedButton.setPadding(padding,padding,padding,padding);
        overlayedButton.setOnTouchListener(this);
        overlayedButton.setOnClickListener(this);
        overlayedButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog alert = alertDialog.create();
                int LAYOUT_FLAG;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                }
                alert.getWindow().setType(LAYOUT_FLAG);
                alert.show();

                return true;
            }
        });
        overlayedButton.setBackground(gd);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.height=(int)(36*factor);
        params.width=(int)(36*factor);
        wm.addView(overlayedButton, params);


        topLeftView = new View(this);

        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, LAYOUT_FLAG, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        topLeftParams.gravity = Gravity.CENTER;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
        topLeftParams.width = 0;
        topLeftParams.height = 0;
        wm.addView(topLeftView, topLeftParams);

        overlayedButton.setVisibility(View.INVISIBLE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                rotation = intent.getIntExtra("deg",0);
                Log.d("ShowOverlay", "onReceive: received "+rotation);
                if(rotation==0 && !locked) return;
                overlayedButton.setVisibility(View.VISIBLE);
                r = new Runnable() {
                    public void run() {
                        overlayedButton.setVisibility(View.INVISIBLE);
                    }
                };
                handler.removeCallbacks(r);
                overlayedButton.setVisibility(View.VISIBLE);
                handler.postDelayed(r, 3000);
            }
        };
        IntentFilter filter = new IntentFilter("rotated");
        registerReceiver(receiver,filter);

        Log.d("ShowOverlay", "onCreate: created");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("service", "onDestroy: called");
        if (overlayedButton != null) {
            wm.removeView(overlayedButton);
            wm.removeView(topLeftView);
            overlayedButton = null;
            topLeftView = null;
            Log.d("service", "onDestroy: destroyed");
        }
        unregisterReceiver(receiver);
        System.exit(0);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        handler.removeCallbacks(r);
        overlayedButton.setVisibility(View.VISIBLE);
        handler.postDelayed(r, 3000);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getRawX();
            float y = event.getRawY();

            moving = false;

            int[] location = new int[2];
            overlayedButton.getLocationOnScreen(location);

            originalXPos = location[0];
            originalYPos = location[1];

            offsetX = originalXPos - x;
            offsetY = originalYPos - y;

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

            System.out.println("topLeftY="+topLeftLocationOnScreen[1]);
            System.out.println("originalY="+originalYPos);

            float x = event.getRawX();
            float y = event.getRawY();

            WindowManager.LayoutParams params = (LayoutParams) overlayedButton.getLayoutParams();

            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);

            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }

            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);

            wm.updateViewLayout(overlayedButton, params);
            moving = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            return moving;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Orientation "+(locked?"unlocked":"locked"), Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(r);
        overlayedButton.setVisibility(View.VISIBLE);
        handler.postDelayed(r, 3000);
        sendBroadcast(new Intent("orientation").putExtra("deg",rotation));

    }



    public static void setButtonText(String text) {
        if(overlayedButton==null) return;
        locked = text.equals("Unlock");
        if (text.equals("Unlock")) {
            overlayedButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.unlock, 0, 0, 0);
        } else {
            overlayedButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, 0, 0);
        }
    }
}
