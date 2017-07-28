package org.jenkinsci.plugins.cppchecker;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link Cppchecker} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class Cppchecker extends Builder implements SimpleBuildStep {

    private final String name;
    private final boolean dump;
    private final String symbol;

    /* Enable additional checks. */
    private final boolean enAll;
    private final boolean enWarning;
    private final boolean enStyle;
    private final boolean enPerformance;
    private final boolean enPortability;
    private final boolean enInformation;
    private final boolean enUnusedFunction;
    private final boolean enMissingInclude;

    private final boolean force;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public Cppchecker(String name, boolean dump, String symbol,
            boolean enAll, boolean enWarning, boolean enStyle,
            boolean enPerformance, boolean enPortability, boolean enInformation,
            boolean enUnusedFunction, boolean enMissingInclude,
            boolean force
    ) {
        this.name = name;

        this.dump = dump;
        this.symbol = symbol;

        /* Enable additional checks. */
        this.enAll = enAll;
        this.enWarning = enWarning;
        this.enStyle = enStyle;
        this.enPerformance = enPerformance;
        this.enPortability = enPortability;
        this.enInformation = enInformation;
        this.enUnusedFunction = enUnusedFunction;
        this.enMissingInclude = enMissingInclude;

        this.force = force;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * <B>--dump</B><br>
     * Dump xml data for each translation unit. The dump files have the
     * extension .dump and contain ast, tokenlist, symboldatabase, valueflow.
     *
     * @return true: Dump xml data for each translation unit<br>
     * false: No dump file
     */
    public boolean getDump() {
        return dump;
    }

    /**
     * <B>-D[ID]</B><br>
     * Define preprocessor symbol. Unless --max-configs or --force is used,
     * Cppcheck will only check the given configuration when -D is used.<br>
     * Example: '-DDEBUG=1 -D__cplusplus'.
     *
     * @return null: not define any preprocessor symbol<br>
     * symbol: Preprocessor symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * <B>--enable=all</B><br>
     * Enable all checks. It is recommended to only use --enable=all when the
     * whole program is scanned, because this enables unusedFunction.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnAll() {
        return enAll;
    }

    /**
     * <B>--enable=warning</B><br>
     * Enable warning messages.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnWarning() {
        return enWarning;
    }

    /**
     * <B>--enable=style</B><br>
     * Enable all coding style checks. All messages with the severities 'style',
     * 'performance' and 'portability' are enabled.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnStyle() {
        return enStyle;
    }

    /**
     * <B>--enable=performance</B><br>
     * Enable performance messages
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnPerformance() {
        return enPerformance;
    }

    /**
     * <B>--enable=portability</B><br>
     * Enable portability messages
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnPortability() {
        return enPortability;
    }

    /**
     * <B>--enable=information</B><br>
     * Enable information messages
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnInformation() {
        return enInformation;
    }

    /**
     * <B>--enable=unusedFunction</B><br>
     * Check for unused functions. It is recommend to only enable this when the
     * whole program is scanned.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnUnusedFunction() {
        return enUnusedFunction;
    }

    /**
     * <B>--enable=missingInclude</B><br>
     * Warn if there are missing includes. For detailed information, use
     * '--check-config'.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnMissingInclude() {
        return enMissingInclude;
    }

    /**
     * <B>-f, --force</B><br>
     * Force checking of all configurations in files. If used together with
     * '--max-configs=', the last option is the one that is effective.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getForce() {
        return force;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        // This also shows how you can consult the global configuration of the builder
        if (getDescriptor().getUseFrench()) {
            listener.getLogger().println("Bonjour, " + name + "!");
        } else {
            listener.getLogger().println("Hello, " + name + "!");
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link Cppchecker}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     *
     * <p>
     * See
     * {@code src/main/resources/hudson/plugins/hello_world/Cppchecker/*.jelly}
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * To persist global configuration information, simply store it in a
         * field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use {@code transient}.
         */
        private boolean useFrench;

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has
         * typed.
         * @return Indicates the outcome of the validation. This is sent to the
         * browser.
         * <p>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message will
         * be displayed to the user.
         * @throws java.io.IOException TODO: Add description
         * @throws javax.servlet.ServletException TODO: Add description
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Please set a name");
            }
            if (value.length() < 4) {
                return FormValidation.warning("Isn't the name too short?");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         *
         * @return The name
         */
        @Override
        public String getDisplayName() {
            return "Execute cppcheck";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req, formData);
        }

        /**
         * This method returns true if the global configuration says we should
         * speak French.
         *
         * The method name is bit awkward because global.jelly calls this method
         * to determine the initial state of the checkbox by the naming
         * convention.
         *
         * @return useFrench
         */
        public boolean getUseFrench() {
            return useFrench;
        }
    }
}
