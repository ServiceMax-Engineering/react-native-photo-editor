package com.ahmedadeltito.photoeditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.content.PermissionChecker;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Button;
import android.view.WindowManager;

import com.ahmedadeltito.photoeditor.widget.SlidingUpPanelLayout;
import com.ahmedadeltito.photoeditorsdk.CustomBrushDrawingView;
import com.ahmedadeltito.photoeditorsdk.OnPhotoEditorSDKListener;
import com.ahmedadeltito.photoeditorsdk.PhotoEditorSDK;
import com.ahmedadeltito.photoeditorsdk.ViewType;
import com.viewpagerindicator.PageIndicator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;
import java.util.UUID;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import ui.photoeditor.R;

public class PhotoEditorActivity extends AppCompatActivity implements View.OnClickListener, OnPhotoEditorSDKListener {

    public static Typeface emojiFont = null;

    protected static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_GALLERY = 0x1;
    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    private final String TAG = "PhotoEditorActivity";
    private RelativeLayout parentImageRelativeLayout;
    private RecyclerView drawingViewColorPickerRecyclerView;
    private TextView undoTextView, undoTextTextView, doneDrawingTextView;
    private SlidingUpPanelLayout mLayout;
    private View topShadow;
    private RelativeLayout topShadowRelativeLayout;
    private View bottomShadow;
    private RelativeLayout bottomShadowRelativeLayout;
    private ArrayList<Integer> colorPickerColors;
    private int colorCodeTextView = -1;
    private PhotoEditorSDK photoEditorSDK;
    private String selectedImagePath;
    private int imageOrientation;

    // CROP OPTION
    private boolean cropperCircleOverlay = false;
    private boolean freeStyleCropEnabled = true;
    private boolean showCropGuidelines = true;
    private boolean hideBottomControls = true;

    private ImageView photoEditImageView;
    private CustomBrushDrawingView brushDrawingView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_editor);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        selectedImagePath = getIntent().getExtras().getString("selectedImagePath");
        
        if (selectedImagePath.contains("content://")) {
            selectedImagePath = getPath(Uri.parse(selectedImagePath));
        }
        doBeforeBitmapLoad();
        Glide.with(this).asBitmap().load(selectedImagePath).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                doAfterBitmapLoad(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private void doBeforeBitmapLoad() {
        String color = getIntent().getExtras().getString("color");
        Button goToNextTextView = (Button) findViewById(R.id.go_to_next_screen_tv);
        goToNextTextView.setBackgroundColor(Color.parseColor(color));
    }

    private void doAfterBitmapLoad(Bitmap bitmap) {   
        Bitmap rotatedBitmap;
        try {
            ExifInterface exif = new ExifInterface(selectedImagePath);
            imageOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            rotatedBitmap = rotateBitmap(bitmap, imageOrientation, false);
        } catch (IOException e) {
            rotatedBitmap = bitmap;
            imageOrientation = ExifInterface.ORIENTATION_NORMAL;

            e.printStackTrace();
        }


        Typeface newFont = getFontFromRes(R.raw.eventtusicons);
        Typeface fontAwesome = getFontFromRes(R.raw.font_awesome_solid);

        emojiFont = getFontFromRes(R.raw.emojioneandroid);

        brushDrawingView = (CustomBrushDrawingView) findViewById(R.id.drawing_view);
        drawingViewColorPickerRecyclerView = (RecyclerView) findViewById(R.id.drawing_view_color_picker_recycler_view);
        parentImageRelativeLayout = (RelativeLayout) findViewById(R.id.parent_image_rl);
        ImageView closeTextView = (ImageView) findViewById(R.id.close_tv);
        ImageView addTextView = (ImageView) findViewById(R.id.add_text_tv);
        ImageView addPencil = (ImageView) findViewById(R.id.add_pencil_tv);
        ImageView arrowPencil = (ImageView) findViewById(R.id.add_arrow_tv);
        RelativeLayout deleteRelativeLayout = (RelativeLayout) findViewById(R.id.delete_rl);
        ImageView deleteTextView = (ImageView) findViewById(R.id.delete_tv);
        TextView addImageEmojiTextView = (TextView) findViewById(R.id.add_image_emoji_tv);
        ImageView addCropTextView = (ImageView) findViewById(R.id.add_crop_tv);
        ImageView saveTextView = (ImageView) findViewById(R.id.save_tv);
        doneDrawingTextView = (TextView) findViewById(R.id.done_drawing_tv);
        TextView clearAllTextView = (TextView) findViewById(R.id.clear_all_tv);
        Button goToNextTextView = (Button) findViewById(R.id.go_to_next_screen_tv);
        photoEditImageView = (ImageView) findViewById(R.id.photo_edit_iv);
        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        topShadow = findViewById(R.id.top_shadow);
        topShadowRelativeLayout = (RelativeLayout) findViewById(R.id.top_parent_rl);
        bottomShadow = findViewById(R.id.bottom_shadow);
        bottomShadowRelativeLayout = (RelativeLayout) findViewById(R.id.bottom_parent_rl);

        ViewPager pager = (ViewPager) findViewById(R.id.image_emoji_view_pager);
        PageIndicator indicator = (PageIndicator) findViewById(R.id.image_emoji_indicator);

        // Changing width of an imageview to maintain aspect ratio
        // and to fix image perfectly in parent relative layout
        int width = rotatedBitmap.getWidth();
        int height = rotatedBitmap.getHeight();
        if(width > height){
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            photoEditImageView.setLayoutParams(params);
        }
        photoEditImageView.setImageBitmap(rotatedBitmap);

        addImageEmojiTextView.setTypeface(newFont);

        final List<Fragment> fragmentsList = new ArrayList<>();

        ImageFragment imageFragment = new ImageFragment();
        ArrayList stickers = (ArrayList<Integer>) getIntent().getExtras().getSerializable("stickers");
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
        indicator.setViewPager(pager);

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

        closeTextView.setOnClickListener(this);
        addImageEmojiTextView.setOnClickListener(this);
        addCropTextView.setOnClickListener(this);
        addTextView.setOnClickListener(this);
        addPencil.setOnClickListener(this);
        arrowPencil.setOnClickListener(this);
        saveTextView.setOnClickListener(this);
        doneDrawingTextView.setOnClickListener(this);
        clearAllTextView.setOnClickListener(this);
        goToNextTextView.setOnClickListener(this);

        ArrayList<Integer> intentColors = (ArrayList<Integer>) getIntent().getExtras().getSerializable("colorPickerColors");

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
        for (int i = 0; i < hiddenControls.size(); i++) {
            if (hiddenControls.get(i).toString().equalsIgnoreCase("text")) {
                addTextView.setVisibility(View.GONE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("clear")) {
                clearAllTextView.setVisibility(View.GONE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("draw")) {
                addPencil.setVisibility(View.GONE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("save")) {
                saveTextView.setVisibility(View.GONE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("sticker")) {
                addImageEmojiTextView.setVisibility(View.GONE);
            }
            if (hiddenControls.get(i).toString().equalsIgnoreCase("crop")) {
                addCropTextView.setVisibility(View.GONE);
            }
        }
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
        if (mLayout != null)
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
    }

    public void addImage(Bitmap image) {
        photoEditorSDK.addImage(image);
        if (mLayout != null)
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
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
        updateView(View.GONE);
        colorCodeTextView = colorCode;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View addTextPopupWindowRootView = inflater.inflate(R.layout.add_text_popup_window, null);
        final EditText addTextEditText = (EditText) addTextPopupWindowRootView.findViewById(R.id.add_text_edit_text);
        addTextEditText.requestFocus();
        TextView addTextDoneTextView = (TextView) addTextPopupWindowRootView.findViewById(R.id.add_text_done_tv);
        RecyclerView addTextColorPickerRecyclerView = (RecyclerView) addTextPopupWindowRootView.findViewById(R.id.add_text_color_picker_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(PhotoEditorActivity.this, LinearLayoutManager.HORIZONTAL, false);
        addTextColorPickerRecyclerView.setLayoutManager(layoutManager);
        addTextColorPickerRecyclerView.setHasFixedSize(true);
        ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(PhotoEditorActivity.this, colorPickerColors);
        colorPickerAdapter.setOnColorPickerClickListener(new ColorPickerAdapter.OnColorPickerClickListener() {
            @Override
            public void onColorPickerClickListener(int colorCode) {
                addTextEditText.setTextColor(colorCode);
                colorCodeTextView = colorCode;
            }
        });
        addTextColorPickerRecyclerView.setAdapter(colorPickerAdapter);
        if (stringIsNotEmpty(text)) {
            addTextEditText.setText(text);
            addTextEditText.setTextColor(colorCode == -1 ? getResources().getColor(R.color.white) : colorCode);
        }
        final PopupWindow pop = new PopupWindow(PhotoEditorActivity.this);
        pop.setContentView(addTextPopupWindowRootView);
        pop.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        pop.setHeight(LinearLayout.LayoutParams.MATCH_PARENT);
        pop.setFocusable(true);
        pop.setBackgroundDrawable(null);
        pop.showAtLocation(addTextPopupWindowRootView, Gravity.TOP, 0, 0);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        addTextDoneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addText(addTextEditText.getText().toString(), colorCodeTextView);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                pop.dismiss();
                updateView(View.VISIBLE);
            }
        });
    }

    private void updateView(int visibility) {
        topShadowRelativeLayout.setVisibility(visibility);
        bottomShadowRelativeLayout.setVisibility(visibility);
    }

    private void updateBrushDrawingView(boolean brushDrawingMode) {
        photoEditorSDK.setBrushDrawingMode(brushDrawingMode);
        if (brushDrawingMode) {
            updateView(View.GONE);
            drawingViewColorPickerRecyclerView.setVisibility(View.VISIBLE);
            doneDrawingTextView.setVisibility(View.VISIBLE);
            LinearLayoutManager layoutManager = new LinearLayoutManager(PhotoEditorActivity.this, LinearLayoutManager.HORIZONTAL, false);
            drawingViewColorPickerRecyclerView.setLayoutManager(layoutManager);
            drawingViewColorPickerRecyclerView.setHasFixedSize(true);
            ColorPickerAdapter colorPickerAdapter = new ColorPickerAdapter(PhotoEditorActivity.this, colorPickerColors);
            colorPickerAdapter.setOnColorPickerClickListener(new ColorPickerAdapter.OnColorPickerClickListener() {
                @Override
                public void onColorPickerClickListener(int colorCode) {
                    photoEditorSDK.setBrushColor(colorCode);
                }
            });
            drawingViewColorPickerRecyclerView.setAdapter(colorPickerAdapter);
        } else {
            updateView(View.VISIBLE);
            drawingViewColorPickerRecyclerView.setVisibility(View.GONE);
            doneDrawingTextView.setVisibility(View.GONE);
        }
    }

    private void updateArrowDrawingView() {
        brushDrawingView.setDrawingMode(CustomBrushDrawingView.DrawingMode.Arrow);
        updateBrushDrawingView(true);
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
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageName = "IMG_" + timeStamp + ".jpg";

                    if (isSDCARDMounted()) {
                        String folderName = "zinc_annotations";
                        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName);
                        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                            Log.d("PhotoEditorSDK", "Failed to create directory");
                        }

                        // Add the image to the gallery
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, imageName);
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                        try {
                            OutputStream out = getContentResolver().openOutputStream(uri);
                            if (parentImageRelativeLayout != null) {
                                parentImageRelativeLayout.setDrawingCacheEnabled(true);

                                Bitmap bitmap = parentImageRelativeLayout.getDrawingCache();
                                Bitmap rotatedBitmap = rotateBitmap(bitmap, imageOrientation, true);
                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                            }

                            out.flush();
                            out.close();

                            try {
                                ExifInterface exifDest = new ExifInterface(uri.getPath());
                                exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(imageOrientation));
                                exifDest.saveAttributes();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception var7) {
                            var7.printStackTrace();
                        }
                    }
                }
            }.start();
            Toast.makeText(this, getString(R.string.save_image_succeed), Toast.LENGTH_SHORT).show();
            updateView(View.VISIBLE);
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
            private String getImagePath() {
                String imageName = UUID.randomUUID().toString() + ".jpg";
                File dir = new File(getCacheDir(), "zinc_annotations");
                if (!dir.exists()) {
                    dir.mkdir();
                }
                return dir.getAbsolutePath() + "/" + imageName;
            }        
            public void onFinish() {
                String selectedImagePath = getIntent().getExtras().getString("selectedImagePath").replaceAll("file://", "");
                String selectedImagePathLower = selectedImagePath.toLowerCase();
                boolean localFile = selectedImagePathLower.startsWith("/");
                File appFile = getFilesDir().getParentFile();
                if (localFile && appFile != null) {
                    localFile = selectedImagePath.startsWith(appFile.getAbsolutePath());
                }
                if (!localFile) {
                    selectedImagePath = getImagePath();
                }
                File file = new File(selectedImagePath);
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    if (parentImageRelativeLayout != null) {
                        parentImageRelativeLayout.setDrawingCacheEnabled(true);
                        Bitmap bitmap = parentImageRelativeLayout.getDrawingCache();
                        Bitmap rotatedBitmap = rotateBitmap(bitmap, imageOrientation, true);
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                    }

                    out.flush();
                    out.close();
                    try {
                        ExifInterface exifDest = new ExifInterface(file.getAbsolutePath());
                        exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString(imageOrientation));
                        exifDest.saveAttributes();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception var7) {
                    var7.printStackTrace();
                }

                Intent returnIntent = new Intent();
                returnIntent.putExtra("imagePath", selectedImagePath);
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
        if (v.getId() == R.id.close_tv) {
            onBackPressed();
        } else if (v.getId() == R.id.add_image_emoji_tv) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        } else if (v.getId() == R.id.add_crop_tv) {
            System.out.println("CROP IMAGE DUD");
            startCropping();
        } else if (v.getId() == R.id.add_text_tv) {
            openAddTextPopupWindow("", -1);
        } else if (v.getId() == R.id.add_arrow_tv) {
            updateArrowDrawingView();
        } else if (v.getId() == R.id.add_pencil_tv) {
            updateBrushDrawingView(true);
        } else if (v.getId() == R.id.done_drawing_tv) {
            updateBrushDrawingView(false);
        } else if (v.getId() == R.id.save_tv) {
            returnBackWithSavedImage();
        } else if (v.getId() == R.id.clear_all_tv) {
            clearAllViews();
        } else if (v.getId() == R.id.go_to_next_screen_tv) {
            returnBackWithUpdateImage();
        }
    }

    @Override
    public void onEditTextChangeListener(String text, int colorCode) {
        openAddTextPopupWindow(text, colorCode);
    }

    @Override
    public void onAddViewListener(ViewType viewType, int numberOfAddedViews) {
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

    private Typeface getFontFromRes(int resource) {
        Typeface tf = null;
        InputStream is = null;
        try {
            is = getResources().openRawResource(resource);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Could not find font in resources!");
        }

        String outPath = getCacheDir() + "/tmp" + System.currentTimeMillis() + ".raw";

        try {
            byte[] buffer = new byte[is.available()];
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outPath));

            int l = 0;
            while ((l = is.read(buffer)) > 0)
                bos.write(buffer, 0, l);

            bos.close();

            tf = Typeface.createFromFile(outPath);

            // clean up
            new File(outPath).delete();
        } catch (IOException e) {
            Log.e(TAG, "Error reading in font!");
            return null;
        }

        Log.d(TAG, "Successfully loaded font.");

        return tf;
    }

    private void startCropping() {
        System.out.println(selectedImagePath);
        String imgPath = getIntent().getExtras().getString("selectedImagePath");
        if (imgPath.contains("file://") || imgPath.contains("content://")) {
            imgPath = getPath(Uri.parse(imgPath));
        }
        Uri uri = Uri.fromFile(new File(imgPath));
        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(100);
        options.setCircleDimmedLayer(cropperCircleOverlay);
        options.setFreeStyleCropEnabled(freeStyleCropEnabled);
        options.setShowCropGrid(showCropGuidelines);
        options.setHideBottomControls(hideBottomControls);
        options.setAllowedGestures(
                UCropActivity.ALL, // When 'scale'-tab active
                UCropActivity.ALL, // When 'rotate'-tab active
                UCropActivity.ALL  // When 'aspect ratio'-tab active
        );


        UCrop uCrop = UCrop
                .of(uri, Uri.fromFile(new File(this.getTmpDir(this), UUID.randomUUID().toString() + ".jpg")))
                .withOptions(options);

        uCrop.start(this);
    }


    private String getTmpDir(Activity activity) {
        String tmpDir = activity.getCacheDir() + "/react-native-photo-editor";
        new File(tmpDir).mkdir();

        return tmpDir;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            if (data != null) {
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    try {
                        selectedImagePath = resultUri.toString();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                        photoEditImageView.setImageBitmap(bitmap);
                    } catch (Exception ex) {
                        System.out.println("NO IMAGE DATA FOUND");
                    }
                } else {
                    System.out.println("NO IMAGE DATA FOUND");
                }
            } else {
                System.out.println("NO RESULT");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected String getPath(final Uri uri) {
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(this, uri)) {
            // ExternalStorageProvider
            if (GalleryUtils.isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }
            }
            // DownloadsProvider
            else if (GalleryUtils.isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return GalleryUtils.getDataColumn(this, contentUri, null, null);
            }
            // MediaProvider
            else if (GalleryUtils.isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return GalleryUtils.getDataColumn(this, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return GalleryUtils.getDataColumn(this, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int orientation, boolean reverse) {
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);

                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);

                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);

                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                if (!reverse) {
                    matrix.setRotate(90);
                } else {
                    matrix.setRotate(-90);
                }

                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                if (!reverse) {
                    matrix.setRotate(90);
                } else {
                    matrix.setRotate(-90);
                }

                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                if (!reverse) {
                    matrix.setRotate(-90);
                } else {
                    matrix.setRotate(90);
                }

                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                if (!reverse) {
                    matrix.setRotate(-90);
                } else {
                    matrix.setRotate(90);
                }

                break;
            default:
                return bitmap;
        }

        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
