/*******************************************************************************
 *  Copyright (c) 2014 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.internal.installer;

/**
 * Install constants
 */
public class IInstallConstants {
	/**
	 * Default install data folder
	 */
	public static final String DEFAULT_INSTALL_DATA_FOLDER = ".p2_installer";
	
	/** 
	 * Log file directory 
	 */
	public final static String LOGS_DIRECTORY = "logs";

	/**
	 * Install description filename.
	 */
	public static final String INSTALL_DESCRIPTION_FILENAME = "installer.properties";
	
	/**
	 * Install manifest filename.
	 */
	public static final String INSTALL_MANIFEST_FILENAME = "install.manifest";
	
	/**
	 * Command line option to suppress splash screen on startup.
	 */
	public static final String COMMAND_LINE_NO_SPLASH = "-nosplash";

	/**
	 * Command line option for silent installation
	 * This should be specified along with the "-nosplash" option to suppress
	 * the splash screen.
	 * To perform uninstallation, add "-install.manifest" and supply the path
	 * to an install manifest file.
	 * <code>
	 * -nosplash -install.silent
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_SILENT = "-install.silent";
	
	/**
	 * Command line option to perform a console installation.  If this option
	 * is specified and the silent command line option is not specified, the
	 * user will be prompted at the console for installation options.
	 * All install wizard pages that support {@link #IInstallConsoleProducer}
	 * will be displayed.
	 * This option takes an optional parameter that is the maximum number of
	 * console lines to display before pagination.
	 * <code>
	 * -nosplash -install.console=80
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_CONSOLE = "-install.console";

	/**
	 * Install monitor name.
	 */
	public static final String INSTALL_MONITOR_NAME = "instmon";
	
	/**
	 * Command line option for location of install.
	 * <code>
	 * -install.location="/temp/new_product"
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_LOCATION = "-install.location";

	/**
	 * Command line option for which users to install for.
	 * <code>
	 * -install.allusers=true
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_ALL_USERS = "-install.allusers";

	/**
	 * Command line option for URL of install description
	 * <code>
	 * -install.desc="file:/install.properties"
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_DESCRIPTION = "-install.desc";
	
	/**
	 * Command line option for location of install manifest file.
	 * <code>
	 * -install.manifest="/home/user/product/install.manifest"
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_MANIFEST = "-install.manifest";
	
	/**
	 * Command line option to run installer once then delete the containing
	 * directory.
	 * ONLY USE THIS OPTION IN A DEPLOYED PRODUCT (NOT DEVELOPMENT), OTHERWISE
	 * IT WILL DELETE YOUR SOURCES.
	 * <code>
	 * -install.once
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_ONCE = "-install.once";

	/**
	 * Command line option to specify the directory to save installer data.
	 * <code>
	 * -install.data="/home/user/data"
	 */
	public static final String COMMAND_LINE_DATA = "-install.data";
	
	/**
	 * Undocumented command line option to clear the installer data.
	 * <code>
	 * -install.clean
	 */
	public static final String COMMAND_CLEAN = "-install.clean";

	/**
	 * Command line option to create a status text file.  The specified file 
	 * will be created after the installer completes and will contain either 
	 * "OK", "CANCELED", or "FAIL:" followed with an error message.
	 * <code>
	 * -install.status="/home/user/status.txt"
	 * </code>
	 */
	public static final String COMMAND_LINE_STATUS = "-install.status";

	/**
	 * Command line option to set an install property.  This will override the
	 * value of the property specified in the installer properties file.
	 * The syntax for this option is:
	 * -install.D<property name>=<property value>
	 */
	public static final String COMMAND_LINE_INSTALL_PROPERTY = "-install.D";

	/**
	 * Command line option to print help and exit.
	 * <code>
	 * -install.help
	 * </code>
	 */
	public static final String COMMAND_LINE_INSTALL_HELP = "-help";

	/**
	 * OSGI install area system property.
	 */
	public static final String OSGI_INSTALL_AREA = "osgi.install.area";
	
	/**
	 * Installer filename (without extension).
	 */
	public static final String INSTALLER_NAME = "setup";
	
	/**
	 * Executable file extension.
	 */
	public static final String EXTENSION_EXE = "exe";
	
	/**
	 * Directory containing the install monitor.
	 */
	public static final String MONITOR_DIRECTORY = "mon";
	
	/**
	 * Directory containing redistributables.
	 */
	public static final String REDIST_DIRECTORY = "redist";

	/**
	 * Uninstall directory
	 */
	public static final String UNINSTALL_DIRECTORY = "uninstall";
	
	/**
	 * Default P2 agent directory
	 */
	public static final String P2_DIRECTORY = "p2";
	
	/** Installation folder (String) */
	public static final String PROPERTY_INSTALL_FOLDER = "com.codesourcery.installer.folder";
	/** Installation folder (String) */
	public static final String PROPERTY_INSTALL_FOR_ALL_USERS = "com.codesourcery.installer.allUsers";
	/** Modify system PATH (Boolean) */
	public static final String PROPERTY_MODIFY_PATHS = "com.codesourcery.installer.modifyPaths";
	/** Create desktop shortcuts (Boolean) */
	public static final String PROPERTY_DESKTOP_SHORTCUTS = "com.codesourcery.installer.desktopShortcuts";
	/** Create program shortcuts (Boolean) */
	public static final String PROPERTY_PROGRAM_SHORTCUTS = "com.codesourcery.installer.programShortcuts";
	/** Create launcher shortcuts (Boolean) */
	public static final String PROPERTY_LAUNCHER_SHORTCUTS = "com.codesourcery.installer.launcherShortcuts";
	/** Program shortcuts folder (String) */
	public static final String PROPERTY_PROGRAM_SHORTCUTS_FOLDER = "com.codesourcery.installer.programShortcutsFolder";
	/** Install add-ons (Boolean) */
	public static final String PROPERTY_INSTALL_ADDONS = "com.codesourcery.installer.installAddons";
	/** Install size in bytes (Long) */
	public static final String PROPERTY_INSTALL_SIZE = "com.codesourcery.installer.installSize";
}
