package org.jvnet.hudson.plugins;

import hudson.FilePath;
import hudson.Plugin;
import hudson.model.Hudson;
import java.net.URL;
import java.util.logging.Logger;

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
    public static final String SITE_PACKAGES_PATH = "Lib/site-packages";
    
    private static final Logger LOG =
        Logger.getLogger(JythonPlugin.class.toString());
    
    @Override
    public void start() throws Exception {
        if (!JYTHON_HOME.child("jython.jar").exists()) {
            JYTHON_HOME.unzipFrom(INSTALLER_URL.openStream());
            JYTHON_HOME.child("jython").chmod(0755);
            JYTHON_HOME.child("tmp").mkdirs();
            LOG.info("Installed Jython runtime.");
        }
    }
}