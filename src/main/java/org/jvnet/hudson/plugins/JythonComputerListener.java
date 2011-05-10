package org.jvnet.hudson.plugins;

import java.io.IOException;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.Which;
import hudson.slaves.ComputerListener;
import org.python.util.jython;

/**
 * {@link ComputerListener} for the Jython plug in.
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
        new FilePath(Which.jarFile(jython.class)).copyTo(
            root.child("tools/jython/jython-standalone.jar"));
        listener.getLogger().println("Copied jython-standalone.jar");
    }
    
}