/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.tracking.MultiBoxTracker;
import org.tensorflow.demo.R; // Explicit import needed for internal Google builds.

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  public static List<String> Y_names= new ArrayList<>();

  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged multibox model.
  private static final int MB_INPUT_SIZE = 224;
  private static final int MB_IMAGE_MEAN = 128;
  private static final float MB_IMAGE_STD = 128;
  private static final String MB_INPUT_NAME = "ResizeBilinear";
  private static final String MB_OUTPUT_LOCATIONS_NAME = "output_locations/Reshape";
  private static final String MB_OUTPUT_SCORES_NAME = "output_scores/Reshape";
  private static final String MB_MODEL_FILE = "file:///android_asset/multibox_model.pb";
  private static final String MB_LOCATION_FILE =
      "file:///android_asset/multibox_location_priors.txt";

  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final String TF_OD_API_MODEL_FILE =
      "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";

  // Configuration values for tiny-yolo-voc. Note that the graph is not included with TensorFlow and
  // must be manually placed in the assets/ directory by the user.
  // Graphs and models downloaded from http://pjreddie.com/darknet/yolo/ may be converted e.g. via
  // DarkFlow (https://github.com/thtrieu/darkflow). Sample command:
  // ./flow --model cfg/tiny-yolo-voc.cfg --load bin/tiny-yolo-voc.weights --savepb --verbalise
  private static final String YOLO_MODEL_FILE = "file:///android_asset/graph-tiny-yolo-voc.pb";
  private static final int YOLO_INPUT_SIZE = 416;
  private static final String YOLO_INPUT_NAME = "input";
  private static final String YOLO_OUTPUT_NAMES = "output";
  private static final int YOLO_BLOCK_SIZE = 32;

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.  Optionally use legacy Multibox (trained using an older version of the API)
  // or YOLO.
  private enum DetectorMode {
    TF_OD_API, MULTIBOX, YOLO;
  }
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;


  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
  private static final float MINIMUM_CONFIDENCE_MULTIBOX = 0.1f;
  private static final float MINIMUM_CONFIDENCE_YOLO = 0.25f;

  private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.YOLO;

  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;

  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private byte[] luminanceCopy;

  private BorderedText borderedText;
  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;
    if (MODE == DetectorMode.YOLO) {
      detector =
          TensorFlowYoloDetector.create(
              getAssets(),
              YOLO_MODEL_FILE,
              YOLO_INPUT_SIZE,
              YOLO_INPUT_NAME,
              YOLO_OUTPUT_NAMES,
              YOLO_BLOCK_SIZE);
      cropSize = YOLO_INPUT_SIZE;
    } else if (MODE == DetectorMode.MULTIBOX) {
      detector =
          TensorFlowMultiBoxDetector.create(
              getAssets(),
              MB_MODEL_FILE,
              MB_LOCATION_FILE,
              MB_IMAGE_MEAN,
              MB_IMAGE_STD,
              MB_INPUT_NAME,
              MB_OUTPUT_LOCATIONS_NAME,
              MB_OUTPUT_SCORES_NAME);
      cropSize = MB_INPUT_SIZE;
    } else {
      try {
        detector = TensorFlowObjectDetectionAPIModel.create(
            getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
        cropSize = TF_OD_API_INPUT_SIZE;
      } catch (final IOException e) {
        LOGGER.e(e, "Exception initializing classifier!");
        Toast toast =
            Toast.makeText(
                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
        toast.show();
        finish();
      }
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            if (!isDebug()) {
              return;
            }
            final Bitmap copy = cropCopyBitmap;
            if (copy == null) {
              return;
            }

            final int backgroundColor = Color.argb(100, 0, 0, 0);
            canvas.drawColor(backgroundColor);

            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                canvas.getWidth() - copy.getWidth() * scaleFactor,
                canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<String>();
            if (detector != null) {
              final String statString = detector.getStatString();
              final String[] statLines = statString.split("\n");
              for (final String line : statLines) {
                lines.add(line);
              }
            }
            lines.add("");

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Rotation: " + sensorOrientation);
            lines.add("Inference time: " + lastProcessingTimeMs + "ms");

            borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
          }
        });
  }

  OverlayView trackingOverlay;

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    byte[] originalLuminance = getLuminance();
    tracker.onFrame(
        previewWidth,
        previewHeight,
        getLuminanceStride(),
        sensorOrientation,
        originalLuminance,
        timestamp);
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    if (luminanceCopy == null) {
      luminanceCopy = new byte[originalLuminance.length];
    }
    System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
              case MULTIBOX:
                minimumConfidence = MINIMUM_CONFIDENCE_MULTIBOX;
                break;
              case YOLO:
                minimumConfidence = MINIMUM_CONFIDENCE_YOLO;
                break;
            }

            final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);
                String resultToKorean = null;
                
                switch(result.getTitle()){
                  case "person": resultToKorean ="사람";
                    break;
                  case "bicycle": resultToKorean = "자전거";
                    break;
                  case "car": resultToKorean = "자동차";
                    break;
                  case "motorcycle": resultToKorean = "오토바이";
                    break;
                  case "airplane": resultToKorean = "비행기";
                    break;
                  case "bus": resultToKorean = "버스";
                    break;
                  case "train": resultToKorean = "기차";
                    break;
                  case "truck": resultToKorean = "트럭";
                    break;
                  case "boat": resultToKorean = "보트";
                    break;
                  case "traffic light": resultToKorean = "신호등";
                    break;
                  case "fire hydrant": resultToKorean = "소화전";
                    break;
                  case "stop sign": resultToKorean = "정지 표시판";
                    break;
                  case "parking meter": resultToKorean = "주차권 자동 판매기";
                    break;
                  case "bench": resultToKorean = "벤치";
                    break;
                  case "bird": resultToKorean = "새";
                    break;
                  case "cat": resultToKorean = "고양이";
                    break;
                  case "dog": resultToKorean = "강아지";
                    break;
                  case "horse": resultToKorean = "말";
                    break;
                  case "sheep": resultToKorean = "양";
                    break;
                  case "cow": resultToKorean = "소";
                    break;
                  case "elephant": resultToKorean = "코끼리";
                    break;
                  case "bear": resultToKorean = "곰";
                    break;
                  case "zebra": resultToKorean = "얼룩말";
                    break;
                  case "giraffe": resultToKorean = "기린";
                    break;
                  case "backpack": resultToKorean = "가방";
                    break;
                  case "umbrella": resultToKorean = "우산";
                    break;
                  case "handbag": resultToKorean = "핸드백";
                    break;
                  case "tie": resultToKorean = "넥타이";
                    break;
                  case "suitcase": resultToKorean = "여행용 가방";
                    break;
                  case "frisbee": resultToKorean = "프리스비";
                    break;
                  case "skis": resultToKorean = "스키";
                    break;
                  case "snowboard": resultToKorean = "스노우보드";
                    break;
                  case "sports ball": resultToKorean = "스포츠 볼";
                    break;
                  case "kite": resultToKorean = "연";
                    break;
                  case "baseball bat": resultToKorean = "야구 방망이";
                    break;
                  case "baseball glove": resultToKorean = "야구 장갑";
                    break;
                  case "skateboard" : resultToKorean = "스케이트보드";
                    break;
                  case "surfboard" : resultToKorean = "서핑보드";
                    break;
                  case "tennis racket" : resultToKorean = "테니스 라켓";
                    break;
                  case "bottle" : resultToKorean = "병";
                    break;
                  case "wine glass" : resultToKorean = "와인 잔";
                    break;
                  case "cup" : resultToKorean = "컵";
                    break;
                  case "fork" : resultToKorean = "포크";
                    break;
                  case "knife" : resultToKorean = "칼";
                    break;
                  case "spoon" : resultToKorean = "수저";
                    break;
                  case "bowl" : resultToKorean = "그릇";
                    break;
                  case "banana" : resultToKorean = "바나나";
                    break;
                  case "apple" : resultToKorean = "사과";
                    break;
                  case "sandwich" : resultToKorean = "샌드위치";
                    break;
                  case "orange" : resultToKorean = "오렌지";
                    break;
                  case "broccoli" : resultToKorean = "브로콜리";
                    break;
                  case "carrot" : resultToKorean = "당근";
                    break;
                  case "hot dog" : resultToKorean = "핫도그";
                    break;
                  case "pizza" : resultToKorean = "피자";
                    break;
                  case "donut" : resultToKorean = "도넛";
                    break;
                  case "cake" : resultToKorean = "케이크";
                    break;
                  case "chair" : resultToKorean = "의자";
                    break;
                  case "couch" : resultToKorean = "침상";
                    break;
                  case "potted plant" : resultToKorean = "실내용 화초";
                    break;
                  case "bed" : resultToKorean = "침대";
                    break;
                  case "dining table" : resultToKorean = "식탁";
                    break;
                  case "toilet" : resultToKorean = "화장실";
                    break;
                  case "tv": resultToKorean = "티비";
                    break;
                  case "laptop": resultToKorean = "노트북";
                    break;
                  case "mouse": resultToKorean = "마우스";
                    break;
                  case "remote": resultToKorean = "리모컨";
                    break;
                  case "keyboard": resultToKorean = "키보드";
                    break;
                  case "cell phone": resultToKorean = "핸드폰";
                    break;
                  case "microwave" : resultToKorean = "전자레인지";
                    break;
                  case "oven" : resultToKorean = "오븐";
                    break;
                  case "toaster" : resultToKorean = "토스터기";
                    break;
                  case "sink" : resultToKorean = "싱크대";
                    break;
                  case "refrigerator" : resultToKorean = "냉장고";
                    break;
                  case "book" : resultToKorean = "책";
                    break;
                  case "clock" : resultToKorean = "시계";
                    break;
                  case "vase" : resultToKorean = "꽃병";
                    break;
                  case "scissors" : resultToKorean = "가위";
                    break;
                  case "teddy bear" : resultToKorean = "곰 인형";
                    break;
                  case "hair drier" : resultToKorean = "드라이기";
                    break;
                  case "toothbrush" : resultToKorean = "칫솔";
                    break;

                }
                Log.e("location",String.valueOf(location));
                if(resultToKorean != null) {
                  if(Y_names.contains(resultToKorean)){
                    Log.i("지금 ","이 값은 있는 값이야");
                  }
                  else{
                    Y_names.add(resultToKorean);
                  }

                }
                mappedRecognitions.add(result);
              }
            }

            tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
            trackingOverlay.postInvalidate();

            requestRender();
            computingDetection = false;
          }
        });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onSetDebug(final boolean debug) {
    detector.enableStatLogging(debug);
  }
}
