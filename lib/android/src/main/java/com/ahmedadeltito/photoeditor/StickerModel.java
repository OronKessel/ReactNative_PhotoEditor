package com.ahmedadeltito.photoeditor;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StickerModel implements Serializable {
    public String mName;
    public String mUrl;
    public Bitmap mBitmap;
    public List<String> mTags = new ArrayList<String>();
}
