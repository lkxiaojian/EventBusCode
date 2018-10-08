package com.example.lk.eventbuscode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.greenrobot.eventbus.EventBus;

public class MainActivity extends AppCompatActivity {
    private Button bt;
    private InfoBean infoBean = new InfoBean("我是要传递的信息", 1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt = findViewById(R.id.bt_frist);
        click();
    }

    private void click() {
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(infoBean);
                startActivity(new Intent(MainActivity.this,ScoundActivity.class));

            }
        });
    }
}
