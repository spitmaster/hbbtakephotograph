package com.example.zyj.hbbtakephotograph.ImageTool;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.example.zyj.hbbtakephotograph.MainActivity;
import com.example.zyj.hbbtakephotograph.MyApplication;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * Created by Administrator on 2016/8/2.
 */
public class HandleImage {

    public static final int WORKOUTDAYSSIZE_SCALE = 12;
    public static final int WORDSSIZE_SCALE = 15;
    public static final int NAMESIZE_SCALE = 20;
    public static final int LINEINTERVAL_SCALE = 30;


    public static File generateImage(File imageFile, String name, String words, String date, int workOutDays) {
        if (name.trim().equals("")) {
            name = null;
        }
        if (words.trim().equals("")) {
            words = null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(String.valueOf(imageFile));
        return generateImage(bitmap, name, words, date, workOutDays);
    }

    public static File generateImage(Bitmap originalBitmap, String name, String words, String date, int workOutDays) {
        String nameAndDate;
        if (name==null){
            nameAndDate = date;
        }else {
            nameAndDate = name + "     " + date;
        }
        //初始化画布,复制原始的画布
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        //根据比例计算要绘制的文字大小
        int wordsSize = width / WORDSSIZE_SCALE;
        int nameAndDateSize = width / NAMESIZE_SCALE;   //同时也是最下行和屏幕的距离,最左边和屏幕的距离
        int workOutDaysSize = width / WORKOUTDAYSSIZE_SCALE;
        int lineInterval = width / LINEINTERVAL_SCALE;

        //初始化画笔
        //p1写名字
        Paint p1 = new Paint();
        p1.setTypeface(Typeface.createFromAsset(MyApplication.getContext().getAssets(), "a4.otf"));
        p1.setAntiAlias(true);
        p1.setColor(Color.WHITE);
        p1.setTextSize(nameAndDateSize);
        canvas.drawText(nameAndDate, nameAndDateSize, height - nameAndDateSize, p1);

        p1.setColor(Color.BLACK);
        p1.setStrokeWidth(3);
        p1.setStyle(Paint.Style.STROKE);
        canvas.drawText(nameAndDate, nameAndDateSize, height - nameAndDateSize, p1);


        //p2写想说的话
        Paint p2 = new Paint();
        p2.setAntiAlias(true);
        p2.setTypeface(Typeface.createFromAsset(MyApplication.getContext().getAssets(), "a4.otf"));
        p2.setColor(Color.WHITE);
        //setTextSize()设置的是像素,要把sp先转化为像素才好绘制
        p2.setTextSize(wordsSize);
        if (words != null) {
            canvas.drawText(words, nameAndDateSize, height - nameAndDateSize * 2 - lineInterval, p2);
            p2.setColor(Color.BLACK);
            p2.setStrokeWidth(3);
            p2.setStyle(Paint.Style.STROKE);
            canvas.drawText(words, nameAndDateSize, height - nameAndDateSize * 2 - lineInterval, p2);

        }


        Paint p3 = new Paint();
        p3.setAntiAlias(true);
        p3.setTypeface(Typeface.createFromAsset(MyApplication.getContext().getAssets(), "a4.otf"));
        p3.setColor(Color.WHITE);
        p3.setTextSize(workOutDaysSize);
        String days = "健身第" + workOutDays + "天!";
        //如果今天练了的话,则多加一行在上面
        if (workOutDays > 0 && words != null) {
            canvas.drawText(days, nameAndDateSize,
                    height - nameAndDateSize * 3 - lineInterval - wordsSize, p3);
            p3.setColor(Color.BLACK);
            p3.setStrokeWidth(3);
            p3.setStyle(Paint.Style.STROKE);
            canvas.drawText(days, nameAndDateSize,
                    height - nameAndDateSize * 3 - lineInterval - wordsSize, p3);
        } else if (workOutDays > 0 && words == null) {
            canvas.drawText(days, nameAndDateSize,
                    height - nameAndDateSize * 2 - lineInterval, p3);
            p3.setColor(Color.BLACK);
            p3.setStrokeWidth(3);
            p3.setStyle(Paint.Style.STROKE);
            canvas.drawText(days, nameAndDateSize,
                    height - nameAndDateSize * 2 - lineInterval, p3);
        }

        File cacheImage = new File(MyApplication.getContext().getCacheDir(), "cacheImage" + ".jpg");
        if (cacheImage.exists()) {
            cacheImage.delete();
        }
        try {
            FileOutputStream fos = new FileOutputStream(cacheImage);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            bos.flush();
            bos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return cacheImage;
    }

    //把文件复制到系统相册
    public static File saveCacheImageToSystemGallery(File cacheImage) {
        File newImage = new File(MainActivity.DCIM_PATH, System.currentTimeMillis() + ".jpg");
        try {

            FileInputStream fis = new FileInputStream(cacheImage);
            FileOutputStream fos = new FileOutputStream(newImage);
            BufferedInputStream bis = new BufferedInputStream(fis);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] b = new byte[1024 * 4];
            int len;
            int i = 0;
            while ((len = fis.read(b)) != -1) {
                bos.write(b, 0, len);
                Log.d("hbb", "len = " + len + " i = " + i);
                i++;
            }
            //写完数据,关闭流
            bos.flush();
            bos.close();
            fis.close();
            Uri localUri = Uri.fromFile(newImage);
            Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
            MyApplication.getContext().sendBroadcast(localIntent);

            return newImage;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int dp2px(float dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                MyApplication.getContext().getResources().getDisplayMetrics());
    }

    public static int sp2px(float spValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                spValue,
                MyApplication.getContext().getResources().getDisplayMetrics());
    }

}
