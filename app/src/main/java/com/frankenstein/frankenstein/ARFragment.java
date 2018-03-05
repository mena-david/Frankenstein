package com.frankenstein.frankenstein;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.nightonke.boommenu.Animation.BoomEnum;
import com.nightonke.boommenu.BoomButtons.BoomButtonBuilder;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;

import java.util.ArrayList;

import static android.content.Context.SENSOR_SERVICE;
import static java.lang.Math.abs;
import static java.lang.Math.toDegrees;


public class ARFragment extends android.app.Fragment {
    private BoomMenuButton mBoomButton;
    //Currently displayed angles
    Float cAzimuth = (float)0.0;
    Float cPitch = (float)0.0;
    Float cRoll = (float)0.0;

    //True angles
    Float azimuth = (float)0.0;
    Float pitch = (float)0.0;
    Float roll = (float)0.0;

    //Sample positions
    Float[] centerN = {(float) 180, (float) 80.0, (float) 0.0,};
    Float[] centerE = {(float) 90, (float) 0.0, (float) 0.0,};
    Float[] centerS = {(float) 0, (float) 0.0, (float) 0.0,};
    Float[] centerW = {(float) 270, (float) 0.0, (float) 0.0,};


    public class CustomDrawableView extends View {
        Rect bounds;
        Paint paint = new Paint();
        Boolean rotating = false;
        //Testing..
        DisplayObject North;
        DisplayObject NorthF;
        int height;
        int width;

        public CustomDrawableView(Context context) {
            super(context);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            height = displayMetrics.heightPixels;
            width = displayMetrics.widthPixels;
            this.North = new DisplayObject(centerN, (int)(width/2), (int)(height/2),
                    (int)(width*.9), (int)(height*.9), 0f,0f,width,height);
            this.NorthF = new DisplayObject(centerN, (int)(width/2), (int)(height/2),
                    (int)(width*.9), (int)(height*.9), 0.0005f,0f,width,height);
            paint.setColor(0xff00ff00);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setTextSize(100);
            paint.setAntiAlias(true);
        };

        protected void onDraw(Canvas canvas) {
            if (azimuth != null) {
                cAzimuth = Global.angleDiff(azimuth,cAzimuth, 60);
                cPitch = Global.angleDiff(pitch,cPitch, 20);
                if(abs(roll-cRoll)>5){
                    rotating = true;
                }
                if(rotating){
                    if(abs(Global.angleDist(roll,90f)) < 5)
                        cRoll = Global.angleDiff(90f, cRoll, 5);
                    cRoll = Global.angleDiff(roll, cRoll, 5);
                    if(abs(roll-cRoll) < 1)
                        rotating = false;
                }
                //Acting weird. Suspended, will ask about removal/readding
                //canvas.rotate(-(cRoll-90), width/2, height/2);
                Rect n = North.getCurrentBound(cAzimuth, cPitch, 0f, 0f);
                Drawable c = North.getImage();
                if(n!=null && c!=null) {
                    Log.d("gb3", ""+n.toString()+c.toString());
                    bounds = n;
                    c.setBounds(n);
                    c.draw(canvas);
                }
                n = NorthF.getCurrentBound(cAzimuth, cPitch, 0f, 0f);
                if(n!=null)
                    canvas.drawRect(n, paint);
                if(abs(azimuth-cAzimuth)> 2 || abs(pitch-cPitch) > 2){
                    mCustomDrawableView.invalidate();
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int touchx = (int)event.getX();
            int touchy = (int)event.getY();
            int i = 0;

            switch(event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    if(bounds != null){
                        Log.d("gb3", ""+touchx+" "+touchy+" "+bounds.toString());
                        if(bounds.left<touchx && bounds.right>touchx
                                && bounds.top<touchy && bounds.bottom>touchy){
                            Toast.makeText(getContext(), "Image selected!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }

            return true;
        }
    }

    CustomDrawableView mCustomDrawableView;
    private Drawable marker;
    private CameraView cameraView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.camera_fragment, container, false);
        super.onCreate(savedInstanceState);
        cameraView = view.findViewById(R.id.camera);
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);
        mCustomDrawableView = new CustomDrawableView(getContext());
        FrameLayout frameLayout = view.findViewById(R.id.frame);
        frameLayout.addView(mCustomDrawableView);

        Bitmap profile = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        Bitmap photo = BitmapFactory.decodeResource(getResources(), R.drawable.puppy);
        String summary = "Cute Puppy";
        if (marker == null) {
            createMarkerThread c = new createMarkerThread(profile, photo, summary);
            c.run();
        } else {
            mCustomDrawableView.North.setImage(marker);
        }
        mBoomButton = getActivity().findViewById(R.id.boombutton_mainAR);
        BoomButtonDisplayMain displayMain = new BoomButtonDisplayMain(mBoomButton, getActivity());
        displayMain.arFragmentDisplay();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraView.destroy();

    }
    public void onSensorChanged(float[] orientation) {
        Log.d("gb", "Arsensor");
        if(abs(azimuth-(toDegrees(orientation[0]))+180) > 6 ||
                abs(roll-(toDegrees(orientation[2]))+180) > 5){
            azimuth = (float)toDegrees(orientation[0])+180;
            pitch = (float)toDegrees(orientation[1])+180;
            roll = (float)toDegrees(orientation[2])+90;
            Log.d("gb", "azimuth: "+azimuth+" pitch: "+pitch+" roll: "+roll);
            if(mCustomDrawableView != null)
                mCustomDrawableView.invalidate();
        }
    }

    public float[] lowPassFilter(float[] in, float[] old){
        float changeRate = (float) .05;
        if(old == null){
            return in;
        }
        for(int i=0; i<old.length; i++){
            old[i] = old[i]+changeRate*(in[i]-old[i]);
        }
        return old;
    }

    private class createMarkerThread extends Thread{
        private Bitmap profile;
        private Bitmap photo;
        private String summary;
        public createMarkerThread(Bitmap profile, Bitmap photo,
                                  String summary){
            this.profile = profile;
            this.photo = photo;
            this.summary = summary;
        }

        public void run() {
            createMarker(profile, photo, summary);
        }
    }

    public void createMarker(Bitmap profile, Bitmap photo, String summary) {
        ImageView markerProfile, markerPhoto;
        TextView markerSummary;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View markerView = inflater.inflate(R.layout.marker, (ViewGroup) getActivity().findViewById(R.id.drawer_layout).getParent());
        final FrameLayout markerLayout = markerView.findViewById(R.id.marker);

        if (markerLayout == null) Log.d("MyApplication", "Layout not found!");
        else Log.d("MyApplication", "Layout found!");

        //Log.d("MyApplication","Width:" + profile.getWidth() + " Height:" + profile.getHeight());
        // Get the views from marker layout
        markerProfile = (ImageView) markerLayout.findViewById(R.id.profile);
        markerPhoto = (ImageView) markerLayout.findViewById(R.id.photo);
        markerSummary = (TextView) markerLayout.findViewById(R.id.summary);
        if (markerProfile != null && markerPhoto != null && markerSummary != null)
            Log.d("MyApplication", "Marker elements found!");

        //Log.d("MyApplication","Width:" + markerProfile.getWidth() + " Height:" + markerProfile.getHeight());
        // Set contents accordingly
        profile = Bitmap.createScaledBitmap(profile, width / 3, height / 4, false);
        //Log.d("MyApplication", "Width:" + profile.getWidth() + " Height:" + profile.getHeight());
        markerProfile.setImageBitmap(profile);
        //Log.d("MyApplication", "Width:" + markerProfile.getWidth() + " Height:" + markerProfile.getHeight());
        markerPhoto.setImageBitmap(Bitmap.createScaledBitmap(photo, width, height * 3 / 4, false));
        markerSummary.setText(summary);

        // Get bitmap representation of marker layout
        markerLayout.post(new Runnable() {
            public void run() {
                markerLayout.setDrawingCacheEnabled(true);
                markerLayout.buildDrawingCache();
                Bitmap markerBM = markerLayout.getDrawingCache();
                markerLayout.removeAllViews();
                marker = new BitmapDrawable(getResources(), markerBM);
                /*Rect bounds = new Rect(0,0,2000,2000);
                marker.setBounds(bounds);*/
                mCustomDrawableView.North.setImage(marker);
                mCustomDrawableView.invalidate();
                Log.d("MyApplication","Executed!");
            }
        });
        Log.d("My", "TEsting");
    }
}