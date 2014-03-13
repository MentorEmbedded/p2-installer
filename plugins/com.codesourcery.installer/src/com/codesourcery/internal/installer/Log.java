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

import org.eclipse.core.runtime.IStatus;

/**
 * Maintains a log of install operations.
 */
public class Log {
	/** Default instance */
	private static Log instance = null;
	/** New line constant */
	public static String NEW_LINE = System.getProperty("line.separator");
	/** Log buffer */
	private StringBuffer buffer = new StringBuffer();
	
	/**
	 * Returns the default instance.
	 * 
	 * @return Instance
	 */
	public static Log getDefault() {
		if (instance == null) {
			instance = new Log();
		}
		
		return instance;
	}
	
	/**
	 * Logs a message.
	 * 
	 * @param message message
	 */
	public void log(String message) {
		if (message != null) {
			buffer.append(message);
			buffer.append(NEW_LINE);
		}
	}
	
	/**
	 * Logs a status.
	 * 
	 * @param status Status
	 */
	public void log(IStatus status) {
		if (status != null) {
			String message = status.getMessage();
			if (message != null) {
				String severity;
				if (status.getSeverity() == IStatus.ERROR) {
					severity = "ERROR: ";
				}
				else if (status.getSeverity() == IStatus.INFO) {
					severity = "INFO: ";
				}
				else if (status.getSeverity() == IStatus.WARNING) {
					severity = "WARNING: ";
				}
				else {
					severity = "";
				}
				
				Throwable exception = status.getException();
				if (exception != null) {
					log(severity + message + exception.getMessage());
				}
				else {
					log(severity + message);
				}
			}
		}
	}
	
	/**
	 * Returns the log contents.
	 * 
	 * @return Contents
	 */
	public String getContents() {
		return buffer.toString();
	}
}
