/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.config.influxdb.generate;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.apputil.args.ArgParser;
import org.csstudio.apputil.args.BooleanOption;
import org.csstudio.apputil.args.StringOption;
import org.csstudio.archive.config.influxdb.Activator;
import org.csstudio.archive.config.xml.XMLArchiveConfig;
import org.csstudio.archive.config.xml.XMLFileUtil;
import org.csstudio.archive.config.xml.XMLFileUtil.SingleURLMap;
import org.csstudio.security.PasswordInput;
import org.csstudio.security.preferences.SecurePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Eclipse Application for CSS archive engine
 *
 * @author Megan Grodowitz
 */
public class GenApp implements IApplication
{
    /** Request file */
    private String root_file;

    private boolean verbose;
    private boolean skip_pv_sample;

    String[] getPrefValue(final String option) {
        if (option != null) { // Split "plugin/key=value"
            String[] pref_val = new String[2];
            final int sep = option.indexOf("=");
            if (sep >= 0) {
                pref_val[0] = option.substring(0, sep);
                pref_val[1] = option.substring(sep + 1);
            } else {
                pref_val[0] = option;
                pref_val[1] = null;
            }
            return pref_val;
        }
        return null;
    }

    void printVersion(final IApplicationContext context) {
        final String version = context.getBrandingBundle().getHeaders().get("Bundle-Version");
        final String app_info = context.getBrandingName() + " " + version;
        System.out.println(app_info);
    }

    void printHelp(final IApplicationContext context, final ArgParser parser) {
        printVersion(context);
        System.out.println("\n\n" + parser.getHelp());
    }

    void printUsageError(final IApplicationContext context, final ArgParser parser, final String msg) {
        printHelp(context, parser);
        System.err.println(msg);
    }

    /** Obtain settings from preferences and command-line arguments
     *  @param args Command-line arguments
     *  @return <code>true</code> if continue, <code>false</code> to end application
     */
    @SuppressWarnings("nls")
    private boolean getSettings(final String args[], final IApplicationContext context)
    {
        // Create the parser and run it.
        final ArgParser parser = new ArgParser();
        final BooleanOption help_opt = new BooleanOption(parser, "-help", "Display help", false);
        final BooleanOption version_opt = new BooleanOption(parser, "-version", "Display version info", false);
        final BooleanOption verbose_opt = new BooleanOption(parser, "-verbose", "Verbose status output", false);
        final BooleanOption skip_pv_sample_opt = new BooleanOption(parser, "-skip_pv_sample",
                "Skip sampling of PVs for real values, all PVs will default to double types", false);
        // final StringOption engine_name_opt = new StringOption(parser,
        // "-engine", "demo_engine", "Engine config name", null);
        final StringOption root_file_opt = new StringOption(parser, "-root_file", "path/to/fileordir",
                "Engine file to import or directory tree root with engine files to import", null);
        final StringOption preference_opt = new StringOption(parser, "-set_pref", "plugin.name/preference=value",
                "Set a preference for a specific plugin", null);
        final StringOption set_password_opt = new StringOption(parser,
                "-set_password", "plugin/key=value", "Set secure preferences", null);
        parser.addEclipseParameters();
        try
        {
            parser.parse(args);
        }
        catch (final Exception ex)
        {   // Bad options
            printUsageError(context, parser, ex.getMessage());
            return false;
        }

        verbose = verbose_opt.get();
        skip_pv_sample = skip_pv_sample_opt.get();

        if (help_opt.get())
        {   // Help requested
            printHelp(context, parser);
            return false;
        }
        if (version_opt.get())
        {   // Version requested
            printVersion(context);
            return false;
        }

        String[] pref_val = getPrefValue(set_password_opt.get());
        if (pref_val != null)
        {
            if (pref_val[1] == null)
            {
                pref_val[1] = PasswordInput.readPassword("Value for " + pref_val[0] + ":");
            }
            try
            {
                SecurePreferences.set(pref_val[0], pref_val[1]);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            return false;
        }

        for (final String pref_opt : preference_opt.getMany())
        {
            pref_val = getPrefValue(pref_opt);
            if (pref_val != null)
            {
                final String pref = pref_val[0];
                final String value = pref_val[1];

                if (value == null) {
                    printUsageError(context, parser, "Malformed option " + preference_opt.getOption() + " " + pref_opt);
                    return false;
                }
                final int sep = pref.indexOf('/');
                if (sep < 0) {
                    printUsageError(context, parser,
                            "Malformed plugin/preference for option " + preference_opt.getOption() + " " + pref_opt);
                    return false;
                }
                final String plugin = pref.substring(0, sep);
                final String preference = pref.substring(sep + 1);

                final IEclipsePreferences pref_node = InstanceScope.INSTANCE.getNode(plugin);
                pref_node.put(preference, value);
                try {
                    pref_node.flush();
                } catch (BackingStoreException e) {
                    Activator.getLogger().log(Level.SEVERE,
                            "Could not set plugin preference " + plugin + "/" + preference, e);
                }
            }
        }

        root_file = root_file_opt.get();

        if (root_file == null)
        {
            printUsageError(context, parser, "Must specificy root file option: " + root_file_opt.getOption());
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("nls")
    public Object start(final IApplicationContext context) throws Exception
    {
        final String args[] =
            (String []) context.getArguments().get("application.args");
        if (!getSettings(args, context))
            return Integer.valueOf(-2);

        // Initialize logging
        // LogConfigurator.configureFromPreferences();

        final Logger logger = Activator.getLogger();
        final XMLFileUtil util = new XMLFileUtil();

        final String dummy_url = "foo://foo.bar";
        final XMLArchiveConfig config = new XMLArchiveConfig();

        util.importAll(config, root_file, new SingleURLMap(dummy_url));

        if (verbose) {
            final List<String> files = util.getImportedFiles();
            System.out.println("Files Imported: ");
            for (String file : files) {
                System.out.println("\t" + file);
            }
        }



        if (skip_pv_sample) {
            // set all PVs to type double
        } else {
            // individually query all PVs to determine a type
        }

        try
        {
            boolean run = true;
            while (run)
            {
                run = false;
            }

            logger.info("ArchiveEngine stopped");
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Unhandled Main Loop Error", ex);
            return Integer.valueOf(-1);
        }

        return EXIT_OK;
    }

    /** {@inheritDoc} */
    @Override
    public void stop()
    {
    }
}
