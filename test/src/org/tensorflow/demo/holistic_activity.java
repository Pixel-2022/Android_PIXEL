package org.tensorflow.demo;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class holistic_activity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Flips the camera-preview frames vertically by default, before sending them into FrameProcessor
    // to be processed in a MediaPipe graph, and flips the processed frames back when they are
    // displayed. This maybe needed because OpenGL represents images assuming the image origin is at
    // the bottom-left corner, whereas MediaPipe in general assumes the image origin is at the
    // top-left corner.
    // NOTE: use "flipFramesVertically" in manifest metadata to override this behavior.
    private static final boolean FLIP_FRAMES_VERTICALLY = false;

    private Button backBtn;
    static {
        // Load all native libraries needed by the app.
        System.loadLibrary("mediapipe_jni");
        try {
            System.loadLibrary("opencv_java3");
        } catch (UnsatisfiedLinkError e) {
            // Some example apps (e.g. template matching) require OpenCV 4.
            System.loadLibrary("opencv_java4");
        }
    }

    // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
    // frames onto a {@link Surface}.
    protected FrameProcessor processor;
    // Handles camera access via the {@link CameraX} Jetpack support library.
    protected CameraXPreviewHelper cameraHelper;

    // {@link SurfaceTexture} where the camera-preview frames can be accessed.
    private SurfaceTexture previewFrameTexture;
    // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
    private SurfaceView previewDisplayView;

    // Creates and manages an {@link EGLContext}.
    private EglManager eglManager;
    // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
    // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
    private ExternalTextureConverter converter;

    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;
    float[][][] input_data = new float[1][30][524];
    float[][] output_data = new float[1][10];
    int l = 0;
    Queue<Float> queue = new LinkedList<>();
    Queue<Integer> answerQueue = new LinkedList<>();

//    String[] motion = {"가족","감사","괜찮아","귀엽다","나","나이","누구","다시","당신","만나다",
//            "먹다","미안","비빔밥","사람","시다","쓰다","아깝다","안경","안녕","앉다",
//            "어디","어제","언제","얼굴","여동생","오전","오토바이","오후","좋다","지금",
//            "책","컵","휴대폰"};
//    String[] motion = {"가족","감사","괜찮아","귀엽다","나","나이","누구","다시","당신","만나다"};
//    String[] motion = {"가족","감사","괜찮아"};
    String[] motion18 = {"감사합니다","괜찮습니다","귀엽다","쓰다","안경","오전","오토바이","오후","책","컵"};

    int listFlag = 0;
    //리사이클러뷰
    Holistic_Adapter adapter;
    private RecyclerView.LayoutManager mLayoutmanager;
    public static List<String> dataList = new ArrayList<>();
    public static List<Holistic_data> recogList= new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holistic_activity);

        HashMap<String, float[][]> LandmarkMap = new HashMap<>();
        LandmarkMap.put("pose",null);
        LandmarkMap.put("leftHand",null);
        LandmarkMap.put("rightHand",null);
        LandmarkMap.put("face",null);

        RetrofitClient retrofitClient = new RetrofitClient();
        retrofitClient.generateClient();

        TextView answerFrame = findViewById(R.id.answerFrame);
        Button recogWordListBtn = findViewById(R.id.recogWordListBtn);
        LinearLayout recogWordList = findViewById(R.id.recogWordList);
        RecyclerView recogWordRecyclerView = findViewById(R.id.recogWordList_recyclerView);
        FrameLayout previewDisplayLayout = findViewById(R.id.preview_display_layout);

        mLayoutmanager = new LinearLayoutManager(getApplication());
        recogWordRecyclerView.setLayoutManager(mLayoutmanager);

        adapter=new Holistic_Adapter(getApplication(),recogList);
        recogWordRecyclerView.setAdapter(adapter);
//돌아가기 버튼
        backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });
//인식된 단어 목록 보기 버튼
        recogWordListBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(listFlag==0){ //목록이 닫혀 있는 상태라면,
                    adapter=new Holistic_Adapter(getApplication(),recogList);
//                1. 인식된 단어 목록 버튼의 글씨가 "단어 목록 닫기"로 변경되기
                    recogWordListBtn.setText("단어 목록 닫기");
//                2. recyclerview visibility=visible
                    recogWordList.setVisibility(View.VISIBLE);
//                3. answerFrame visibility=gone
                    answerFrame.setVisibility(View.GONE);
//                4. preview_display_layout visibility=gone
                    previewDisplayLayout.setVisibility(View.GONE);
//                5. listFlag값 1로 변경(열림 상태)
                    listFlag=1;
//               어댑터 연결
                    recogWordRecyclerView.setAdapter(adapter);


                } else{ //목록이 열려 있는 상태라면,
//                1. 인식된 단어 목록 버튼의 글씨가 "인식된 단어 목록"으로 변경되기
                    recogWordListBtn.setText("인식된 단어 목록");
//                2. recyclerview visibility=gone
                    recogWordList.setVisibility(View.GONE);
//                3. answerFrame visibility=visible
                    answerFrame.setVisibility(View.VISIBLE);
//                4. preview_display_layout visibility=visible
                    previewDisplayLayout.setVisibility(View.VISIBLE);
//                5. listFlag값 0으로 변경(닫힘 상태)
                    listFlag=0;
//                    dataList.clear();
//                    recogList.clear();
                }
            }
        });
//단어 목록 recyclerview


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
                    try {
//                        Log.d("ㄱ", "face");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_FL",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));

                        LandmarkMap.put("face",getPoseLandmarksDebugAry(poseLandmarks));
//                        Log.e("입력된 값", String.valueOf(getPoseLandmarksDebugAry(poseLandmarks)));
                        if(LandmarkMap.get("leftHand")==null && LandmarkMap.get("rightHand")==null){
//                            answerFrame.setText("손이 보이지 않아서 인식이 되지 않아요");
                        }else {
                            Call<JsonElement> callAPI = retrofitClient.getApi().sendLandmark(LandmarkMap);
                            Log.e("입력된 값", String.valueOf(LandmarkMap));


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
                                    Log.e("받아온 값", String.valueOf(DictResponseArray));

                                    String StringResponse = String.valueOf(DictResponseArray);
                                    StringResponse = StringResponse.replace("[", "");
                                    StringResponse = StringResponse.replace("]", "");
                                    String[] strArr = StringResponse.split(",");

                                    try {
                                        //1. 배열에 계산된 좌표값을 30개씩 받아와야 함. (String[] -> Float)
                                        //1-(1). 배열은 stack형식으로 받아야 함!!
                                        if (l < 30) {
                                            for (int j = 0; j < 524; j++) {
                                                queue.offer(Float.parseFloat(strArr[j]));
//                                                Log.e("큐 offer1", String.valueOf(queue.size()));
                                            }
                                            l++;
                                        } else {
                                            for (int j = 0; j < 524; j++) {
                                                queue.poll();
                                                queue.offer(Float.parseFloat(strArr[j]));
                                            }
                                            Iterator iter = queue.iterator();
                                            while (iter.hasNext()) {
                                                for (int j = 0; j < 30; j++) {
                                                    for (int k = 0; k < 524; k++) {
                                                        input_data[0][j][k] = (float) iter.next();
                                                    }
                                                }
                                            }
                                            // 2. 30개가 되면 모델에게 보내기
                                            Interpreter lite = getTfliteInterpreter("AAAA18.tflite");
                                            lite.run(input_data, output_data);
                                            // 3. 모델에서 계산된 분석값을 이용해 올바른 번역 결과 보여주기
                                            // 3-(1). 모델에서 계산된 단어 별 분석값을 로그에 출력
                                            for(int l=0; l<10; l++){
                                                Log.e("최고가 되고 싶은 분석 값",String.valueOf(l)+":"+String.valueOf(output_data[0][l]));
                                            }
                                            // 3-(2). 분석값 중 최고값을 찾기 maxNum:최고값, maxLoc:최고값의 배열 내 위치
                                            float maxNum = 0;
                                            int maxLoc = -1;
                                            for (int x = 0; x < 10; x++) {
                                                if (maxNum < output_data[0][x]) {
                                                    maxNum = output_data[0][x];
                                                    maxLoc = x;
                                                }
                                            }
                                            Log.e("최고값!!!",String.valueOf(maxNum));

                                            // 3-(3). 정확도를 높이기 위해 (1)최고값이 0.7이상이고 (2)최고값이 5번 연속으로 출력되어야만 옳은 결과값으로 선택하기
                                                if(maxNum >= 0.5){

                                                    //🎃최고값이 5번 연속 출력될 때의 조건 구현 아직 못 했음.. (._.

                                                    //maxLoc값을 5개 받는다
//                                            answerQueue.offer(maxLoc);
                                                    //값이 5개가 되면 각 원소들이 모두 일치하는지 확인한다.
//                                            if(answerQueue.size()==5){
//
//                                            }
                                                    //값이 같다면, 그대로 출력하기
                                                    //새로운 6번째 값이 들어오면 poll후 offer한다.
//                                            if(answerQueue.size()<5){
//                                                answerQueue.offer(maxLoc);
//                                            }else{
//                                                answerQueue.poll();
//                                                answerQueue.offer(maxLoc);
//                                                Iterator answeriter = answerQueue.iterator();
//                                                while (answeriter.hasNext()) {
//                                                    for (int j = 0; j < 4; j++) {
////                                                        answeriter.next()
//                                                    }
//                                                }
//                                            }
                                                    // 3-(4). 올바른 번역값 출력하기
                                                    if (maxLoc != -1) {
                                                        Log.e("번역 : ", motion18[maxLoc]);
                                                        answerFrame.setText(motion18[maxLoc]);

                                                        //[단어 저장 기능]인식된 단어 배열에 저장하기
                                                        if(dataList.contains(motion18[maxLoc])==false){
                                                            dataList.add(motion18[maxLoc]);
                                                            recogList.add(new Holistic_data(motion18[maxLoc]));
                                                            recogWordRecyclerView.setAdapter(adapter);
//                                                            Log.e("햄이네박사님","단어를 추가했습니다.");
//                                                            Log.e("햄이네박사님","수고했다네~");
                                                        }
//                                                        Log.e("햄이네박사님", String.valueOf(recogList.size()));
//                                                        for(int i=0; i<recogList.size();i++){
//                                                            Log.e("햄이네박사님 recogList입니다->", String.valueOf(recogList.get(i).getTitle()));
//                                                        }
                                                    }
                                                } else {//분석값이 낮아서 무슨 동작인지 인식이 되지 않을 때
                                                answerFrame.setText("  ");
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
//                        Log.d("ㄱ", "pose");
                        byte[] landmarksRaw = PacketGetter.getProtoBytes(packet);
                        LandmarkProto.NormalizedLandmarkList poseLandmarks = LandmarkProto.NormalizedLandmarkList.parseFrom(landmarksRaw);
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_PL",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("pose",getPoseLandmarksDebugAry(poseLandmarks));
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
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_LH",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("leftHand",getPoseLandmarksDebugAry(poseLandmarks));
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
//                        Log.v("AAA", String.valueOf(packet));
//                        LandmarkProto.NormalizedLandmarkList poseLandmarks =
//                                PacketGetter.getProto(packet, LandmarkProto.NormalizedLandmarkList.class);
//                        Log.v(
//                                "AAA_RH",
//                                "[TS:"
//                                        + packet.getTimestamp()
//                                        + "] "
//                                        + getPoseLandmarksDebugString(poseLandmarks));
                        LandmarkMap.put("rightHand",getPoseLandmarksDebugAry(poseLandmarks));
                    } catch (InvalidProtocolBufferException e) {
                        Log.e("AAA", "Failed to get proto.", e);
                    }

                });

        processor
                .getVideoSurfaceOutput()
                .setFlipY(
                        applicationInfo.metaData.getBoolean("flipFramesVertically", FLIP_FRAMES_VERTICALLY));


        PermissionHelper.checkAndRequestCameraPermissions(this);


    }

    // 좌표값 숫자 배열로 변환해서 반환하는 코드
    private static float[][] getPoseLandmarksDebugAry(LandmarkProto.NormalizedLandmarkList poseLandmarks){
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
        ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
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
    private Interpreter getTfliteInterpreter(String modelPath){
        try{
            return new Interpreter(loadModelFile(holistic_activity.this, modelPath));
        }
        catch(Exception e){
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
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }
}

