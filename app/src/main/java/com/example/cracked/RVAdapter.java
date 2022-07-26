package com.example.cracked;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.RecipeViewHolder>{

    ArrayList<Recipe> recipeList;
    RecyclerView mRecyclerView;
    Context mContext;

    // constructor
    RVAdapter(ArrayList<Recipe> recipeList){
        this.recipeList = recipeList;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int itemPosition = mRecyclerView.getChildAdapterPosition(v);
            Intent intent = new Intent(mContext, RecipeActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("id", recipeList.get(itemPosition).id);
            bundle.putString("title", recipeList.get(itemPosition).title);
            bundle.putString("imageURL", recipeList.get(itemPosition).imageURL);
            bundle.putStringArrayList("ingredients", recipeList.get(itemPosition).ingredients);
            bundle.putStringArrayList("directions", recipeList.get(itemPosition).directions);
            intent.putExtras(bundle);
            mContext.startActivity(intent);
        }
    };


    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
        RecipeViewHolder recipeViewHolder = new RecipeViewHolder(view);
        view.setOnClickListener(mOnClickListener);
        mContext = parent.getContext();
        return recipeViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        holder.cardTitle.setText(recipeList.get(position).title);
        Picasso.get().load(recipeList.get(position).imageURL).into(holder.cardImage);
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    // cardView xml
    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView cardTitle;
        ImageView cardImage;

        RecipeViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView)itemView.findViewById(R.id.cardView);
            cardTitle = (TextView)itemView.findViewById(R.id.cardTitle);
            cardImage = (ImageView)itemView.findViewById(R.id.cardImage);
        }
    }

}
