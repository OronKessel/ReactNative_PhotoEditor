package com.ahmedadeltito.photoeditor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import ui.photoeditor.R;

/**
 * Created by Ahmed Adel on 5/8/17.
 */

public class ColorPickerAdapter extends RecyclerView.Adapter<ColorPickerAdapter.ViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    private List<Integer> colorPickerColors;
    public int selectColor;
    private OnColorPickerClickListener onColorPickerClickListener;
    private List<ViewHolder> holders = new ArrayList<>();

    public ColorPickerAdapter(@NonNull Context context, @NonNull List<Integer> colorPickerColors) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.colorPickerColors = colorPickerColors;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.color_picker_item_list, parent, false);
        ViewHolder vHolder = new ViewHolder(view);
        holders.add(vHolder);
        return vHolder;
    }

    public void setSelectColor(int color)
    {
        clearUnderline();
        for (int i = 0;i < colorPickerColors.size();i++)
        {
            if (colorPickerColors.get(i) == color)
            {
                if (holders.size() > i)
                {
                    holders.get(i).underLine.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        buildColorPickerView(holder.colorPickerView,holder.underLine, colorPickerColors.get(position));
    }

    @Override
    public int getItemCount() {
        return colorPickerColors.size();
    }

    private void buildColorPickerView(View view, View underView,int colorCode) {
        view.setVisibility(View.VISIBLE);
        if (colorCode == selectColor)
        {
            underView.setVisibility(View.VISIBLE);
        }
        else underView.setVisibility(View.GONE);

        ShapeDrawable biggerCircle = new ShapeDrawable(new OvalShape());
        biggerCircle.setIntrinsicHeight(20);
        biggerCircle.setIntrinsicWidth(20);
        biggerCircle.setBounds(new Rect(0, 0, 20, 20));
        biggerCircle.getPaint().setColor(colorCode);

        ShapeDrawable smallerCircle = new ShapeDrawable(new OvalShape());
        smallerCircle.setIntrinsicHeight(5);
        smallerCircle.setIntrinsicWidth(5);
        smallerCircle.setBounds(new Rect(0, 0, 5, 5));
        smallerCircle.getPaint().setColor(Color.WHITE);
        smallerCircle.setPadding(10, 10, 10, 10);
        Drawable[] drawables = {smallerCircle, biggerCircle};

        LayerDrawable layerDrawable = new LayerDrawable(drawables);

        view.setBackgroundDrawable(layerDrawable);
    }

    public void setOnColorPickerClickListener(OnColorPickerClickListener onColorPickerClickListener) {
        this.onColorPickerClickListener = onColorPickerClickListener;
    }

    public void clearUnderline()
    {
        for (int i = 0;i < holders.size();i++)
        {
            holders.get(i).underLine.setVisibility(View.GONE);
        }
    }
    class ViewHolder extends RecyclerView.ViewHolder {
        View colorPickerView;
        View underLine;

        public ViewHolder(View itemView) {
            super(itemView);
            colorPickerView = itemView.findViewById(R.id.color_picker_view);
            underLine = itemView.findViewById(R.id.vwColorUnderline);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearUnderline();
                    underLine.setVisibility(View.VISIBLE);
                    if (onColorPickerClickListener != null)
                        onColorPickerClickListener.onColorPickerClickListener(colorPickerColors.get(getAdapterPosition()));
                }
            });
        }
    }

    public interface OnColorPickerClickListener {
        void onColorPickerClickListener(int colorCode);
    }
}
