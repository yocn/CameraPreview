package com.yocn.tui;

import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback, SurfaceHolder.Callback, View.OnTouchListener {
    Camera camera;
    SurfaceView sv_main;
    SurfaceHolder holder;
    Camera.Parameters parameters;
    private int camWidth = 320;
    private int camHeight = 240;
    byte[] h264 = new byte[camWidth * camHeight * 3 / 2];
    Encoder avcCodec;
    int framerate = 20;
    int bitrate = 2500000;
    FileOutputStream file_out = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        avcCodec = new Encoder(camWidth, camHeight, framerate, bitrate);
        initSurfaceView();
    }

    /**
     * 初始化surfaceView
     */
    private void initSurfaceView() {
        sv_main = (SurfaceView) findViewById(R.id.sv_main);
        sv_main.setOnTouchListener(this);
        holder = sv_main.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * 开始camera的预览
     */
    private void initCamera() {
        /**设置了一堆属性并给camera设置这些属性*/
        parameters = camera.getParameters();
        parameters.setFlashMode("off"); // 无闪光灯
        parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPictureSize(camWidth, camHeight);
        parameters.setPreviewSize(camWidth, camHeight);
        //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错
        camera.setParameters(parameters);
        setOrientation();

        byte[] buf = new byte[camWidth * camHeight * 3 / 2];
        camera.addCallbackBuffer(buf);
        camera.setPreviewCallback(this);
    }

    private void setOrientation() {
        // 横竖屏镜头自动调整
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait"); //
            parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
            camera.setDisplayOrientation(90); // 在2.2以上可以使用
        } else {// 如果是横屏
            parameters.set("orientation", "landscape"); //
            camera.setDisplayOrientation(0); // 在2.2以上可以使用
        }
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }

    /**
     * 释放掉camera的引用
     */
    private void releaseCamera() {
        if (camera != null) {
            this.camera.setPreviewCallback(null);
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        /**Camera.PreviewCallback 的override*/
        if (data == null) {
            return;
        }
        try {
            if (file_out == null) {
                file_out = new FileOutputStream("/sdcard/test.yuv", false);
            }
            file_out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        int ret = avcCodec.offerEncoder(data, h264);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        /**SurfaceHolder.Callback 的override*/
        System.out.println("surfaceCreated");
        camera = Camera.open();
        initCamera();
        try {
            file_out = new FileOutputStream("/sdcard/test.yuv", false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /**SurfaceHolder.Callback 的override*/
        System.out.println("surfaceChanged");
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            System.out.println("startPreview");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        /**SurfaceHolder.Callback 的override*/
        System.out.println("surfaceDestroyed");
        try {
            if (file_out != null) {
                file_out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {//按下时自动对焦
            camera.autoFocus(null);
        }
        return true;
    }
}
