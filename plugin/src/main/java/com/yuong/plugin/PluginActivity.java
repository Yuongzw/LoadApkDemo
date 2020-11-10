package com.yuong.plugin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.yuong.plugin.base.BaseActivity;

public class PluginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        Log.d("yuongzw", "PluginActivity onCreate");
        findViewById(R.id.btn_jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PluginActivity.this, Plugin2Activity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("yuongzw", "PluginActivity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("yuongzw", "PluginActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("yuongzw", "PluginActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("yuongzw", "PluginActivity onDestroy");
    }
}
