package org.jvnet.hudson.plugins;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.remoting.Which;
import org.python.util.jython;

/**
 * The Jython plug-in entry point. Copies the Jython runtime to the tools
 * directory on the master node.
 *
 * @author Jack Leow
 */
public class JythonPlugin extends Plugin {
    @Override
    public void start() throws Exception {
        FilePath jythonHome =
            Hudson.getInstance().getRootPath().child("tools/jython");
        
        new FilePath(Which.jarFile(jython.class)).copyTo(
            jythonHome.child("jython-standalone.jar"));
        jythonHome.child("tmp").mkdirs();
    }
}