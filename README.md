# Tomcat源码

此项目来自于我的Tomcat源码解析，对应的Tomcat8.5.X的源码，在学习过程中会加入很多的中文注释，并且有相关的博客专栏来记录对Tomcat源码的学习。让我们一起进入tomcat的世界，拥抱源码吧.如有问题，欢迎来私信交流

[Tomcat结构设计](https://blog.csdn.net/qq_34037358/article/details/114373580?spm=1001.2014.3001.5501)

[Tomcat启动过程](https://blog.csdn.net/qq_34037358/article/details/114698048?spm=1001.2014.3001.5501)

[Tomcat类加载器](https://blog.csdn.net/qq_34037358/article/details/115261618?spm=1001.2014.3001.5501)

[Tomcat的Web请求和处理过程](https://blog.csdn.net/qq_34037358/article/details/115579034)

下面对Tomcat启动过程的源码为例，进行源码分析，其他的就去看博客吧。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210320173030962.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM0MDM3MzU4,size_16,color_FFFFFF,t_70)

回顾一下上一节Tomcat的架构：
（1）Server：代表整个Servelt容器
（2）service：包含一个和多个Connector和一个Container组件
（3）Connector：连接器
（4）Container：通用的容器概念
（5）Engine：Servelt引擎
（6）Host:主机
（7）Context：WebApp
（8）Wrapper：Servelt
（9）Excutor：线程池

<hr style=" border:solid; width:100px; height:1px;" color=#000000 size=1">

## 一、概述
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210324214014355.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM0MDM3MzU4,size_16,color_FFFFFF,t_70)
Tomcat的启动分为两步，分别是init和start。Tomcat的请求处理是经过多个责任链模式，从Engine容器开始的子容器都含有Pipleline责任链模式。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210412154822822.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM0MDM3MzU4,size_16,color_FFFFFF,t_70)
## 二、init阶段
（1）用户通过Bootstrap类的main函数创建Bootstrap对象，并调用Bootstrap的init方法;
初始化Tomcat自定义的类加载器
利用反射创建org.apache.catalina.startup.Catalina并为其设置父类加载器。
```java
 public void init() throws Exception {
        ///初始化类加载器
        initClassLoaders();
        // 设置当前线程的类加载器
        Thread.currentThread().setContextClassLoader(catalinaLoader);

        SecurityClassLoad.securityClassLoad(catalinaLoader);

        // 加载我们的启动类，并且调用他的process方法。
        if (log.isDebugEnabled())
            log.debug("Loading startup class");
        Class<?> startupClass = catalinaLoader.loadClass("org.apache.catalina.startup.Catalina");
        Object startupInstance = startupClass.getConstructor().newInstance();

        // Set the shared extensions class loader
        // 设置共享的拓展类加载器
        if (log.isDebugEnabled())
            log.debug("Setting startup class properties");
        String methodName = "setParentClassLoader";
        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Class.forName("java.lang.ClassLoader");
        Object paramValues[] = new Object[1];
        paramValues[0] = sharedLoader;
        Method method =
                startupInstance.getClass().getMethod(methodName, paramTypes);
        method.invoke(startupInstance, paramValues);
        // catalinaDaemon赋值为Catalina
        catalinaDaemon = startupInstance;
    }
```
（2）执行load方法，加载Catalina（通过反射）
```java
private void load(String[] arguments) throws Exception {
        // 调用load方法
        String methodName = "load";
        Object param[];
        Class<?> paramTypes[];
        if (arguments == null || arguments.length == 0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method =
                catalinaDaemon.getClass().getMethod(methodName, paramTypes);
        if (log.isDebugEnabled()) {
            log.debug("Calling startup class " + method);
        }
        // 反射执行Catalina的load方法。
        method.invoke(catalinaDaemon, param);
    }
```
（3）Catalina的load方法通过Degister解析conf/server.xml创建Server对象，并执行Server的init方法。
（4）Server接口的标准实现类StandardServer，由于StandardServer的父类的父类LifecycleBase继承了Lifecycle，并重写了init方法将  initInternal方法作为模板方法，所以执行Server的init方法会调用StandardServer的initInternal方法。在该方法中，执行了Service的init方法。
注意从这里，一个Server包含多个Service。
```java
// 初始化我们的service
// services的数量取决于server.xml内配置的<service></service>标签的数量
for (Service service : services) {
      service.init();
  }
```
（5）同样的道理执行Service接口的标准实现类StandardService的initInternal方法，在该方法中启动了Engine、线程池、连接器。
```java
  protected void initInternal() throws LifecycleException {

        super.initInternal();
        // 启动engine!!!!
        if (engine != null) {
            engine.init();
        }

        // 初始化连接池
        for (Executor executor : findExecutors()) {
            if (executor instanceof JmxEnabled) {
                ((JmxEnabled) executor).setDomain(getDomain());
            }
            executor.init();
        }

        // 初始化mapper监听器
        mapperListener.init();

        // 初始化我们的连接器！！！
        synchronized (connectorsLock) {
            for (Connector connector : connectors) {
                try {
                    connector.init();
                } catch (Exception e) {
                    String message = sm.getString(
                            "standardService.connector.initFailed", connector);
                    log.error(message, e);

                    if (Boolean.getBoolean("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE"))
                        throw new LifecycleException(message);
                }
            }
        }
    }
```
(7)connector.init()也是同样的道理，执行的是initInternal方法，在该方法中初始化了CoyoteAdapter适配器，也初始化了protocolHandler协议处理器。
其中Connector的构造函数里面默认指定了ProtocolHandler为Http11NioProtocol。所以这里初始化的是Http11NioProtocol。
```java
 protected void initInternal() throws LifecycleException {

        super.initInternal();

        // 初始化适配器
        adapter = new CoyoteAdapter(this);
        protocolHandler.setAdapter(adapter);

        // Make sure parseBodyMethodsSet has a default
        if (null == parseBodyMethodsSet) {
            setParseBodyMethods(getParseBodyMethods());
        }

        if (protocolHandler.isAprRequired() && !AprLifecycleListener.isInstanceCreated()) {
            throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerNoAprListener",
                    getProtocolHandlerClassName()));
        }
        if (protocolHandler.isAprRequired() && !AprLifecycleListener.isAprAvailable()) {
            throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerNoAprLibrary",
                    getProtocolHandlerClassName()));
        }
        if (AprLifecycleListener.isAprAvailable() && AprLifecycleListener.getUseOpenSSL() &&
                protocolHandler instanceof AbstractHttp11JsseProtocol) {
            AbstractHttp11JsseProtocol<?> jsseProtocolHandler =
                    (AbstractHttp11JsseProtocol<?>) protocolHandler;
            if (jsseProtocolHandler.isSSLEnabled() &&
                    jsseProtocolHandler.getSslImplementationName() == null) {
                // OpenSSL is compatible with the JSSE configuration, so use it if APR is available
                jsseProtocolHandler.setSslImplementationName(OpenSSLImplementation.class.getName());
            }
        }

        try {
            /// 初始化协议处理器
            protocolHandler.init();
        } catch (Exception e) {
            throw new LifecycleException(
                    sm.getString("coyoteConnector.protocolHandlerInitializationFailed"), e);
        }
    }
```
（8）ProtocolHandler的实现类AbstractProtocol执行init方法初始化EndPoint的实现类NioEndPoint
```java
  public void init() throws Exception {
        if (getLog().isInfoEnabled()) {
            getLog().info(sm.getString("abstractProtocolHandler.init", getName()));
        }

        if (oname == null) {
            // Component not pre-registered so register it
            oname = createObjectName();
            if (oname != null) {
                Registry.getRegistry(null, null).registerComponent(this, oname, null);
            }
        }

        if (this.domain != null) {
            ObjectName rgOname = new ObjectName(domain + ":type=GlobalRequestProcessor,name=" + getName());
            this.rgOname = rgOname;
            Registry.getRegistry(null, null).registerComponent(
                    getHandler().getGlobal(), rgOname, null);
        }

        String endpointName = getName();
        endpoint.setName(endpointName.substring(1, endpointName.length() - 1));
        endpoint.setDomain(domain);
        // 初始化endpoint！！
        endpoint.init();
    }
```
## 三、 start阶段
（1）执行BootStrap的start方法，启动Catalina（通过反射）
```java
 public void start() throws Exception {
       if (catalinaDaemon == null) {
           init();
       }
       // 启动Catalina
       Method method = catalinaDaemon.getClass().getMethod("start", (Class[]) null);
       method.invoke(catalinaDaemon, (Object[]) null);
   }
```
（2）Catalina的start方法通过getServer().start();启动Server
（3）StandardServer的startInternal方法启动了Service
```java
protected void startInternal() throws LifecycleException {

        fireLifecycleEvent(CONFIGURE_START_EVENT, null);
        setState(LifecycleState.STARTING);

        globalNamingResources.start();

        // 启动我们自定义的Service
        synchronized (servicesLock) {
            for (Service service : services) {
                // 启动Service
                service.start();
            }
        }
    }
```
（4）StandardService的startInternal方法启动了Engine、Excutor、mapperListener、Connector 。Connector 的启动，启动了protocolHandler，启动了端口进行监听用户的请求。
```java
 protected void startInternal() throws LifecycleException {

        if (log.isInfoEnabled())
            log.info(sm.getString("standardService.start.name", this.name));
        setState(LifecycleState.STARTING);

        // 首先启动我们定义的容器
        if (engine != null) {
            synchronized (engine) {
                engine.start();
            }
        }
        // 启动线程池
        synchronized (executors) {
            for (Executor executor : executors) {
                executor.start();
            }
        }
        // 启动Mapper映射监听器
        mapperListener.start();

        // 启动 Connectors
        synchronized (connectorsLock) {
            for (Connector connector : connectors) {
                try {
                    // If it has already failed, don't try and start it
                    if (connector.getState() != LifecycleState.FAILED) {
                        connector.start();
                    }
                } catch (Exception e) {
                    log.error(sm.getString(
                            "standardService.connector.startFailed",
                            connector), e);
                }
            }
        }
    }
```
（5）从StandardEngine开始都含有子容器，并且都可以启动、关闭、添加和启动子容器。所以Engine、Host、Context还有Wrapper都继承了实现了Container接口的ContainerBase类。ContainerBase的startInternal方法抽象了容器的公共操作。所以这些容器的启动流程都是先执行一些自己的操作，然后调用父类的startInternal执行集群、域、子容器的启动并设置LifecycleState.STARTING事件。
```java
  @Override
    protected synchronized void startInternal() throws LifecycleException {

        // Log our server identification information
        if (log.isInfoEnabled())
            log.info("Starting Servlet Engine: " + ServerInfo.getServerInfo());

        // Standard container startup
        // 标准容器启动，调用父类ContainerBase的startInternal方法。
        super.startInternal();
    }
```
ContainerBase的startInternal实现如下：
```java
  protected synchronized void startInternal() throws LifecycleException {

        // Start our subordinate components, if any
        logger = null;
        getLogger();
        // 启动集群
        Cluster cluster = getClusterInternal();
        if (cluster instanceof Lifecycle) {
            ((Lifecycle) cluster).start();
        }
        // 启动域
        Realm realm = getRealmInternal();
        if (realm instanceof Lifecycle) {
            ((Lifecycle) realm).start();
        }

        // 获取所有子节点
        Container children[] = findChildren();
        List<Future<Void>> results = new ArrayList<>();
        for (Container child : children) {
            // 启动子节点，触发StandardHost的启动，进入步骤（6）
            results.add(startStopExecutor.submit(new StartChild(child)));
        }

        MultiThrowable multiThrowable = null;

        // Future设计模式，拿着票据等待
        for (Future<Void> result : results) {
            try {
                result.get();
            } catch (Throwable e) {
                log.error(sm.getString("containerBase.threadedStartFailed"), e);
                if (multiThrowable == null) {
                    multiThrowable = new MultiThrowable();
                }
                multiThrowable.add(e);
            }

        }
        if (multiThrowable != null) {
            throw new LifecycleException(sm.getString("containerBase.threadedStartFailed"),
                    multiThrowable.getThrowable());
        }

        // 启动我们PipeLine管道的每个节点（Value），包括最基本的。
        if (pipeline instanceof Lifecycle) {
            ((Lifecycle) pipeline).start();
        }

        // 通过设置状态，监听器激活XXXConfig。
        setState(LifecycleState.STARTING);

        // Start our thread
        threadStart();
    }
```
设置当前事件LifecycleState.STARTING，会激发EngineConfig，EngineConfig的start事件只打印了日志没有做其他处理。
XXXConfig是容器类事件的响应类
```java
 public void lifecycleEvent(LifecycleEvent event) {

        // Identify the engine we are associated with
        try {
            engine = (Engine) event.getLifecycle();
        } catch (ClassCastException e) {
            log.error(sm.getString("engineConfig.cce", event.getLifecycle()), e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            stop();

    }

  // Engine的start事件处理函数
    protected void start() {
        // Egine的start事件没做其他处理，只打印了日志。
        if (engine.getLogger().isDebugEnabled())
            engine.getLogger().debug(sm.getString("engineConfig.start"));
    }
```
（5）StandardHost的启动
```java
 @Override
    protected synchronized void startInternal() throws LifecycleException {

        // 设置错误报错
        String errorValve = getErrorReportValveClass();
        if ((errorValve != null) && (!errorValve.equals(""))) {
            try {
                boolean found = false;
                Valve[] valves = getPipeline().getValves();
                for (Valve valve : valves) {
                    if (errorValve.equals(valve.getClass().getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Valve valve =
                            (Valve) Class.forName(errorValve).getConstructor().newInstance();
                    getPipeline().addValve(valve);
                }
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                log.error(sm.getString(
                        "standardHost.invalidErrorReportValveClass",
                        errorValve), t);
            }
        }
        // 调用父类ContainerBase的startInternal方法，同样进行日志处理、集群、域、线程池、管道、添加子容器的处理。
        super.startInternal();
    }
```
HostConfig的start事件处理函数，执行了部署war包的操作。
```java
 public void start() {

        if (log.isDebugEnabled())
            log.debug(sm.getString("hostConfig.start"));

        try {
            ObjectName hostON = host.getObjectName();
            oname = new ObjectName
                    (hostON.getDomain() + ":type=Deployer,host=" + host.getName());
            Registry.getRegistry(null, null).registerComponent
                    (this, oname, this.getClass().getName());
        } catch (Exception e) {
            log.warn(sm.getString("hostConfig.jmx.register", oname), e);
        }

        if (!host.getAppBaseFile().isDirectory()) {
            log.error(sm.getString("hostConfig.appBase", host.getName(),
                    host.getAppBaseFile().getPath()));
            host.setDeployOnStartup(false);
            host.setAutoDeploy(false);
        }
        // 部署WebApps
        if (host.getDeployOnStartup())
            deployApps();
    }
```
（5）StandardContext的startInternal方法很复杂，并且重写之后并没有执行父类的startInternal方法，具体工作是：启动命名空间的资源、生成work目录（apache-tomcat-9.0.14\work）、添加必要的四类资源、启动子节点、Servelt容器初始化、加载并且初始化所有的load on startup的Servelt、setState(LifecycleState.STARTING)。
```java
// 启动命名空间的资源
if (namingResources != null) {
 	namingResources.start();
}
// 生成work目录。apache-tomcat-9.0.14\work
postWorkDirectory(); 
  // Add missing components as necessary
// 添加必要的资源
 // 资源分为四种：
 // 1、Pre：在Context.xml中定义的标签<PreResource></PreResource>
 // 2、Main：Web应用的主要资源，主要是WEB-INT/lib和WEB-INT/classes资源。
 // 3、JARs：<JarResource></JarResource>
 // 4、Post：<PostResource></PostResource>
if (getResources() == null) {   // (1) Required by Loader
     if (log.isDebugEnabled())
         log.debug("Configuring default Resources");

     try {
         setResources(new StandardRoot(this));
     } catch (IllegalArgumentException e) {
         log.error(sm.getString("standardContext.resourcesInit"), e);
         ok = false;
     }
 }
 if (ok) {
     // 启动资源。
     resourcesStart();
 }
 // 加载WebappLoader，webapp的类加载器。
 if (getLoader() == null) {
     WebappLoader webappLoader = new WebappLoader();
     webappLoader.setDelegate(getDelegate());
     setLoader(webappLoader);
 }
 // 通知生命周期监听器Configure启动事件。
 // 触发了ContextConfig的configureStart方法，configureStart执行webConfig方法，webConfig执行了configureContext启动了容器
 fireLifecycleEvent(Lifecycle.CONFIGURE_START_EVENT, null);

  // 如果没有被启动，那么启动我们的子节点
  for (Container child : findChildren()) {
      if (!child.getState().isAvailable()) {
          child.start();
      }
  }
 
 // 调用Servelt容器初始化
 for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry :
         initializers.entrySet()) {
     try {
         // 这里的ServletContext是ApplicationContext
         // 会执行StandardWrapper的load方法。
         entry.getKey().onStartup(entry.getValue(),
                 getServletContext());
     } catch (ServletException e) {
         log.error(sm.getString("standardContext.sciFail"), e);
         ok = false;
         break;
     }
 }
 // 加载并且初始化所有的load on startup的Servelt
 if (ok) {
      if (!loadOnStartup(findChildren())) {
          log.error(sm.getString("standardContext.servletFail"));
          ok = false;
      }
  }
 // 注意这个事件没人处理。
 setState(LifecycleState.STARTING);     
```
回顾一下load-on-startup
```xml
 web.xml中可以配置Servelt的启动时机
<servlet>
     <servlet-name>S1</servlet-name>
     <servlet-class>com.web.servlet.S1</servlet-class>
     <load-on-startup>1</load-on-startup>
</servlet>
load-on-startup的取值：
大于等于0：tomcat启动的时候就加载。同时也代表优先级，即数字越大优先级越低，0的优先级最高。
小于0：懒加载，即第一次调用Servelt的时候加载。默认就是-1
```
下面是这个逻辑的代码
```java
public boolean loadOnStartup(Container children[]) {

        // 收集所有需要启动时初始化的servlets
        TreeMap<Integer, ArrayList<Wrapper>> map = new TreeMap<>();
        for (Container child : children) {
            Wrapper wrapper = (Wrapper) child;
            int loadOnStartup = wrapper.getLoadOnStartup();
            if (loadOnStartup < 0) {
                // loadOnStartup小于0是懒加载，就是第一次使用Servelt时才加载。
                continue;
            }
            Integer key = Integer.valueOf(loadOnStartup);
            ArrayList<Wrapper> list = map.get(key);
            if (list == null) {
                list = new ArrayList<>();
                map.put(key, list);
            }
            list.add(wrapper);
        }

        // 加载收集到的启动时加载的servlet
        for (ArrayList<Wrapper> list : map.values()) {
            for (Wrapper wrapper : list) {
                try {
                    // load StandardWrapper
                    wrapper.load();
                } catch (ServletException e) {
                    getLogger().error(sm.getString("standardContext.loadOnStartup.loadException",
                            getName(), wrapper.getName()), StandardWrapper.getRootCause(e));
                    // NOTE: load errors (including a servlet that throws
                    // UnavailableException from the init() method) are NOT
                    // fatal to application startup
                    // unless failCtxIfServletStartFails="true" is specified
                    if (getComputedFailCtxIfServletStartFails()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
```
（6）StandardContext的startInternal里面的Lifecycle.CONFIGURE_START_EVENT事件，触发了ContextConfig的configureStart方法，configureStart执行webConfig方法，webConfig执行了configureContext启动了Wrapper。这里很绕。

## 四、UML类图
放一张很详细的类图。
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210505204245964.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM0MDM3MzU4,size_16,color_FFFFFF,t_70)