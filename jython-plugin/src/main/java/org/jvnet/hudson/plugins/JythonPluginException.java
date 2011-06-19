package org.jvnet.hudson.plugins;

/**
 * Indicated a general error with the Jython plug-in.
 * 
 * @author Jack Leow
 */
public class JythonPluginException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public JythonPluginException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
    
    public JythonPluginException(String string) {
        super(string);
    }
}