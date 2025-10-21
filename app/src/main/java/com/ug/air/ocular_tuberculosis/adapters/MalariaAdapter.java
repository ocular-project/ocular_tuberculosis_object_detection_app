package com.ug.air.ocular_tuberculosis.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ug.air.ocular_tuberculosis.R;
import com.ug.air.ocular_tuberculosis.models.Current;

import java.util.List;

public class MalariaAdapter extends RecyclerView.Adapter<MalariaAdapter.MalariaViewHolder>{

    List<Current> modelList;
    Context context;
    private OnItemClickListener mListener;

    public MalariaAdapter(List<Current> modelList, Context context) {
        this.modelList = modelList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    public interface OnItemClickListener {
        void onClick(int position);
    }

    @NonNull
    @Override
    public MalariaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.model_item, parent, false);
        return new MalariaViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MalariaViewHolder holder, int position) {
        Current model = modelList.get(position);

//        if (model.getCurrent_model_type().equals("type")){
//            holder.name.setText("Cervix type");
//        }
//        else if (model.getCurrent_model_type().equals("detector")){
//            holder.name.setText("Lesion detector");
//        }
        holder.name.setText(model.getModel_name());
        int version = model.getVersion();
        holder.version.setText("model version: " + String.valueOf(version));

        String status = model.getDownload();
        if (status.isEmpty()){
            holder.action.setText("");
        }
        else {
            holder.action.setText(status);
        }
    }

    @Override
    public int getItemCount() {
        return modelList.size();
    }

    public void updateModelList(List<Current> newModelList){
        this.modelList = newModelList;
        notifyDataSetChanged();
    }

    public class MalariaViewHolder extends RecyclerView.ViewHolder {

        TextView name, action, version;

        public MalariaViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            action = itemView.findViewById(R.id.download);
            version = itemView.findViewById(R.id.version);

            action.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION){
                    listener.onClick(position);
                }
            });
        }
    }
}
