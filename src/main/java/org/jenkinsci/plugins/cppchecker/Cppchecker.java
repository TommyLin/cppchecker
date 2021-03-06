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
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link Cppchecker} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #oFile}) to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class Cppchecker extends Builder implements SimpleBuildStep {

    private final String oFile;
    private final String target;

    private final boolean dump;
    private final String symbol;

    /* Enable additional checks. */
    private final boolean enAll;
    private final boolean enWarn;
    private final boolean enStyle;
    private final boolean enPerformance;
    private final boolean enPortability;
    private final boolean enInfo;
    private final boolean enUnusedFunc;
    private final boolean enMissingInc;

    private final boolean force;
    private final String includeDir;
    private final boolean inconclusive;
    private final boolean quiet;

    /* Set standard. */
    private final boolean posix;
    private final boolean c89;
    private final boolean c99;
    private final boolean c11;
    private final boolean cpp03;
    private final boolean cpp11;

    /* Suppress warnings */
    private final boolean unmatchSuppress;
    private final boolean unusedFunc;
    private final boolean varScope;

    private final boolean verbose;

    private final boolean xml;
    private final boolean xmlVer;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public Cppchecker(String oFile, String target, boolean dump, String symbol,
            boolean enAll, boolean enWarn, boolean enStyle,
            boolean enPerformance, boolean enPortability, boolean enInfo,
            boolean enUnusedFunc, boolean enMissingInc,
            boolean force, String includeDir, boolean inconclusive, boolean quiet,
            boolean posix, boolean c89, boolean c99, boolean c11, boolean cpp03, boolean cpp11,
            boolean unmatchSuppress, boolean unusedFunc, boolean varScope,
            boolean verbose, boolean xml, boolean xmlVer
    ) {
        this.oFile = oFile;
        this.target = target;

        this.dump = dump;
        this.symbol = symbol;

        /* Enable additional checks. */
        this.enAll = enAll;
        this.enWarn = enWarn;
        this.enStyle = enStyle;
        this.enPerformance = enPerformance;
        this.enPortability = enPortability;
        this.enInfo = enInfo;
        this.enUnusedFunc = enUnusedFunc;
        this.enMissingInc = enMissingInc;

        this.force = force;
        this.includeDir = includeDir;
        this.inconclusive = inconclusive;
        this.quiet = quiet;

        /* Set standard. */
        this.posix = posix;
        this.c89 = c89;
        this.c99 = c99;
        this.c11 = c11;
        this.cpp03 = cpp03;
        this.cpp11 = cpp11;

        /* Suppress warnings */
        this.unmatchSuppress = unmatchSuppress;
        this.unusedFunc = unusedFunc;
        this.varScope = varScope;

        this.verbose = verbose;

        this.xml = xml;
        this.xmlVer = xmlVer;
    }

    /**
     * We'll use this from the {@code config.jelly}.
     *
     * @return oFile
     */
    public String getOFile() {
        return oFile;
    }

    public String getTarget() {
        return target;
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
    public boolean getEnWarn() {
        return enWarn;
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
    public boolean getEnInfo() {
        return enInfo;
    }

    /**
     * <B>--enable=unusedFunction</B><br>
     * Check for unused functions. It is recommend to only enable this when the
     * whole program is scanned.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnUnusedFunc() {
        return enUnusedFunc;
    }

    /**
     * <B>--enable=missingInclude</B><br>
     * Warn if there are missing includes. For detailed information, use
     * '--check-config'.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getEnMissingInc() {
        return enMissingInc;
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

    /**
     * <B>-I [dir]</B><br>
     * Give path to search for include files. Give several -I parameters to give
     * several paths. First given path is searched for contained header files
     * first. If paths are relative to source files, this is not needed.
     *
     * @return Path to search for include files
     */
    public String getIncludeDir() {
        return includeDir;
    }

    /**
     * <B>--inconclusive</B><br>
     * Allow that Cppcheck reports even though the analysis is inconclusive.
     * There are false positives with this option. Each result must be carefully
     * investigated before you know if it is good or bad.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getInconclusive() {
        return inconclusive;
    }

    /**
     * <B>-q, --quiet</B><br>
     * Do not show progress reports.
     *
     * @return true: Enable<br>
     * false: Disable
     */
    public boolean getQuiet() {
        return quiet;
    }

    public boolean getPosix() {
        return posix;
    }

    public boolean getC89() {
        return c89;
    }

    public boolean getC99() {
        return c99;
    }

    public boolean getC11() {
        return c11;
    }

    public boolean getCpp03() {
        return cpp03;
    }

    public boolean getCpp11() {
        return cpp11;
    }

    public boolean getUnmatchSuppress() {
        return unmatchSuppress;
    }

    public boolean getUnusedFunc() {
        return unusedFunc;
    }

    public boolean getVarScope() {
        return varScope;
    }

    public boolean getVerbose() {
        return verbose;
    }

    public boolean getXml() {
        return xml;
    }

    public boolean getXmlVer() {
        return xmlVer;
    }

    private static String getEnableOptions(boolean enAll, boolean enWarn, boolean enStyle,
            boolean enPerformance, boolean enPortability, boolean enInfo,
            boolean enUnusedFunc, boolean enMissingInc
    ) {
        String selections, enableOptions;

        selections = (enAll ? "all" : "")
                + (enWarn ? ",warning" : "")
                + (enStyle ? ",style" : "")
                + (enPerformance ? ",performance" : "")
                + (enPortability ? ",portability" : "")
                + (enInfo ? ",information" : "")
                + (enUnusedFunc ? ",unusedFunction" : "")
                + (enMissingInc ? ",missingInclude" : "");

        if (selections.startsWith(",")) {
            selections = selections.substring(1);
        }

        if (selections.length() == 0) {
            enableOptions = "";
        } else {
            enableOptions = " --enable=" + selections;
        }

        return enableOptions;
    }

    private static String getStandardOptions(boolean posix, boolean c89, boolean c99,
            boolean c11, boolean cpp03, boolean cpp11) {
        String standardOptions;

        standardOptions = (posix ? " --std=posix" : "")
                + (c89 ? " --std=c89" : "")
                + (c99 ? " --std=c99" : "")
                + (c11 ? " --std=c11" : "")
                + (cpp03 ? " --std=c++03" : "")
                + (cpp11 ? " --std=c++11" : "");

        return standardOptions;
    }

    private static String getSuppressions(boolean unmatchSuppress, boolean unusedFunc, boolean varScope) {
        String suppressions;

        suppressions = (unmatchSuppress ? " --suppress=unmatchedSuppression" : "")
                + (unusedFunc ? " --suppress=unusedFunction" : "")
                + (varScope ? " --suppress=variableScope" : "");

        return suppressions;
    }

    private static String getOptions(String oFile, String target, boolean dump,
            String symbol, boolean enAll, boolean enWarn, boolean enStyle,
            boolean enPerformance, boolean enPortability, boolean enInfo,
            boolean enUnusedFunc, boolean enMissingInc,
            boolean force, String includeDir, boolean inconclusive, boolean quiet,
            boolean posix, boolean c89, boolean c99, boolean c11, boolean cpp03, boolean cpp11,
            boolean unmatchSuppress, boolean unusedFunc, boolean varScope,
            boolean verbose, boolean xml, boolean xmlVer) {

        String options;

        options = (dump ? " --dump" : "")
                + ((symbol.trim().length() > 0) ? (" -D" + symbol.trim()) : "")
                + getEnableOptions(enAll, enWarn, enStyle, enPerformance,
                        enPortability, enInfo, enUnusedFunc, enMissingInc)
                + (force ? " -f" : "")
                + ((includeDir.trim().length() > 0) ? (" -I" + includeDir.trim()) : "")
                + (inconclusive ? " --inconclusive" : "")
                + (quiet ? " -q" : "")
                + getStandardOptions(posix, c89, c99, c11, cpp03, cpp11)
                + getSuppressions(unmatchSuppress, unusedFunc, varScope)
                + (verbose ? " -v" : "")
                + (xml ? " --xml" : "")
                + (xmlVer ? " --xml-version=2" : "")
                + ((target.trim().length() > 0) ? (" " + target.trim()) : " .")
                + " 2>" + oFile.trim();

        return options;
    }

    private ArgumentListBuilder getArgs() {
        ArgumentListBuilder args = new ArgumentListBuilder();
        String command, options;

        args.clear();

        options = getOptions(this.oFile, this.target, this.dump,
                this.symbol, this.enAll, this.enWarn, this.enStyle,
                this.enPerformance, this.enPortability, this.enInfo,
                this.enUnusedFunc, this.enMissingInc,
                this.force, this.includeDir, this.inconclusive, this.quiet,
                this.posix, this.c89, this.c99, this.c11, this.cpp03, this.cpp11,
                this.unmatchSuppress, this.unusedFunc, this.varScope,
                this.verbose, this.xml, this.xmlVer);

        if (!getDescriptor().getUseDefault() && (getDescriptor().getExePath() == null)) {
            command = getDescriptor().getExePath();
        } else {
            command = "cppcheck";
        }

        args.addTokenized(command + options);

        return args;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        /*
        // This also shows how you can consult the global configuration of the builder
        if (getDescriptor().getUseFrench()) {
            listener.getLogger().println("Bonjour, " + oFile + "!");
        } else {
            listener.getLogger().println("Hello, " + oFile + "!");
        }
         */

        listener.getLogger().println("[Cppchecker] " + "Starting the cppcheck.");
        try {
            ArgumentListBuilder args = getArgs();
            OutputStream out = listener.getLogger();
            FilePath of = new hudson.FilePath(workspace.getChannel(), workspace + "/" + this.oFile.trim());

            launcher.launch().cmds(args).stderr(of.write()).stdout(out).pwd(workspace).join();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Cppchecker.class.getName()).log(Level.SEVERE, null, ex);
        }

        listener.getLogger().println("[Cppchecker] " + "Ending the cppcheck.");
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
        private boolean useDefault;
        private String exePath;

        /**
         * In order to load the persisted global configuration, you have to call
         * load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of various form field.
         *
         * @param value Output file name
         * @param target Saving results in file
         * @param dump Dump xml data for each translation unit. The dump files
         * have the extension .dump and contain ast, tokenlist, symboldatabase,
         * valueflow.
         * @param symbol Define preprocessor symbol. Unless --max-configs or
         * --force is used, Cppcheck will only check the given configuration
         * when -D is used. Example: '-DDEBUG=1 -D__cplusplus'.
         * @param enAll Enable all checks. It is recommended to only use
         * --enable=all when the whole program is scanned, because this enables
         * unusedFunction.
         * @param enWarn Enable warning messages
         * @param enStyle Enable all coding style checks. All messages with the
         * severities 'style', 'performance' and 'portability' are enabled.
         * @param enPerformance Enable performance messages
         * @param enPortability Enable portability messages
         * @param enInfo Enable information messages
         * @param enUnusedFunc Check for unused functions. It is recommend to
         * only enable this when the whole program is scanned.
         * @param enMissingInc Warn if there are missing includes. For detailed
         * information, use '--check-config'.
         * @param force Force checking of all configurations in files. If used
         * together with '--max-configs=', the last option is the one that is
         * effective.
         * @param includeDir Give path to search for include files. Give several
         * -I parameters to give several paths. First given path is searched for
         * contained header files first. If paths are relative to source files,
         * this is not needed.
         * @param inconclusive Allow that Cppcheck reports even though the
         * analysis is inconclusive. There are false positives with this option.
         * Each result must be carefully investigated before you know if it is
         * good or bad.
         * @param quiet Do not show progress reports.
         * @param posix POSIX compatible code
         * @param c89 C code is C89 compatible
         * @param c99 C code is C99 compatible
         * @param c11 C code is C11 compatible (default)
         * @param cpp03 C++ code is C++03 compatible
         * @param cpp11 C++ code is C++11 compatible (default)
         * @param unusedFunc Suppress warnings
         * unmatchedSuppressionunusedFunction
         * @param unmatchSuppress Suppress warnings unmatchedSuppression
         * @param varScope Suppress warnings variableScope
         * @param verbose Output more detailed error information.
         * @param xml Write results in xml format to error stream (stderr).
         * @param xmlVer Select the XML file version. Currently versions 1 and 2
         * are available. The default version is 1.
         * @return Indicates the outcome of the validation. This is sent to the
         * browser.
         * <p>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message will
         * be displayed to the user.
         * @throws java.io.IOException TODO: Add description
         * @throws javax.servlet.ServletException TODO: Add description
         */
        public FormValidation doCheckOFile(@QueryParameter String value,
                @QueryParameter String target,
                @QueryParameter boolean dump,
                @QueryParameter String symbol,
                @QueryParameter boolean enAll,
                @QueryParameter boolean enWarn,
                @QueryParameter boolean enStyle,
                @QueryParameter boolean enPerformance,
                @QueryParameter boolean enPortability,
                @QueryParameter boolean enInfo,
                @QueryParameter boolean enUnusedFunc,
                @QueryParameter boolean enMissingInc,
                @QueryParameter boolean force,
                @QueryParameter String includeDir,
                @QueryParameter boolean inconclusive,
                @QueryParameter boolean quiet,
                @QueryParameter boolean posix,
                @QueryParameter boolean c89,
                @QueryParameter boolean c99,
                @QueryParameter boolean c11,
                @QueryParameter boolean cpp03,
                @QueryParameter boolean cpp11,
                @QueryParameter boolean unmatchSuppress,
                @QueryParameter boolean unusedFunc,
                @QueryParameter boolean varScope,
                @QueryParameter boolean verbose,
                @QueryParameter boolean xml,
                @QueryParameter boolean xmlVer
        )
                throws IOException, ServletException {

            String command, options;

            options = getOptions(value, target, dump, symbol,
                    enAll, enWarn, enStyle, enPerformance, enPortability, enInfo,
                    enUnusedFunc, enMissingInc,
                    force, includeDir, inconclusive, quiet,
                    posix, c89, c99, c11, cpp03, cpp11,
                    unmatchSuppress, unusedFunc, varScope,
                    verbose, xml, xmlVer);

            if (getUseDefault() && (getExePath() == null)) {
                command = getExePath();
            } else {
                command = "cppcheck";
            }

            return FormValidation.ok(command + options);
        }

        public FormValidation doCheckXmlVer(@QueryParameter String value)
                throws IOException, ServletException {
            if ("true".equals(value)) {
                return FormValidation.ok();
            } else {
                return FormValidation.warning("Please use the new version if you can.");
            }
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
            useDefault = formData.getBoolean("useDefault");
            exePath = formData.getString("exePath");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseDefault)
            save();
            return super.configure(req, formData);
        }

        /**
         * This method returns true if the global configuration says we should
         * use default cppcheck installed in system.
         *
         * The method name is bit awkward because global.jelly calls this method
         * to determine the initial state of the checkbox by the naming
         * convention.
         *
         * @return useDefault
         */
        public boolean getUseDefault() {
            return useDefault;
        }

        public String getExePath() {
            return exePath;
        }
    }
}
