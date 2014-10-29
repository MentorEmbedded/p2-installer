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

import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;

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

		InstallMode mode = (InstallMode)Installer.getDefault().getInstallManager().getInstallMode();
		
		// Silent installation
		if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_SILENT)) {
			mode.setRunMode(InstallerRunMode.SILENT);
			operation = createSilentInstallOperation();
		}
		// Console installation
		else if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE)) {
			mode.setRunMode(InstallerRunMode.CONSOLE);
			operation = createConsoleInstallOperation();
		}
		// Wizard based UI installation
		else {
			mode.setRunMode(InstallerRunMode.GUI);
			operation = createGuiInstallOperation();
			// If no display is available, use console install
			if (operation == null) {
				operation = createConsoleInstallOperation();
			}
		}
		
		// Status file option
		if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_STATUS)) {
			operation.setStatusFile(new Path(Installer.getDefault().getCommandLineOption(IInstallConstants.COMMAND_LINE_STATUS)));
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

		// Set maximum number of lines to display
		String consoleArg = Installer.getDefault().getCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE);
		if (consoleArg != null) {
			consoleOperation.setMaxLines(Integer.parseInt(consoleArg));
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
		boolean runOnce = Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_ONCE);
		if (runOnce) {
			try {
				File selfDirectory = Installer.getDefault().getInstallFile("");
				ShutdownHandler.getDefault().addDirectoryToRemove(selfDirectory.getAbsolutePath(), false);
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

		initializeProxySupport();

		// Create install operation
		InstallOperation installOperation = createInstallOperation();
		if (installOperation == null) {
			Installer.logError(InstallMessages.Error_InstallOperation);
		}
		// Run install operation
		installOperation.run();
		
		return IApplication.EXIT_OK;
	}
	
	/**
	 * Initializes proxy support
	 */
	private void initializeProxySupport() {
		// Note: Proxies are not currently supported.  If this functionality
		// is required, see the original P2 stand-alone installer.
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
		printHelp(IInstallConstants.COMMAND_LINE_INSTALL_ALL_USERS + "=true (or false)", InstallMessages.Help_InstallForAllUsers);
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
