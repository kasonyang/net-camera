package site.kason.netcamera;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NV21;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN;
import static site.kason.netcamera.util.CameraUtil.setFocusMode;
import static site.kason.netcamera.util.CameraUtil.setNearSize;
import static site.kason.netcamera.util.CameraUtil.setPreferredPreviewFormat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacpp.BytePointer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import site.kason.netcamera.util.Performance;
import site.kason.netcamera.util.PermissionManager;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = MainActivity.class.getName();
    private SurfaceView surfaceview;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private int cameraId = 0;
    private byte[] outBuffer;
    private transient DataOutputStream outputStream;
    private ServerSocket socketServer;
    private H264Encoder encoder;
    private Socket socket;

    private final Performance performance = new Performance();

    private final PermissionManager permissionsMgr = new PermissionManager(
            this,
            Manifest.permission.CAMERA
    );

    private Frame frame;
    private PreviewBuffer previewBuffer;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        Button switchCameraBtn = (Button) this.findViewById(R.id.swithCamera);
        Button stopBtn = (Button) this.findViewById(R.id.stop);
        surfaceview = (SurfaceView) this.findViewById(R.id.surfaceview);
        assert switchCameraBtn != null;
        assert stopBtn != null;
        switchCameraBtn.setOnClickListener(v -> switchCamera());
        stopBtn.setOnClickListener(v -> this.stopPreview());
        SurfaceHolder holder = surfaceview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        new Thread(this::startStreamServer).start();
    }

    private void startStreamServer() {
        try (ServerSocket srvSocket = new ServerSocket(6666)) {
            this.socketServer = srvSocket;
            for (; ; ) {
                Socket socket = srvSocket.accept();
                PreviewBuffer previewBuffer = this.previewBuffer;
                if (previewBuffer == null) {
                    Log.e(TAG, "camera is not initialized");
                    socket.close();
                    continue;
                }
                // TODO cleanup
                H264Encoder encoder = new H264Encoder();
                try {
                    encoder.init(previewBuffer.width, previewBuffer.height, new int[]{AV_PIX_FMT_YUV420P});
                    this.outputStream = new DataOutputStream(socket.getOutputStream());
                    this.encoder = encoder;
                    this.socket = socket;
                } catch (IOException ex) {
                    socket.close();
                    encoder.cleanup();
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
    }

    private void stopServer() {
        if (socketServer != null) {
            try {
                socketServer.close();
                socketServer = null;
            } catch (IOException ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        }
    }

    private void switchCamera() {
        cameraId = 1 - cameraId;
        stopPreview();
        startPreview();
    }

    private void startPreview() {
        try {
            doStartPreview();
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }
    }


    private void doStartPreview() throws IOException {
        openCamera(cameraId);
        initCamera();
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        int previewFormat = parameters.getPreviewFormat();
        int width = size.width;
        int height = size.height;
        int bufferSize;
        int yStride;
        int uvStride;
        if (previewFormat == ImageFormat.YV12) {
            yStride = (int) Math.ceil(width / 16.0) * 16;
            uvStride = (int) Math.ceil((yStride / 2.0) / 16.0) * 16;
            bufferSize = yStride * height + uvStride * height;
        } else {
            yStride = width;
            uvStride = width / 2;
            bufferSize = size.width * size.height * ImageFormat.getBitsPerPixel(previewFormat) / 8;
        }
        PreviewBuffer buffer = new PreviewBuffer();
        buffer.width = width;
        buffer.height = height;
        buffer.yStride = yStride;
        buffer.uvStride = uvStride;
        buffer.data = new byte[bufferSize];
        buffer.previewFormat = previewFormat;
        this.previewBuffer = buffer;
        camera.setPreviewCallbackWithBuffer(this);
        camera.addCallbackBuffer(buffer.data);
        camera.startPreview();
    }

    private void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPreview();
        stopServer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceHolder = holder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        if (!permissionsMgr.isGranted()) {
            permissionsMgr.request();
            return;
        }
        startPreview();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsMgr.isGranted()) {
            startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceview = null;
        surfaceHolder = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera c) {
        Log.i(TAG, "onPreviewFrame");
        try {
            DataOutputStream os = outputStream;
            H264Encoder encoder = this.encoder;
            if (encoder == null || os == null) {
                return;
            }
            buildFrameFromPreviewBuffer();
            int result = encoder.recordFrame(frame);
            if (result == AVERROR_EAGAIN()) {
                return;
            }
            if (result != 0) {
                Log.e(TAG, "encode error");
                //TODO stop and cleanup
                System.exit(-1);
            }
            AVPacket pkg = encoder.getPacket();
            if (outBuffer == null || outBuffer.length < pkg.size()) {
                outBuffer = new byte[pkg.size()];
            }
            BytePointer pkgData = pkg.data();
            if (pkgData == null) {
                return;
            }
            int pkgSize = pkg.size();
            if (pkgSize > 0) {
                pkgData.get(outBuffer, 0, pkgSize);
                performance.begin("writePacket");
                os.writeInt(pkgSize);
                os.write(outBuffer, 0, pkgSize);
                performance.end("writePacket");
            }
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            //TODO clean up connection
        } finally {
            c.addCallbackBuffer(previewBuffer.data);
        }
    }

    private void initCamera() throws IOException {
        Camera.Parameters parameters = camera.getParameters();
        setFocusMode(parameters, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        setPreferredPreviewFormat(parameters, ImageFormat.YV12);
        Camera.Size size = setNearSize(parameters, 2048, 2048);
        camera.setParameters(parameters);
        camera.setPreviewDisplay(surfaceHolder);
    }

    private void openCamera(int cameraId) {
        class CameraHandlerThread extends HandlerThread {

            private Handler mHandler;

            public CameraHandlerThread(String name) {
                super(name);
                start();
                mHandler = new Handler(getLooper());
            }

            synchronized void notifyCameraOpened() {
                notify();
            }

            void openCamera() {
                mHandler.post(() -> {
                    camera = Camera.open(cameraId);
                    notifyCameraOpened();
                });
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "wait was interrupted");
                }
            }
        }
        if (camera == null) {
            CameraHandlerThread mThread = new CameraHandlerThread("camera thread");
            synchronized (mThread) {
                mThread.openCamera();
            }
        }
    }

    private void buildFrameFromPreviewBuffer() {
        performance.begin("buildFrameFromPreviewBuffer");
        if (frame == null) {
            frame = new Frame();
        }
        int previewFmt = previewBuffer.previewFormat;
        frame.width = previewBuffer.width;
        frame.height = previewBuffer.height;
        frame.yStride = previewBuffer.yStride;
        frame.uvStride = previewBuffer.uvStride;
        byte[] srcData = previewBuffer.data;
        if (previewFmt == ImageFormat.YV12) {
            frame.pixelFormat = AV_PIX_FMT_YUV420P;
            frame.align = 16;
            if (frame.data == null || frame.data == previewBuffer.data || frame.data.length < srcData.length) {
                frame.data = new byte[srcData.length];
            }
            int yOffset = 0;
            int ySize = frame.yStride * frame.height;
            int uOffset = ySize;
            int uSize = frame.uvStride * frame.height / 2;
            int vOffset = ySize + uSize;
            int vSize = uSize;
            System.arraycopy(srcData, yOffset, frame.data, yOffset, ySize);
            System.arraycopy(srcData, uOffset, frame.data, vOffset, uSize);
            System.arraycopy(srcData, vOffset, frame.data, uOffset, vSize);
        } else if (previewFmt == ImageFormat.NV21) {
            frame.pixelFormat = AV_PIX_FMT_NV21;
            frame.data = srcData;
            frame.align = 1;
        }
        performance.end("buildFrameFromPreviewBuffer");
    }

}
