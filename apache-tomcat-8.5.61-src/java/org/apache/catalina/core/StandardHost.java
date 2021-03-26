/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.JmxEnabled;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.catalina.util.ContextName;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;

/**
 * Standard implementation of the <b>Host</b> interface.  Each
 * child container must be a Context implementation to process the
 * requests directed to a particular web application.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 */
public class StandardHost extends ContainerBase implements Host {

    private static final Log log = LogFactory.getLog(StandardHost.class);

    // ----------------------------------------------------------- Constructors


    /**
     * Create a new StandardHost component with the default basic Valve.
     */
    public StandardHost() {

        super();
        pipeline.setBasic(new StandardHostValve());

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The set of aliases for this Host.
     */
    private String[] aliases = new String[0];

    private final Object aliasesLock = new Object();


    /**
     * The application root for this Host.
     */
    private String appBase = "webapps";
    private volatile File appBaseFile = null;

    /**
     * The XML root for this Host.
     */
    private String xmlBase = null;

    /**
     * host's default config path
     */
    private volatile File hostConfigBase = null;

    /**
     * The auto deploy flag for this Host.
     */
    private boolean autoDeploy = true;

    // 默认的Context的配置类。日，竟然写死在了这里。
    private String configClass =
            "org.apache.catalina.startup.ContextConfig";


    /**
     * The Java class name of the default Context implementation class for
     * deployed web applications.
     */
    private String contextClass =
            "org.apache.catalina.core.StandardContext";


    /**
     * The deploy on startup flag for this Host.
     */
    private boolean deployOnStartup = true;


    /**
     * deploy Context XML config files property.
     */
    private boolean deployXML = !Globals.IS_SECURITY_ENABLED;


    /**
     * Should XML files be copied to
     * $CATALINA_BASE/conf/&lt;engine&gt;/&lt;host&gt; by default when
     * a web application is deployed?
     */
    private boolean copyXML = false;


    /**
     * The Java class name of the default error reporter implementation class
     * for deployed web applications.
     */
    private String errorReportValveClass =
            "org.apache.catalina.valves.ErrorReportValve";


    /**
     * Unpack WARs property.
     */
    private boolean unpackWARs = true;


    /**
     * Work Directory base for applications.
     */
    private String workDir = null;


    /**
     * Should we create directories upon startup for appBase and xmlBase
     */
    private boolean createDirs = true;


    /**
     * Track the class loaders for the child web applications so memory leaks
     * can be detected.
     */
    private final Map<ClassLoader, String> childClassLoaders =
            new WeakHashMap<>();


    /**
     * Any file or directory in {@link #appBase} that this pattern matches will
     * be ignored by the automatic deployment process (both
     * {@link #deployOnStartup} and {@link #autoDeploy}).
     */
    private Pattern deployIgnore = null;


    private boolean undeployOldVersions = false;

    private boolean failCtxIfServletStartFails = false;


    // ------------------------------------------------------------- Properties

    @Override
    public boolean getUndeployOldVersions() {
        return undeployOldVersions;
    }


    @Override
    public void setUndeployOldVersions(boolean undeployOldVersions) {
        this.undeployOldVersions = undeployOldVersions;
    }


    @Override
    public ExecutorService getStartStopExecutor() {
        return startStopExecutor;
    }


    /**
     * Return the application root for this Host.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     */
    @Override
    public String getAppBase() {
        return this.appBase;
    }


    /**
     * ({@inheritDoc}
     */
    @Override
    public File getAppBaseFile() {

        if (appBaseFile != null) {
            return appBaseFile;
        }

        File file = new File(getAppBase());

        // If not absolute, make it absolute
        if (!file.isAbsolute()) {
            file = new File(getCatalinaBase(), file.getPath());
        }

        // Make it canonical if possible
        try {
            file = file.getCanonicalFile();
        } catch (IOException ioe) {
            // Ignore
        }

        this.appBaseFile = file;
        return file;
    }


    /**
     * Set the application root for this Host.  This can be an absolute
     * pathname, a relative pathname, or a URL.
     *
     * @param appBase The new application root
     */
    @Override
    public void setAppBase(String appBase) {

        if (appBase.trim().equals("")) {
            log.warn(sm.getString("standardHost.problematicAppBase", getName()));
        }
        String oldAppBase = this.appBase;
        this.appBase = appBase;
        support.firePropertyChange("appBase", oldAppBase, this.appBase);
        this.appBaseFile = null;
    }


    /**
     * ({@inheritDoc}
     */
    @Override
    public String getXmlBase() {
        return this.xmlBase;
    }


    /**
     * ({@inheritDoc}
     */
    @Override
    public void setXmlBase(String xmlBase) {
        String oldXmlBase = this.xmlBase;
        this.xmlBase = xmlBase;
        support.firePropertyChange("xmlBase", oldXmlBase, this.xmlBase);
    }


    /**
     * ({@inheritDoc}
     */
    @Override
    public File getConfigBaseFile() {
        if (hostConfigBase != null) {
            return hostConfigBase;
        }
        String path = null;
        if (getXmlBase() != null) {
            path = getXmlBase();
        } else {
            StringBuilder xmlDir = new StringBuilder("conf");
            Container parent = getParent();
            if (parent instanceof Engine) {
                xmlDir.append('/');
                xmlDir.append(parent.getName());
            }
            xmlDir.append('/');
            xmlDir.append(getName());
            path = xmlDir.toString();
        }
        File file = new File(path);
        if (!file.isAbsolute())
            file = new File(getCatalinaBase(), path);
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {// ignore
        }
        this.hostConfigBase = file;
        return file;
    }


    /**
     * @return <code>true</code> if the Host will attempt to create directories for appBase and xmlBase
     * unless they already exist.
     */
    @Override
    public boolean getCreateDirs() {
        return createDirs;
    }

    /**
     * Set to <code>true</code> if the Host should attempt to create directories for xmlBase and appBase upon startup
     *
     * @param createDirs the new flag value
     */
    @Override
    public void setCreateDirs(boolean createDirs) {
        this.createDirs = createDirs;
    }

    /**
     * @return the value of the auto deploy flag.  If true, it indicates that
     * this host's child webapps will be dynamically deployed.
     */
    @Override
    public boolean getAutoDeploy() {
        return this.autoDeploy;
    }


    /**
     * Set the auto deploy flag value for this host.
     *
     * @param autoDeploy The new auto deploy flag
     */
    @Override
    public void setAutoDeploy(boolean autoDeploy) {

        boolean oldAutoDeploy = this.autoDeploy;
        this.autoDeploy = autoDeploy;
        support.firePropertyChange("autoDeploy", oldAutoDeploy,
                this.autoDeploy);

    }


    /**
     * @return the Java class name of the context configuration class
     * for new web applications.
     */
    @Override
    public String getConfigClass() {
        return this.configClass;
    }


    /**
     * Set the Java class name of the context configuration class
     * for new web applications.
     *
     * @param configClass The new context configuration class
     */
    @Override
    public void setConfigClass(String configClass) {

        String oldConfigClass = this.configClass;
        this.configClass = configClass;
        support.firePropertyChange("configClass",
                oldConfigClass, this.configClass);

    }


    /**
     * @return the Java class name of the Context implementation class
     * for new web applications.
     */
    public String getContextClass() {
        return this.contextClass;
    }


    /**
     * Set the Java class name of the Context implementation class
     * for new web applications.
     *
     * @param contextClass The new context implementation class
     */
    public void setContextClass(String contextClass) {

        String oldContextClass = this.contextClass;
        this.contextClass = contextClass;
        support.firePropertyChange("contextClass",
                oldContextClass, this.contextClass);

    }


    /**
     * @return the value of the deploy on startup flag.  If <code>true</code>, it indicates
     * that this host's child webapps should be discovered and automatically
     * deployed at startup time.
     */
    @Override
    public boolean getDeployOnStartup() {
        return this.deployOnStartup;
    }


    /**
     * Set the deploy on startup flag value for this host.
     *
     * @param deployOnStartup The new deploy on startup flag
     */
    @Override
    public void setDeployOnStartup(boolean deployOnStartup) {

        boolean oldDeployOnStartup = this.deployOnStartup;
        this.deployOnStartup = deployOnStartup;
        support.firePropertyChange("deployOnStartup", oldDeployOnStartup,
                this.deployOnStartup);

    }


    /**
     * @return <code>true</code> if XML context descriptors should be deployed.
     */
    public boolean isDeployXML() {
        return deployXML;
    }


    /**
     * Deploy XML Context config files flag mutator.
     *
     * @param deployXML <code>true</code> if context descriptors should be deployed
     */
    public void setDeployXML(boolean deployXML) {
        this.deployXML = deployXML;
    }


    /**
     * @return the copy XML config file flag for this component.
     */
    public boolean isCopyXML() {
        return this.copyXML;
    }


    /**
     * Set the copy XML config file flag for this component.
     *
     * @param copyXML The new copy XML flag
     */
    public void setCopyXML(boolean copyXML) {
        this.copyXML = copyXML;
    }


    /**
     * @return the Java class name of the error report valve class
     * for new web applications.
     */
    public String getErrorReportValveClass() {
        return this.errorReportValveClass;
    }


    /**
     * Set the Java class name of the error report valve class
     * for new web applications.
     *
     * @param errorReportValveClass The new error report valve class
     */
    public void setErrorReportValveClass(String errorReportValveClass) {

        String oldErrorReportValveClassClass = this.errorReportValveClass;
        this.errorReportValveClass = errorReportValveClass;
        support.firePropertyChange("errorReportValveClass",
                oldErrorReportValveClassClass,
                this.errorReportValveClass);

    }


    /**
     * @return the canonical, fully qualified, name of the virtual host
     * this Container represents.
     */
    @Override
    public String getName() {
        return name;
    }


    /**
     * Set the canonical, fully qualified, name of the virtual host
     * this Container represents.
     *
     * @param name Virtual host name
     * @throws IllegalArgumentException if name is null
     */
    @Override
    public void setName(String name) {

        if (name == null)
            throw new IllegalArgumentException
                    (sm.getString("standardHost.nullName"));

        name = name.toLowerCase(Locale.ENGLISH);      // Internally all names are lower case

        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, this.name);

    }


    /**
     * @return <code>true</code> if WARs should be unpacked on deployment.
     */
    public boolean isUnpackWARs() {
        return unpackWARs;
    }


    /**
     * Unpack WARs flag mutator.
     *
     * @param unpackWARs <code>true</code> to unpack WARs on deployment
     */
    public void setUnpackWARs(boolean unpackWARs) {
        this.unpackWARs = unpackWARs;
    }


    /**
     * @return host work directory base.
     */
    public String getWorkDir() {
        return workDir;
    }


    /**
     * Set host work directory base.
     *
     * @param workDir the new base work folder for this host
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }


    /**
     * @return the regular expression that defines the files and directories in
     * the host's {@link #getAppBase} that will be ignored by the automatic
     * deployment process.
     */
    @Override
    public String getDeployIgnore() {
        if (deployIgnore == null) {
            return null;
        }
        return this.deployIgnore.toString();
    }


    /**
     * @return the compiled regular expression that defines the files and
     * directories in the host's {@link #getAppBase} that will be ignored by the
     * automatic deployment process.
     */
    @Override
    public Pattern getDeployIgnorePattern() {
        return this.deployIgnore;
    }


    /**
     * Set the regular expression that defines the files and directories in
     * the host's {@link #getAppBase} that will be ignored by the automatic
     * deployment process.
     *
     * @param deployIgnore the regexp
     */
    @Override
    public void setDeployIgnore(String deployIgnore) {
        String oldDeployIgnore;
        if (this.deployIgnore == null) {
            oldDeployIgnore = null;
        } else {
            oldDeployIgnore = this.deployIgnore.toString();
        }
        if (deployIgnore == null) {
            this.deployIgnore = null;
        } else {
            this.deployIgnore = Pattern.compile(deployIgnore);
        }
        support.firePropertyChange("deployIgnore",
                oldDeployIgnore,
                deployIgnore);
    }


    /**
     * @return <code>true</code> if a webapp start should fail if a Servlet startup fails
     */
    public boolean isFailCtxIfServletStartFails() {
        return failCtxIfServletStartFails;
    }


    /**
     * Change the behavior of Servlet startup errors on web application starts.
     *
     * @param failCtxIfServletStartFails <code>false</code> to ignore errors on Servlets which
     *                                   are stated when the web application starts
     */
    public void setFailCtxIfServletStartFails(
            boolean failCtxIfServletStartFails) {
        boolean oldFailCtxIfServletStartFails = this.failCtxIfServletStartFails;
        this.failCtxIfServletStartFails = failCtxIfServletStartFails;
        support.firePropertyChange("failCtxIfServletStartFails",
                oldFailCtxIfServletStartFails,
                failCtxIfServletStartFails);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add an alias name that should be mapped to this same Host.
     *
     * @param alias The alias to be added
     */
    @Override
    public void addAlias(String alias) {

        alias = alias.toLowerCase(Locale.ENGLISH);

        synchronized (aliasesLock) {
            // Skip duplicate aliases
            for (String s : aliases) {
                if (s.equals(alias))
                    return;
            }
            // Add this alias to the list
            String newAliases[] = Arrays.copyOf(aliases, aliases.length + 1);
            newAliases[aliases.length] = alias;
            aliases = newAliases;
        }
        // Inform interested listeners
        fireContainerEvent(ADD_ALIAS_EVENT, alias);

    }


    /**
     * Add a child Container, only if the proposed child is an implementation
     * of Context.
     *
     * @param child Child container to be added
     */
    @Override
    public void addChild(Container child) {

        if (!(child instanceof Context))
            throw new IllegalArgumentException
                    (sm.getString("standardHost.notContext"));

        child.addLifecycleListener(new MemoryLeakTrackingListener());

        // Avoid NPE for case where Context is defined in server.xml with only a
        // docBase
        Context context = (Context) child;
        if (context.getPath() == null) {
            ContextName cn = new ContextName(context.getDocBase(), true);
            context.setPath(cn.getPath());
        }

        super.addChild(child);

    }


    /**
     * Used to ensure the regardless of {@link Context} implementation, a record
     * is kept of the class loader used every time a context starts.
     */
    private class MemoryLeakTrackingListener implements LifecycleListener {
        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (event.getType().equals(Lifecycle.AFTER_START_EVENT)) {
                if (event.getSource() instanceof Context) {
                    Context context = ((Context) event.getSource());
                    childClassLoaders.put(context.getLoader().getClassLoader(),
                            context.getServletContext().getContextPath());
                }
            }
        }
    }


    /**
     * Attempt to identify the contexts that have a class loader memory leak.
     * This is usually triggered on context reload. Note: This method attempts
     * to force a full garbage collection. This should be used with extreme
     * caution on a production system.
     *
     * @return a list of possibly leaking contexts
     */
    public String[] findReloadedContextMemoryLeaks() {

        System.gc();

        List<String> result = new ArrayList<>();

        for (Map.Entry<ClassLoader, String> entry :
                childClassLoaders.entrySet()) {
            ClassLoader cl = entry.getKey();
            if (cl instanceof WebappClassLoaderBase) {
                if (!((WebappClassLoaderBase) cl).getState().isAvailable()) {
                    result.add(entry.getValue());
                }
            }
        }

        return result.toArray(new String[0]);
    }

    /**
     * @return the set of alias names for this Host.  If none are defined,
     * a zero length array is returned.
     */
    @Override
    public String[] findAliases() {
        synchronized (aliasesLock) {
            return this.aliases;
        }
    }


    /**
     * Remove the specified alias name from the aliases for this Host.
     *
     * @param alias Alias name to be removed
     */
    @Override
    public void removeAlias(String alias) {

        alias = alias.toLowerCase(Locale.ENGLISH);

        synchronized (aliasesLock) {

            // Make sure this alias is currently present
            int n = -1;
            for (int i = 0; i < aliases.length; i++) {
                if (aliases[i].equals(alias)) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified alias
            int j = 0;
            String results[] = new String[aliases.length - 1];
            for (int i = 0; i < aliases.length; i++) {
                if (i != n)
                    results[j++] = aliases[i];
            }
            aliases = results;

        }

        // Inform interested listeners
        fireContainerEvent(REMOVE_ALIAS_EVENT, alias);

    }


    /**
     * Start this component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that prevents this component from being used
     */
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


    // -------------------- JMX  --------------------

    /**
     * @return the MBean Names of the Valves associated with this Host
     * @throws Exception if an MBean cannot be created or registered
     */
    public String[] getValveNames() throws Exception {
        Valve[] valves = this.getPipeline().getValves();
        String[] mbeanNames = new String[valves.length];
        for (int i = 0; i < valves.length; i++) {
            if (valves[i] instanceof JmxEnabled) {
                ObjectName oname = ((JmxEnabled) valves[i]).getObjectName();
                if (oname != null) {
                    mbeanNames[i] = oname.toString();
                }
            }
        }

        return mbeanNames;
    }

    public String[] getAliases() {
        synchronized (aliasesLock) {
            return aliases;
        }
    }

    @Override
    protected String getObjectNameKeyProperties() {

        StringBuilder keyProperties = new StringBuilder("type=Host");
        keyProperties.append(getMBeanKeyProperties());

        return keyProperties.toString();
    }

}
