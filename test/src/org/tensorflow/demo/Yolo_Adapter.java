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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
                }
            });
        }
    }
    public void custom_dialog(View v, int position) {
        View dialogView = inflater.inflate(R.layout.dialog_yoloselect, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView out_btn = dialogView.findViewById(R.id.out_btn);
        out_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                alertDialog.dismiss();
            }
        });
    }

}
