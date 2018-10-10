package com.example.lk.eventbuscode;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lk on 2018/10/9.
 */

public class CustomerEventBus {
    private static volatile CustomerEventBus customerEventBus;
    private Map<Object, List<CustomerSubscribeMethod>> catchMap;
    private Handler mHandler;
    private ExecutorService executorService;

    private CustomerEventBus() {
        catchMap = new HashMap<>();
        //让handler 在主线程中调用
        mHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }


    /**
     * 单例  获取CustomerEventBus 对象
     *
     * @return
     */
    public static CustomerEventBus getDefault() {
        if (customerEventBus == null) {
            synchronized (CustomerEventBus.class) {
                if (customerEventBus == null) {
                    customerEventBus = new CustomerEventBus();
                }
            }
        }
        return customerEventBus;
    }

    /**
     * 注册
     *
     * @param object
     */
    public void register(Object object) {
        List<CustomerSubscribeMethod> customerThreadModes = catchMap.get(object);
        if (customerThreadModes == null) {
            customerThreadModes = findSubscriberMethods(object);
            catchMap.put(object, customerThreadModes);
        }
    }

    private List<CustomerSubscribeMethod> findSubscriberMethods(Object object) {
        List<CustomerSubscribeMethod> threadModeList = new ArrayList<>();
        Class<?> aClass = object.getClass();
        while (aClass != null) {
            //判断是否是系统类，如果是的，就退出循环
            String name = aClass.getName();
            if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
                break;
            }
            //得到该类的所有方法
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                //看看该方法中，是否存在CustomerSubscribe 注解
                CustomerSubscribe annotation = method.getAnnotation(CustomerSubscribe.class);
                if (annotation == null) {
                    continue;
                }
                //得到类型参数，并保证传递的参数的个数为1，否则抛出异常
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new RuntimeException("EventBus Only one parameter can be received!!!");
                }
                //得到该方法的运行的线程
                CustomerThreadMode customerThreadMode = annotation.threadMode();
                CustomerSubscribeMethod customerSubscribeMethod = new CustomerSubscribeMethod(method, customerThreadMode, parameterTypes[0]);
                threadModeList.add(customerSubscribeMethod);
            }
            aClass = aClass.getSuperclass();
        }

        return threadModeList;

    }

    /**
     * 传递信息的方法，参数只能只一个
     *
     * @param object
     */
    public void post(final Object object) {
        //遍历 已经注册的 EventBus的方法，找到后，通过反射，来进行把信息传递过去
        Set<Object> set = catchMap.keySet();
        Iterator<Object> iterator = set.iterator();
        while (iterator.hasNext()) {
            final Object objnext = iterator.next();
            List<CustomerSubscribeMethod> list = catchMap.get(objnext);
            if (list == null) {
                break;
            }
            for (final CustomerSubscribeMethod customerSubscribeMethod : list) {
                //判断是否传过来的类型，如果是，才能调用反射
                if (customerSubscribeMethod.getType().isAssignableFrom(object.getClass())) {

                    switch (customerSubscribeMethod.getCustomerThreadMode()) {
                        case MAIN:
                            //通过Looper 来判断是否在主线程
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                invoke(customerSubscribeMethod, objnext, object);
                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(customerSubscribeMethod, objnext, object);
                                    }
                                });

                            }
                            break;
                        case BACKGROUND:

                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(customerSubscribeMethod, objnext, object);
                                    }
                                });
                            } else {
                                invoke(customerSubscribeMethod, objnext, object);
                            }
                            break;
                    }


                }

            }
        }


    }

    private void invoke(CustomerSubscribeMethod customerSubscribeMethod, Object next, Object object) {
        Method method = customerSubscribeMethod.getMethod();

        try {
            method.invoke(next, object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

}
