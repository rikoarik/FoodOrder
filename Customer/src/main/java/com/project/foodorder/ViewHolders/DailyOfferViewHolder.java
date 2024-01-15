package com.project.foodorder.ViewHolders;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.project.foodorder.Items.DishItem;
import com.project.foodorder.R;

import java.io.InputStream;

public class DailyOfferViewHolder extends RecyclerView.ViewHolder{
    private ImageView dishPhoto;
    private TextView dishName, dishDesc, dishPrice, dishQuantity;
    private View view;


    public DailyOfferViewHolder(View itemView) {
        super(itemView);
        view = itemView;
        dishName = itemView.findViewById(R.id.dish_name);
        dishDesc = itemView.findViewById(R.id.dish_desc);
        dishPrice = itemView.findViewById(R.id.dish_price);
        dishPhoto = itemView.findViewById(R.id.dish_image);

    }

    public void setData(DishItem current, int position) {
        InputStream inputStream = null;

        this.dishName.setText(current.getName());
        this.dishDesc.setText(current.getDesc());
        this.dishPrice.setText("Rp " +current.getPrice());
        if (current.getPhotoUri() != null) {
            Glide.with(view.getContext()).load(current.getPhotoUri()).override(80,80).into(dishPhoto);
        }
    }

    public View getView() {
        return view;
    }

}
