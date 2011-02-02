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
import java.util.Hashtable;
import java.util.Map;
import org.python.core.PyDictionary;
import org.python.core.PyString;

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

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)  throws IOException, InterruptedException {
        PySystemState sys = new PySystemState();
        sys.setCurrentWorkingDir(build.getWorkspace().getRemote());

        // Put Hudson global variables and build variables in '_hudson_env_'
        PyDictionary environ = new PyDictionary();
        for (Map.Entry<String,String> entry :
                build.getEnvironment(listener).entrySet()) {
            environ.__setitem__(
                new PyString(entry.getKey()), new PyString(entry.getValue()));
        }
        for (Map.Entry<String,String> entry :
                build.getBuildVariables().entrySet()) {
            environ.__setitem__(
                new PyString(entry.getKey()), new PyString(entry.getValue()));
        }
        PyDictionary namespace = new PyDictionary();
        namespace.__setitem__(new PyString("_hudson_env_"), environ);

        PythonInterpreter interp =
            new PythonInterpreter(namespace, sys);

        interp.setOut(listener.getLogger());
        interp.setErr(listener.getLogger());
        // Make _hudson_env_ contents available in os.environ
        interp.exec("import os");
        interp.exec("os.environ.update(_hudson_env_)");
        interp.exec("del(os)");

        interp.exec(this.getCommand());
        interp.cleanup();

        build.setResult(Result.SUCCESS);
        return true;
    }
}
