package com.ahmedadeltito.photoeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ahmedadeltito.photoeditor.filter.SpacesItemDecoration;
import com.ahmedadeltito.photoeditor.filter.ThumbAdapter;
import com.ahmedadeltito.photoeditor.widget.SlidingUpPanelLayout;
import com.ahmedadeltito.photoeditorsdk.BrushDrawingView;
import com.ahmedadeltito.photoeditorsdk.OnPhotoEditorSDKListener;
import com.ahmedadeltito.photoeditorsdk.PhotoEditorSDK;
import com.ahmedadeltito.photoeditorsdk.ViewType;
import com.viewpagerindicator.PageIndicator;
import com.zomato.photofilters.FilterPack;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.utils.ThumbnailItem;
import com.zomato.photofilters.utils.ThumbnailsManager;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ui.photoeditor.R;
public class PhotoEditorActivity extends AppCompatActivity implements View.OnClickListener, OnPhotoEditorSDKListener,ThumbAdapter.ThumbnailsAdapterListener {

    public static Typeface emojiFont = null;

    protected static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_GALLERY = 0x1;

    private final String TAG = "PhotoEditorActivity";
    private RelativeLayout parentImageRelativeLayout;
    private RecyclerView drawingViewColorPickerRecyclerView;
    private TextView doneDrawingTextView;
    private ImageView eraseDrawingTextView;
    private SlidingUpPanelLayout mLayout;
    private View topShadow;
    private TextView undoTextTextView;
    private RelativeLayout topShadowRelativeLayout;
    private View bottomShadow;
    private RelativeLayout bottomShadowRelativeLayout;
    private ArrayList<Integer> colorPickerColors;
    private int colorCodeTextView = -1;
    private ImageView imgClose;
    private PhotoEditorSDK photoEditorSDK;
    private EditText editSticker;
    private ViewPager pager;
    private List<Fragment> fragmentsList;
    private ImageView imgFilter;
    private RelativeLayout relFilter,relTopFilter;
    private TextView txtFilterDone;
    private ImageView photoEditImageView;
    private RelativeLayout relMask;
    //Filter
    private RecyclerView filterRecycle;
    private ThumbAdapter mAdapter;
    private List<ThumbnailItem> thumbnailItemList;
    private Bitmap bitmap,filteredImage,finalImage;

    int brightnessFinal = 0;
    float saturationFinal = 1.0f;
    float contrastFinal = 1.0f;
    private boolean isErase = false;

    static {
        System.loadLibrary("NativeImageProcessor");
    }

    Handler mHandler = new Handler()
    {
        public void handleMessage(Message paramMessage)
        {
            switch (paramMessage.what)
            {
                case 1:
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                    return;
                case 2:
                    mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    bottomShadowRelativeLayout.setVisibility(View.VISIBLE);
                    topShadowRelativeLayout.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    public Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                            boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_editor);
        //String selectedImagePath = getIntent().getExtras().getString("selectedImagePath");
        String selectedImagePath = getIntent().getExtras().getString("selectedImagePath").replace("file://","");
        File imgFile = new File(selectedImagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
        bitmap = scaleDown(bitmap,1500,false);

        Typeface newFont = getFontFromRes(R.raw.eventtusicons);
        emojiFont = getFontFromRes(R.raw.emojioneandroid);

        BrushDrawingView brushDrawingView = (BrushDrawingView) findViewById(R.id.drawing_view);
        brushDrawingView.setVisibility(View.GONE);
        drawingViewColorPickerRecyclerView = (RecyclerView) findViewById(R.id.drawing_view_color_picker_recycler_view);
        parentImageRelativeLayout = (RelativeLayout) findViewById(R.id.parent_image_rl);
        relFilter = (RelativeLayout) findViewById(R.id.relImageFilter);
        relMask = (RelativeLayout) findViewById(R.id.relMask);
        relTopFilter = (RelativeLayout) findViewById(R.id.topFilterBar);
        filterRecycle = (RecyclerView) findViewById(R.id.recycler_view);
        txtFilterDone = (TextView) findViewById(R.id.txtFilterDone);

        //ImageView closeTextView = (ImageView) findViewById(R.id.close_tv);
        ImageView addTextView = (ImageView) findViewById(R.id.add_text_tv);
        ImageView addPencil = (ImageView) findViewById(R.id.add_pencil_tv);
        RelativeLayout deleteRelativeLayout = (RelativeLayout) findViewById(R.id.delete_rl);
        TextView deleteTextView = (TextView) findViewById(R.id.delete_tv);
        ImageView addImageEmojiTextView = (ImageView) findViewById(R.id.add_image_emoji_tv);
        ImageView saveTextView = (ImageView) findViewById(R.id.save_tv);
        undoTextTextView = (TextView) findViewById(R.id.undo_text_tv);
        imgFilter = (ImageView) findViewById(R.id.add_filter);
        doneDrawingTextView = (TextView) findViewById(R.id.done_drawing_tv);
        eraseDrawingTextView = (ImageView) findViewById(R.id.erase_drawing_tv);
        ImageView clearAllTextView = (ImageView) findViewById(R.id.clear_all_tv);
//        TextView goToNextTextView = (TextView) findViewById(R.id.go_to_next_screen_tv);
        photoEditImageView = (ImageView) findViewById(R.id.photo_edit_iv);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        topShadow = findViewById(R.id.top_shadow);
        topShadowRelativeLayout = (RelativeLayout) findViewById(R.id.top_parent_rl);
        bottomShadow = findViewById(R.id.bottom_shadow);
        bottomShadowRelativeLayout = (RelativeLayout) findViewById(R.id.bottom_parent_rl);
        imgClose = (ImageView) findViewById(R.id.imgClosePanel);
        editSticker = (EditText) findViewById(R.id.editSearchSticker);
        imgClose.setOnClickListener(this);
        editSticker.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (pager.getCurrentItem() == 0)
                {
                    ((ImageFragment) fragmentsList.get(0)).filter(charSequence.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        pager = (ViewPager) findViewById(R.id.image_emoji_view_pager);
        //PageIndicator indicator = (PageIndicator) findViewById(R.id.image_emoji_indicator);

        photoEditImageView.setImageBitmap(bitmap);
        deleteTextView.setTypeface(newFont);
        fragmentsList = new ArrayList<>();

        ImageFragment imageFragment = new ImageFragment();
        ArrayList<StickerModel> stickers = (ArrayList<StickerModel>) getIntent().getExtras().getSerializable("stickers");
        if (stickers != null && stickers.size() > 0) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("stickers", stickers);
            imageFragment.setArguments(bundle);
        }

        fragmentsList.add(imageFragment);

        EmojiFragment emojiFragment = new EmojiFragment();
        fragmentsList.add(emojiFragment);

        PreviewSlidePagerAdapter adapter = new PreviewSlidePagerAdapter(getSupportFragmentManager(), fragmentsList);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(5);
        //indicator.setViewPager(pager);

        photoEditorSDK = new PhotoEditorSDK.PhotoEditorSDKBuilder(PhotoEditorActivity.this)
                .parentView(parentImageRelativeLayout) // add parent image view
                .childView(photoEditImageView) // add the desired image view
                .deleteView(deleteRelativeLayout) // add the deleted view that will appear during the movement of the views
                .brushDrawingView(brushDrawingView) // add the brush drawing view that is responsible for drawing on the image view
                .buildPhotoEditorSDK(); // build photo editor sdk
        photoEditorSDK.setOnPhotoEditorSDKListener(this);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    mLayout.setScrollableView(((ImageFragment) fragmentsList.get(position)).imageRecyclerView);
                else if (position == 1)
                    mLayout.setScrollableView(((EmojiFragment) fragmentsList.get(position)).emojiRecyclerView);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //closeTextView.setOnClickListener(this);
        imgFilter.setOnClickListener(this);
        addImageEmojiTextView.setOnClickListener(this);
        addTextView.setOnClickListener(this);
        addPencil.setOnClickListener(this);
        saveTextView.setOnClickListener(this);
        undoTextTextView.setOnClickListener(this);
        doneDrawingTextView.setOnClickListener(this);
        eraseDrawingTextView.setOnClickListener(this);
        txtFilterDone.setOnClickListener(this);
        clearAllTextView.setOnClickListener(this);

        ArrayList<Integer> intentColors = (ArrayList<Integer>) getIntent().getExtras().getSerializable("colorPickerColors");

        initFilter();
        colorPickerColors = new ArrayList<>();
        if (intentColors != null) {
            colorPickerColors = intentColors;
        } else {
            colorPickerColors.add(getResources().getColor(R.color.black));
            colorPickerColors.add(getResources().getColor(R.color.blue_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.brown_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.green_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.orange_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.red_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.red_orange_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.sky_blue_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.violet_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.white));
            colorPickerColors.add(getResources().getColor(R.color.yellow_color_picker));
            colorPickerColors.add(getResources().getColor(R.color.yellow_green_color_picker));
        }


        new CountDownTimer(500, 100) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                mLayout.setScrollableView(((ImageFragment) fragmentsList.get(0)).imageRecyclerView);
            }

        }.start();

        ArrayList hiddenControls = (ArrayList<Integer>) getIntent().getExtras().getSerializable("hiddenControls");
        for (int i = 0;i < hiddenControls.size();i++) {
            if (hiddenControls.get(i).toString().equalsIgnoreCase("text")) {
                addTextView.setVisibility(View.INVISIBLE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("clear")) {
                clearAllTextView.setVisibility(View.INVISIBLE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("crop")) {

            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("draw")) {
                addPencil.setVisibility(View.INVISIBLE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("save")) {
                saveTextView.setVisibility(View.INVISIBLE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("share")) {

            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("sticker")) {
                addImageEmojiTextView.setVisibility(View.INVISIBLE);
            }
        }
        mHandler.sendEmptyMessageDelayed(1,1000);
    }

    private void initFilter()
    {
        thumbnailItemList = new ArrayList<>();
        mAdapter = new ThumbAdapter(this, thumbnailItemList, this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        filterRecycle.setLayoutManager(mLayoutManager);
        filterRecycle.setAdapter(mAdapter);
        prepareThumbnail(bitmap);

    }

    public void prepareThumbnail(final Bitmap bitmap) {
        Runnable r = new Runnable() {
            public void run() {
                Bitmap thumbImage;

                thumbImage = Bitmap.createScaledBitmap(bitmap, 250, 250, false);

                if (thumbImage == null)
                    return;

                ThumbnailsManager.clearThumbs();
                thumbnailItemList.clear();

                // add normal bitmap first
                ThumbnailItem thumbnailItem = new ThumbnailItem();
                thumbnailItem.image = thumbImage;
                ThumbnailsManager.addThumb(thumbnailItem);
                List<Filter> filters = FilterPack.getFilterPack(PhotoEditorActivity.this);

                for (Filter filter : filters) {
                    ThumbnailItem tI = new ThumbnailItem();
                    tI.image = thumbImage;
                    tI.filter = filter;
                    tI.filterName = filter.getName();
                    ThumbnailsManager.addThumb(tI);
                }

                thumbnailItemList.addAll(ThumbnailsManager.processThumbs(PhotoEditorActivity.this));

                PhotoEditorActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

        new Thread(r).start();
    }


    private boolean stringIsNotEmpty(String string) {
        if (string != null && !string.equals("null")) {
            if (!string.trim().equals("")) {
                return true;
            }
        }
        return false;
    }

    public void addEmoji(String emojiName) {
        photoEditorSDK.addEmoji(emojiName, emojiFont);
        if (mLayout != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(imgFilter.getWindowToken(), 0);
            mHandler.sendEmptyMessageDelayed(2,300);

        }
    }

    public void addImage(Bitmap image) {
        photoEditorSDK.addImage(image);
        if (mLayout != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(imgFilter.getWindowToken(), 0);
            mHandler.sendEmptyMessageDelayed(2,300);

        }
    }

    private void addText(String text, int colorCodeTextView) {
        photoEditorSDK.addText(text, colorCodeTextView);
    }

    private void clearAllViews() {
        photoEditorSDK.clearAllViews();
    }

    private void undoViews() {
        photoEditorSDK.viewUndo();
    }

    private void eraseDrawing() {
        photoEditorSDK.brushEraser();
    }


    private void openAddTextPopupWindow(String text, int colorCode) {
        //Hide Bottom Bar
        bottomShadowRelativeLayout.setVisibility(View.GONE);
        //Hide top right bar
        topShadowRelativeLayout.setVisibility(View.GONE);

        colorCodeTextView = colorCode;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View addTextPopupWindowRootView = inflater.inflate(R.layout.add_text_popup_window, null);
        final EditText addTextEditText = (EditText) addTextPopupWindowRootView.findViewById(R.id.add_text_edit_text);
        TextView addTextDoneTextView = (TextView) addTextPopupWindowRootView.findViewById(R.id.add_text_done_tv);
        RecyclerView addTextColorPickerRecyclerView = (RecyclerView) addTextPopupWindowRootView.findViewById(R.id.add_text_color_picker_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(PhotoEditorActivity.this, LinearLayoutManager.HORIZONTAL, false);
        addTextColorPickerRecyclerView.setLayoutManager(layoutManager);
        addTextColorPickerRecyclerView.setHasFixedSize(true);
        ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(PhotoEditorActivity.this, colorPickerColors);
        if (colorPickerColors.size() > 0)
            addTextEditText.setTextColor(colorPickerColors.get(1));
        colorPickerAdapter.setOnColorPickerClickListener(new ColorPickerAdapter.OnColorPickerClickListener() {
            @Override
            public void onColorPickerClickListener(int colorCode) {
                addTextEditText.setTextColor(colorCode);
                colorCodeTextView = colorCode;
            }
        });
        addTextColorPickerRecyclerView.setAdapter(colorPickerAdapter);
        colorPickerAdapter.selectColor = addTextEditText.getCurrentTextColor();
        if (stringIsNotEmpty(text)) {
            addTextEditText.setText(text);
            addTextEditText.setTextColor(colorCode == -1 ? getResources().getColor(R.color.white) : colorCode);
            colorPickerAdapter.selectColor = addTextEditText.getCurrentTextColor();
        }
        final PopupWindow pop = new PopupWindow(PhotoEditorActivity.this);
        pop.setContentView(addTextPopupWindowRootView);
        pop.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        pop.setHeight(LinearLayout.LayoutParams.MATCH_PARENT);
        pop.setFocusable(true);
        pop.setBackgroundDrawable(null);
        pop.showAtLocation(addTextPopupWindowRootView, Gravity.TOP, 0, 0);
        colorPickerAdapter.setSelectColor(addTextEditText.getCurrentTextColor());
        pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                bottomShadowRelativeLayout.setVisibility(View.VISIBLE);
                topShadowRelativeLayout.setVisibility(View.VISIBLE);
            }
        });
        //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        addTextDoneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShadowRelativeLayout.setVisibility(View.VISIBLE);
                topShadowRelativeLayout.setVisibility(View.VISIBLE);
                addText(addTextEditText.getText().toString(), colorCodeTextView);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                pop.dismiss();
            }
        });
    }

    private void updateView(int visibility) {
        topShadow.setVisibility(visibility);
        topShadowRelativeLayout.setVisibility(visibility);
        bottomShadow.setVisibility(visibility);
        bottomShadowRelativeLayout.setVisibility(visibility);
    }

    private void updateBrushDrawingView(boolean brushDrawingMode) {
        photoEditorSDK.setBrushDrawingMode(brushDrawingMode);
        if (brushDrawingMode) {
            updateView(View.GONE);
            drawingViewColorPickerRecyclerView.setVisibility(View.VISIBLE);
            doneDrawingTextView.setVisibility(View.VISIBLE);
            eraseDrawingTextView.setVisibility(View.VISIBLE);
            LinearLayoutManager layoutManager = new LinearLayoutManager(PhotoEditorActivity.this, LinearLayoutManager.HORIZONTAL, false);
            drawingViewColorPickerRecyclerView.setLayoutManager(layoutManager);
            drawingViewColorPickerRecyclerView.setHasFixedSize(true);
            final ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(PhotoEditorActivity.this, colorPickerColors);
            colorPickerAdapter.setOnColorPickerClickListener(new ColorPickerAdapter.OnColorPickerClickListener() {
                @Override
                public void onColorPickerClickListener(int colorCode) {
                    photoEditorSDK.setBrushColor(colorCode);
                    colorPickerAdapter.selectColor = colorCode;
                }
            });
            if (colorPickerColors.size() > 0) {
                //addTextEditText.setTextColor(colorPickerColors.get(1));

            }
            colorPickerAdapter.selectColor = photoEditorSDK.getBrushColor();
            drawingViewColorPickerRecyclerView.setAdapter(colorPickerAdapter);
        } else {
            updateView(View.VISIBLE);
            drawingViewColorPickerRecyclerView.setVisibility(View.GONE);
            doneDrawingTextView.setVisibility(View.GONE);
            eraseDrawingTextView.setVisibility(View.GONE);
        }
    }

    private void returnBackWithSavedImage() {
        int permissionCheck = PermissionChecker.checkCallingOrSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            updateView(View.GONE);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            parentImageRelativeLayout.setLayoutParams(layoutParams);
            new CountDownTimer(1000, 500) {
                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    byte[] byteArray = null;
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        if (parentImageRelativeLayout != null) {
                            parentImageRelativeLayout.setDrawingCacheEnabled(true);
                            parentImageRelativeLayout.getDrawingCache().compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                            byteArray = byteArrayOutputStream .toByteArray();
                        }

                        byteArrayOutputStream.flush();
                        byteArrayOutputStream.close();
                    } catch (Exception var7) {
                        var7.printStackTrace();
                    }
                    Intent returnIntent = new Intent();
                    if (byteArray != null) {
                        String imgString = Base64.encodeToString(byteArray, Base64.NO_WRAP);
                        returnIntent.putExtra("image", imgString);
                    }
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            }.start();
            //Toast.makeText(this, getString(R.string.save_image_succeed), Toast.LENGTH_SHORT).show();
        } else {
            showPermissionRequest();
        }
    }


    private void returnBackWithUpdateImage() {
        updateView(View.GONE);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        parentImageRelativeLayout.setLayoutParams(layoutParams);
        new CountDownTimer(1000, 500) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageName = "IMG_" + timeStamp + ".jpg";

                String selectedImagePath = getIntent().getExtras().getString("selectedImagePath");
                File file = new File(selectedImagePath);

                try {
                    FileOutputStream out = new FileOutputStream(file);
                    if (parentImageRelativeLayout != null) {
                        parentImageRelativeLayout.setDrawingCacheEnabled(true);
                        parentImageRelativeLayout.getDrawingCache().compress(Bitmap.CompressFormat.JPEG, 80, out);
                    }

                    out.flush();
                    out.close();
                } catch (Exception var7) {
                    var7.printStackTrace();
                }

                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK, returnIntent);

                finish();
            }
        }.start();
    }

    private boolean isSDCARDMounted() {
        String status = Environment.getExternalStorageState();
        return status.equals("mounted");
    }

    public void showPermissionRequest() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.access_media_permissions_msg));
        builder.setPositiveButton(getString(R.string.continue_txt), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ActivityCompat.requestPermissions(PhotoEditorActivity.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_GALLERY);
            }
        });
        builder.setNegativeButton(getString(R.string.not_now), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(PhotoEditorActivity.this, getString(R.string.media_access_denied_msg), Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_GALLERY) {
            // If request is cancelled, the result arrays are empty.
            int permissionCheck = PermissionChecker.checkCallingOrSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                returnBackWithSavedImage();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, getString(R.string.media_access_denied_msg), Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }
    @Override
    public void onClick(View v) {
//        if (v.getId() == R.id.close_tv) {
//            onBackPressed();
//        } else if (v.getId() == R.id.add_image_emoji_tv) {
        if (v.getId() == R.id.add_image_emoji_tv) {
            editSticker.requestFocus();
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            bottomShadowRelativeLayout.setVisibility(View.GONE);
            topShadowRelativeLayout.setVisibility(View.GONE);
        } else if (v.getId() == R.id.add_text_tv) {
            openAddTextPopupWindow("", -1);
        } else if (v.getId() == R.id.add_pencil_tv) {
            updateBrushDrawingView(true);
        } else if (v.getId() == R.id.done_drawing_tv) {
            isErase = false;
            eraseDrawingTextView.setBackground(null);
            updateBrushDrawingView(false);
        } else if (v.getId() == R.id.save_tv) {
            returnBackWithSavedImage();
        } else if (v.getId() == R.id.clear_all_tv) {
            //clearAllViews();
            onBackPressed();
        } else if (v.getId() == R.id.undo_text_tv) {
            undoViews();
        } else if (v.getId() == R.id.erase_drawing_tv) {
            isErase  = !isErase;
            if (isErase) {
                eraseDrawingTextView.setBackground(getResources().getDrawable(R.drawable.drawable_white_oval));
                eraseDrawing();
            }
            else
            {
                photoEditorSDK.setBrushDrawingMode(true);
                eraseDrawingTextView.setBackground(null);
            }
//        } else if (v.getId() == R.id.go_to_next_screen_tv) {
//            returnBackWithUpdateImage();
//        }
        }else if (v.getId() == R.id.imgClosePanel)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(imgFilter.getWindowToken(), 0);
            mHandler.sendEmptyMessageDelayed(2,300);
        }
        else if (v.getId() == R.id.add_filter)
        {
            relFilter.setVisibility(View.VISIBLE);
            bottomShadowRelativeLayout.setVisibility(View.GONE);
            relTopFilter.setVisibility(View.VISIBLE);
            topShadowRelativeLayout.setVisibility(View.GONE);
        }
        else if (v.getId() == R.id.txtFilterDone)
        {
            relFilter.setVisibility(View.GONE);
            bottomShadowRelativeLayout.setVisibility(View.VISIBLE);
            relTopFilter.setVisibility(View.GONE);
            topShadowRelativeLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditTextChangeListener(String text, int colorCode) {
        openAddTextPopupWindow(text, colorCode);
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
        if (numberOfAddedViews > 0) {
            undoTextTextView.setTextColor(0xffffffff);
            //undoTextTextView.setVisibility(View.VISIBLE);
        }
        switch (viewType) {
            case BRUSH_DRAWING:
                Log.i("BRUSH_DRAWING", "onAddViewListener");
                break;
            case EMOJI:
                Log.i("EMOJI", "onAddViewListener");
                break;
            case IMAGE:
                Log.i("IMAGE", "onAddViewListener");
                break;
            case TEXT:
                Log.i("TEXT", "onAddViewListener");
                break;
        }
    }

    @Override
    public void onRemoveViewListener(int numberOfAddedViews) {
        Log.i(TAG, "onRemoveViewListener");
        if (numberOfAddedViews == 0) {
            //undoTextTextView.setVisibility(View.GONE);
            undoTextTextView.setTextColor(0x50ffffff);
        }
    }

    @Override
    public void onStartViewChangeListener(ViewType viewType) {
        switch (viewType) {
            case BRUSH_DRAWING:
                Log.i("BRUSH_DRAWING", "onStartViewChangeListener");
                break;
            case EMOJI:
                Log.i("EMOJI", "onStartViewChangeListener");
                break;
            case IMAGE:
                Log.i("IMAGE", "onStartViewChangeListener");
                break;
            case TEXT:
                Log.i("TEXT", "onStartViewChangeListener");
                break;
        }
    }

    @Override
    public void onStopViewChangeListener(ViewType viewType) {
        switch (viewType) {
            case BRUSH_DRAWING:
                Log.i("BRUSH_DRAWING", "onStopViewChangeListener");
                break;
            case EMOJI:
                Log.i("EMOJI", "onStopViewChangeListener");
                break;
            case IMAGE:
                Log.i("IMAGE", "onStopViewChangeListener");
                break;
            case TEXT:
                Log.i("TEXT", "onStopViewChangeListener");
                break;
        }
    }

    @Override
    public void onTouchDownListener() {
        topShadowRelativeLayout.setVisibility(View.GONE);
        bottomShadowRelativeLayout.setVisibility(View.GONE);
    }

    @Override
    public void onMoveItemListener() {

    }

    @Override
    public void onTouchUpListener() {
        topShadowRelativeLayout.setVisibility(View.VISIBLE);
        bottomShadowRelativeLayout.setVisibility(View.VISIBLE);

    }

    private void resetControls() {
        brightnessFinal = 0;
        saturationFinal = 1.0f;
        contrastFinal = 1.0f;
    }
    @Override
    public void onFilterSelected(final Filter filter) {
        relMask.setVisibility(View.VISIBLE);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetControls();
                // applying the selected filter
                filteredImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                // preview filtered image
                photoEditImageView.setImageBitmap(filter.processFilter(filteredImage));

                finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888, true);
                relMask.setVisibility(View.GONE);
            }
        });
    }

    private class PreviewSlidePagerAdapter extends FragmentStatePagerAdapter {
        private List<Fragment> mFragments;

        PreviewSlidePagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            if (mFragments == null) {
                return (null);
            }
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private Typeface getFontFromRes(int resource)
    {
        Typeface tf = null;
        InputStream is = null;
        try {
            is = getResources().openRawResource(resource);
        }
        catch(Resources.NotFoundException e) {
            Log.e(TAG, "Could not find font in resources!");
        }

        String outPath = getCacheDir() + "/tmp" + System.currentTimeMillis() + ".raw";

        try
        {
            byte[] buffer = new byte[is.available()];
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outPath));

            int l = 0;
            while((l = is.read(buffer)) > 0)
                bos.write(buffer, 0, l);

            bos.close();

            tf = Typeface.createFromFile(outPath);

            // clean up
            new File(outPath).delete();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error reading in font!");
            return null;
        }

        Log.d(TAG, "Successfully loaded font.");

        return tf;
    }
}
