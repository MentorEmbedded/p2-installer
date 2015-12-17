/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Mentor Graphics - Modified from original P2 stand-alone installer
 *******************************************************************************/
package com.codesourcery.internal.installer;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode.InstallerRunMode;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.ui.GUIInstallOperation;

/**
 * This is an installer application build using P2.  The application must be
 * provided an "install description" for installation or "install manifest" for
 * uninstallation.
 */
public class InstallApplication implements IApplication {
	/**
	 * Creates the install operation.
	 * 
	 * @return Install operation
	 */
	private InstallOperation createInstallOperation() {
		InstallOperation operation = null;

		InstallerRunMode runMode;
		// Silent installation
		if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_SILENT)) {
			runMode = InstallerRunMode.SILENT;
			operation = createSilentInstallOperation();
		}
		// Console installation
		else if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE)) {
			runMode = InstallerRunMode.CONSOLE;
			operation = createConsoleInstallOperation();
		}
		// Wizard based UI installation
		else {
			runMode = InstallerRunMode.GUI;
			operation = createGuiInstallOperation();
			// If no display is available, use console install
			if (operation == null) {
				operation = createConsoleInstallOperation();
			}
		}
		
		// Set install mode
		if (Installer.getDefault().getInstallManager() != null) {
			InstallMode installMode = (InstallMode)Installer.getDefault().getInstallManager().getInstallMode();
			installMode.setRunMode(runMode);
		}
		
		// Status file option
		if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_STATUS)) {
			String statusFile = Installer.getDefault().getCommandLineOption(IInstallConstants.COMMAND_LINE_STATUS);
			operation.setStatusFile(InstallUtils.resolvePath(statusFile));
		}
		
		return operation;
	}

	/**
	 * Creates a console install operation.
	 * 
	 * @return Console install operation
	 */
	private InstallOperation createConsoleInstallOperation() {
		ConsoleInstallOperation consoleOperation = new ConsoleInstallOperation();

		try {
			// Set maximum number of lines to display
			String consoleArg = Installer.getDefault().getCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE);
			if (consoleArg != null) {
				consoleOperation.setMaxLines(Integer.parseInt(consoleArg));
			}
		}
		catch (NumberFormatException e) {
			consoleOperation.showError(InstallMessages.Error_InvalidNumberOfLines);
		}
		
		return consoleOperation;
	}
	
	/**
	 * Creates a silent install operation.
	 * 
	 * @return Silent install operation
	 */
	private InstallOperation createSilentInstallOperation() {
		return new SilentInstallOperation();
	}
	
	/**
	 * Creates a GUI install operation.
	 * 
	 * @return GUI install operation or <code>null</code> if no display is
	 * available
	 */
	private InstallOperation createGuiInstallOperation() {
		GUIInstallOperation guiInstallOperation = null;
		
		Display display = Display.getCurrent();
		if (display == null) {
			try {
				display = new Display();
			}
			catch (Throwable e) {
				// Ignore - no display available
			}
		}
		if (display != null) {
			guiInstallOperation = new GUIInstallOperation();
		}

		return guiInstallOperation;
	}

	@Override
	public Object start(IApplicationContext appContext) {
		// Run once then delete installer
		File selfDirectory = null;
		boolean runOnce = Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_ONCE);
		if (runOnce) {
			try {
				selfDirectory = Installer.getDefault().getInstallFile("");
				ShutdownHandler.getDefault().addDirectoryToRemove(selfDirectory.getAbsolutePath(), false);

			    String tempDir = System.getenv(IInstallConstants.P2_INSTALLER_TEMP_PATH);
				if (tempDir == null) {
					 tempDir = System.getProperty("java.io.tmpdir");
				}
				if (tempDir != null) {
					removeParentDirectories(selfDirectory, tempDir);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		// Print usage and exit
		if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_HELP)) {
			printUsage();
			// Due to a bug, an exception will be thrown if an RCP
			// application exits before the Gogo shell is initialized.
			// Wait for shell to initialize before exiting.
			// See: https://issues.apache.org/jira/browse/FELIX-3354
			try { Thread.sleep(1000); } catch (InterruptedException e) {}
			return IApplication.EXIT_OK;
		}
		
		// Set application running
		appContext.applicationRunning();

		initializeNetworkSettings();
		initializeProxySupport();

		// Create install operation
		InstallOperation installOperation = createInstallOperation();
		if (installOperation == null) {
			Installer.logError(InstallMessages.Error_InstallOperation);
		}
		
		// Another instance is already running
		if (!Installer.getDefault().hasLock()) {
			installOperation.showError(InstallMessages.Error_AlreadyRunning);
		}
		// Run install operation
		else {
			installOperation.run();
		}
		
		// If run once and an unlocked repos directory is present, delete it before shutting down as it may contain
		// P2 repository files larger than instmon can handle currently (> 2GB).
		if (runOnce && (selfDirectory != null)) {
			try {
				File reposDirectory = new File(selfDirectory, "repos");
				if (reposDirectory.exists()) {
					FileUtils.deleteDirectory(reposDirectory.toPath());
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		return IApplication.EXIT_OK;
	}

	/**
	 * Remove any parent directories in the temporary path above the installer. This is to 
	 * remove any temporary directories created by SFX. Note the installerDir itself will 
	 * be removed before any parent directories are removed. Furthermore, none of the additional
	 * parent directories will be removed if they are not already empty. If the temp directory
	 * is not a prefix of the installer directory, no parent directories are removed.
	 * 
	 * @param installerDirectory - Directory containing the installer
	 * @param tempDir - Temp directory used to extract installer
	 */
	private void removeParentDirectories(File installerDirectory,
			String tempDir) {
		IPath tempDirPath = new Path(tempDir);
		IPath installDirPath = new Path(installerDirectory.getAbsolutePath());
		
		if (tempDirPath.isPrefixOf(installDirPath)) {
			// The following segment was already removed by the calling method
			installDirPath = installDirPath.removeLastSegments(1);
			File tempDirFile = tempDirPath.toFile();
	        while (!installDirPath.toFile().equals(tempDirFile)) {
					ShutdownHandler.getDefault().addDirectoryToRemove(installDirPath.toOSString(), true);
					installDirPath = installDirPath.removeLastSegments(1);
			}
		}
		else {
			Installer.log("Parent temp directory for installer not removed because \"" + tempDirPath.toOSString() + "\" is not a prefix of \"" + installDirPath.toOSString() + "\".");
		}
	}
	
	/**
	 * Initializes proxy support
	 */
	private void initializeProxySupport() {
		// Note: Proxies are not currently supported.  If this functionality
		// is required, see the original P2 stand-alone installer.
	}

	/**
	 * Initialize network settings.
	 */
	private void initializeNetworkSettings() {
		try {
			IInstallDescription desc = Installer.getDefault().getInstallManager().getInstallDescription();
			if (desc != null) {
				int timeout = desc.getNetworkTimeout();
				if (timeout != -1) {
					String timeoutValue = Integer.toString(timeout);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.retrieve.connectTimeout", timeoutValue);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.retrieve.closeTimeout", timeoutValue);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.retrieve.readTimeout", timeoutValue);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.httpclient.browse.connectTimeout", timeoutValue);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.httpclient.retrieve.connectTimeout", timeoutValue);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.httpclient.retrieve.connectTimeout", timeoutValue);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.httpclient.retrieve.readTimeout", timeoutValue);
				}
				int retry = desc.getNetworkRetry();
				if (retry != -1) {
					String retryValue = Integer.toString(retry);
					System.setProperty("org.eclipse.ecf.provider.filetransfer.retrieve.retryAttempts", retryValue);
					System.setProperty("org.eclipse.equinox.p2.transport.ecf.retry", retryValue);				
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
	}

	/**
	 * Prints usage
	 */
	private void printUsage() {
		System.out.println(InstallMessages.Help_Usage);
		
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_HELP, InstallMessages.Help_Help);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_LOCATION + "=\"<location>\"", InstallMessages.Help_InstallLocation);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_DESCRIPTION + "=\"<install.properties>\"", InstallMessages.Help_InstallDescription);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_MANIFEST + "=\"<install.manifest>\"", InstallMessages.Help_InstallManifest);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE, InstallMessages.Help_Console);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_SILENT, InstallMessages.Help_Silent);
		printHelp("-nosplash", InstallMessages.Help_NoSplash);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_ONCE, InstallMessages.Help_InstallOnce);
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_PROPERTY + "<property>=\"<value>\"", InstallMessages.Help_Property);
		printHelp(IInstallConstants.COMMAND_LINE_STATUS + "=\"<status file path>\"", InstallMessages.Help_Status);
	}
	
	/**
	 * Prints formatted help for a command.
	 * 
	 * @param command Command
	 * @param description Command description
	 */
	private void printHelp(String command, String description) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(command);
		for (int i = 0; i < 40 - command.length(); i ++) {
			buffer.append(' ');
		}
		buffer.append(description);
		
		System.out.println(buffer.toString());
	}	
}
