package org.jvnet.hudson.plugins;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * Jython plugin entry point
 *
 * <p>
 * There must be one {@link Plugin} class in each plugin.
 * See javadoc of {@link Plugin} for more about what can be done on this class.
 *
 * @author R. Tyler Ballance
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        // plugins normally extend Hudson by providing custom implementations
        // of 'extension points'. In this example, we'll add one builder.
        BuildStep.BUILDERS.add(Jython.DESCRIPTOR);
    }
}
