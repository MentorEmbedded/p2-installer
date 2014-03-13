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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.LaunchItem;


/**
 * This interfaces provides the context for
 * an installation.  All operations are performed
 * using it.
 */
public interface IInstallContext {
	/**
	 * Returns the install description.  This is only available for
	 * an installation.
	 * 
	 * @return Install description or <code>null</code>.
	 * @see #isInstall()
	 */
	public IInstallDescription getInstallDescription();

	/**
	 * Returns the install manifest.  This is only available for
	 * an uninstallation.
	 * 
	 * @return Install manifest or <code>null</code>.
	 * @see #isInstall()
	 */
	public InstallManifest getInstallManifest();
	
	/**
	 * Returns if the context is for an
	 * installation.
	 * 
	 * @return <code>true</code> if install,
	 * <code>false</code> if uninstall
	 */
	public boolean isInstall();
	
	/**
	 * Returns if the context is for a product upgrade.
	 * 
	 * @return <code>true</code> if upgrade
	 */
	public boolean isUpgrade();
	
	/**
	 * Performs the installation.
	 * 
	 * @param installData Install data
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void install(IInstallData installData, IProgressMonitor monitor) throws CoreException;

	/**
	 * Performs the uninstallation.
	 * 
	 * @param product Products to uninstall
	 * @param monitor Progress monitor
	 * @return The result of the uninstallation.
	 * @see #getClass()
	 */
	public void uninstall(IInstallProduct[] products, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Launches a product item.
	 * 
	 * @param item Launch item
	 * @throws CoreException on failure
	 */
	public void launch(LaunchItem item) throws CoreException;
	
	/**
	 * Returns the registered install wizard pages.
	 * 
	 * @return Wizard pages
	 */
	IInstallWizardPage[] getWizardPages();
}
