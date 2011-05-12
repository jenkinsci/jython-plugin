/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
        new FilePath(Which.jarFile(jython.class)).copyTo(
            Hudson.getInstance().getRootPath().
            child("tools/jython/jython-standalone.jar"));
    }
}