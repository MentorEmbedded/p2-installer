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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import com.codesourcery.installer.Installer;

/**
 * Abstract class for an install operation.
 */
public abstract class InstallOperation {
	/** Status file */
	private IPath statusFile;
	
	/**
	 * Runs the install operation.
	 * 
	 * @param context Install context
	 */
	public abstract void run(IInstallContext context);
	
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
		
		if (getStatusFile() != null) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(getStatusFile().toOSString()));
				
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
}
