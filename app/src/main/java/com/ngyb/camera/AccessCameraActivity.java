package com.ngyb.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 作者：南宫燚滨
 * 描述：接入摄像头的图像
 * 邮箱：nangongyibin@gmail.com
 * 日期：2020/7/30 15:00
 */
public class AccessCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "AccessCameraActivity";
    private SurfaceView sfv1, sfv2;
    private SurfaceHolder surfaceHolder1;
    private SurfaceHolder surfaceHolder2;
    private Camera camera1, camera2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_camera);
        init();
    }

    private void init() {
        initView();
        initImage();
    }

    private void initImage() {
        //检查是否有存储权限，以免崩溃
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            Toast.makeText(this, "请开启存储或相机的权限", Toast.LENGTH_SHORT).show();
            return;
        }
        // 后置
        surfaceHolder1 = sfv1.getHolder();
        surfaceHolder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder1.addCallback(this);
        //前置
        surfaceHolder2 = sfv2.getHolder();
        surfaceHolder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder2.addCallback(this);
    }

    private void initView() {
        sfv1 = findViewById(R.id.sfv1);
        sfv2 = findViewById(R.id.sfv2);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (holder == surfaceHolder1) {
                //获取camera对象
                int cameraCount = Camera.getNumberOfCameras();
                if (cameraCount > 0) {
                    //后置摄像头
                    camera1 = Camera.open(0);
                    //设置预览监听
                    camera1.setPreviewDisplay(holder);
                    Camera.Parameters parameters01 = camera1.getParameters();
                    if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        parameters01.set("orientation", "portrait");
                        camera1.setDisplayOrientation(90);
                        parameters01.setRotation(90);
                    } else {
                        parameters01.set("orientation", "landscape");
                        camera1.setDisplayOrientation(0);
                        parameters01.setRotation(0);
                    }
                    camera1.setParameters(parameters01);
                    //启动摄像头预览
                    camera1.startPreview();
                }
            } else if (holder == surfaceHolder2) {
                //获取camera对象
                int cameraCount = Camera.getNumberOfCameras();
                if (cameraCount > 1) {
                    //前置摄像头
                    camera2 = Camera.open(1);
                    //设置预览监听
                    camera2.setPreviewDisplay(holder);
                    Camera.Parameters parameters02 = camera2.getParameters();
                    if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        parameters02.set("orientation", "portrait");
                        camera2.setDisplayOrientation(90);
                        parameters02.setRotation(90);
                    } else {
                        parameters02.set("orientation", "landscape");
                        camera2.setDisplayOrientation(0);
                        parameters02.setRotation(0);
                    }
                    camera2.setParameters(parameters02);
                    //启动摄像头预览
                    camera2.startPreview();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder == surfaceHolder1) {
            Log.e(TAG, "surfaceChanged: surfaceHolder1");
            camera1.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        initCamera(camera);//实现相机的参数初始化
                        camera.cancelAutoFocus();//只有加上这一句，才会自动对焦
                    }
                }
            });
        } else if (holder == surfaceHolder2) {
            Log.e(TAG, "surfaceChanged: surfaceHolder2");
            camera2.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
//                        initCamera(camera);//实现相机的参数初始化
                        camera.cancelAutoFocus();//只有加上这一句，才会自动对焦
                    }
                }
            });
        }
    }

    /**
     * 相机参数的初始化设置
     *
     * @param sendCamera
     */
    private void initCamera(Camera sendCamera) {
        Camera.Parameters sendParameters = sendCamera.getParameters();
        sendParameters.setPictureFormat(PixelFormat.JPEG);
        sendParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//连续对焦
        setDisplay(sendParameters, sendCamera);
        sendCamera.setParameters(sendParameters);
        sendCamera.startPreview();
        sendCamera.cancelAutoFocus();//如果要实现连续的自动对焦，这一句必须加上
    }

    /**
     * 控制图像的正确显示方向
     *
     * @param parameters
     * @param camera
     */
    private void setDisplay(Camera.Parameters parameters, Camera camera) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    private void setDisplayOrientation(Camera camera, int i) {
        try {
            Method downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "setDisplayOrientation: 图像出错1");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "setDisplayOrientation: 图像出错2");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(TAG, "setDisplayOrientation: 图像出错3");
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
