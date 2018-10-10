package com.example.lk.eventbuscode;

import java.lang.reflect.Method;

/**
 * Created by lk on 2018/10/9.
 */

public class CustomerSubscribeMethod {
    //回调方法
    private Method method;
    //返回回调的线程
    private CustomerThreadMode customerThreadMode;
    //回调方法中的参数
    private Class<?> type;

    public CustomerSubscribeMethod(Method method, CustomerThreadMode customerThreadMode, Class<?> type) {
        this.method = method;
        this.customerThreadMode = customerThreadMode;
        this.type = type;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public CustomerThreadMode getCustomerThreadMode() {
        return customerThreadMode;
    }

    public void setCustomerThreadMode(CustomerThreadMode customerThreadMode) {
        this.customerThreadMode = customerThreadMode;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }
}
