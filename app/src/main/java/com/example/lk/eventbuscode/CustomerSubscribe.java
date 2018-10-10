package com.example.lk.eventbuscode;

import org.greenrobot.eventbus.ThreadMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by lk on 2018/10/9.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomerSubscribe {
    CustomerThreadMode threadMode() default CustomerThreadMode.POSTING;
}
