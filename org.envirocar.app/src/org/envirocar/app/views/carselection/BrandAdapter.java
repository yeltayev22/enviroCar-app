package org.envirocar.app.views.carselection;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
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

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> implements Filterable {

    private List<Brand> brandsList = new ArrayList<>();
    private List<Brand> brandsFilterList = new ArrayList<>();
    private OnItemClickListener itemClickListener = null;
    private BrandFilter brandFilter;

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
        this.brandsFilterList.clear();
        this.brandsFilterList.addAll(brandList);
        this.brandsList.clear();
        this.brandsList.addAll(brandList);

        this.itemClickListener = itemClickListener;
        notifyDataSetChanged();
    }

    void clear() {
        brandsFilterList.clear();
        brandsList.clear();

        itemClickListener = null;
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        if (brandFilter == null) {
            brandFilter = new BrandFilter();
        }
        return brandFilter;
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

            brandItem.setOnClickListener(v -> itemClickListener.onItemClick(brand));
        }
    }

    private class BrandFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint != null && constraint.length() > 0) {
                List<Brand> filterList = new ArrayList<>();
                for (int i = 0; i < brandsFilterList.size(); i++) {
                    Manufacturer manufacturer = brandsFilterList.get(i).getManufacturer();
                    Vehicle vehicle = brandsFilterList.get(i).getVehicle();
                    String name;
                    if (manufacturer != null) {
                        name = manufacturer.getName();
                    } else {
                        name = vehicle.getCommercialName();
                    }

                    if ((name.toLowerCase()).contains(constraint.toString().toLowerCase())) {
                        filterList.add(brandsFilterList.get(i));
                    }
                }
                results.count = filterList.size();
                results.values = filterList;
            } else {
                results.count = brandsFilterList.size();
                results.values = brandsFilterList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            brandsList = (List<Brand>) results.values;
            notifyDataSetChanged();
        }

    }
}
