package com.ngyb.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 作者：南宫燚滨
 * 描述：
 * 邮箱：nangongyibin@gmail.com
 * 日期：2020/7/30 21:18
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AccessCameraTwoActivity extends AppCompatActivity {
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    private static final String TAG = "AccessCameraTwoActivity";

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private TextureView ttvf, ttvb;
    private String cameraIdB, cameraIdF;
    private Size previewSizeB, previewSizeF;
    private Size captureSizeB, captureSizeF;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private CameraDevice cameraDeviceB, cameraDeviceF;
    private ImageReader imageReaderB, imageReaderF;
    private CaptureRequest.Builder captureRequestBuilderB, captureRequestBuilderF;
    private CaptureRequest captureRequestB, captureRequestF;
    private CameraCaptureSession cameraCaptureSessionB, cameraCaptureSessionF;
    private SurfaceTexture surfaceTextureB, surfaceTextureF;
    private Surface surfaceB, surfaceF;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initScreen();
        setContentView(R.layout.activity_access_camera_two);
        init();
    }

    private void init() {
        //检查是否有存储权限，以免崩溃
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            Toast.makeText(this, "请开启存储或相机的权限", Toast.LENGTH_SHORT).show();
            return;
        }
        initView();
    }

    private void initView() {
        ttvf = findViewById(R.id.ttvf);
        ttvb = findViewById(R.id.ttvb);
    }

    /**
     * 全屏无状态栏
     */
    private void initScreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraThread();
        //前置0
        if (!ttvf.isAvailable()) {
            ttvf.setSurfaceTextureListener(listenerF);
        } else {
            startPreview(0);
        }
        //后置1
        if (!ttvb.isAvailable()) {
            ttvb.setSurfaceTextureListener(listenerB);
        } else {
            startPreview(1);
        }
    }

    private void startCameraThread() {
        cameraThread = new HandlerThread("CameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private TextureView.SurfaceTextureListener listenerB = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height, 1);
            openCamera(1);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private TextureView.SurfaceTextureListener listenerF = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height, 0);
            openCamera(0);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera(int i) {
        CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        try {
            if (i == 0) {
                cm.openCamera(cameraIdF, stateCallbackF, cameraHandler);
            } else if (i == 1) {
                cm.openCamera(cameraIdB, stateCallbackB, cameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupCamera(int width, int height, int i) {
        try {
            //获取摄像头的管理者CameraManager
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            //遍历所有的摄像头
            for (String cameraId : cm.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cm.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                //获取StreamConfigurationMap,它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                assert map != null;
                //此处默认打开前置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT && i == 0) {
                    cameraIdF = cameraId;
                    //根据TextureView的尺寸设置预览尺寸
                    previewSizeF = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    // 获取相机支持的最大拍照尺寸
                    captureSizeF = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                        @Override
                        public int compare(Size o1, Size o2) {
                            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                        }
                    });
                    //此处默认打开后置摄像头
                } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK && i == 1) {
                    cameraIdB = cameraId;
                    //根据TextureView的尺寸设置预览尺寸
                    previewSizeB = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    // 获取相机支持的最大拍照尺寸
                    captureSizeB = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                        @Override
                        public int compare(Size o1, Size o2) {
                            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                        }
                    });
                } else {
                    continue;
                }
                //此ImageReader用户拍照所需
                setupImageReader(i);
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupImageReader(int i) {
        if (i == 0) {
            //代表ImageReader中最多可以获取两帧图像流
            imageReaderF = ImageReader.newInstance(captureSizeF.getWidth(), captureSizeF.getHeight(), ImageFormat.JPEG, 2);
            imageReaderF.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    cameraHandler.post(new ImageSaver(reader.acquireNextImage()));
                }
            }, cameraHandler);
        } else if (i == 1) {
            //代表
            imageReaderB = ImageReader.newInstance(captureSizeB.getWidth(), captureSizeB.getHeight(), ImageFormat.JPEG, 2);
            imageReaderB.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    cameraHandler.post(new ImageSaver(reader.acquireNextImage()));
                }
            }, cameraHandler);
        }
    }

    /**
     * 选择sizeMap中大于并且最接近width和height的size
     *
     * @param sizeMap
     * @param width
     * @param height
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void startPreview(int i) {
        try {
            if (i == 1) {
                surfaceTextureB = ttvb.getSurfaceTexture();
                Log.e(TAG, "startPreview: " + (previewSizeB == null));
                surfaceTextureB.setDefaultBufferSize(previewSizeB.getWidth(), previewSizeB.getHeight());
                surfaceB = new Surface(surfaceTextureB);
                captureRequestBuilderB = cameraDeviceB.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilderB.addTarget(surfaceB);
                cameraDeviceB.createCaptureSession(Arrays.asList(surfaceB, imageReaderB.getSurface()), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            captureRequestB = captureRequestBuilderB.build();
                            cameraCaptureSessionB = session;
                            cameraCaptureSessionB.setRepeatingRequest(captureRequestB, null, cameraHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                }, cameraHandler);
            } else if (i == 0) {
                surfaceTextureF = ttvf.getSurfaceTexture();
                surfaceTextureF.setDefaultBufferSize(previewSizeF.getWidth(), previewSizeF.getHeight());
                surfaceF = new Surface(surfaceTextureF);
                captureRequestBuilderF = cameraDeviceF.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilderF.addTarget(surfaceF);
                cameraDeviceF.createCaptureSession(Arrays.asList(surfaceF, imageReaderF.getSurface()), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            captureRequestF = captureRequestBuilderF.build();
                            cameraCaptureSessionF = session;
                            cameraCaptureSessionF.setRepeatingRequest(captureRequestF, null, cameraHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                }, cameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback stateCallbackB = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDeviceB = camera;
            startPreview(1);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDeviceB = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDeviceB = null;
        }
    };


    private CameraDevice.StateCallback stateCallbackF = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDeviceF = camera;
            startPreview(0);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDeviceF = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDeviceF = null;
        }
    };


    public void takePhoto(View view) {
        loadFocus();
    }

    private void loadFocus() {
        try {
            captureRequestBuilderB.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            cameraCaptureSessionB.capture(captureRequestBuilderB.build(), captureCallbackB, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallbackB = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            capture();
        }
    };

    private void capture() {
        try {
            CaptureRequest.Builder captureRequestBuilderB = cameraDeviceB.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureRequestBuilderB.addTarget(imageReaderB.getSurface());
            captureRequestBuilderB.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Toast.makeText(AccessCameraTwoActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    unLockFocus();
                }
            };
            cameraCaptureSessionB.stopRepeating();
            cameraCaptureSessionB.capture(this.captureRequestBuilderB.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unLockFocus() {
        try {
            captureRequestBuilderB.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            cameraCaptureSessionB.setRepeatingRequest(captureRequestB, null, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static class ImageSaver implements Runnable {
        private Image image;

        public ImageSaver(Image image) {
            this.image = image;
        }


        @Override
        public void run() {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String path = Environment.getExternalStorageDirectory() + "/DCIM/CameraV2/";
            File imageFile = new File(path);
            if (!imageFile.exists()) {
                imageFile.mkdir();
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = path + "IMG_" + timeStamp + ".jpg";
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(fileName);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraCaptureSessionB != null) {
            cameraCaptureSessionB.close();
            cameraCaptureSessionB = null;
        }
        if (cameraDeviceB != null) {
            cameraDeviceB.close();
            cameraDeviceB = null;
        }
        if (imageReaderB != null) {
            imageReaderB.close();
            imageReaderB = null;
        }
    }
}
