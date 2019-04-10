/*
 * EffectsAdapter
 *
 * Copyright (c) 2018 Ryota Yamauchi. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.edolfzoku.hayaemon2;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class EffectsAdapter extends RecyclerView.Adapter<EffectsAdapter.ViewHolder>
{
    private final MainActivity activity;
    private final List<EffectItem> items;

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RelativeLayout effectItem;
        final TextView textEffect;
        final ImageButton buttonEffectDetail;
        final ImageView imgRight;

        ViewHolder(View view) {
            super(view);
            effectItem = view.findViewById(R.id.effectItem);
            textEffect = view.findViewById(R.id.textEffect);
            buttonEffectDetail = view.findViewById(R.id.buttonEffectDetail);
            imgRight = view.findViewById(R.id.imgRight);
        }
    }

    EffectsAdapter(Context context, List<EffectItem> items)
    {
        this.activity = (MainActivity)context;
        this.items = items;
    }

    @Override
    public @NonNull EffectsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.effect_item, parent, false);

        return new EffectsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final EffectsAdapter.ViewHolder holder, int position)
    {
        EffectItem item = items.get(position);
        String name = item.getEffectName();
        holder.textEffect.setText(name);

        if(activity.effectFragment.isSelectedItem(position))
            holder.itemView.setBackgroundColor(Color.argb(255, 221, 221, 221));
        else
            holder.itemView.setBackgroundColor(Color.argb(255, 255, 255, 255));
        holder.effectItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.effectFragment.onEffectItemClick(holder.getAdapterPosition());
            }
        });

        if(!item.isEditEnabled()) {
            holder.buttonEffectDetail.setVisibility(View.GONE);
            holder.imgRight.setVisibility(View.GONE);
        }
        else {
            holder.buttonEffectDetail.setVisibility(View.VISIBLE);
            holder.imgRight.setVisibility(View.VISIBLE);
            holder.buttonEffectDetail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.effectFragment.onEffectDetailClick(holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount()
    {
        return items.size();
    }
}
