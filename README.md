# EventBusCode
EventBus源码解析，并手动实现
 在EventBus源码解析.md是对EventBus的源码解析 ,但是排版不是太清晰
 在EventBus源码解析.html是清晰的，但是需要下载下来看，下面是手动实现

1 创建CustomerSubscribeMethod bean类  

     //回调方法
    private Method method;
    //返回回调的线程
    private CustomerThreadMode customerThreadMode;
    //回调方法中的参数
    private Class<?> type;
    包含三个字段 ，主要是用与线程的切换，回调方法，回调方法中的参数
    
2 创建 CustomerEventBus 仿造EventBus register() post()方法
   
       public void register(Object object) {
        List<CustomerSubscribeMethod> customerThreadModes = catchMap.get(object);
        if (customerThreadModes == null) {
            customerThreadModes = findSubscriberMethods(object);
            catchMap.put(object, customerThreadModes);
        }
    }
主要是把 注册的时候，该类的信息存进一个map里面，进行对注册的信息进行判断 如 是否是系统类，传递参数的个数，是否存在对应的注解


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
    
    

 3.post()遍历注册时候的map,根据传递的类型做判断，和实现线程直接的切换


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
                        case MAIN: //注册的时候，注解是主线程
                            //通过Looper 来判断post()方法是否在主线程 是主线程 直接调用
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                invoke(customerSubscribeMethod, objnext, object);
                            } else {
                                //如果post()是子线程，通过handler 让其跳转到主线程
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(customerSubscribeMethod, objnext, object);
                                    }
                                });

                            }
                            break;
                        case BACKGROUND://注册的时候，注解是子线程
                            //通过Looper 来判断post()方法是否在主线程  是主线程   通过线程池，让其在子线程中运行
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(customerSubscribeMethod, objnext, object);
                                    }
                                });
                            } else {
                                //post()方法是在子线程中运行 ，直接调用
                                invoke(customerSubscribeMethod, objnext, object);
                            }
                            break;
                    }


                }

            }
        }


    }
