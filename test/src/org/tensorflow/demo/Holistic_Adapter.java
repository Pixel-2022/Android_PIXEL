package org.tensorflow.demo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Retrofit;

public class Holistic_Adapter extends RecyclerView.Adapter<Holistic_Adapter.ItemViewHolder> {
    private LayoutInflater inflater;
    private List<Holistic_data> holistic_data;
    //백연결
    private Retrofit retrofit;
    private RetrofitInterface retrofitInterface;
    private String BASE_URL = LoginActivity.getBASE_URL();
    private int p_userId = MainActivity.p_userID;
    private String stringp_userId = String.valueOf(p_userId);
    private String selectedWord;
    private Context context;

    public Holistic_Adapter(Context context, List<Holistic_data> holistic_data){
        this.inflater = LayoutInflater.from(context);
        this.holistic_data = holistic_data;
//        Log.e("햄이네박사님", String.valueOf(holistic_data.size()));
//        for(int i=0; i<holistic_data.size();i++){
//            Log.e("햄이네박사님 holistic_data입니다->", String.valueOf(holistic_data.get(i).getTitle()));
//        }
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_holistic,parent,false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Holistic_data holistic_data1 = holistic_data.get(position);
        holder.Holistic_title.setText(holistic_data1.getTitle());

    }

    @Override
    public int getItemCount() {
        return holistic_data.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        private TextView Holistic_title;
        private Button Holistic_selectBtn;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
            Holistic_title = itemView.findViewById(R.id.Holistic_title);
            Holistic_selectBtn = itemView.findViewById(R.id.Holistic_selectBtn);
            Holistic_selectBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    Log.e("선택되었어",String.valueOf(Holistic_title.getText()));

                    selectedWord= String.valueOf(Holistic_title.getText());
                }
            });
        }
    }

}
