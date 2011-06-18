package org.jvnet.hudson.plugins;

import hudson.remoting.Which;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.kohsuke.stapler.DataBoundConstructor;
import org.python.util.PythonInterpreter;
import org.python.util.jython;

/**
 *
 * @author Jack Leow
 */
public class PythonPackage {
    private final URL url;
    
    @DataBoundConstructor
    public PythonPackage(URL url) {
        if (url == null) {
            throw new NullPointerException("url cannot be null");
        }
        this.url = url;
    }
    
    public String getName() {
        String name;
        
        String path = url.getPath();
        String pathLower = path.toLowerCase();
        int nameStartIndex = path.lastIndexOf("/") + 1;
        if (pathLower.endsWith(".egg")) {
            name = path.substring(nameStartIndex, path.length() - 4);
        } else if (pathLower.endsWith(".tar.gz")) {
            name = path.substring(nameStartIndex, path.length() - 7);
        } else if (pathLower.endsWith(".zip")) {
            name = path.substring(nameStartIndex, path.length() - 4);
        } else {
            name = path.substring(nameStartIndex);
        }
        
        return name;
    }
    
    public URL getUrl() {
        return url;
    }
    
    public synchronized void install() {
        try {
            Properties props = new Properties();
            File jythonJar = Which.jarFile(jython.class);
            String pathSep = System.getProperty("path.separator");
            props.setProperty(
                "python.path",
                jythonJar + "/Lib" + pathSep +
                jythonJar.getParent() + "/Lib/site-packages" + pathSep +
                jythonJar.getParent() + "/Lib/site-packages/setuptools-0.6c11-py2.5.egg");
            PythonInterpreter.initialize(
                System.getProperties(), props,
                new String[] {"", "install", getUrl().toString()});
            PythonInterpreter jython = new PythonInterpreter();
            jython.exec("import sys");
            jython.exec("sys.executable = ''");
            jython.exec("print sys.prefix");
            jython.exec("print sys.path");
            jython.exec("print sys.argv");
            jython.exec("import pip");
            jython.exec("sys.exit(pip.main())");
            jython.cleanup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void remove() {
        throw new UnsupportedOperationException("not implemented yet");
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
        if (this.url != other.url && (this.url == null || !this.url.equals(other.url))) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.url != null ? this.url.hashCode() : 0);
        return hash;
    }
}