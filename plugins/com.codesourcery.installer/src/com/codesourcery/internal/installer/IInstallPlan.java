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
 * Provides information about an installation plan.
 */
public interface IInstallPlan {
	/**
	 * Returns the plan status.
	 * 
	 * @return Status
	 */
	public IStatus getStatus();
	
	/**
	 * Returns the error message if the installation plan failed.  This message
	 * will contain the combined status message with any replacements specified
	 * in the installer properties (eclipse.p2.missingRequirement).
	 * 
	 * @return Error message for a failed status or <code>null</code> otherwise.
	 */
	public String getErrorMessage();
	
	/**
	 * Returns the size of the installation.  This will include the size of
	 * the uninstaller if is included.
	 * 
	 * @return Installation size in bytes
	 */
	public long getSize();
}
