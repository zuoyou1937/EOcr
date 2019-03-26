package test.com.ocrtest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sikang on 2017/4/21.
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = "CameraView";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewOn;
    //默认预览尺寸
    private int imageWidth = 1920;
    private int imageHeight = 1080;
    //帧率
    private int frameRate = 30;

     private String str = Data.getA();



    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        //设置SurfaceView 的SurfaceHolder的回调函数
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Surface创建时开启Camera
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //设置Camera基本参数
        if (mCamera != null)
            initCameraParams();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            release();
        } catch (Exception e) {
        }
    }

    public boolean isScanning = false;
    private long starTime, endTime;



    /**
     * Camera帧数据回调用
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        //识别中不处理其他帧数据
        if (!isScanning) {
            isScanning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        Log.d("scantest", "-------------Start------------------");
                        starTime = System.currentTimeMillis();
                        //获取Camera预览尺寸
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        int left = (int) (size.width / 2 - getResources().getDimension(R.dimen.x60));
                        int top = (int) (size.height / 2 - getResources().getDimension(R.dimen.x160));
                        int right = (int) (left + getResources().getDimension(R.dimen.x120));
                        int bottom = (int) (top + getResources().getDimension(R.dimen.x320));
                        //将帧数据转为bitmap
                        final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        if (image != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(left, top, right, bottom), getQuality(size.height), stream);
                            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                            endTime = System.currentTimeMillis();
                            Log.d("scantest", "帧数据转Bitmap: " + (endTime - starTime) + "ms");
                            starTime = endTime;
                            if (bmp == null) {
                                isScanning = false;
                                return;
                            }

                            //bmp旋转90度，因为安卓的摄像Camera天生是横的，竖屏拍照后需要将获得的图片旋转90度
                            bmp = rotateToDegrees(bmp, 90);



                            //此处添加：图片二值化、过滤等操作

                            //开始识别
                            TesseractUtil.getInstance().scanOcr(bmp, new SimpleCallback() {
                                @Override
                                public void response(String result) {
                                    endTime = System.currentTimeMillis();
                                    Log.d("scantest", "内容识别: " + (endTime - starTime) + "ms");
                                    starTime = endTime;

                                    //这是区域内扫除的所有内容
                                    Log.d("scantest", "扫描结果:" + result);

                                    //结果result处理，因为有一些字符识别不准确
                                    result = dealResult(result);


                                    Log.d("scantest", "索书号：" + str);
                                    if(result.equals(str))
                                    {
                                        Intent intent = new Intent(getContext(), MainActivity.class);//跳转
                                        getContext().startActivity(intent);
                                        mCamera.release();
                                    }

                                    isScanning = false;
                                    Log.d("scantest", "-------------End------------------");
                                }
                            });
                        }
                    } catch (Exception ex) {
                        Log.d("scantest", ex.getMessage());
                        isScanning = false;
                    }

                }
            }).start();
        }

    }

    //压缩比例
    private int getQuality(int width) {
        int quality = 100;
        if (width > 480) {
            float w = 480 / (float) width;
            quality = (int) (w * 100);
        }
        return quality;
    }


    /**
     * 对result进行处理
     * @param result 识别出的字符串
     */
    public String dealResult(String result)
    {
        String str = result.replace(" ","");//首先将是识别出字符串中的空格去掉
        char[] ch = str.toCharArray();
        if(ch[0] == '1')
        {
            ch[0] = 'I';
        }
        result = String.valueOf(ch);
        return result;
    }


    /**
     * Bitmap裁剪
     *
     * @param bitmap 原图
     * @param width  宽
     * @param height 高
     */
    /*public static Bitmap bitmapCrop(Bitmap bitmap, int left, int top, int width, int height) {
        if (null == bitmap || width <= 0 || height < 0) {
            return null;
        }
        int widthOrg = bitmap.getWidth();
        int heightOrg = bitmap.getHeight();
        if (widthOrg >= width && heightOrg >= height) {
            try {
                bitmap = Bitmap.createBitmap(bitmap, left, top, width, height);
            } catch (Exception e) {
                return null;
            }
        }
        return bitmap;
    }*/


    /**
     * 图片旋转
     *
     * @param tmpBitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                true);
    }


    /**
     * 摄像头配置
     */
    public void initCameraParams() {
        stopPreview();
        Camera.Parameters camParams = mCamera.getParameters();
        List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
        for (int i = 0; i < sizes.size(); i++) {
            if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                imageWidth = sizes.get(i).width;
                imageHeight = sizes.get(i).height;
//                Log.v(TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
                break;
            }
        }
        camParams.setPreviewSize(imageWidth, imageHeight);
        camParams.setPictureSize(imageWidth, imageHeight);
//        Log.v(TAG, "Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

        camParams.setPreviewFrameRate(frameRate);
//        Log.v(TAG, "Preview Framerate: " + camParams.getPreviewFrameRate());

        mCamera.setParameters(camParams);
        //取到的图像默认是横向的，这里旋转90度，保持和预览画面相同
        mCamera.setDisplayOrientation(90);
        // Set the holder (which might have changed) again
        startPreview();
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);//set the surface to be used for live preview
            mCamera.startPreview();
            mCamera.autoFocus(autoFocusCB);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }

    /**
     * 打开指定摄像头
     */
    public void openCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    mCamera = Camera.open(cameraId);
                } catch (Exception e) {
                    if (mCamera != null) {
                        mCamera.release();
                        mCamera = null;
                    }
                }
                break;
            }
        }
    }

    /**
     * 摄像头自动聚焦
     */
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            postDelayed(doAutoFocus, 1000);
        }
    };
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (mCamera != null) {
                try {
                    mCamera.autoFocus(autoFocusCB);
                } catch (Exception e) {
                }
            }
        }
    };

    /**
     * 释放
     */
    public void release() {
        if (isPreviewOn && mCamera != null) {
            isPreviewOn = false;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

}
