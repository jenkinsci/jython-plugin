package org.jvnet.hudson.plugins;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.Hudson;
import hudson.util.StreamTaskListener;
import java.net.URL;

/**
 * The Jython plug-in entry point. Copies the Jython runtime to the tools
 * directory on the master node.
 *
 * @author Jack Leow
 */
public final class JythonPlugin extends Plugin {
    public static final URL INSTALLER_URL =
        JythonPlugin.class.getResource("jython-installer-2.5.2.JENKINS.zip");
    public static final FilePath JYTHON_HOME =
        Hudson.getInstance().getRootPath().child("tools/jython");
    
    @Override
    public void start() throws Exception {
        JYTHON_HOME.installIfNecessaryFrom(
            INSTALLER_URL, StreamTaskListener.fromStdout(),
            "Installed Jython runtime.");
        JYTHON_HOME.child("jython").chmod(0755);
        JYTHON_HOME.child("tmp").mkdirs();
    }
}