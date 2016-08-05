package com.example.zyj.hbbtakephotograph;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.Preference;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.zyj.hbbtakephotograph.ImageTool.PermissionRelative;

import java.io.File;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements DialogInterface.OnClickListener {

    File shootPic;

    private LinearLayout base_layout;
    private ImageView background_pic;

    public int[] background = {R.drawable.a0, R.drawable.a1, R.drawable.a2,
            R.drawable.a3, R.drawable.a4, R.drawable.a5, R.drawable.a6, R.drawable.a7};

    public static final int SHOOT_PIC = 0;
    public static final int CHOOSE_PIC = 1;
    public static final int REQUEST_PERMISSION_EXTERNAL_STORAGE = 0;
    public static final String WORKOUT_PREFERENCE = "workout_preference";


    //系统相册的目录
    public static String DCIM_PATH;


    private boolean isFirstUse;
    private String name;
    private int workOutDays;
    private int lastWorkOutYear;
    private int lastWorkOutDay;

    private View dialogView;
    private EditText et_d_name;
    private AlertDialog dialog;


    private View dialogView_recountDays;
    private EditText et_d_recount_days;
    private AlertDialog dialog_recount_days;

    private boolean isExternalStorageAccessed;
    SharedPreferences preference;
    SharedPreferences.Editor editor;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        background_pic= (ImageView) findViewById(R.id.background_pic);
        int i = (int) (Math.random() * 8);
        background_pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        background_pic.setImageResource(background[i]);
        //系统版本大于6.0,要请求申请使用外部储存空间的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionRelative.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                isExternalStorageAccessed = true;
            } else {
                isExternalStorageAccessed = false;
            }
        } else {        //对于api小于23的系统,只要安装上能获取到权限,不需要获取运行时权限
            isExternalStorageAccessed = true;
        }

        preference = getSharedPreferences(WORKOUT_PREFERENCE, MODE_PRIVATE);
        editor = preference.edit();
        isFirstUse = preference.getBoolean("is_first_use", true);
        name = preference.getString("name", null);
        workOutDays = preference.getInt("work_out_days", 0);
        lastWorkOutYear = preference.getInt("last_work_out_year", -1);
        lastWorkOutDay = preference.getInt("last_work_out_day", -1);


        File systemCameraPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!systemCameraPath.exists()) {
            systemCameraPath.mkdir();
        }
        DCIM_PATH = systemCameraPath.toString();


        if (isFirstUse) {
            getNameDialog().show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        editor.putBoolean("is_first_use", false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_change_name:
                setNameDialog();
                break;
            case R.id.item_recount_days:
                getRecountDaysDialog().show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    private AlertDialog getNameDialog() {
        if (dialog == null) {
            dialogView = getLayoutInflater().inflate(R.layout.dialog_view, null);
            et_d_name = (EditText) dialogView.findViewById(R.id.et_d_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            dialog = builder.setTitle("第一次使用")
                    .setView(dialogView)
                    .setIcon(R.mipmap.ic_launcher)
                    .setNegativeButton("cancel", this)
                    .setPositiveButton("OK", this).create();
        }
        return dialog;
    }


    private AlertDialog getRecountDaysDialog() {
        if (dialog_recount_days == null) {
            dialogView_recountDays = getLayoutInflater().inflate(R.layout.dialog_view_recount_days, null);
            et_d_recount_days = (EditText) dialogView_recountDays.findViewById(R.id.et_d_recount_days);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            dialog_recount_days = builder.setTitle("设置已经锻炼的天数(不包括今天)")
                    .setView(dialogView_recountDays)
                    .setIcon(R.mipmap.ic_launcher)
                    .setNegativeButton("cancel", this)
                    .setPositiveButton("OK", this).create();
        }
        return dialog_recount_days;
    }

    public void shoot_pic(View view) {
//        暂时不需要判断有没有名字,没有名字一样能生成图片
//        if (name == null || name.equals("")) {
//            setNameDialog();
//            return;
//        }
        if (!isExternalStorageAccessed) {
            PermissionRelative.obtainPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    REQUEST_PERMISSION_EXTERNAL_STORAGE);
        }
        if (isExternalStorageAccessed) {
            shootPic = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM), System.currentTimeMillis() + ".jpg");
        } else {
            shootPic = new File(getFilesDir(), System.currentTimeMillis() + ".jpg");
        }
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(shootPic));
        startActivityForResult(intent, SHOOT_PIC);
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    public void choose_pic(View view) {
//        if (name == null || name.equals("")) {
//            setNameDialog();
//            return;
//        }
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, CHOOSE_PIC);
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    private void setNameDialog() {
        getNameDialog().setTitle("设置名字");
        getNameDialog().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "获取图片失败,请找周亿进解决!1", Toast.LENGTH_LONG).show();
            return;
        }
        switch (requestCode) {
            case SHOOT_PIC:
                // TODO: 2016/8/2 启动编辑图片的activity ,intent添加file
                Intent intent1 = new Intent(this, HandleImageActivity.class);
                intent1.putExtra("handle_pic", shootPic);
                startActivity(intent1);
                break;
            case CHOOSE_PIC:
                if (data == null) {
                    break;
                }
                //用系统提供的contentprovider来获取选择图片的uri
                Uri selectedImage = data.getData();
                //设置一个与系统提供的query方法参数对应的String数组
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                //获取到的图片遍历出来
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                File chosen_pic = new File(picturePath);
                if (!chosen_pic.exists()) {
                    Toast.makeText(this, "获取图片失败,请找周亿进解决!2", Toast.LENGTH_LONG).show();
                    break;
                }
                DCIM_PATH = chosen_pic.getParent();
                Log.d("hbb", DCIM_PATH);
                //// TODO: 2016/8/2 把file传到处理图像的activity中
                Intent intent2 = new Intent(this, HandleImageActivity.class);
                intent2.putExtra("handle_pic", chosen_pic);
                startActivity(intent2);
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isExternalStorageAccessed = true;
                    return;
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("你不让我操作外部储存空间")
                        .setMessage("如果想再次开启,进入系统设置菜单,设置权限")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                break;
        }
    }


    //弹出的对话框获取用户姓名
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (dialogInterface.equals(dialog)) {
            switch (i) {
                case AlertDialog.BUTTON_POSITIVE:
                    name = et_d_name.getText().toString();
                    editor.putString("name", name.trim());
                    editor.commit();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    et_d_name.setText("");
                    break;
            }

        } else if (dialogInterface.equals(dialog_recount_days)) {
            switch (i) {
                case AlertDialog.BUTTON_POSITIVE:
                    workOutDays = Integer.valueOf(et_d_recount_days.getText().toString());
                    et_d_recount_days.setText("");
                    editor.putInt("work_out_days", workOutDays);
                    editor.putInt("last_work_out_year", -1);
                    editor.putInt("last_work_out_day", -1);
                    editor.commit();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    et_d_recount_days.setText("");
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        if (dialog_recount_days != null) {
            dialog_recount_days.dismiss();
            dialog_recount_days = null;
        }
    }
}

