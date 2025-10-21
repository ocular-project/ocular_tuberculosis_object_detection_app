package com.ug.air.ocular_tuberculosis.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ug.air.ocular_tuberculosis.R;

public class SpinnerAdapter extends ArrayAdapter<String> {

    Context context;
    private String[] items;

    public SpinnerAdapter(Context context, int resource, String[] items) {
        super(context, resource, items);
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    public View createView(final int position, View contentView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_spinner_item, parent, false);

        TextView label = view.findViewById(R.id.textItem);

        label.setText(items[position]);

        return view;
    }
}
