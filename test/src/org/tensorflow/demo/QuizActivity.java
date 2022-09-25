package org.tensorflow.demo;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
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
    private ImageView imgview;
    private static final String TAG = "QuizActivity";
    ArrayList<Dict> dictlist=new ArrayList();
    String[] names;
    String[] images;
    String selectImage;
    int state=-1;
    String[] videoURLs;
    private String BASE_URL=LoginActivity.getBASE_URL();

    // ApplicationInfo for retrieving metadata defined in the manifest.
    private ApplicationInfo applicationInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_activity);
        imgview=findViewById(R.id.dict_image);


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
                names = new String[DictResponseArray.size()];
                images = new String[DictResponseArray.size()];
                videoURLs = new String[DictResponseArray.size()];
                for (int i=0; i<DictResponseArray.size();i++){
                    JsonElement jsonElement = DictResponseArray.get(i);
                    String name = jsonElement.getAsJsonObject().get("Word").getAsString();
                    String videoURL = jsonElement.getAsJsonObject().get("videoURL").getAsString();
                    String wordImg = jsonElement.getAsJsonObject().get("wordImg").getAsString();
                    names[i]=name;
                    images[i]=wordImg;
                    videoURLs[i] = videoURL;
                    //딕트리스트에 사전 단어들 저장.
                    dictlist.add(new Dict(names[i], images[i], videoURLs[i]));
                }
                state=1;
            }
            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("퀴즈 연결 실패","연결 실패");
            }
        });

        if(state>-1){
            selectImage=getRandom(images);
            Glide.with(imgview.getContext()).load(selectImage).into(imgview);
            state=-1;
        }

        backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                finish();
            }
        });
        try {
            applicationInfo =
                    getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot find application info: " + e);
        }

    }
    public static String getRandom(String[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

}
