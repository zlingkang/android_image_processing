package com.example.lingkang.camerademo;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private byte[] 	mVideoSource;
    private Bitmap 	mBackBuffer;
    private int[]	mImgDims;
    private Paint 	mPaint;
    private Paint   mPaintRec;
    private Rect	mSrcRect;
    private Rect	mTrgtRect;
    private int     mChoice;
    private String mDisplayStr;

    public CameraPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);

        setWillNotDraw(false);
        mImgDims = new int[3];
        mImgDims[2] = 4;
        mPaint = new Paint();
        mPaint.setTextSize(64);
        mPaint.setColor(0xFFFF0000);
        mDisplayStr = new String("inspiRED Robotics Demo");
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);
        mPaintRec = new Paint();
        mPaintRec.setColor(Color.RED);
        mPaintRec.setStyle(Paint.Style.STROKE);
        mPaintRec.setStrokeWidth(5);
    }

    private static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            Log.d(TAG, "camera is not available");
        }
        return c;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = getCameraInstance();
        mCamera.setPreviewCallback(this);
        try {
            //mCamera.setPreviewDisplay(holder);
            //mCamera.startPreview();
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewSize(Constants.MAX_DISP_IMG_WIDTH,
                    Constants.MAX_DISP_IMG_HEIGHT);
            params.setPreviewFpsRange(Constants.MIN_FPS,Constants.MAX_FPS);
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(0);
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallbackWithBuffer(this);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        /*
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        */
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            releaseCamera();
            mVideoSource = null;
            mBackBuffer = null;
        }
    }

    /*
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }
*/

    public void surfaceChanged(SurfaceHolder pHolder, int pFormat, int pW, int pH) {

        if (pHolder.getSurface() == null) {
            Log.i(TAG,"No proper holder");
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.i(TAG,"tried to stop a non-existent preview");
            return;
        }
        PixelFormat pxlFrmt = new PixelFormat();
        PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pxlFrmt);
        int srcSize 		= Constants.MAX_DISP_IMG_WIDTH * Constants.MAX_DISP_IMG_HEIGHT * pxlFrmt.bitsPerPixel/8;
        mVideoSource        = new byte[srcSize];
        mBackBuffer         = Bitmap.createBitmap(Constants.MAX_DISP_IMG_WIDTH,
                Constants.MAX_DISP_IMG_HEIGHT,Bitmap.Config.ARGB_8888);
        Camera.Parameters camParams = mCamera.getParameters();
        camParams.setPreviewSize(Constants.MAX_DISP_IMG_WIDTH,
                Constants.MAX_DISP_IMG_HEIGHT);
        camParams.setPreviewFormat(ImageFormat.NV21);
        camParams.setPreviewFpsRange(Constants.MIN_FPS,Constants.MAX_FPS);
        mCamera.setParameters(camParams);

        mImgDims[0] = Constants.MAX_DISP_IMG_WIDTH;
        mImgDims[1] = Constants.MAX_DISP_IMG_HEIGHT;

        mSrcRect	= new Rect(0,0,mImgDims[0],mImgDims[1]);
        mTrgtRect	= pHolder.getSurfaceFrame();

        mCamera.addCallbackBuffer(mVideoSource);

        try {
            mCamera.setPreviewDisplay(pHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "@SurfaceChanged:Error starting camera preview: " + e.getMessage());
        }
    }

    public void setProcessedPreview(int choice) {
        mChoice = choice;
        if (choice==0)
            mDisplayStr = "nv21 to rgba8888";
        else if (choice==1)
            mDisplayStr = "Decode & Laplacian operator";
    }

    /*
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.i(TAG, "processing frame");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    */
    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }

       Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0 , width, 0, 0, width, height);
        return bmp;
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            //runfilter(mBackBuffer,data,mImgDims[0],mImgDims[1],mChoice);
            mBackBuffer = rawByteArray2RGBABitmap2(data, mImgDims[0], mImgDims[1]);
        } catch(Exception e) {
            Log.i(TAG, e.getMessage());
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas pCanvas) {
        if( mCamera != null ) {
            if( mBackBuffer!=null ) {
                pCanvas.drawBitmap(mBackBuffer, mSrcRect, mTrgtRect, null);
                pCanvas.drawText(mDisplayStr, 64, 64, mPaint);
                int left, top, right, bottom;
                left = 100;
                top = 300;
                right = 500;
                bottom = 700;

                pCanvas.drawRect(left, top, right, bottom, mPaintRec);
            }
            mCamera.addCallbackBuffer(mVideoSource);
        }
    }
}
