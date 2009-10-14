package org.jvnet.hudson.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author R. Tyler Ballance
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

    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)  throws IOException, InterruptedException {
        PySystemState sys = new PySystemState();
        sys.setCurrentWorkingDir(build.getWorkspace().getRemote());
        PythonInterpreter interp = new PythonInterpreter(null, sys);

        interp.setOut(listener.getLogger());
        interp.setErr(listener.getLogger());
        interp.exec(this.getCommand());
        interp.cleanup();

        build.setResult(Result.SUCCESS);
        return true;
    }
}
