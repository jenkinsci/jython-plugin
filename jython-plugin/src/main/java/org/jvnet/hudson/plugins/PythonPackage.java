package org.jvnet.hudson.plugins;

import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.Hudson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An installable Python package.
 * 
 * @author Jack Leow
 */
public class PythonPackage {
    public static final Set<PythonPackage> PREINSTALLED_PACKAGES =
        Collections.unmodifiableSet(new HashSet<PythonPackage>(Arrays.asList(
            new PythonPackage("distribute"),
            new PythonPackage("pip"),
            new PythonPackage("setuptools")
        )));
    
    @DataBoundConstructor
    public PythonPackage(String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        this.name = name;
    }
    
    private String name;
    
    public String getName() {
        return name;
    }
    
    private void invokeJython(String scriptPath, String... options) {
        try {
            List<String> procCmd = new ArrayList<String>(2 + options.length);
            
            FilePath jythonScript = JythonPlugin.JYTHON_HOME.child("jython");
            // Windows workaround:
            // if we're on Windows, use jython.bat instead of jython.
            boolean win32 = jythonScript.mode() == -1;
            if (win32) {
                jythonScript = JythonPlugin.JYTHON_HOME.child("jython.bat");
            }
            procCmd.add(jythonScript.getRemote());
            
            procCmd.add(JythonPlugin.JYTHON_HOME.child(scriptPath).getRemote());
            procCmd.addAll(Arrays.asList(options));
            ProcessBuilder procBuilder = new ProcessBuilder(procCmd).
                redirectErrorStream(true);
            
            // Set "http_proxy" environment variable to Jenkins proxy value
            ProxyConfiguration proxy = Hudson.getInstance().proxy;
            if (proxy != null) {
                procBuilder.environment().put(
                    "http_proxy", "http://" + proxy.name + ":" + proxy.port);
            }
            Process proc = procBuilder.start();
            BufferedReader stdOut = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
            StringBuilder lines = new StringBuilder();
            String line = stdOut.readLine();
            while (line != null) {
                System.out.println(line);
                lines.append(line);
                line = stdOut.readLine();
            }
            if (proc.waitFor() != 0) {
                throw new JythonPluginException(
                    "Package installation failed:\n" + lines);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void install() {
        invokeJython("bin/easy_install", getName());
    }
    
    public void uninstall() {
        invokeJython("bin/pip", "uninstall", "--yes", getName());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PythonPackage other = (PythonPackage) obj;
        if (this.name != other.name && (this.name == null ||
                !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}