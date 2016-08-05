package com.example.zyj.hbbtakephotograph.ImageTool;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.example.zyj.hbbtakephotograph.MyApplication;

/**
 * Created by Administrator on 2016/8/2.
 */
public class PermissionRelative {
    public static boolean isPermissionGranted(String permission) {
        if (ContextCompat.checkSelfPermission(MyApplication.getContext(), permission)
                == PackageManager.PERMISSION_DENIED) {
            return false;
        }
        return true;
    }

    public static void obtainPermission(Activity activity, String permission, int requestCode) {
        if (isPermissionGranted(permission)) {
            return;
        }
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
    }

}
