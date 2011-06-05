package org.jvnet.hudson.plugins;

import java.net.URL;
import org.kohsuke.stapler.DataBoundConstructor;

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
    
    public void install() {
        throw new UnsupportedOperationException("not implemented yet");
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