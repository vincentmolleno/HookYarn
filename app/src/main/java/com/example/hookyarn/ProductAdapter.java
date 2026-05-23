package com.example.hookyarn;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<ProductModel> products;
    private OnProductClickListener listener;
    private boolean isHorizontal = true;

    public interface OnProductClickListener {
        void onProductClick(ProductModel product);
    }

    public ProductAdapter(List<ProductModel> products, OnProductClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductModel product = products.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvPrice.setText(formatPrice(product.getPrice()));

        if (product.getOriginalPrice() > product.getPrice()) {
            holder.tvOriginalPrice.setText(formatPrice(product.getOriginalPrice()));
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvDiscount.setVisibility(View.VISIBLE);
            int discount = (int) ((1 - product.getPrice() / product.getOriginalPrice()) * 100);
            holder.tvDiscount.setText(discount + "% OFF");
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
            holder.tvDiscount.setVisibility(View.GONE);
        }

        if (product.getStock() <= 10) {
            holder.tvStock.setVisibility(View.VISIBLE);
            holder.tvStock.setText("Only " + product.getStock() + " left");
        } else {
            holder.tvStock.setVisibility(View.GONE);
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_product_placeholder)
                    .into(holder.ivProductImage);
        } else if (product.getImageResId() != 0) {
            holder.ivProductImage.setImageResource(product.getImageResId());
        } else {
            holder.ivProductImage.setImageResource(R.drawable.ic_product_placeholder);
        }

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        };
        holder.itemView.setOnClickListener(clickListener);
        holder.ivProductImage.setOnClickListener(clickListener);
    }

    private String formatPrice(double price) {
        DecimalFormat df = new DecimalFormat("#,###.00");
        return "$" + df.format(price);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvPrice;
        TextView tvOriginalPrice;
        TextView tvDiscount;
        TextView tvStock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvStock = itemView.findViewById(R.id.tvStock);
        }
    }
}