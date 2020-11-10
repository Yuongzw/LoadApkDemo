package com.yuong.plugin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import com.yuong.plugin.base.BaseAppCompatActivity;

public class Plugin2Activity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Light_NoTitleBar);
        Log.d("yuongzw", "Plugin2Activity onCreate");
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_plugin2, null);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("yuongzw", "Plugin2Activity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("yuongzw", "Plugin2Activity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("yuongzw", "Plugin2Activity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("yuongzw", "Plugin2Activity onDestroy");
    }
}
