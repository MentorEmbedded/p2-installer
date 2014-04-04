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
package com.codesourcery.installer;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallPlatform;
import com.codesourcery.internal.installer.Log;
import com.codesourcery.internal.installer.ShutdownHandler;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("restriction") // Access p2 internal logging API's
public class Installer implements BundleActivator {
	/** The plug-in identifier */
	public static final String ID = "com.codesourcery.installer"; //$NON-NLS-1$
	/** The shared instance */
	private static Installer plugin;
	/** Bundle context */
	private BundleContext context;
	/** Image registry */
	private ImageRegistry imageRegistry;
	/** Install platform */
	private IInstallPlatform installPlatform;
	/** <code>true</code> if running on Windows platform */
	private static boolean isWindows;
	/** Data folder */
	private IPath dataFolder;
	/** Install description */
	private IInstallDescription installDescription;

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Installer getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public Installer() {
	}

	/**
	 * Returns the bundle context for this bundle.
	 * @return the bundle context
	 */
	public BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext aContext) throws Exception {
		this.context = aContext;
		plugin = this;

		// Determine platform
		isWindows = Platform.getOS().equals(Platform.OS_WIN32);
		
		// Get log command line argument
		String dataDirectory = getCommandLineOption(IInstallConstants.COMMAND_LINE_DATA);
		if (dataDirectory != null) {
			dataFolder = new Path(dataDirectory);
		}
		// Use default location
		else {
			String home = System.getProperty("user.home");
			dataFolder = new Path(home).append(IInstallConstants.DEFAULT_INSTALL_DATA_FOLDER);
		}
		// Installer data folder
		if (!dataFolder.toFile().exists()) {
			Files.createDirectories(dataFolder.toFile().toPath());
		}

		// Initialize install platform
		initializeInstallPlatform();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext aContext) throws Exception {
		// Run shutdown operations
		ShutdownHandler.getDefault().run();
		
		plugin = null;
		
		// Dispose of images
		if (imageRegistry != null) {
			imageRegistry.dispose();
		}
	}
	
	/**
	 * Returns if the installer is running on
	 * the Windows platform.
	 * 
	 * @return <code>true</code> if Windows
	 */
	public static boolean isWindows() {
		return isWindows;
	}
	
	/**
	 * Returns if the installer is running on
	 * the Linux platform.
	 * 
	 * @return <code>true</code> if running on Linux
	 */
	public static boolean isLinux() {
		return !isWindows;
	}
	
	/**
	 * Returns if the installer is running on
	 * the OSX platform.
	 * 
	 * @return <code>true</code> if running on OSX
	 */
	public static boolean isOSX() {
		// Not currently supported
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Initializes the install platform.
	 * 
	 * @throws CoreException on failure
	 */
	private void initializeInstallPlatform() throws CoreException {
		try {
			Bundle bundle = Platform.getBundle(Installer.ID);
			IPath dataFolderPath = getDataFolder();
			String os = Platform.getOS();
			String arch = Platform.getOSArch();

			// Install monitor platform specific name
			String monName = MessageFormat.format("{0}-{1}-{2}{3}", new Object[] {
					IInstallConstants.INSTALL_MONITOR_NAME,
					os,
					arch,
					isWindows() ? "." + IInstallConstants.EXTENSION_EXE : ""
			});
			
			URL url = FileLocator.find(bundle, new Path("/exe/" + monName), null); //$NON-NLS-1$
			url = FileLocator.resolve(url);
			File monFile = new File(url.getFile());
			IPath tempMonPath = dataFolderPath.append(monName);
			File tempMonFile = tempMonPath.toFile();
			// Copy the install monitor to the temporary directory so its 
			// execution does not lock removal of the product directory on the
			// Windows platform.
			if (tempMonFile.exists()) {
				tempMonFile.delete();
			}
			FileUtils.copyFile(monFile, tempMonFile);
			// Set execute attribute
			tempMonFile.setExecutable(true);
			
			// Get the installer PID (only works for a SUN JVM)
			String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
			int seperator = runtimeName.indexOf('@');
			String pid = runtimeName.substring(0, seperator);
			
			// Redistributables directory (if available)
			String redistPath = null;
			File redistDir = getInstallFile(IInstallConstants.REDIST_DIRECTORY);
			if ((redistDir != null) && redistDir.exists())
				redistPath = redistDir.getAbsolutePath();
			
			IPath monitorLogPath = getLogPath().append(IInstallConstants.INSTALL_MONITOR_NAME + ".log");
			// Create the install monitor
			installPlatform = new InstallPlatform(tempMonFile.getAbsolutePath(), redistPath, 
					pid, monitorLogPath.toOSString());
		}
		catch (Exception e) {
			fail(InstallMessages.Error_FailedInstallMonitor, e);
		}
	}
	
	/**
	 * Returns the path to the log directory.
	 * 
	 * @return Log directory path
	 */
	public IPath getLogPath() {
		IPath logDirPath = null;
		
		try {
			IPath platformLogPath = Platform.getLogFileLocation().removeLastSegments(1);
			File installArea = getInstallFile("");
			IPath installPath = new Path(installArea.getAbsolutePath());
			
			// If the platform log directory is in the installer directory,
			// use a log location in the data directory.  This prevents the
			// log from blocking removal of the directory during uninstallation.
			if (installPath.isPrefixOf(platformLogPath)) {
				logDirPath = Installer.getDefault().getDataFolder().append(IInstallConstants.LOGS_DIRECTORY);
				String dateNow = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
				logDirPath = logDirPath.append(dateNow);
				try {
					Files.createDirectories(logDirPath.toFile().toPath());
				} catch (IOException e) {
					log(e);
				}
			}
			// Otherwise write the log to the platform log directory
			else {
				logDirPath = platformLogPath;
			}
		}
		catch (Exception e) {
			log(e);
		}
		
		return logDirPath;
	}
	
	/**
	 * Returns the install Platform.
	 * 
	 * @return Install platform
	 */
	public IInstallPlatform getInstallPlatform() {
		return installPlatform;
	}
	
	/**
	 * Logs an exception.
	 * 
	 * @param e Exception
	 */
	public static void log(Throwable failure) {
		IStatus status;
		Throwable cause = failure;
		//unwrap target exception if applicable
		if (failure instanceof InvocationTargetException) {
			cause = ((InvocationTargetException) failure).getTargetException();
			if (cause == null)
				cause = failure;
		}
		if (cause instanceof CoreException) {
			status = ((CoreException) cause).getStatus();
		}
		else {
			status = new Status(IStatus.ERROR, ID, failure.getLocalizedMessage(), cause);
		}
		
		LogHelper.log(status);
		Log.getDefault().log(status);
	}

	/**
	 * Logs a message.
	 * 
	 * @param message Message
	 */
	public static void log(int severity, String message) {
		Status status = new Status(severity, ID, 0, message, null);
		LogHelper.log(status);
		Log.getDefault().log(status);
	}

	/**
	 * Logs a message.
	 * 
	 * @param message Message
	 */
	public static void log(String message) {
		log(IStatus.INFO, message);
	}

	/**
	 * Logs an error.
	 * 
	 * @param message Error message
	 */
	public static void logError(String message) {
		log(IStatus.ERROR, message);
	}
	
	/**
	 * Returns the image registry.
	 * 
	 * @return Image registry
	 */
	public ImageRegistry getImageRegistry() {
		return imageRegistry;
	}
	
	/**
	 * Called to initialize images.
	 */
	public void initializeImages() {
		if (imageRegistry == null) {
			try {
				imageRegistry = new ImageRegistry();
	
				Bundle bundle = getContext().getBundle();
				URL imagePathUrl;
				imagePathUrl = FileLocator.find(bundle, new Path("icons/bullet-empty.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.BULLET_EMPTY, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/bullet-solid.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.BULLET_SOLID, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/bullet-error.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.BULLET_ERROR, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/bullet-checked.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.BULLET_CHECKED, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/arrow-right.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.ARROW_RIGHT, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/title.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.PAGE_BANNER, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/comp.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.COMPONENT, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/comp-opt.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.COMPONENT_OPTIONAL, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/refresh.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.REFRESH, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/folder.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.FOLDER, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/csl-16x16-32bit.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.TITLE_ICON, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/info.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.INFO, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/error.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.ERROR, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/warning.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WARNING, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait0.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT0, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait1.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT1, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait2.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT2, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait3.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT3, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait4.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT4, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait5.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT5, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait6.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT6, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait7.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT7, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait8.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT8, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait9.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT9, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait10.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT10, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait11.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT11, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait12.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT12, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait13.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT13, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/wait14.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.WAIT14, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/comp-required.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.COMP_REQUIRED_OVERLAY, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/comp-optional.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.COMP_OPTIONAL_OVERLAY, ImageDescriptor.createFromURL(imagePathUrl));
				imagePathUrl = FileLocator.find(bundle, new Path("icons/comp-addon.png"), null); //$NON-NLS-1$
				imageRegistry.put(IInstallerImages.COMP_ADDON_OVERLAY, ImageDescriptor.createFromURL(imagePathUrl));
			}
			catch (Exception e) {
				log(e);
			}
		}
	}

	/**
	 * Copied from ServiceHelper because we need to obtain services
	 * before p2 has been started.
	 */
	public Object getService(String name) {
		BundleContext bundleContext = getContext();
		if (bundleContext == null)
			return null;
		ServiceReference<?> reference = bundleContext.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = bundleContext.getService(reference);
		bundleContext.ungetService(reference);
		return result;
	}

	/**
	 * Throws a new exception of severity error with the given error message.
	 * @throws CoreException Exception
	 */
	public static void fail(String message) throws CoreException {
		fail(message, null);
	}

	/**
	 * Throws a new exception of severity error with the given error message.
	 * @throws CoreException Exception
	 */
	public static void fail(String message, Throwable throwable) throws CoreException {
		Status status = new Status(IStatus.ERROR, Installer.ID, message, throwable);
		Log.getDefault().log(status);
		throw new CoreException(status);
	}

	/**
	 * Returns the path to an install file.
	 * 
	 * @param site File
	 * @return Install file
	 * @throws URISyntaxException on bad format
	 */
	public File getInstallFile(String site) throws URISyntaxException {
		String installArea = System.getProperty(IInstallConstants.OSGI_INSTALL_AREA);
		URI fileUri = URIUtil.append(URIUtil.fromString(installArea), site);
		File file = URIUtil.toFile(fileUri);

		return file;
	}
	
	/**
	 * Returns the installer data folder.
	 * 
	 * @return Installer data folder
	 */
	public IPath getDataFolder() {
		return dataFolder;
	}

	/**
	 * Returns if a command line option is
	 * present.
	 * 
	 * @param option Option
	 * @return <code>true</code> if option is found
	 */
	public boolean hasCommandLineOption(String option) {
		boolean hasOption = false;
		String[] args = Platform.getCommandLineArgs();
		for (String arg : args) {
			if (arg.startsWith(option)) {
				hasOption = true;
				break;
			}
		}
		
		return hasOption;
	}
	
	/**
	 * Returns the argument to a command line option.
	 * The argument will be stripped of quotes.
	 * 
	 * @param option Option
	 * @return Option argument or <code>null</code> if
	 * option was not found.
	 */
	public String getCommandLineOption(String option) {
		String argument = null;
		
		String[] args = Platform.getCommandLineArgs();
		for (int index = 0; index < args.length; index++) {
			if (args[index].startsWith(option)) {
				int offset = args[index].indexOf('=');
				if (offset != -1) {
					argument = args[index].substring(offset + 1).trim();
					// Strip quotes
					argument = argument.replace("\"", "");
					break;
				}
			}
		}

		return argument;
	}
	
	/**
	 * Returns the set of defined command line properties.
	 * 
	 * @return Command line properties
	 */
	public Map<String, String> getCommandLineProperties() {
		HashMap<String, String> defines = new HashMap<String, String>();
		
		String[] args = Platform.getCommandLineArgs();
		for (int index = 0; index < args.length; index++) {
			if (args[index].startsWith(IInstallConstants.COMMAND_LINE_INSTALL_PROPERTY)) {
				int nameOffset = IInstallConstants.COMMAND_LINE_INSTALL_PROPERTY.length();
				int offset = args[index].indexOf('=', nameOffset);
				if (offset != -1) {
					String name = args[index].substring(nameOffset, offset);
					String argument = args[index].substring(offset + 1).trim();
					// Strip quotes
					argument = argument.replace("\"", "");
					defines.put(name, argument);
				}
			}
		}

		return defines;
	}
	
	/**
	 * Sets the install description.
	 * 
	 * @param installDescription Install description
	 */
	public void setInstallDescripton(IInstallDescription installDescription) {
		this.installDescription = installDescription;
	}
	
	/**
	 * Returns the install description.
	 * 
	 * @return Install description for install, or <code>null</code> if uninstall
	 */
	public IInstallDescription getInstallDescription() {
		return installDescription;
	}
}
