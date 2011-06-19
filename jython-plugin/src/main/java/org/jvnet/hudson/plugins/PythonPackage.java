package org.jvnet.hudson.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An installable Python package.
 * 
 * @author Jack Leow
 */
public class PythonPackage {
    private final URL url;
    
    enum PipCommands {
        install, uninstall
    }
    
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
            // TODO parse name from output
            Process proc =
                new ProcessBuilder(
                    JythonPlugin.JYTHON_HOME.child("jython").getRemote(),
                    JythonPlugin.JYTHON_HOME.child("bin/pip").getRemote(),
                    PipCommands.install.toString(),
                    getUrl().toString()).
                redirectErrorStream(true).
                start();
            BufferedReader stdOut = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
            String line;
            do {
                line = stdOut.readLine();
                System.out.println(line);
            } while (line != null);
            System.out.println("packaged installed, exit: " + proc.waitFor());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void remove() {
//        throw new UnsupportedOperationException("not implemented yet");
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
        if (this.url != other.url && (this.url == null ||
                !this.url.toString().equals(other.url.toString()))) {
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