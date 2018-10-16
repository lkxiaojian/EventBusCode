一  基于EventBus 3.0以后的版本 主要包括


   1.Subscribe注解
   @Subscribe(threadMode = ThreadMode.MAIN)
public void onMessageEvent(MessageEvent event) {
    Toast.makeText(getActivity(), event.message, Toast.LENGTH_SHORT).show();
}

   2.注册事件的订阅方法
EventBus.getDefault().register(this);

   3.取消注册方法
 EventBus.getDefault().unregister(this);

   4.发送事件
EventBus.getDefault().post(new MessageEvent("Hello everyone!"));

   5.事件的处理

   6.粘性事件


二 Subscribe 注解

    
    @Documented
    @Retention(RetentionPolicy.RUNTIME) 
    @Target({ElementType.METHOD})
    public @interface Subscribe {
    
    ThreadMode threadMode() default ThreadMode.POSTING;
    
    /**
    *控制粘性事件的 如果为true  支持粘性事件,就会调用postSticky 到订阅事件中  默认为fasle
    */
    boolean sticky() default false;
    
    /** 
    *优先级
    */
    int priority() default 0;
    }
    


这几个是元注解  （用来标记注解的注解）
@Documented
Documented注解表明制作javadoc时，是否将注解信息加入文档。如果注解在声明时使用了@Documented，则在制作javadoc时注解信息会加入javadoc

@Retention(RetentionPolicy.RUNTIME) 
注解的保留位置　

    public enum RetentionPolicy {
    //保留到项目里里面
    SOURCE,
    //保留到java 编译之后的class类里面
    CLASS,
    
    //保存到运行时
    RUNTIME
    }
    

@Target({ElementType.METHOD})
注解的使用方式，就是这个注解只能放在特定的位置


    public enum ElementType {
    /** 
    *   接口、类、枚举、注解
     */
    TYPE,
    
    /** 
    * 字段、枚举的常量
    */
    FIELD,
    
    /** 
    * 方法
    */
    METHOD,
    
    /** 
    * 方法参数
     */
    PARAMETER,
    
    /** 
    *  构造函数
    */
    CONSTRUCTOR,
    
    /** 
    * 局部变量
    */
    LOCAL_VARIABLE,
    
    /** 
    * 注解
    */
    ANNOTATION_TYPE,
    
    /** 
    * 包 
     */
    PACKAGE,
    
    /**
    * Type parameter declaration
    *
    * @since 1.8
    */
    TYPE_PARAMETER,
    
    /**
    * Use of a type
    *
    * @since 1.8
    */
    TYPE_USE
    }
    

实际上还有一个注解  @Inherited  ： 子类可以继承父类中的该注解  这是jdk1.5之后提供的方法


ThreadMode threadMode() default ThreadMode.POSTING;

这个是判断你想要的回调方法在哪个线程,就是在什么线程处理订阅的方法


public enum ThreadMode {
/**
* post方法跑在那个方法，回调就跑在那个方法  默认的线程，避免切换线程，效率高点
*/
POSTING,

/**
*主线程发送事件，则直接在主线程中处理，子线程发送事件，通过handler切换到主线程
*
* 回调在主线程  
*/
MAIN,

/**
* y有序的主线程   
*无论在那个线程发送事件，都先将事件入队列，然后通过 Handler 切换到主线程，依次处理事件
*/
MAIN_ORDERED,

/**
*  如果在主线程发送事件，则先将事件入队列，然后通过线程池依次处理事件；如果在子线程发送事件，则直接在发送事件的线程处理事件
*回调在子线程
*/
BACKGROUND,

/**
* 回调在子线程，并重新创建一个线程  通过线程池处理
*/
ASYNC
}

三 注册事件的订阅方法


EventBus.getDefault().register(this);

getDefault()是一个单利方法 ，保证并返回一个Eventbus对象



    /**
    * Convenience singleton for apps using a process-wide EventBus instance.
    * <p>
    * 单利 创建对象
    */
    public static EventBus getDefault() {
    EventBus instance = defaultInstance;
    if (instance == null) {
    synchronized (EventBus.class) {
    instance = EventBus.defaultInstance;
    if (instance == null) {
    instance = EventBus.defaultInstance = new EventBus();
    }
    }
    }
    return instance;
    }
    

这里的new EventBus(); 是对相关属性的初始化


    public EventBus() {
    this(DEFAULT_BUILDER);
    }
    

    EventBus(EventBusBuilder builder) {
    logger = builder.getLogger();
    subscriptionsByEventType = new HashMap<>();
    typesBySubscriber = new HashMap<>();
    stickyEvents = new ConcurrentHashMap<>();
    mainThreadSupport = builder.getMainThreadSupport();
    mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
    backgroundPoster = new BackgroundPoster(this);
    asyncPoster = new AsyncPoster(this);
    indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
    subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
    builder.strictMethodVerification, builder.ignoreGeneratedIndex);
    logSubscriberExceptions = builder.logSubscriberExceptions;
    logNoSubscriberMessages = builder.logNoSubscriberMessages;
    sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
    sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
    throwSubscriberException = builder.throwSubscriberException;
    eventInheritance = builder.eventInheritance;
    executorService = builder.executorService;
    }
    

DEFAULT_BUILDER 就是一个默认的EventBusBuilder


    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();


可以通过EventBusBuilder 改变EventBus的属性

    
    EventBus.builder() .eventInheritance(false) .logSubscriberExceptions(false) .build() .register(this);

然后就是注册了


    public void register(Object subscriber) {
    Class<?> subscriberClass = subscriber.getClass();
    // 获取订阅者的订阅方法并用List封装
    List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
    synchronized (this) {
    // 逐个订阅
    for (SubscriberMethod subscriberMethod : subscriberMethods) {
    subscribe(subscriber, subscriberMethod);
    }
    }
    }


冲代码中可以看出，register()分为查找和注册  分别是findSubscriberMethods（） 和subscribe（）
findSubscriberMethods（）

    `
    
    List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
    //METHOD_CACHE 是一个map集合，已订阅者的Class对象为key,订阅者的订阅的方法为value  包括 优先级，粘性事件，订阅方法，事件类型 ，线程池
    List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
    //缓存中已经注册过了，就直接缓存
    if (subscriberMethods != null) {
    return subscriberMethods;
    }
    //没有从缓存中获取注册信息，就要 添加缓存
    // SubscriberMethodFinder通过EventBusBuilder的同名属性赋值的 ，builder.ignoreGeneratedIndex 这个值默认是false
    if (ignoreGeneratedIndex) {
    //通过反射获取
    subscriberMethods = findUsingReflection(subscriberClass);
    } else {
    //使用SubscriberIndex方式获取
    subscriberMethods = findUsingInfo(subscriberClass);
    }
    if (subscriberMethods.isEmpty()) {
    throw new EventBusException("Subscriber " + subscriberClass
    + " and its super classes have no public methods with the @Subscribe annotation");
    } else {
    //将注册信息存到缓存中
    METHOD_CACHE.put(subscriberClass, subscriberMethods);
    return subscriberMethods;
    }
    }
    
    `




现从缓存中查找，如果查找到直接返回，如果没有，再进行查找 ，并将注册信息放入到缓存中
因为 ignoreGeneratedIndex 这个值默认为false,所以默认执行findUsingInfo(subscriberClass);方法
findUsingInfo（）


    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
    //获取FindState对象
    FindState findState = prepareFindState();
    // findState与subscriberClass关联
    findState.initForSubscriber(subscriberClass);
    while (findState.clazz != null) {
    // 获取当前clazz对应的SubscriberInfo
    findState.subscriberInfo = getSubscriberInfo(findState);
    if (findState.subscriberInfo != null) {
    SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
    for (SubscriberMethod subscriberMethod : array) {
    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
    findState.subscriberMethods.add(subscriberMethod);
    }
    }
    } else {
    findUsingReflectionInSingleClass(findState);
    }
    findState.moveToSuperclass();
    }
    return getMethodsAndRelease(findState);
    }
    

FindState类，它是SubscriberMethodFinder的内部类，用来辅助查找订阅事件的方法  查找的方法 findUsingReflectionInSingleClass（） 主要通过反射查找订阅事件的


    private void findUsingReflectionInSingleClass(FindState findState) {
    Method[] methods;
    try {
    // This is faster than getMethods, especially when subscribers are fat classes like Activities
    //获取该订阅者所有的方法，比jdk 的getMethods更快
    methods = findState.clazz.getDeclaredMethods();
    } catch (Throwable th) {
    // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
    methods = findState.clazz.getMethods();
    findState.skipSuperClasses = true;
    }
    for (Method method : methods) {
    //获取该方法的修饰字段，如public、private、static、final、synchronized、abstract等
    int modifiers = method.getModifiers();
    //修饰的方法只能是public,static否则会抛出异常 所以当我们在订阅类中，使用的不是public 的时候，就收到信息
    if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
    //获取方法参数
    Class<?>[] parameterTypes = method.getParameterTypes();
    //该参数的长度只能是一个，如果是多个就是抛出异常
    if (parameterTypes.length == 1) {
    //判断该方法的是否存在Subscribe注解
    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
    if (subscribeAnnotation != null) {
    //因为该参数就只有一个，所以直接写成[0]  得到该参数的类型
    Class<?> eventType = parameterTypes[0];
    //检查eventType决定是否订阅
    if (findState.checkAdd(method, eventType)) {
    //获取当前线程
    ThreadMode threadMode = subscribeAnnotation.threadMode();
    //将订阅方法存进list
    findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
    }
    }
    } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
    throw new EventBusException("@Subscribe method " + methodName +
    "must have exactly 1 parameter but has " + parameterTypes.length);
    }
    } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
    throw new EventBusException(methodName +
    " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
    }
    }
    }
    

到此register()方法中findSubscriberMethods()流程就分析完了，我们已经找到了当前注册类及其父类中订阅事件的方法的集合。接下来分析具体的注册流程，即register()中的subscribe()方法


private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
    Class<?> eventType = subscriberMethod.eventType;
    // 创建Subscription封装订阅者和订阅方法信息
    Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
    // 根据事件类型从subscriptionsByEventType这个Map中获取Subscription集合
    CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
    // 若Subscription集合为空，创建并put进Map中
    if (subscriptions == null) {
        subscriptions = new CopyOnWriteArrayList<>();
        subscriptionsByEventType.put(eventType, subscriptions);
    } else {
        // 若集合中已包含该Subscription则抛异常
        if (subscriptions.contains(newSubscription)) {
            throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                    + eventType);
        }
    }

    int size = subscriptions.size();
    for (int i = 0; i <= size; i++) {
        // 按照优先级插入Subscription
        if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
            subscriptions.add(i, newSubscription);
            break;
        }
    }

    List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
    if (subscribedEvents == null) {
        subscribedEvents = new ArrayList<>();
        typesBySubscriber.put(subscriber, subscribedEvents);
    }
    subscribedEvents.add(eventType);
    // 订阅方法是否设置黏性模式
    if (subscriberMethod.sticky) {
        // 是否设置了事件继承
        if (eventInheritance) {
            // Existing sticky events of all subclasses of eventType have to be considered.
            // Note: Iterating over all events may be inefficient with lots of sticky events,
            // thus data structure should be changed to allow a more efficient lookup
            // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
            Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
            for (Map.Entry<Class<?>, Object> entry : entries) {
                Class<?> candidateEventType = entry.getKey();
                // 判断当前事件类型是否为黏性事件或者其子类
                if (eventType.isAssignableFrom(candidateEventType)) {
                    Object stickyEvent = entry.getValue();
                    // 执行设置了sticky模式的订阅方法
                    checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                }
            }
        } else {
            Object stickyEvent = stickyEvents.get(eventType);
            checkPostStickyEventToSubscription(newSubscription, stickyEvent);
        }
    }
}

这就是注册的核心流程，所以subscribe()方法主要是得到了subscriptionsByEventType、typesBySubscriber两个 HashMap。我们在发送事件的时候要用到subscriptionsByEventType，完成事件的处理。当取消 EventBus 注册的时候要用到typesBySubscriber、subscriptionsByEventType，完成相关资源的释放

四 取消注册

   EventBus.getDefault().unregister(this);

    
    public synchronized void unregister(Object subscriber) {
    //得到事件
    List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
    if (subscribedTypes != null) {
    for (Class<?> eventType : subscribedTypes) {
    unsubscribeByEventType(subscriber, eventType);
    }
    typesBySubscriber.remove(subscriber);
    } else {
    logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
    }
    }


先根据subscriber 从缓存读取，不为null ，执行 unsubscribeByEventType（）方法



    `
    /**
    * subscriptionsByEventType是存储事件类型对应订阅信息的Map，代码逻辑非常清晰，找出某事件类型的订阅信息List，遍历订阅信息，
    * 将要取消订阅的订阅者和订阅信息封装的订阅者比对，如果是同一个，则说明该订阅信息是将要失效的，于是将该订阅信息移除
    *
    */
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
    //得到事件
    List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
    if (subscriptions != null) {
    int size = subscriptions.size();
    //循环遍历  集合，判断当前事件是否与取消的事件相等
    for (int i = 0; i < size; i++) {
    Subscription subscription = subscriptions.get(i);
    if (subscription.subscriber == subscriber) {
    subscription.active = false;
    subscriptions.remove(i);
    i--;
    size--;
    }
    }
    }
    }
    `



五发送事件

EventBus.getDefault().post(new MessageEvent("Hello everyone!"));

发送事件主要通过post()方法
   `
    public void post(Object event) {
    // 获取当前线程的posting状态
    PostingThreadState postingState = currentPostingThreadState.get();
    List<Object> eventQueue = postingState.eventQueue;
    // 将事件添加进当前线程的事件队列
    eventQueue.add(event);
    // 判断当前线程是否正在发布事件
    if (!postingState.isPosting) {
    //判断当前线程是否是主线程
    postingState.isMainThread = isMainThread();
    //设置当前线程正在发布事件
    postingState.isPosting = true;
    //判断当前线程是否取消发布
    if (postingState.canceled) {
    throw new EventBusException("Internal error. Abort state was not reset");
    }
    try {
       //遍历事件
    while (!eventQueue.isEmpty()) {
       //发送单个事件
       //eventQueue.remove(0) 从事件队列中移除
    postSingleEvent(eventQueue.remove(0), postingState);
    }
    } finally {
    postingState.isPosting = false;
    postingState.isMainThread = false;
    }
    }
    }

` 




循环出队列，执行postSingleEvent（）方法


    private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
    //得到事件类型
    Class<?> eventClass = event.getClass();
    //是否找打事件发布
    boolean subscriptionFound = false;
    //是否是继承  若是，找出发布事件的所有父类
    if (eventInheritance) {
    //找到所有的父类 在循环遍历
    List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
    int countTypes = eventTypes.size();
    for (int h = 0; h < countTypes; h++) {
    Class<?> clazz = eventTypes.get(h);
    subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
    }
    } else {
    //不是继承，  直接发布事件
    subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
    }
    //没有找到事件 发布，发布一个 NoSubscriberEvent事件
    if (!subscriptionFound) {
    if (logNoSubscriberMessages) {
    logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
    }
    if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
    eventClass != SubscriberExceptionEvent.class) {
    post(new NoSubscriberEvent(this, event));
    }
    }
    }
    
postSingleEvent()方法中，根据eventInheritance属性，决定是否向上遍历事件的父类型，然后用postSingleEventForEventType()方法进一步处理事件：

    
    private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
    CopyOnWriteArrayList<Subscription> subscriptions;
    synchronized (this) {
    // 根据事件类型找出相关的订阅信息
    subscriptions = subscriptionsByEventType.get(eventClass);
    }
    if (subscriptions != null && !subscriptions.isEmpty()) {
    for (Subscription subscription : subscriptions) {
    postingState.event = event;
    postingState.subscription = subscription;
    boolean aborted = false;
    try {
    postToSubscription(subscription, event, postingState.isMainThread);
    aborted = postingState.canceled;
    } finally {
    postingState.event = null;
    postingState.subscription = null;
    postingState.canceled = false;
    }
    if (aborted) {
    break;
    }
    }
    return true;
    }
    return false;
    }
    
postSingleEvent()方法中，根据eventInheritance属性，决定是否向上遍历事件的父类型，然后用postSingleEventForEventType()方法进一步处理事件：
六 事件的处理postToSubscription（）

    private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
    switch (subscription.subscriberMethod.threadMode) {
    // 订阅线程跟随发布线程
    case POSTING:
    // 订阅线程和发布线程相同，直接订阅
    invokeSubscriber(subscription, event);
    break;
    // 订阅线程为主线程
    case MAIN:
    if (isMainThread) {
    // 发布线程和订阅线程都是主线程，直接订阅
    invokeSubscriber(subscription, event);
    } else {
    // 发布线程不为主线程，通过线handler 进行转换后，发布
    mainThreadPoster.enqueue(subscription, event);
    }
    break;
    //订阅在主线程  有序的
    case MAIN_ORDERED:
    if (mainThreadPoster != null) {
    mainThreadPoster.enqueue(subscription, event);
    } else {
    // temporary: technically not correct as poster not decoupled from subscriber
    //暂时：技术上不正确，因为发布者与订阅者没有解耦
    invokeSubscriber(subscription, event);
    }
    break;
    // 订阅线程为后台线程
    case BACKGROUND:
    if (isMainThread) {
    // 发布线程为主线程，通过线程池切换到后台线程订阅
    backgroundPoster.enqueue(subscription, event);
    } else {
    // 发布线程不为主线程，直接订阅
    invokeSubscriber(subscription, event);
    }
    break;
    // 订阅线程为异步线程
    case ASYNC:
    // 使用线程池线程订阅 例如线程池
    asyncPoster.enqueue(subscription, event);
    break;
    default:
    throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
    }
    }

一种是通过反射  invokeSubscriber（） 主要是用反射


/**
* 通过反射，发布事件
* @param subscription
* @param event
*/
void invokeSubscriber(Subscription subscription, Object event) {
    try {
        subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
    } catch (InvocationTargetException e) {
        handleSubscriberException(subscription, event, e.getCause());
    } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unexpected exception", e);
    }
}

一种是 通过handler 主要是实现 HandlerPoster 这个类

    mainThreadPoster.enqueue(subscription, event);

    public class HandlerPoster extends Handler implements Poster {
    private final PendingPostQueue queue;
    private final int maxMillisInsideHandleMessage;
    private final EventBus eventBus;
    private boolean handlerActive;

    protected HandlerPoster(EventBus eventBus, Looper looper, int maxMillisInsideHandleMessage) {
        super(looper);
        this.eventBus = eventBus;
        this.maxMillisInsideHandleMessage = maxMillisInsideHandleMessage;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription, Object event) {
        // 用subscription和event封装一个PendingPost对象
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            // 入队列
            queue.enqueue(pendingPost);
            if (!handlerActive) {
                handlerActive = true;
                // 发送开始处理事件的消息，handleMessage()方法将被执行，完成从子线程到主线程的切换
                if (!sendMessage(obtainMessage())) {
                    throw new EventBusException("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean rescheduled = false;
        try {
            long started = SystemClock.uptimeMillis();
            // 死循环遍历队列
            while (true) {
                // 出队列
                PendingPost pendingPost = queue.poll();
                if (pendingPost == null) {
                    synchronized (this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll();
                        if (pendingPost == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                // 进一步处理pendingPost
                eventBus.invokeSubscriber(pendingPost);
                long timeInMethod = SystemClock.uptimeMillis() - started;
                if (timeInMethod >= maxMillisInsideHandleMessage) {
                    if (!sendMessage(obtainMessage())) {
                        throw new EventBusException("Could not send handler message");
                    }
                    rescheduled = true;
                    return;
                }
            }
        } finally {
            handlerActive = rescheduled;
        }
    }
}
所以HandlerPoster的enqueue()方法主要就是将subscription、event对象封装成一个PendingPost对象，然后保存到队列里，之后通过Handler切换到主线程，在handleMessage()方法将中将PendingPost对象循环出队列，交给invokeSubscriber()方法进一步处理：


void invokeSubscriber(PendingPost pendingPost) {
    Object event = pendingPost.event;
    // 释放pendingPost引用的资源
    Subscription subscription = pendingPost.subscription;
    PendingPost.releasePendingPost(pendingPost);
    if (subscription.active) {
        // 用反射来执行订阅事件的方法
        invokeSubscriber(subscription, event);
    }
}

这个方法很简单，主要就是从pendingPost中取出之前保存的event、subscription，然后用反射来执行订阅事件的方法，又回到了第一种处理方式。所以mainThreadPoster.enqueue(subscription, event)的核心就是先将将事件入队列，然后通过Handler从子线程切换到主线程中去处理事件。
backgroundPoster.enqueue()和asyncPoster.enqueue也类似，内部都是先将事件入队列，然后再出队列，但是会通过线程池去进一步处理事件

七 粘性事件
接受


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void receiveSoundRecongnizedmsg(String insType) {
     
    }

发送

    EventBus.getDefault().postSticky("Hello World!");


    public void postSticky(Object event) {
    synchronized (stickyEvents) {
    stickyEvents.put(event.getClass(), event);
    }
    // Should be posted after it is putted, in case the subscriber wants to remove immediately
    post(event);
    }

postSticky()方法主要做了两件事，先将事件类型和对应事件保存到stickyEvents中，方便后续使用；然后执行post(event)继续发送事件，这个post()方法就是之前发送的post()方法。所以，如果在发送粘性事件前，已经有了对应类型事件的订阅者，及时它是非粘性的，依然可以接收到发送出的粘性事件

发送完粘性事件后，再准备订阅粘性事件的方法，并完成注册。核心的注册事件流程还是我们之前的register()方法中的subscribe()方法，前边分析subscribe()方法时，有一段没有分析的代码，就是用来处理粘性事件的：


    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
    Class<?> eventType = subscriberMethod.eventType;
    // 创建Subscription封装订阅者和订阅方法信息
    Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
    // 根据事件类型从subscriptionsByEventType这个Map中获取Subscription集合
    CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
    // 若Subscription集合为空，创建并put进Map中
    if (subscriptions == null) {
        subscriptions = new CopyOnWriteArrayList<>();
        subscriptionsByEventType.put(eventType, subscriptions);
    } else {
        // 若集合中已包含该Subscription则抛异常
        if (subscriptions.contains(newSubscription)) {
            throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                    + eventType);
        }
    }

    int size = subscriptions.size();
    for (int i = 0; i <= size; i++) {
        // 按照优先级插入Subscription
        if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
            subscriptions.add(i, newSubscription);
            break;
        }
    }

    List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
    if (subscribedEvents == null) {
        subscribedEvents = new ArrayList<>();
        typesBySubscriber.put(subscriber, subscribedEvents);
    }
    subscribedEvents.add(eventType);
    // 订阅方法是否设置黏性模式
    if (subscriberMethod.sticky) {
        // 是否设置了事件继承
        if (eventInheritance) {
            // Existing sticky events of all subclasses of eventType have to be considered.
            // Note: Iterating over all events may be inefficient with lots of sticky events,
            // thus data structure should be changed to allow a more efficient lookup
            // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
            Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
            for (Map.Entry<Class<?>, Object> entry : entries) {
                Class<?> candidateEventType = entry.getKey();
                // 判断当前事件类型是否为黏性事件或者其子类
                if (eventType.isAssignableFrom(candidateEventType)) {
                    Object stickyEvent = entry.getValue();
                    // 执行设置了sticky模式的订阅方法
                    checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                }
            }
        } else {
            Object stickyEvent = stickyEvents.get(eventType);
            checkPostStickyEventToSubscription(newSubscription, stickyEvent);
        }
    }
}
红色部分是粘性事件的处理
可以看到，处理粘性事件就是在 EventBus 注册时，遍历stickyEvents，如果当前要注册的事件订阅方法是粘性的，并且该方法接收的事件类型和stickyEvents中某个事件类型相同或者是其父类，则取出stickyEvents中对应事件类型的具体事件，做进一步处理。继续看checkPostStickyEventToSubscription()处理方法


    private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
    if (stickyEvent != null) {
    // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
    // --> Strange corner case, which we don't take care of here.
    postToSubscription(newSubscription, stickyEvent, isMainThread());
    }
    }

regiser

https://user-gold-cdn.xitu.io/2018/4/27/1630655a5f017123?imageView2/0/w/1280/h/960/format/webp/ignore-error/1

post

https://user-gold-cdn.xitu.io/2018/4/27/1630655a5f12b2d7?imageView2/0/w/1280/h/960/format/webp/ignore-error/1


unregister

https://user-gold-cdn.xitu.io/2018/4/27/1630655a5f7e683b?imageView2/0/w/1280/h/960/format/webp/ignore-error/1

