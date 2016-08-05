package com.example.zyj.hbbtakephotograph;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.zyj.hbbtakephotograph.ImageTool.HandleImage;

import java.io.File;
import java.util.Calendar;

/**
 * Created by Administrator on 2016/8/2.
 * 这个activity用来编辑拍下的照片,添加一些内容
 * 有个复选框,如果打勾了,就记录下今天是健身多少天了
 * 如果没打勾,就不记录
 * *****
 * 当点击生成图片的时候,就把添加的内容生成一个新图片放入指定的文件夹中"new File(Environment.getExternalStorageDirectory(), "hbb")"
 * 文件名  System.currentTimeMillis() + ".jpg"
 * 并把图片显示出来
 */
public class HandleImageActivity extends AppCompatActivity implements View.OnClickListener {

    private File cacheImage;
    private File originalImage;
    private ImageView iv_handle_image;
    private EditText et_your_words;
    private Button btn_generate_image, btn_save_image;
    private CheckBox checkBox_1;


    private int screenWidth;
    private int screenHeight;
    //true的时候不可点,false的时候可点;
    private boolean preventTooMuchClick;
    private boolean preventTooMuchClick_2;

    private Handler handler;
    private String name;


    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    //这些值是当前的日期,从Calendar中获取
    private Calendar calendar;
    private int year;
    private int day;

    //这些值要从preference中获取
    private int workOutDays;
    private int lastWorkOutYear;
    private int lastWorkOutDay;


    private AlertDialog alertDialogGenerating;
    private AlertDialog alertDialogSaving;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_image);

        handler = new Handler();

        initData();
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //获取preference准备存入今天的日期
        preferences = getSharedPreferences(MainActivity.WORKOUT_PREFERENCE, MODE_PRIVATE);
        editor = preferences.edit();
        workOutDays = preferences.getInt("work_out_days", 0);
        lastWorkOutYear = preferences.getInt("last_work_out_year", -1);
        lastWorkOutDay = preferences.getInt("last_work_out_day", -1);
        name = preferences.getString("name", name);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        day = calendar.get(Calendar.DAY_OF_YEAR);
    }

    private void initData() {
        Intent intent = getIntent();
        originalImage = (File) intent.getSerializableExtra("handle_pic");
        //如果上一个activity没有传过来image的file
        if (originalImage == null) {
            Log.d("hbb", "originalImage == null");
            finish();
        }
        //获取屏幕的尺寸,好计算照片该缩放的比例
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        //计算好屏幕高度,控制imageView的大小
        screenHeight = size.y;
        cacheImage = new File(MyApplication.getContext().getCacheDir(), "cacheImage" + ".jpg");
    }

    private void initView() {
        iv_handle_image = (ImageView) findViewById(R.id.iv_handle_image);
        ViewGroup.LayoutParams params = iv_handle_image.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, screenHeight / 4);
        }
        params.height = screenHeight * 5 / 7;
        iv_handle_image.setLayoutParams(params);

        et_your_words = (EditText) findViewById(R.id.et_your_words);
        btn_generate_image = (Button) findViewById(R.id.btn_generate_image);
        btn_generate_image.setOnClickListener(this);
        btn_save_image = (Button) findViewById(R.id.btn_save_image);
        btn_save_image.setOnClickListener(this);
        displayImageView(iv_handle_image, originalImage);
        checkBox_1 = (CheckBox) findViewById(R.id.checkBox_1);
    }

    private void displayImageView(ImageView imageView, File imageFile) {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.toString(), option);
        int realWidth = option.outWidth;
        if (!(realWidth <= screenWidth)) {
            option.inSampleSize = (int) Math.ceil(option.outWidth / screenWidth);
        }
        option.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(imageFile.toString(), option);
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_generate_image:
                //防止重复猛点
                if (preventTooMuchClick) {
                    return;
                }
                preventTooMuchClick = true;
                //获取今天的日期,要显示在图片上
                String date = getDate();

                if (checkBox_1.isChecked()) {
                    //说明preference中记录的就是今天的,则不用改动
                    if (year != lastWorkOutYear || day != lastWorkOutDay) {
                        //说明preference中记录的不是今天的则需要改动
                        lastWorkOutDay = day;
                        lastWorkOutYear = year;
                        workOutDays += 1;
                        //并要存入preference中
                        editor.putInt("last_work_out_year", lastWorkOutYear);
                        editor.putInt("last_work_out_day", lastWorkOutDay);
                        editor.putInt("work_out_days", workOutDays);
                        editor.commit();
                    }
                    generateImage(originalImage, name, et_your_words.getText().toString(), date, workOutDays);
                } else {
                    generateImage(originalImage, name, et_your_words.getText().toString(), date, -1);
                }
                break;
            case R.id.btn_save_image:
                if (preventTooMuchClick) {
                    if (alertDialogGenerating == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        alertDialogGenerating = builder.setTitle("请耐心等待图片生成")
                                .setMessage("图片正在生成...")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .create();
                    }
                    alertDialogGenerating.show();
                    return;
                }
                if (preventTooMuchClick_2) {
                    return;
                }
                if (alertDialogSaving == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    alertDialogSaving = builder.setTitle("图片保存完毕!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create();
                }
                preventTooMuchClick_2 = true;
                saveCacheImageToSystemGallery(cacheImage);
                break;
        }


    }

    @NonNull
    private String getDate() {
        String date;
        String dayOfWeek = null;
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                dayOfWeek = "一";
                break;
            case Calendar.TUESDAY:
                dayOfWeek = "二";
                break;
            case Calendar.WEDNESDAY:
                dayOfWeek = "三";
                break;
            case Calendar.THURSDAY:
                dayOfWeek = "四";
                break;
            case Calendar.FRIDAY:
                dayOfWeek = "五";
                break;
            case Calendar.SATURDAY:
                dayOfWeek = "六";
                break;
            case Calendar.SUNDAY:
                dayOfWeek = "日";
                break;

        }
        date = year + "年"
                + (calendar.get(Calendar.MONTH) + 1) + "月"
                + calendar.get(Calendar.DAY_OF_MONTH) + "日"
                + "    星期" + dayOfWeek;
        return date;
    }

    private void saveCacheImageToSystemGallery(final File image) {
        new Thread() {
            @Override
            public void run() {
                HandleImage.saveCacheImageToSystemGallery(image);
                preventTooMuchClick_2 = false;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        alertDialogSaving.show();
                    }
                });
            }
        }.start();
    }

    private void generateImage(final File originalImage, final String name, final String s, final String date, final int workOutDays) {
        new Thread() {
            @Override
            public void run() {
                cacheImage = HandleImage.generateImage(originalImage, name, s, date, workOutDays);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        displayImageView(iv_handle_image, cacheImage);
                        preventTooMuchClick = false;
                        if (alertDialogGenerating != null) {
                            if (alertDialogGenerating.isShowing()) {
                                alertDialogGenerating.dismiss();
                            }
                        }
                    }
                });
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
