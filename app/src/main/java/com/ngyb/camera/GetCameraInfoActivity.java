package com.ngyb.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

/**
 * 作者：南宫燚滨
 * 描述：
 * 邮箱：nangongyibin@gmail.com
 * 日期：2020/7/30 20:41
 */
public class GetCameraInfoActivity extends AppCompatActivity {
    private static final String TAG = "GetCameraInfoActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_camera_info);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void click1(View view) throws CameraAccessException {
        // 获取摄像头的管理者camermanager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //遍历所有的摄像头
        String[] cameraIdList = manager.getCameraIdList();
        for (int i = 0; i < cameraIdList.length; i++) {
            String cameraId = cameraIdList[i];
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
            Integer integer = cameraCharacteristics.get(CameraCharacteristics.SCALER_CROPPING_TYPE);
            Log.e(TAG, "click1: " + integer);
        }
    }
}
