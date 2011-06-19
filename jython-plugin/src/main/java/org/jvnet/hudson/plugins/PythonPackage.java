package org.jvnet.hudson.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An installable Python package.
 * 
 * @author Jack Leow
 */
public class PythonPackage {
    enum PipCommand {
        install, uninstall
    }
    
    @DataBoundConstructor
    public PythonPackage(String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }
        this.name = name;
    }
    
    private String name;
    
    public String getName() {
        return name;
    }
    
    private void invokePip(
            PipCommand command, List<String> options, String pkg) {
        try {
            List<String> procCmd = Arrays.asList(
                JythonPlugin.JYTHON_HOME.child("jython").getRemote(),
                JythonPlugin.JYTHON_HOME.child("bin/pip").getRemote(),
                command.toString());
            procCmd.addAll(options);
            procCmd.add(pkg);
            Process proc = new ProcessBuilder(procCmd).
                redirectErrorStream(true).
                start();
            BufferedReader stdOut = new BufferedReader(
                new InputStreamReader(proc.getInputStream()));
            StringBuilder lines = new StringBuilder();
            String line = stdOut.readLine();
            boolean nameSet = false;
            while (line != null) {
                System.out.println(line);
                lines.append(line);
                line = stdOut.readLine();
            }
            if (proc.waitFor() != 0) {
                throw new JythonPluginException(
                    "Package installation failed:\n" + lines);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void install() {
        invokePip(
            PipCommand.install, Collections.<String>emptyList(), getName());
    }
    
    public void uninstall() {
        invokePip(PipCommand.uninstall, Arrays.asList("--yes"), getName());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PythonPackage other = (PythonPackage) obj;
        if (this.name != other.name && (this.name == null ||
                !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}