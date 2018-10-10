package com.example.lk.eventbuscode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ScoundActivity extends AppCompatActivity {
    private String TAG = "INFO";
    private InfoBean infoBean = new InfoBean("我是ScoundActivity要传递的信息", 1);
    private Button bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scound);
        bt = findViewById(R.id.bt_two);
//        CustomerEventBus.getDefault().register(this);
        click();
    }

    private void click() {
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //主线程 传递
                CustomerEventBus.getDefault().post(infoBean);
                //子线程传递
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        CustomerEventBus.getDefault().post(infoBean);
//                    }
//                }).start();


                startActivity(new Intent(ScoundActivity.this, MainActivity.class));
            }
        });
    }

//    @CustomerSubscribe(threadMode = CustomerThreadMode.MAIN)
//    public void onMessageEvent(InfoBean event) {
//        Log.e(TAG, event.getMessage().toString());
//
//    }

    @Override
    protected void onStop() {
        super.onStop();
//        CustomerEventBus.getDefault().unregister(this);
    }
}
