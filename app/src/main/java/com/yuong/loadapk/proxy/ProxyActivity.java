package com.yuong.loadapk.proxy;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @author :
 * date   : 2020/6/1
 * desc   :
 */
public class ProxyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "我是代理的Activity", Toast.LENGTH_SHORT).show();
    }
}
