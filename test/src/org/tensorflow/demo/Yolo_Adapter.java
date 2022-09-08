package org.tensorflow.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

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

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
            Yolo_title=itemView.findViewById(R.id.Yolo_title);
        }
    }
}
