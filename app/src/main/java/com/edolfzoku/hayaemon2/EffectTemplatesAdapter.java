/*
 * EffectTemplatesAdapter
 *
 * Copyright (c) 2019 Ryota Yamauchi. All rights reserved.
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class EffectTemplatesAdapter extends RecyclerView.Adapter<EffectTemplatesAdapter.ViewHolder>
{
    private final MainActivity mActivity;
    private List<EffectTemplateItem> mItems;

    static class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout mEffectTemplateItem;
        private TextView mTextEffectTemplate;
        private ImageButton mBtnEffectTemplateDetail;
        private ImageView mImgEffectTemplateRight;
        private ImageView mImgEffectTemplateMenu;

        RelativeLayout getEffectTemplateItem() { return mEffectTemplateItem; }
        TextView getTextEffectTemplate() { return mTextEffectTemplate; }
        ImageButton getBtnEffectTemplateDetail() { return mBtnEffectTemplateDetail; }
        ImageView getImgEffectTemplateRight() { return mImgEffectTemplateRight; }
        ImageView getImgEffectTemplateMenu() { return mImgEffectTemplateMenu; }

        ViewHolder(View view) {
            super(view);
            mEffectTemplateItem = view.findViewById(R.id.effectTemplateItem);
            mTextEffectTemplate = view.findViewById(R.id.textEffectTemplate);
            mBtnEffectTemplateDetail = view.findViewById(R.id.btnEffectTemplateDetail);
            mImgEffectTemplateRight = view.findViewById(R.id.imgEffectTemplateRight);
            mImgEffectTemplateMenu = view.findViewById(R.id.imgEffectTemplateMenu);
        }
    }

    EffectTemplatesAdapter(Context context, List<EffectTemplateItem> items)
    {
        mActivity = (MainActivity)context;
        mItems = items;
    }

    void changeItems(List<EffectTemplateItem> items)
    {
        mItems = items;
    }

    @Override
    public @NonNull EffectTemplatesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.effect_template_item, parent, false);

        return new EffectTemplatesAdapter.ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final EffectTemplatesAdapter.ViewHolder holder, int position)
    {
        EffectTemplateItem item = mItems.get(position);
        String name = item.getEffectTemplateName();
        holder.getTextEffectTemplate().setText(name);

        if(mActivity.effectFragment.isSelectedTemplateItem(position))
            holder.itemView.setBackgroundColor(Color.argb(255, 224, 239, 255));
        else
            holder.itemView.setBackgroundColor(Color.argb(255, 255, 255, 255));
        holder.getEffectTemplateItem().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.effectFragment.onEffectTemplateItemClick(holder.getAdapterPosition());
            }
        });

        if(mActivity.effectFragment.isSorting()) {
            holder.getImgEffectTemplateMenu().setOnClickListener(null);
            holder.getImgEffectTemplateMenu().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    mActivity.effectFragment.getEffectTemplateTouchHelper().startDrag(holder);
                    return true;
                }
            });
            holder.getImgEffectTemplateMenu().setImageResource(R.drawable.ic_sort);
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)holder.getImgEffectTemplateMenu().getLayoutParams();
            param.leftMargin = param.rightMargin = (int) (8 * mActivity.getDensity());
            holder.getBtnEffectTemplateDetail().setVisibility(View.GONE);
            holder.getImgEffectTemplateRight().setVisibility(View.GONE);
        }
        else {
            holder.getBtnEffectTemplateDetail().setVisibility(View.VISIBLE);
            holder.getImgEffectTemplateRight().setVisibility(View.VISIBLE);
            holder.getBtnEffectTemplateDetail().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    if(event.getAction() == MotionEvent.ACTION_DOWN)
                        holder.getBtnEffectTemplateDetail().setColorFilter(new PorterDuffColorFilter(Color.parseColor("#ffcce4ff"), PorterDuff.Mode.SRC_IN));
                    else if(event.getAction() == MotionEvent.ACTION_UP)
                        holder.getBtnEffectTemplateDetail().setColorFilter(null);
                    else if(event.getAction() == MotionEvent.ACTION_CANCEL)
                        holder.getBtnEffectTemplateDetail().setColorFilter(null);
                    return false;
                }
            });
            holder.getBtnEffectTemplateDetail().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.effectFragment.onEffectCustomizeClick(holder.getAdapterPosition());
                }
            });
            holder.getImgEffectTemplateMenu().setOnTouchListener(null);
            holder.getImgEffectTemplateMenu().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.effectFragment.showMenu(holder.getAdapterPosition());
                }
            });
            holder.getImgEffectTemplateMenu().setImageResource(R.drawable.ic_button_more);
            RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)holder.getImgEffectTemplateMenu().getLayoutParams();
            param.leftMargin = param.rightMargin = 0;
        }
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }
}