package org.jvnet.hudson.plugins;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Jython builder.
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author R. Tyler Ballance
 * @author Jack Leow
 */
public class Jython extends Builder {
    private final String command;
    
    private Jython(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public DescriptorImpl() {
            super(Jython.class);
            load();
        }
        
        private boolean syntaxHighlightingEnabled = true;
        
        public boolean isSyntaxHighlightingEnabled() {
            return syntaxHighlightingEnabled;
        }
        
        private boolean tabKeyToIndentEnabled = true;
        
        public boolean isTabKeyToIndentEnabled() {
            return tabKeyToIndentEnabled;
        }

        // From PEP 8 (http://www.python.org/dev/peps/pep-0008/) Code lay-out:
        // "Use 4 spaces per indentation level."
        private int numSpacesPerIndentation = 4;
        
        public int getNumSpacesPerIndentation() {
            return numSpacesPerIndentation;
        }
        
        private String getPackageName(FilePath pkgInfo) {
            String name = null;
            final String NAME_PREFIX = "Name: ";
            
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(
                        new InputStreamReader(pkgInfo.read()));
                    
                    String line;
                    do {
                        line = reader.readLine();
                        if (line.startsWith(NAME_PREFIX)) {
                            name = line.substring(NAME_PREFIX.length());
                            break;
                        }
                    } while (line != null);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (IOException e) {
                throw new JythonPluginException(
                    "error while determining package name", e);
            }
            if (name == null) {
                throw new JythonPluginException(
                    "unable to determine package name");
            }
            
            return name;
        }
        
        private transient Set<PythonPackage> pythonPackages;
        
        public Set<PythonPackage> getPythonPackages() {
            return pythonPackages;
        }

        private Date lastModified;
        
        public Date getLastModified() {
            return lastModified;
        }
        
        private Set<PythonPackage> scanPackages() {
            try {
                Set<PythonPackage> pkgs = new HashSet<PythonPackage>();
                
                List<FilePath> pkgFiles;
                pkgFiles = JythonPlugin.JYTHON_HOME.
                    child(JythonPlugin.SITE_PACKAGES_PATH).
                    list(new SuffixFileFilter(".egg"));
                for (FilePath pkgFile : pkgFiles) {
                    pkgs.add(new PythonPackage(
                        getPackageName(pkgFile.child("EGG-INFO/PKG-INFO"))));
                }
                pkgFiles = JythonPlugin.JYTHON_HOME.
                    child(JythonPlugin.SITE_PACKAGES_PATH).
                    list(new SuffixFileFilter(".egg-info"));
                for (FilePath pkgFile : pkgFiles) {
                    if (pkgFile.isDirectory()) {
                        pkgs.add(new PythonPackage(
                            getPackageName(pkgFile.child("PKG-INFO"))));
                    } else {
                        pkgs.add(new PythonPackage(getPackageName(pkgFile)));
                    }
                }
                pkgs.removeAll(PythonPackage.PREINSTALLED_PACKAGES);
                return pkgs;
            } catch (IOException e) {
                throw new JythonPluginException(
                    "error determining installed packages", e);
            } catch (InterruptedException e) {
                throw new JythonPluginException(
                    "error determining installed packages", e);
            }
        }
        
        @Override
        public void load() {
            super.load();
            pythonPackages = scanPackages();
        }
        
        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) {
            return new Jython(formData.getString("jython"));
        }
        
        @Override
        public String getDisplayName() {
            return "Execute Jython script";
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
                throws FormException {
            syntaxHighlightingEnabled =
                req.getParameter("syntaxHighlightingEnabled") != null;
            tabKeyToIndentEnabled =
                req.getParameter("tabKeyToIndentEnabled") != null;
            numSpacesPerIndentation = Integer.parseInt(
                req.getParameter("numSpacesPerIndentation"));
            
            // Process Python packages
            List<PythonPackage> newPythonPackages = req.bindParametersToList(
                PythonPackage.class, "pythonPackage.");
            boolean packageListModified = false;
            
            // Install new items
            for (PythonPackage pkg : newPythonPackages) {
                if (!pythonPackages.contains(pkg)) {
                    pkg.install();
                    packageListModified = true;
                }
            }
            // Uninstall removed items
            for (PythonPackage pkg : pythonPackages) {
                if (!newPythonPackages.contains(pkg)) {
                    pkg.uninstall();
                    packageListModified = true;
                }
            }
            
            if (packageListModified) {
                pythonPackages = scanPackages();
                lastModified = new Date();
            }
            
            save();
            return super.configure(req, json);
        }
        
        @Override
        public String getHelpFile() {
            return "/plugin/jython/help.html";
        }
    }
    
    @Override
    public boolean perform(
            AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        Node builtOn = build.getBuiltOn();
        
        JDK configuredJdk = build.getProject().getJDK();
        String javaCmd = configuredJdk != null ?
            configuredJdk.forNode(builtOn, listener).getHome() + "/bin/java" :
            "java";
        
        final FilePath jythonHome = builtOn.getRootPath().child("tools/jython");
        final String jythonJar = jythonHome.child("jython.jar").getRemote();
        
        // Synchronize Python packages with slaves
        final FilePath jythonSitePackages =
            jythonHome.child(JythonPlugin.SITE_PACKAGES_PATH);
        final FilePath jythonSitePackagesMaster =
            JythonPlugin.JYTHON_HOME.child(JythonPlugin.SITE_PACKAGES_PATH);
        Date lastModified = getDescriptor().getLastModified();
        if (!jythonSitePackages.equals(jythonSitePackagesMaster) &&
                lastModified != null &&
                getDescriptor().getLastModified().after(
                    new Date(jythonSitePackages.lastModified()))) {
            JythonPlugin.syncSitePackages(jythonHome, listener);
            jythonSitePackages.touch(System.currentTimeMillis());
        }
        
        FilePath jythonScript = jythonHome.child("tmp").
            createTextTempFile("script", ".py", getCommand());
        
        Map<String,String> envVar =
            new HashMap<String,String>(build.getEnvironment(listener));
        envVar.putAll(build.getBuildVariables());
        if (!envVar.containsKey("http_proxy")) {
            ProxyConfiguration proxy = Hudson.getInstance().proxy;
            if (proxy != null) {
                envVar.put(
                    "http_proxy", "http://" + proxy.name + ":" + proxy.port);
            }
        }
        
        final String DEFAULT_JAVA_XMX = "-Xmx512m";
        String javaOpts = envVar.get("JAVA_OPTS");
        if (javaOpts == null) {
            javaOpts = DEFAULT_JAVA_XMX;
        } else if (javaOpts.indexOf("-Xmx") == -1) {
            javaOpts += " " + DEFAULT_JAVA_XMX;
        }
        
        ArgumentListBuilder argBuilder = new ArgumentListBuilder(javaCmd);
        argBuilder.addTokenized(javaOpts);
        argBuilder.add("-jar");
        argBuilder.add(jythonJar);
        argBuilder.add(jythonScript.getRemote());
        boolean success = 0 == launcher.launch().
            cmds(argBuilder).
            envs(envVar).
            stdout(listener).
            pwd(build.getWorkspace()).
            join();
        
        jythonScript.delete();
        
        build.setResult(success ? Result.SUCCESS : Result.FAILURE);
        return success;
    }
}