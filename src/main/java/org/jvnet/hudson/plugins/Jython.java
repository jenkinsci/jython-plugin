package org.jvnet.hudson.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.remoting.Which;
import hudson.tasks.Builder;
import java.io.ByteArrayInputStream;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

import org.python.util.jython;

/**
 * Jython builder.
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author R. Tyler Ballance
 * @author Jack Leow
 */
public class Jython extends Builder {
    private final String command;

    private Jython(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {
        public DescriptorImpl() {
            super(Jython.class);
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) {
            return new Jython(formData.getString("jython"));
        }

        public String getDisplayName() {
            return "Execute Jython script";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/jython/help.html";
        }
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)  throws IOException, InterruptedException {
        Node builtOn = build.getBuiltOn();
        boolean master = builtOn == Hudson.getInstance();
        
        JDK configuredJdk = build.getProject().getJDK();
        String javaCmd = configuredJdk != null ?
            configuredJdk.forNode(builtOn, listener).getHome() + "/bin/java" : "java";
        
        String jythonRuntime = master ?
            Which.jarFile(jython.class).getAbsolutePath() :
            builtOn.getRootPath().child("tools/jython/jython-standalone.jar").getRemote();
        
        boolean success = 0 == launcher.launch().
            cmds(javaCmd, "-jar", jythonRuntime, "-c", getCommand()).
            masks(false, false, false, false, true).
            envs(build.getEnvironment(listener)).
            stdout(listener).
            pwd(build.getWorkspace()).
            join();
        
        build.setResult(success ? Result.SUCCESS : Result.FAILURE);
        return success;
    }
}