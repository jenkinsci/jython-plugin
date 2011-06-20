package org.jvnet.hudson.plugins;

import hudson.model.Descriptor.FormException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        
        @Override
        public void load() {
            super.load();
            try {
                Set<PythonPackage> pkgs = new HashSet<PythonPackage>();
                List<FilePath> pkgFiles = JythonPlugin.JYTHON_HOME.
                    child(JythonPlugin.SITE_PACKAGES_PATH).
                    list(new SuffixFileFilter(".egg-info"));

                for (FilePath pkgFile : pkgFiles) {
                    String pkgName;

                    String pkgFileName = pkgFile.getName();
                    if (pkgFile.isDirectory()) {
                        pkgName = getPackageName(
                            pkgFile.child("PKG-INFO"));
                    } else {
                        pkgName = getPackageName(pkgFile);
                    }
                    pkgs.add(new PythonPackage(pkgName));
                }
                pkgs.removeAll(PythonPackage.PREINSTALLED_PACKAGES);
                pythonPackages = pkgs;
            } catch (IOException e) {
                throw new JythonPluginException(
                    "error determining installed packages", e);
            } catch (InterruptedException e) {
                throw new JythonPluginException(
                    "error determining installed packages", e);
            }
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
			List<PythonPackage> newPythonPackages = req.bindParametersToList(
                PythonPackage.class, "pythonPackage.");
            boolean packageListUpdated = false;
            
            // Install new items
            for (PythonPackage pkg : newPythonPackages) {
                if (!pythonPackages.contains(pkg)) {
                    pkg.install();
                    packageListUpdated = true;
                }
            }
            // Uninstall removed items
            for (PythonPackage pkg : pythonPackages) {
                if (!newPythonPackages.contains(pkg)) {
                    pkg.uninstall();
                    packageListUpdated = true;
                }
            }
            // TODO update timestamp somewhere.
            //save();
            
            if (packageListUpdated) {
                load();
            }
            
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
        // Copying new packages
        PrintStream logger = listener.getLogger();
        for (FilePath pkgSrc : jythonSitePackagesMaster.list()) {
            String pkgName = pkgSrc.getName();
            FilePath pkgTgt = jythonSitePackages.child(pkgName);
            if (!pkgTgt.exists() ||
                    pkgSrc.lastModified() > pkgTgt.lastModified()) {
                logger.println(
                    "Copying package \"" + pkgName + "\" from master.");
                if (pkgSrc.isDirectory()) {
                    pkgSrc.copyRecursiveTo(pkgTgt);
                } else {
                    pkgSrc.copyTo(pkgTgt);
                }
            }
        }
        // Deleting uninstalled packages
        for (FilePath pkgTgt : jythonSitePackages.list()) {
            String pkgName = pkgTgt.getName();
            FilePath pkgSrc = jythonSitePackagesMaster.child(pkgName);
            if (!pkgSrc.exists()) {
                logger.println("Deleting package \"" + pkgName + "\".");
                try {
                    if (pkgTgt.isDirectory()) {
                        pkgTgt.deleteRecursive();
                    } else {
                        pkgTgt.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace(
                        listener.error(
                            "error deleting package - continuing with build"
                        )
                    );
                }
            }
        }
        
        FilePath jythonScript = jythonHome.child("tmp").
            createTextTempFile("script", ".py", getCommand());
        
        Map<String,String> envVar =
            new HashMap<String,String>(build.getEnvironment(listener));
        envVar.putAll(build.getBuildVariables());
        
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