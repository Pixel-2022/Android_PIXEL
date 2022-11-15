package org.tensorflow.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private Context context;

    public Yolo_Adapter(Context context, List<Yolo_data> yolo_data) {
        this.inflater = LayoutInflater.from(context);
        this.yolo_data = yolo_data;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_yolo, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Yolo_data yolo_data1 = yolo_data.get(position);
        holder.Yolo_title.setText(yolo_data1.getTitle());
    }

    @Override
    public int getItemCount() {
        return yolo_data.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        private TextView Yolo_title;
        private Button Yolo_selectBtn;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            Yolo_title = itemView.findViewById(R.id.Yolo_title);
            Yolo_selectBtn = itemView.findViewById(R.id.Yolo_selectBtn);
            Yolo_selectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    custom_dialog(view, getAdapterPosition());
                    selectedWord = String.valueOf(Yolo_title.getText());
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
        videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                DB에 단어가 있는지 먼저 판별할 것 => DB에 없는거 부르면 response가 안되는듯 추후 수정
                if(selectedWord=="컵"||selectedWord=="책"||selectedWord=="휴대폰"||selectedWord=="사람"||selectedWord=="기차"
                        ||selectedWord=="비빔밥"||selectedWord=="안경"||selectedWord=="오토바이"||selectedWord=="얼굴"||selectedWord=="호떡") {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("Word", selectedWord);
                    Call<JsonElement> call1 = retrofitInterface.getDictWord(map);

                    call1.enqueue(new Callback<JsonElement>() {
                        @Override
                        public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                            JsonObject DictResponseArray = response.body().getAsJsonObject();
                            if (response.code() == 201) {
                                String vv = DictResponseArray.get("videoURL").getAsString();
                                videoDialog(vv);
                            } else {
                                Toast.makeText(v.getContext(), "DB에 없는 단어입니다. 추후 업데이트 될 예정입니다.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonElement> call, Throwable t) {
                            Log.e("('-' 여기는 욜로어댑터! (", "연결 실패!");
                        }
                    });
                }else {
                    Toast.makeText(v.getContext(), "DB에 없는 단어입니다. 추후 업데이트 될 예정입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

//       물체 수어 단어를 단어 카드에 저장
        Button saveBtn = dialogView.findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HashMap<String, String> map = new HashMap<>();
                map.put("Word", selectedWord);
                Call<JsonElement> call2 = retrofitInterface.getDictWord(map);
                call2.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if (response.code() == 201) {
                            wordAdd();
                        } else {
                            Toast.makeText(v.getContext(), "[추가 실패] DB에 없는 단어입니다. 추후 업데이트 될 예정입니다.", Toast.LENGTH_SHORT).show();
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
        out_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    public void wordAdd() {
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
                    Toast.makeText(context, "단어장에 추가되었습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "이미 추가된 단어입니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Log.e("('-' 여기는 딕트 (", "연결 실패!");
            }
        });
    }

    MediaController mc;
    public void videoDialog(String geturl) {

        PlayerView pv;
        SimpleExoPlayer player;




        VideoView vv;
        View dialogView = inflater.inflate(R.layout.dialog_video, null);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        //exoplayer
        pv=dialogView.findViewById(R.id.EXOplayer);
        player= new SimpleExoPlayer.Builder(context).build();
        pv.setPlayer(player);
        DataSource.Factory factory=new DefaultDataSource.Factory(context);
        ProgressiveMediaSource mediaSource= new ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(geturl));

        player.prepare(mediaSource);
        //player.setPlayWhenReady(true);


        //vv = dialogView.findViewById(R.id.videoV);

        Uri videoUri = Uri.parse(geturl);
        //vv.setMediaController(new MediaController(context));
        //vv.setVideoURI(videoUri);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

//        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//
//            @Override
//            public void onPrepared(MediaPlayer mediaPlayer) {
////오류나서 일단 보류
////                    mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
////                        @Override
////                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
////                            mc = new MediaController(dialogView.getContext());
////                            vv.setMediaController(mc);
////                            mc.setAnchorView(vv);
////                            ((ViewGroup) mc.getParent()).removeView(mc);
////                            ((FrameLayout) dialogView.findViewById(R.id.videoViewWrapper)).addView(mc);
////                            mc.setVisibility(View.VISIBLE);
////                        }
////                    });
//
//                mediaPlayer.start();
//
//            }
//        });

        TextView ok_btn = dialogView.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

    }
}