package org.tensorflow.demo;

import static org.tensorflow.demo.MainActivity.p_email;
import static org.tensorflow.demo.MainActivity.p_name;
import static org.tensorflow.demo.MainActivity.p_password;
import static org.tensorflow.demo.MainActivity.p_userID;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class QuizActivity extends AppCompatActivity {
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private Button backBtn;
    private LinearLayout hint;
    private Button action;
    private ImageView imgview;
    private static final String TAG = "QuizActivity";
    ArrayList<Dict> dictlist = new ArrayList();
    String[] names;
    String[] images;
    String selectImage;
    String[] videoURLs;
    int rannum;
    private String BASE_URL = LoginActivity.getBASE_URL();

    private String[] word_only={"귀엽다", "나", "만나다","미안","사람", "앉다",
            "어디","여동생","오전","지금","휴대폰","아깝다"};

    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_activity);
        imgview = findViewById(R.id.dict_image);

        //사전에서 받아와서 랜덤하게 띄우겠습니다.
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);
        Call<JsonElement> call = retrofitInterface.getDictAll();
        call.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                JsonArray DictResponseArray = response.body().getAsJsonArray();
                //names = new String[DictResponseArray.size()];
                //images = new String[DictResponseArray.size()];
                //videoURLs = new String[DictResponseArray.size()];

                ArrayList<String> names=new ArrayList<>();
                ArrayList<String> images=new ArrayList<>();
                ArrayList<String> videoURLs=new ArrayList<>();

                for (int i = 0; i < DictResponseArray.size(); i++) {
                    JsonElement jsonElement = DictResponseArray.get(i);
                    String name = jsonElement.getAsJsonObject().get("Word").getAsString();
                    String videoURL = jsonElement.getAsJsonObject().get("videoURL").getAsString();
                    String wordImg = jsonElement.getAsJsonObject().get("wordImg").getAsString();
                    //딕트리스트에 사전 단어들 저장.
                    if(Arrays.asList(word_only).contains(name)){
                        names.add(name);
                        images.add(wordImg);
                        videoURLs.add(videoURL);
                        //names[i] = name;
                        //images[i] = wordImg;
                        //videoURLs[i] = videoURL;
                        dictlist.add(new Dict(name, wordImg, videoURL));
                    }
                }
                rannum = getRandom(images.size());
                selectImage = images.get(rannum);
                Glide.with(imgview.getContext()).load(selectImage).into(imgview);
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("퀴즈 연결 실패", "연결 실패");
            }
        });

        backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Bundle bundle = new Bundle();

                bundle.putString("email", p_email);
                bundle.putString("password", p_password);
                bundle.putString("name", p_name);
                bundle.putInt("UserID", p_userID);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
        try {
            applicationInfo =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find application info: " + e);
        }
        action = findViewById(R.id.quiz_act);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent actintent = new Intent(getApplicationContext(), quiz_media.class);
                actintent.putExtra("단어이름", names[rannum]);
                actintent.putExtra("단어영상", videoURLs[rannum]);
                startActivity(actintent);
            }
        });
        hint = findViewById(R.id.hint_btn);
        hint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                custom_dialog3(rannum);
            }
        });

    }

    public static int getRandom(int size) {
    //public static int getRandom(String[] array) {
        int rnd = new Random().nextInt(size);
        return rnd;
    }

    public void custom_dialog3(int position) {

        Dict dict2 = dictlist.get(position);

        VideoView vv2;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_video, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(QuizActivity.this);
        builder.setView(dialogView);

        vv2 = dialogView.findViewById(R.id.videoV);
        Uri videoUri = Uri.parse(dict2.getVideoURL());
        vv2.setMediaController(new MediaController(QuizActivity.this));
        vv2.setVideoURI(videoUri);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        vv2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                //비디오 시작
                vv2.start();
            }
        });

        TextView ok_btn = dialogView.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }
}
