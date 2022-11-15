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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WordCardAdapter extends RecyclerView.Adapter<WordCardAdapter.ItemViewHolder> {
    //삭제 위해서 백이랑 연결
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private int p_userId = MainActivity.p_userID;
    private String BASE_URL = LoginActivity.getBASE_URL();
    private String stringp_userId = String.valueOf(p_userId);

    private Context context;

    private LayoutInflater inflater;
    //adapter에 들어갈 list
    private ArrayList<Data> data;

    public WordCardAdapter(Context context, ArrayList<Data> data) {
        this.inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.wordcard_item, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Data data1 = data.get(position);
        holder.wordTitle.setText(data1.getWord());
        Glide.with(holder.wordImage.getContext()).load(data1.getImage()).into(holder.wordImage);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        private TextView wordTitle;
        private ImageView wordImage;
        private LinearLayout expanded;
        private ImageButton wordDeleteBtn;
        private ImageButton viBtn;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            wordTitle = itemView.findViewById(R.id.wordTitle);
            wordImage = itemView.findViewById(R.id.wordImage);
            expanded = itemView.findViewById(R.id.expandedLayout);
            wordDeleteBtn = itemView.findViewById(R.id.wordDeleteBtn);
            viBtn = itemView.findViewById(R.id.wordFilmBtn);

            mView.setOnClickListener(new View.OnClickListener() {
                //클릭 했을 때 펼쳐지기
                @Override
                public void onClick(View v) {
                    int isVisible = expanded.getVisibility();
                    if (isVisible == 8) {
                        expanded.setVisibility(v.VISIBLE);
                    } else if (isVisible == 0) {
                        expanded.setVisibility(v.GONE);
                    }
                }
            });

            wordDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    custom_dialog(v, position);
                }
            });

            viBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    custom_dialog2(v, getAdapterPosition());
                }
            });
        }
    }

    public void custom_dialog(View v, int position) {
        View dialogView = inflater.inflate(R.layout.dialog_delete, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView ok_btn = dialogView.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //클릭 한 값의 단어를 알아내서 삭제한다.
                Data data2 = data.get(position);
                String word22 = data2.getWord();

                //백과 연결하여 삭제
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                retrofitInterface = retrofit.create(RetrofitInterface.class);

                HashMap<String, String> map = new HashMap<>();
                map.put("UserId", stringp_userId);
                map.put("Word", word22);
                System.out.println(map);

                Call<JsonElement> call2 = retrofitInterface.delList(map);
                call2.enqueue(new Callback<JsonElement>() {
                    @Override
                    public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                        if (response.code() == 200) {
                            Fragment_WordCard.delFilter(data2.getWord());
                        } else {
                            Log.e("(._. 연결은 했으나", "삭제 실패!");
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonElement> call, Throwable t) {
                        Log.e("카드 어댑터", "연결 실패!");
                    }
                });
                alertDialog.dismiss();
            }
        });
        TextView cancel_btn = dialogView.findViewById(R.id.cancel_btn);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
    }

    MediaController mc;
    public void custom_dialog2(View v, int position) {
        Data data2 = data.get(position);
        VideoView vv;
        View dialogView = inflater.inflate(R.layout.dialog_video, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        vv = dialogView.findViewById(R.id.videoV);
        //URL설정
        Uri videoUri = Uri.parse(data2.getVideoURL());
        //vv.setMediaController(new MediaController(context));
        vv.setVideoURI(videoUri);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        mc = new MediaController(inflater.getContext());
                        vv.setMediaController(mc);
                        mc.setAnchorView(vv);
                        ((ViewGroup) mc.getParent()).removeView(mc);
                        ((FrameLayout) dialogView.findViewById(R.id.videoViewWrapper)).addView(mc);
                        mc.setVisibility(View.VISIBLE);
                    }
                });

                //비디오 시작
                mediaPlayer.start();
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

    //검색 후 갱신
    public void filterList(ArrayList<Data> filterList) {
        data = filterList;
        notifyDataSetChanged();
    }

    //삭제 후 갱신
    public void refresh1(ArrayList<Data> dellist) {
        data = dellist;
        notifyDataSetChanged();
    }
}