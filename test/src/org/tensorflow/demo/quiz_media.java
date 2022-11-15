package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.PacketGetter;
import com.google.mediapipe.glutil.EglManager;
import com.google.protobuf.InvalidProtocolBufferException;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class quiz_media extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final boolean FLIP_FRAMES_VERTICALLY = false;

    private Button backBtn;

    static {
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (UnsatisfiedLinkError e) {
            System.loadLibrary("opencv_java4");
        }
    }

    protected FrameProcessor processor;
    protected CameraXPreviewHelper cameraHelper;
    private SurfaceTexture previewFrameTexture;
    private SurfaceView previewDisplayView;
    private EglManager eglManager;
    private ExternalTextureConverter converter;
    private ApplicationInfo applicationInfo;
    float[][][] input_data = new float[1][30][58];
    float[][] output_data = new float[1][20];
    int l = 0;
    private int flag = 0;
    Queue<Float> queue = new LinkedList<>();

    String name;
    String video;

    //    String[] motion = {"가족","감사","괜찮아","귀엽다","나","나이","누구","다시","당신","만나다",
//            "먹다","미안","비빔밥","사람","시다","쓰다","아깝다","안경","안녕","앉다",
//            "어디","어제","언제","얼굴","여동생","오전","오토바이","오후","좋다","지금",
//            "책","컵","휴대폰"};
//    String[] motion = {"가족","감사","괜찮아","귀엽다","나","나이","누구","다시","당신","만나다","먹다"};
    String[] motion18 = {"가족", "감사", "귀엽다", "나","다시",
            "만나다","미안","비빔밥","사람","아깝다", "안녕","앉다",
            "어디","언제","여동생","오전","지금","책","컵","휴대폰"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_media);

        Intent intent = getIntent();
        name = intent.getStringExtra("단어이름");
        video = intent.getStringExtra("단어영상");

        HashMap<String, float[][]> LandmarkMap = new HashMap<>();
        LandmarkMap.put("pose", null);
        LandmarkMap.put("leftHand", null);
        LandmarkMap.put("rightHand", null);
        LandmarkMap.put("face", null);

        RetrofitClient retrofitClient = new RetrofitClient();
        retrofitClient.generateClient();

        TextView actResult = findViewById(R.id.actResult);
        LinearLayout cor_show_video = findViewById(R.id.cor_show_video);
        Button nextQuiz = findViewById(R.id.nextQuiz);
        LinearLayout showButtons = findViewById(R.id.showButtons);
        FrameLayout preview_display_layout2 = findViewById(R.id.preview_display_layout2);
        //VideoView videoV = findViewById(R.id.videoV);
        PlayerView pv;
        pv=findViewById(R.id.EXOplayer);

        backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        try {
            applicationInfo =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find application info: " + e);
        }

        previewDisplayView = new SurfaceView(this);
        setupPreviewDisplayView();

        // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
        // binary graphs.
        AndroidAssetUtil.initializeNativeAssetManager(this);
        eglManager = new EglManager(null);
        processor =
                new FrameProcessor(
                        this,
                        eglManager.getNativeContext(),
                        applicationInfo.metaData.getString("binaryGraphName"),
                        applicationInfo.metaData.getString("inputVideoStreamName"),
                        applicationInfo.metaData.getString("outputVideoStreamName")
                );

        processor
                .addPacketCallback("face_landmarks", (packet) -> {
                    final String[] nowAnswer = {""};

                    try {
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);

                        LandmarkMap.put("face", getPoseLandmarksDebugAry(poseLandmarks));
                        if (LandmarkMap.get("leftHand") == null && LandmarkMap.get("rightHand") == null) {
                        } else {
                            Call<JsonElement> callAPI = retrofitClient.getApi().sendLandmark(LandmarkMap);
                            callAPI.enqueue(new Callback<JsonElement>() {
                                @Override
                                public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                                    // Landmark Map 값 초기화
                                    LandmarkMap.put("pose", null);
                                    LandmarkMap.put("leftHand", null);
                                    LandmarkMap.put("rightHand", null);
                                    LandmarkMap.put("face", null);
                                    // api로부터 받은 계산된 좌표값을 모델의 input 형태에 맞게 변환 (JsonElement -> JsonArray -> String -> String[])
                                    JsonArray DictResponseArray = response.body().getAsJsonArray();

                                    String StringResponse = String.valueOf(DictResponseArray);
                                    StringResponse = StringResponse.replace("[", "");
                                    StringResponse = StringResponse.replace("]", "");
                                    String[] strArr = StringResponse.split(",");

                                    try {
                                        //1. 배열에 계산된 좌표값을 30개씩 받아와야 함. (String[] -> Float)
                                        //1-(1). 배열은 stack형식으로 받아야 함!!
                                        if (l < 30) {
                                            for (int j = 0; j < 58; j++) {
                                                queue.offer(Float.parseFloat(strArr[j]));
                                            }
                                            l++;
                                        } else {
                                            for (int j = 0; j < 58; j++) {
                                                queue.poll();
                                                queue.offer(Float.parseFloat(strArr[j]));
                                            }
                                            Iterator iter = queue.iterator();
                                            while (iter.hasNext()) {
                                                for (int j = 0; j < 30; j++) {
                                                    for (int k = 0; k < 58; k++) {
                                                        input_data[0][j][k] = (float) iter.next();
                                                    }
                                                }
                                            }
                                            // 2. 30개가 되면 모델에게 보내기
                                            Interpreter lite = getTfliteInterpreter("tjwjd_78.tflite");
                                            lite.run(input_data, output_data);
                                            // 3. 모델에서 계산된 분석값을 이용해 올바른 번역 결과 보여주기
                                            // 3-(1). 모델에서 계산된 단어 별 분석값을 로그에 출력
                                            for (int l = 0; l < 20; l++) {
                                                Log.e("최고가 되고 싶은 분석 값", String.valueOf(l) + ":" + String.valueOf(output_data[0][l]));
                                            }
                                            // 3-(2). 분석값 중 최고값을 찾기 maxNum:최고값, maxLoc:최고값의 배열 내 위치
                                            float maxNum = 0;
                                            int maxLoc = -1;
                                            for (int x = 0; x < 20; x++) {
                                                if (maxNum < output_data[0][x]) {
                                                    maxNum = output_data[0][x];
                                                    maxLoc = x;
                                                }
                                            }
                                            Log.e("최고값!!!", String.valueOf(maxNum));

                                            // 3-(3). 정확도를 높이기 위해 (1)최고값이 0.7이상이고 (2)최고값이 5번 연속으로 출력되어야만 옳은 결과값으로 선택하기
                                            if (maxNum >= 0.5) {

                                                // 3-(4). 올바른 번역값 출력하기
                                                if (maxLoc != -1) {
                                                    Log.e("번역 : ", motion18[maxLoc]);
                                                    nowAnswer[0] = motion18[maxLoc];
                                                    if (nowAnswer[0].equals(name)) {
                                                        flag = 1;
                                                        //A. 숨겨져있던 버튼들 보여주기
                                                        showButtons.setVisibility(View.VISIBLE);
                                                        //B. 글씨도 보여주기
                                                        actResult.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            } else {//분석값이 낮아서 무슨 동작인지 인식이 되지 않을 때
                                            }
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JsonElement> call, Throwable t) {
                                    Log.e("실패군", "실패다");
                                }
                            });
                        }


                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }


                });
        processor
                .addPacketCallback("pose_landmarks", (packet) -> {
                    try {
                        Log.d("ㄱ", "pose");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);

                        LandmarkMap.put("pose", getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });
        processor
                .addPacketCallback("left_hand_landmarks", (packet) -> {
                    try {
                        Log.d("ㄱ", "left");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);

                        LandmarkMap.put("leftHand", getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });
        processor
                .addPacketCallback("right_hand_landmarks", (packet) -> {
                    try {
                        Log.d("ㄱ", "right");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);

                        LandmarkMap.put("rightHand", getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });

        processor
                .getVideoSurfaceOutput()
                .setFlipY(
                        applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));

        PermissionHelper.checkAndRequestCameraPermissions(this);

        //다음문제 버튼
        nextQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), QuizActivity.class);
                startActivity(intent);
            }
        });
        cor_show_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //C. 카메라 안 보여주기
                preview_display_layout2.setVisibility(View.GONE);
                //D. 동영상 보여주기
                //videoV.setVisibility(View.VISIBLE);
                pv.setVisibility(View.VISIBLE);
                //videoV.setMediaController(new MediaController(quiz_media.this));
                Uri videoUri = Uri.parse(video);
                //videoV.setVideoURI(videoUri);

                SimpleExoPlayer player;
                player= new SimpleExoPlayer.Builder(quiz_media.this).build();
                pv.setPlayer(player);
                DataSource.Factory factory=new DefaultDataSource.Factory(quiz_media.this);
                ProgressiveMediaSource mediaSource= new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(video));

                player.prepare(mediaSource);
                //player.setPlayWhenReady(true);


//                videoV.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                    @Override
//                    public void onPrepared(MediaPlayer mediaPlayer) {
//                        //비디오 시작
//                        videoV.start();
//                    }
//                });
            }
        });
    }

    // 좌표값 숫자 배열로 변환해서 반환하는 코드
    private static float[][] getPoseLandmarksDebugAry(LandmarkProto.NormalizedLandmarkList poseLandmarks) {
        float[][] poseLandmarkAry = new float[poseLandmarks.getLandmarkCount()][3];
        int landmarkIndex = 0;
        for (LandmarkProto.NormalizedLandmark landmark : poseLandmarks.getLandmarkList()) {
            poseLandmarkAry[landmarkIndex][0] = landmark.getX();
            poseLandmarkAry[landmarkIndex][1] = landmark.getY();
            poseLandmarkAry[landmarkIndex][2] = landmark.getZ();
            ++landmarkIndex;
        }
        return poseLandmarkAry;
    }

    @Override
    protected void onResume() {
        super.onResume();
        converter = new ExternalTextureConverter(eglManager.getContext());
        converter.setFlipY(
                applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));
        converter.setConsumer(processor);
        if (PermissionHelper.cameraPermissionsGranted(this)) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        converter.close();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onCameraStarted(SurfaceTexture surfaceTexture) {
        previewFrameTexture = surfaceTexture;
        // Make the display view visible to start showing the preview. This triggers the
        // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
        previewDisplayView.setVisibility(View.VISIBLE);
    }

    protected Size cameraTargetResolution() {
        return null; // No preference and let the camera (helper) decide.
    }

    public void startCamera() {
        cameraHelper = new CameraXPreviewHelper();
        cameraHelper.setOnCameraStartedListener(
                surfaceTexture -> {
                    onCameraStarted(surfaceTexture);
                });
        CameraHelper.CameraFacing cameraFacing =
                applicationInfo.metaData.getBoolean("cameraFacingFront", false)
                        ? CameraHelper.CameraFacing.BACK
                        : CameraHelper.CameraFacing.FRONT;
        cameraHelper.startCamera(
                this, cameraFacing, /*surfaceTexture=*/ null, cameraTargetResolution());
    }

    protected Size computeViewSize(int width, int height) {
        return new Size(width, height);
    }

    protected void onPreviewDisplaySurfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        // (Re-)Compute the ideal size of the camera-preview display (the area that the
        // camera-preview frames get rendered onto, potentially with scaling and rotation)
        // based on the size of the SurfaceView that contains the display.
        Size viewSize = computeViewSize(width, height);
        Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);
        boolean isCameraRotated = cameraHelper.isCameraRotated();

        // Connect the converter to the camera-preview frames as its input (via
        // previewFrameTexture), and configure the output width and height as the computed
        // display size.
        converter.setSurfaceTextureAndAttachToGLContext(
                previewFrameTexture,
                isCameraRotated ? displaySize.getHeight() : displaySize.getWidth(),
                isCameraRotated ? displaySize.getWidth() : displaySize.getHeight());
    }

    private void setupPreviewDisplayView() {
        previewDisplayView.setVisibility(View.GONE);
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout2);
        viewGroup.addView(previewDisplayView);

        previewDisplayView
                .getHolder()
                .addCallback(
                        new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                onPreviewDisplaySurfaceChanged(holder, format, width, height);
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder holder) {
                                processor.getVideoSurfaceOutput().setSurface(null);
                            }
                        });
    }

    //    tflite 관련 코드
    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(quiz_media.this, modelPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}
