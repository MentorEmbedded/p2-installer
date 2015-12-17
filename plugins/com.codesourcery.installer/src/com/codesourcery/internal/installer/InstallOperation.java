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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import com.codesourcery.installer.IInstallManager;
import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.Installer;

/**
 * Abstract class for an install operation.
 */
public abstract class InstallOperation {
	/** Status file */
	private IPath statusFile;
	
	/**
	 * Runs the install operation.
	 */
	public abstract void run();
	
	/**
	 * Shows an error to the user.
	 * 
	 * @param message Error message
	 */
	public abstract void showError(String message);
	
	/**
	 * Returns the install manager.
	 * 
	 * @return Install manager
	 */
	protected IInstallManager getInstallManager() {
		return Installer.getDefault().getInstallManager();
	}
	
	/**
	 * Sets the path to the status text file to create.  The status file will
	 * contain the result of the installation operation.  For a successful
	 * operation, it will contain "OK".  If the operation was not successful,
	 * the file will contain "FAIL:" followed with an error message.  
	 * 
	 * @param statusFile Status file or <code>null</code>
	 */
	public void setStatusFile(IPath statusFile) {
		this.statusFile = statusFile;
	}
	
	/**
	 * Returns the status file path.
	 * 
	 * @return Status file or <code>null</code>
	 */
	protected IPath getStatusFile() {
		return statusFile;
	}
	
	/**
	 * Writes the status file is available.
	 * 
	 * @param status Status
	 */
	protected void writeStatus(IStatus status) {
		if (status == null)
			return;
		
		IPath statusFile = getStatusFile();
		if (statusFile != null) {
			BufferedWriter writer = null;
			try {
				// Create directories for file if needed
				File statusDirectory = getStatusFile().removeLastSegments(1).toFile();
				if (!statusDirectory.exists()) {
					Files.createDirectories(statusDirectory.toPath());
				}
				//writer = new BufferedWriter(new FileWriter(getStatusFile().toOSString()));
				writer = new BufferedWriter(new FileWriter(statusFile.toFile()));
				
				// Successful
				if (status.isOK()) {
					writer.write("OK");
				}
				// Canceled
				else if (status.getSeverity() == IStatus.CANCEL) {
					writer.write("CANCELED");
				}
				// Failed
				else {
					String message = status.getMessage();
					Throwable error = status.getException();
					if (error != null) {
						message = error.getLocalizedMessage();
						if (error instanceof InvocationTargetException) {
							error = ((InvocationTargetException)error).getTargetException();
							message = error.getLocalizedMessage();
						}
					}

					writer.write("FAIL: ");
					writer.write(message);
				}
				writer.newLine();
			}
			catch (Exception e) {
				Installer.log(e);
			}
			finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}
	}
	
	/**
	 * Runs a product's uninstaller.
	 * If the installer was started in console mode, the uninstaller is run in
	 * console mode.  If the installer was started in GUI mode, the uninstaller
	 * is run in GUI mode.
	 * 
	 * @param product Product
	 * @throws CoreException on failure
	 */
	protected void runProductUninstaller(IInstalledProduct product, boolean wait) throws CoreException {
		try {
			ArrayList<String> args = new ArrayList<String>();
			args.add(product.getUninstaller().toOSString());
			if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE)) {
				args.add(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE);
			}
			if (Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_DATA)) {
				args.add(IInstallConstants.COMMAND_LINE_DATA);
			}
			// No splash screen
			args.add(IInstallConstants.COMMAND_LINE_NO_SPLASH);
	
			ProcessBuilder processBuilder = new ProcessBuilder(args);
			Process process = processBuilder.start();
			// Wait for uninstaller
			if ((process != null)  && wait)
				process.waitFor();
		}
		catch (Exception e) {
			Installer.fail("Failed to launch uninstaller.", e);
		}
	}
	
	/**
	 * Cleans up and removes any installation directories created.
	 * Call this method if the installation is cancelled.
	 */
	protected void cleanupInstallation() {
		try {
			Installer.getDefault().getInstallManager().setInstallLocation(null, null);
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
}
