package com.example.asus_pc.cekbulyeni;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class Kamera extends AppCompatActivity {


    private static final String TAG = "AndroidKameraAPI";
    private Button takePictureButton;
    private TextureView textureView;

    private ArrayList<String> detectedTexts;
    int width = 640;
    int height = 480;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    public Intent inte;
    private String userToken;
    final Context context = this;


    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(Kamera.this, "Kaydedilmiş:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kamera);
        inte = this.getIntent();
        userToken = inte.getExtras().getString("token");


        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Kamera Arkaplanda");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void sendTC(Matcher matcher) throws java.net.SocketTimeoutException, java.net.ConnectException, JSONException {
        String url = "http://192.168.12.1:8080/api/gbtSorgu";
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(5000);
            RequestParams postParams = new RequestParams();
            final String tcNo = matcher.group(1);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

            }
            Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            String lokasyon="Enlem : "+loc.getLatitude()+" Boylam :"+loc.getLongitude();
            postParams.put("tcNo", tcNo);
            postParams.put("token",userToken);
            postParams.put("lokasyon",lokasyon);
            //postParams.put("yöneticiMi",pozisyonID); error tcNo adi soyadi dogumTarihi sicil askerlik araniyorMu
            client.post(url,postParams,new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject json){
                    try {
                            String message_ = json.getString("message");

                            if(message_.equals("tokenGecersiz")){
                                showDialog("Lütfen Uygulamayı Tekrar Başlatınız",Kamera.this);
                            }
                            else if(message_.equals("tcGecersiz"))
                            {
                                Dialog dialog=new Dialog(context);
                                dialog.setContentView(R.layout.activity_gecersiztcdialog);
                                dialog.show();
                            }
                            else if(message_.equals("kisiYok"))
                            {
                                showDialog("Bu kişi veritabanında bulunamamıştır.",Kamera.this);
                            }
                            else if(message_.equals("basarili")){
                                    String tcNo_ = json.getString("tcNo");
                                    String adi_ = json.getString("adi");
                                    String soyadi_ = json.getString("soyadi");
                                    String dogumTarihi_ = json.getString("dogumTarihi");
                                    String sabıka_ = json.getString("sabika");
                                    String askerlik_ = json.getString("askerlik");
                                    String araniyorMu_ = json.getString("araniyorMu");
                                    final Dialog dialog=new Dialog(context);
                                    dialog.setContentView(R.layout.activity_bilgidialog);
                                    Button tamamlaButton=(Button) dialog.findViewById(R.id.tamamla);
                                    TextView tcNoTextView=(TextView) dialog.findViewById(R.id.tc);
                                    TextView adTextView=(TextView) dialog.findViewById(R.id.ad);
                                    TextView soyadTextView=(TextView) dialog.findViewById(R.id.soyad);
                                    TextView dogumTarihiTextView=(TextView) dialog.findViewById(R.id.dogumTarihi);
                                    TextView sabıkaTextView=(TextView) dialog.findViewById(R.id.sabıka);
                                    TextView askerlikTextView=(TextView) dialog.findViewById(R.id.askerlik);
                                    TextView aranmaTextView=(TextView) dialog.findViewById(R.id.araniyorMu);
                                    ImageView attentionImage=(ImageView) dialog.findViewById(R.id.attentionImage);

                                tcNoTextView.setText(tcNo_);
                                    adTextView.setText(adi_);
                                    soyadTextView.setText(soyadi_);
                                    dogumTarihiTextView.setText(dogumTarihi_);
                                    sabıkaTextView.setText(sabıka_);
                                    askerlikTextView.setText(askerlik_);
                                    aranmaTextView.setText(araniyorMu_);
                                    if(araniyorMu_.equals("Evet")){
                                        attentionImage.setImageResource(R.drawable.attenti);
                                        attentionImage.getLayoutParams().width=300;
                                        attentionImage.getLayoutParams().height=300;
                                    }
                                    dialog.show();
                                    tamamlaButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dialog.dismiss();
                                        }
                                    });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject e) {
                    t.printStackTrace();
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "CameraDevice boş");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");

            ImageReader.OnImageAvailableListener readerListener = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                readerListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = null;
                        try {
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];
                            buffer.get(bytes);
                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                            inspectFromBitmap(bitmapImage);

                        } finally {
                            //                        if (image != null) {
                            //                            image.close();
                            //                        }
                        }
                    }

                    private void inspectFromBitmap(Bitmap bitmap) {
                        TextRecognizer textRecognizer = new TextRecognizer.Builder(Kamera.this).build();
                        try {
                            if (!textRecognizer.isOperational()) {
                                new AlertDialog.
                                        Builder(Kamera.this).
                                        setMessage("Text recognizer could not be set up on your device").show();
                                return;
                            }

                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<TextBlock> origTextBlocks = textRecognizer.detect(frame);
                            List<TextBlock> textBlocks = new ArrayList<>();
                            for (int i = 0; i < origTextBlocks.size(); i++) {
                                TextBlock textBlock = origTextBlocks.valueAt(i);
                                textBlocks.add(textBlock);
                            }
                            Collections.sort(textBlocks, new Comparator<TextBlock>() {
                                @Override
                                public int compare(TextBlock o1, TextBlock o2) {
                                    int diffOfTops = o1.getBoundingBox().top - o2.getBoundingBox().top;
                                    int diffOfLefts = o1.getBoundingBox().left - o2.getBoundingBox().left;
                                    if (diffOfTops != 0) {
                                        return diffOfTops;
                                    }
                                    return diffOfLefts;
                                }
                            });

                            detectedTexts = new ArrayList<String>();

                            for (TextBlock textBlock : textBlocks) {
                                if (textBlock != null && textBlock.getValue() != null) {
                                    detectedTexts.add(textBlock.getValue());
//                                    if ( Pattern.matches("[0-9]{11}", textBlock.getValue()) ){
//                                        showDialog(textBlock.getValue());
//                                    }
                                }
                            }

//                            detectedTexts = new ArrayList<String>();
//                            detectedTexts.add("Ce C RINET gh\n" +
//                                    "SER FTI YETI TURKIYE TÜRKIY NI3\n" +
//                                    "T.C. KIMLIK NO. 20128630484");
//                            detectedTexts.add("asd \n12352416545 asda");
                            Pattern pat = Pattern.compile(".*\\D*(\\d{11})\\D*.*");
                            Matcher matcher;
                            Boolean matched = false;

                            for (String text: detectedTexts) {
                                matcher = pat.matcher(text);
                                if ( matcher.matches() ){
                                    try {
                                        sendTC(matcher);
                                    }catch (ConnectException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (SocketTimeoutException e) {
                                        e.printStackTrace();
                                    }
                                    matched = true;
                                    break;
                                }
                            }
                            if ( matched == false )
                                showDialog("TC KİMLİK NO OKUNAMADI LÜTFEN TEKRAR DENEYİNİZ",
                                        Kamera.this);
                            detectedTexts = new ArrayList<String>();

//                            showDialog(detectedTexts.toString());
                        }
                        finally {
                            textRecognizer.release();
                        }
                    }

                    private void save(byte[] bytes) throws IOException {
                        OutputStream output = null;
                        try {
                            output = new FileOutputStream(file);
                            output.write(bytes);
                        } finally {
                            if (null != output) {
                                output.close();
                            }
                        }
                    }
                };
            }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(MainActivity.this, "Kayıt:" + detectedTexts, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Kamera.this, "Yapılandırma değişikliği", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Kamera.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "Önizleme hatası, dönüş");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    public static void showDialog(String message,Context c){
        new AlertDialog.
                Builder(c).
                setMessage(message).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(Kamera.this, "Üzgünüm!, Izin vermeden bu uygulamayı kullanamazsınız!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}