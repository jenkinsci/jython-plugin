package org.jvnet.hudson.plugins;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.remoting.Which;
import hudson.util.StreamTaskListener;
import org.python.util.jython;

/**
 * The Jython plug-in entry point. Copies the Jython runtime to the tools
 * directory on the master node.
 *
 * @author Jack Leow
 */
public final class JythonPlugin extends Plugin {
    public static final String INSTALLER_FILE =
        "jython-installer-2.5.2.JENKINS.zip";
    
    @Override
    public void start() throws Exception {
        FilePath jythonHome =
            Hudson.getInstance().getRootPath().child("tools/jython");
        
        jythonHome.installIfNecessaryFrom(
            getClass().getResource(JythonPlugin.INSTALLER_FILE),
            StreamTaskListener.fromStdout(), "Installed Jython runtime.");
        jythonHome.child("tmp").mkdirs();
    }
}