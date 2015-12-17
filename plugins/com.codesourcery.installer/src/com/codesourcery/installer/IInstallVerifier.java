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
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;

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
	public IStatus verifyInstallLocation(IPath installLocation);
	
	/**
	 * Verifies an install.  This method is called after the install location
	 * has been chosen and the installer has initialized the location.
	 * This method should return an OK status if the chosen installation is
	 * valid.  If there are problems, the method should return an ERROR status.
	 * 
	 * @param agent Provisioning agent for the install
	 * @param profile Install profile
	 * @return <code>IStatus.OK</code> if the install is valid, otherwise
	 * <code>IStatus.ERROR</code>.
	 */
	public IStatus verifyInstall(IProvisioningAgent agent, IProfile profile);
	
	/**
	 * Verifies install components.  This method is called after the install location has been chosen and install
	 * components have been loaded.
	 * 
	 * @param components Loaded install components
	 * @return Status <code>IStatus.OK</code> if the location is valid, 
	 * otherwise <code>IStatus.WARNING</code> or <code>IStatus.ERROR</code>.
	 */
	public IStatus verifyInstallComponents(IInstallComponent[] components);
	
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
	public IStatus verifyCredentials(String username, String password);
	
	/**
	 * Verifies install components selection.  This method is called any time
	 * an install component selection is made.  This method should return an
	 * <code>OK</code> status if the component selection is valid.  If there is
	 * a problem with the selected components, this method should return
	 * information in a <code>WARNING</code> or <code>ERROR</code> status.
	 * 
	 * @param selectedComponents Currently selected components
	 * @return Status <code>IStatus.OK</code> if the selection is valid, 
	 * otherwise <code>IStatus.WARNING</code> or <code>IStatus.ERROR</code>.
	 */
	public IStatus verifyInstallComponentSelection(IInstallComponent[] selectedComponents);
}
