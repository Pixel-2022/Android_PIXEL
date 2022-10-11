package org.tensorflow.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Yolo_Adapter extends RecyclerView.Adapter<Yolo_Adapter.ItemViewHolder> {
    private LayoutInflater inflater;
    private List<Yolo_data> yolo_data;
    //백연결
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private String BASE_URL = LoginActivity.getBASE_URL();
    private int p_userId = MainActivity.p_userID;
    private String stringp_userId = String.valueOf(p_userId);
    private String selectedWord;

    public Yolo_Adapter(Context context, List<Yolo_data> yolo_data){
        this.inflater = LayoutInflater.from(context);
        this.yolo_data=yolo_data;
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_yolo, parent, false);
        return new ItemViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Yolo_data yolo_data1=yolo_data.get(position);
        holder.Yolo_title.setText(yolo_data1.getTitle());
    }

    @Override
    public int getItemCount() {
        return yolo_data.size();
    }


    public class ItemViewHolder extends RecyclerView.ViewHolder{
        public final View mView;
        private TextView Yolo_title;
        private Button Yolo_selectBtn;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
            Yolo_title=itemView.findViewById(R.id.Yolo_title);
            Yolo_selectBtn = itemView.findViewById(R.id.Yolo_selectBtn);
            Yolo_selectBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Log.e("음",String.valueOf(Yolo_title.getText()));
                    custom_dialog(view, getAdapterPosition());
                    selectedWord= String.valueOf(Yolo_title.getText());
                }
            });
        }
    }
    public void custom_dialog(View v, int position) {
        View dialogView = inflater.inflate(R.layout.dialog_yoloselect, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);


        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//      해당 단어에 대한 수어 영상을 보여준다.
        LinearLayout videoBtn = dialogView.findViewById(R.id.videoBtn);
        videoBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
//                DB에 단어가 있는지 먼저 판별할 것
                HashMap<String, String> map = new HashMap<>();
                map.put("Word", selectedWord);
                Call<JsonElement> call1 = retrofitInterface.getDictWord(map);

                call1.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        JsonArray DictResponseArray = response.body().getAsJsonArray();
                        if (response.code() == 201) {
                            Log.e("햄이네 박사 : ", "단어가 DB에 있다네");
//                            DB에 단어가 있으면,
//                            해당 단어의 영상을 보여줄 겁니다.
                            Log.e("햄이네",DictResponseArray.getAsJsonObject().get("videoURL").getAsString());
                        } else {
                            Toast.makeText(v.getContext(), "DB에 없는 단어입니다. 추후 업데이트 될 예정입니다.", Toast.LENGTH_SHORT).show();
                            Log.e("햄이네 박사 : ", "그런 단어는 없다네");
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        Log.e("('-' 여기는 욜로어댑터! (", "연결 실패!");
                    }
                });
            }
        });
//       물체 수어 단어를 단어 카드에 저장
        Button saveBtn = dialogView.findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //                DB에 단어가 있는지 먼저 판별할 것
                HashMap<String, String> map = new HashMap<>();
                map.put("Word", selectedWord);
                Call<JsonElement> call2= retrofitInterface.getDictWord(map);

                call2.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if (response.code() == 201) {
                            Toast.makeText(v.getContext(),"단어장에 추가되었습니다",Toast.LENGTH_SHORT);
                            Log.e("햄이네 박사 : ", "단어가 DB에 있다네");
                            wordAdd();
                        } else {
                            Toast.makeText(v.getContext(), "[추가 실패] DB에 없는 단어입니다. 추후 업데이트 될 예정입니다.", Toast.LENGTH_SHORT).show();
                            Log.e("햄이네 박사 : ", "그런 단어는 없다네");
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        Log.e("('-' 여기는 욜로어댑터! (", "연결 실패!");
                    }
                });
            }
        });
        
        TextView out_btn = dialogView.findViewById(R.id.out_btn);
        out_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                alertDialog.dismiss();
            }
        });
    }
    public void wordAdd(){
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        retrofitInterface = retrofit.create(RetrofitInterface.class);

        HashMap<String, String> map = new HashMap<>();
        map.put("UserId", stringp_userId);
        map.put("Word", selectedWord);

        Call<JsonElement> call1 = retrofitInterface.addList(map);
        call1.enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.code() == 200) {
                    Log.e("('a' ", "추가 성공!");
                } else {
//                    Toast.makeText(v.getContext(), "이미 추가된 단어입니다.", Toast.LENGTH_SHORT).show();
                    Log.e("(._. ", "추가 실패!");
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("('-' 여기는 딕트 (", "연결 실패!");
            }
        });
    }
}
