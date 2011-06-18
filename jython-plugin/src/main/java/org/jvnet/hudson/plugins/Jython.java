package org.jvnet.hudson.plugins;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

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
    public boolean perform(
            AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {
        Node builtOn = build.getBuiltOn();
        
        JDK configuredJdk = build.getProject().getJDK();
        String javaCmd = configuredJdk != null ?
            configuredJdk.forNode(builtOn, listener).getHome() + "/bin/java" :
            "java";
        
        FilePath jythonHome = builtOn.getRootPath().child("tools/jython");
        String jythonJar =
            jythonHome.child("jython-standalone.jar").getRemote();
        
        FilePath jythonScript = jythonHome.child("tmp").
            createTextTempFile("script", ".py", getCommand());
        
        Map<String,String> envVar =
            new HashMap<String,String>(build.getEnvironment(listener));
        envVar.putAll(build.getBuildVariables());
        
        String DEFAULT_JAVA_XMX = "-Xmx512m";
        String javaOpts = envVar.get("JAVA_OPTS");
        if (javaOpts == null) {
            javaOpts = DEFAULT_JAVA_XMX;
        } else if (javaOpts.indexOf("-Xmx") == -1) {
            javaOpts += " " + DEFAULT_JAVA_XMX;
        }
        
        ArgumentListBuilder argBuilder = new ArgumentListBuilder(javaCmd);
        argBuilder.addTokenized(javaOpts);
        argBuilder.add("-jar");
        argBuilder.add(jythonJar);
        argBuilder.add(jythonScript.getRemote());
        boolean success = 0 == launcher.launch().
            cmds(argBuilder).
            envs(envVar).
            stdout(listener).
            pwd(build.getWorkspace()).
            join();
        
        jythonScript.delete();
        
        build.setResult(success ? Result.SUCCESS : Result.FAILURE);
        return success;
    }
}