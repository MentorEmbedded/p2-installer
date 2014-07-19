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

import org.eclipse.core.runtime.IStatus;

import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.Installer;

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
	/** Installation size */
	private long size = -1;
	
	/**
	 * Constructor
	 * 
	 * @param status Plan status
	 * @param size Installation size
	 */
	public InstallPlan(IStatus status, long size) {
		this.status = status;
		this.size = size;

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
		return status;
	}
	
	@Override
	public long getSize() {
		return size;
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
		return errorMessage;
	}
}
