package org.envirocar.app.views.carselection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.envirocar.app.R;
import org.envirocar.core.entity.Manufacturer;
import org.envirocar.core.entity.Vehicle;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> {

    private List<Brand> brandsList = new ArrayList<>();
    private OnItemClickListener itemClickListener = null;

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_brand, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        holder.bind(brandsList.get(position), itemClickListener);
    }

    @Override
    public int getItemCount() {
        return brandsList.size();
    }

    void set(List<Brand> brandList, OnItemClickListener itemClickListener) {
        this.brandsList.addAll(brandList);
        this.itemClickListener = itemClickListener;
        notifyDataSetChanged();
    }

    void clear() {
        brandsList.clear();
        itemClickListener = null;
        notifyDataSetChanged();
    }

    public static class BrandViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.brand_item)
        protected RelativeLayout brandItem;
        @BindView(R.id.brand_label)
        protected TextView brandLabel;
        @BindView(R.id.next_icon)
        protected ImageView nextIcon;

        BrandViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bind(Brand brand, OnItemClickListener itemClickListener) {
            Manufacturer manufacturer = brand.getManufacturer();
            Vehicle vehicle = brand.getVehicle();

            if (manufacturer != null) {
                brandLabel.setText(manufacturer.getName());
                nextIcon.setVisibility(View.VISIBLE);

            } else if (vehicle != null) {
                brandLabel.setText(vehicle.getCommercialName());
                nextIcon.setVisibility(View.GONE);
            }

            brandItem.setOnClickListener(v -> {
                itemClickListener.onItemClick(brand);
            });
        }

    }
}
