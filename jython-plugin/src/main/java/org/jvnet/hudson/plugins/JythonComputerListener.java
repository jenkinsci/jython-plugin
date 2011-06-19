package org.jvnet.hudson.plugins;

import java.io.IOException;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;

/**
 * {@link ComputerListener} for the Jython plug-in.
 * <p>
 * Transfers the Jython JAR file to slave nodes.
 *
 * @author Jack Leow
 */
@Extension
public class JythonComputerListener extends ComputerListener {
    @Override
    public void preOnline(
            Computer c, Channel channel, FilePath root, TaskListener listener)
            throws IOException, InterruptedException {
        final FilePath JYTHON_HOME = root.child("tools/jython");
        
        JYTHON_HOME.installIfNecessaryFrom(
            JythonPlugin.INSTALLER_URL, listener, "Installed Jython runtime.");
        JYTHON_HOME.child("jython").chmod(0755);
        JYTHON_HOME.child("tmp").mkdirs();
    }
}