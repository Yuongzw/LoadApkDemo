
package com.yuong.loadapk;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yuong.loadapk.manager.HookManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
    }

    public void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,

                    Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //通过requestCode来识别是否同一个请求
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户同意，执行操作
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //先hook AMS检查
                            HookManager.getInstance(MainActivity.this.getApplication()).hookAMSAction();
                            //hook ActivityThread
                            HookManager.getInstance(MainActivity.this.getApplication()).hookLaunchActivity();
                            //hook LoadApk
                            HookManager.getInstance(MainActivity.this.getApplication()).customLoadApkAction();
                            MyApplication.isHookFinish = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            } else {
                //用户不同意，向用户展示该权限作用
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setMessage("请设置读写SD卡权限")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                }
            }
        }
    }

    public void jumpPlugin(View view) {
        Log.d("yuongzw", "MyApplication.isHookFinish=" + MyApplication.isHookFinish);
        if (MyApplication.isHookFinish) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.yuong.plugin", "com.yuong.plugin.PluginActivity"));
            startActivity(intent);
        } else {
            Log.d("yuongzw", "没Hook住系统Api，不能启动插件Activity");
        }
    }

}
