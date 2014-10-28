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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

/**
 * Verifiers are called by the installer to validate information.
 * Verifies are registered using the
 * <code>com.codesourcery.installer.verifiers</code> extension point.
 */
public interface IInstallVerifier {
	/**
	 * Verifies an install location.  This method should return an OK status
	 * if the install location is valid.  If there are problems with the install
	 * location then this method should return information in a WARNING or 
	 * ERROR status.  For a WARNING, the user will be prompted to continue.  
	 * If the installer is run in silent mode, the installation will be cancelled.
	 * For an ERROR, the installation will be cancelled.
	 * The root install location can be obtained from 
	 * {@link IInstallDescription#getRootLocation()}.
	 * 
	 * @param installLocation Install location
	 * @return Status <code>IStatus.OK</code> if the location is valid, 
	 * otherwise <code>IStatus.WARNING</code> or <code>IStatus.ERROR</code>.
	 */
	public IStatus[] verifyInstallLocation(IPath installLocation);
	
	/**
	 * Verifies users credentials.  This method should return an <code>OK</code>
	 * status if the credentials are valid.  If there is a problem then this 
	 * method should return information in a <code>WARNING</code> or 
	 * <code>ERROR</code> status.
	 * 
	 * @param username User name
	 * @param password Password
	 * @return Status <code>IStatus.OK</code> if the credentials are valid, 
	 * otherwise <code>IStatus.WARNING</code> or <code>IStatus.ERROR</code>.
	 */
	public IStatus[] verifyCredentials(String username, String password);
}
