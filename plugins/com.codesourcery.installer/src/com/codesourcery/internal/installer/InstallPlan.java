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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.ui.UIUtils;

/**
 * Default implementation of an install plan.
 * 
 * @see com.codesourcery.internal.installer.IInstallPlan
 */
public class InstallPlan implements IInstallPlan {
	/** Plan status */
	private IStatus status;
	/** Error message */
	private String errorMessage;
	/** Install size */
	private long size = -1;
	/** Required installation free space */
	private long requiredSize = -1;
	/** Install location */
	private IPath location;
	
	/**
	 * Constructor
	 * 
	 * @param location Install location
	 * @param requiredSize Required size
	 */
	public InstallPlan(IPath location, long requiredSize) {
		this.location = location;
		this.requiredSize = requiredSize;
	}
	
	/**
	 * Constructor
	 * 
	 * @param location Install location
	 * @param status Plan status
	 * @param size Install size
	 * @param requiredSize Required installation space
	 */
	public InstallPlan(IPath location, IStatus status, long size, long requiredSize) {
		this.location = location;
		this.status = status;
		this.size = size;
		this.requiredSize = requiredSize;

		// Failed status
		try {
			if (!status.isOK()) {
				StringBuilder buffer = new StringBuilder();
				getStatusMessage(buffer, status);
				errorMessage = buffer.toString();
	
				IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
				if (description != null) {
					// Filter the status message according to any find/replace
					// expressions from the install description.
					String[][] expressions = description.getMissingRequirementExpressions();
					if (expressions != null) {
						for (String[] expression : expressions) {
							// Apply expression across new lines
							Pattern findPattern = Pattern.compile(expression[0], Pattern.DOTALL | Pattern.MULTILINE);
							Matcher match = findPattern.matcher(errorMessage);
							// Match found so replace status message
							if ((match != null) && match.find()) {
								errorMessage = match.replaceFirst(expression[1]);
								break;
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	@Override
	public IStatus getStatus() {
		if ((status == null) || status.isOK()) {
			// Insufficient space
			if (getRequiredSize() > getAvailableSpace()) {
				String warningMsg = NLS.bind(InstallMessages.Error_InsufficientSpace1, 
						UIUtils.formatBytes(getRequiredSize()), 
						UIUtils.formatBytes(getAvailableSpace()));
				return new Status(IStatus.WARNING, Installer.ID, warningMsg);
			}
		}
		
		return status;
	}
	
	@Override
	public long getSize() {
		return size;
	}

	@Override
	public long getRequiredSize() {
		return requiredSize;
	}
	
	@Override
	public long getAvailableSpace() {
		if (location != null) {
			return location.toFile().getUsableSpace();
		}
		else {
			return 0;
		}

	}

	/**
	 * Combines all status messages.
	 * 
	 * @param buffer Buffer for messages.  Each status message will be terminated
	 * with a new line. 
	 * @param status Status to combine.
	 */
	private void getStatusMessage(StringBuilder buffer, IStatus status) {
		buffer.append(status.getMessage());
		buffer.append('\n');
		IStatus[] children = status.getChildren();
		for (IStatus child : children) {
			getStatusMessage(buffer, child);
		}
	}
	
	@Override
	public String getErrorMessage() {
		String message = "";
		
		if (errorMessage != null) {
			message = errorMessage;
		}
		else {
			IStatus status = getStatus();
			if (!status.isOK()) {
				message = status.getMessage();
			}
		}
		
		return message;
	}
}
