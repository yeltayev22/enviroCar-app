package org.envirocar.app.views.carselection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.envirocar.app.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.ManufacturerViewHolder> {

    private List<String> brandsList = new ArrayList<>();

    @NonNull
    @Override
    public ManufacturerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manufacturer, parent, false);
        return new ManufacturerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManufacturerViewHolder holder, int position) {
        holder.bind(brandsList.get(position));
    }

    @Override
    public int getItemCount() {
        return brandsList.size();
    }

    public void setItems(Collection<String> brands) {
        brandsList.addAll(brands);
        notifyDataSetChanged();
    }

    public void clearItems() {
        brandsList.clear();
        notifyDataSetChanged();
    }

    public class ManufacturerViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.manufacturer_label)
        protected TextView manufacturerLabel;

        public ManufacturerViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bind(String brand) {
            manufacturerLabel.setText(brand);
        }

    }
}
