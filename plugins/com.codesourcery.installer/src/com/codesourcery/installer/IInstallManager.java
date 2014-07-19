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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IInstallManager {
	/**
	 * Sets the install location and creates initial directory.
	 * The directories for any previous set install location will be deleted.
	 * This method also will adjust the install mode if there is an existing
	 * installation at the location.
	 * 
	 * @param path Install location
	 * @throws CoreException on failure
	 */
	public void setInstallLocation(IPath path) throws CoreException;
	
	/**
	 * Returns the install location.
	 * 
	 * @return Install location
	 */
	public IPath getInstallLocation();
	
	/**
	 * Returns the install mode.
	 * 
	 * @return Install mode
	 */
	public IInstallMode getInstallMode();
	
	/**
	 * Sets the install description.
	 * 
	 * @param installDescription Install description
	 */
	public void setInstallDescription(IInstallDescription installDescription);
	
	/**
	 * Returns the install description.  This is only available for
	 * an installation.
	 * 
	 * @return Install description or <code>null</code>.
	 * @see #isInstall()
	 */
	public IInstallDescription getInstallDescription();

	/**
	 * Sets the install manifest.
	 * 
	 * @param installManifest Install manifest
	 */
	public void setInstallManifest(IInstallManifest installManifest);
	
	/**
	 * Returns the install manifest.  This is only available for
	 * an uninstallation.
	 * 
	 * @return Install manifest or <code>null</code>.
	 * @see #isInstall()
	 */
	public IInstallManifest getInstallManifest();
	
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
	public IInstallWizardPage[] getWizardPages();

	/**
	 * Returns all wizard pages that are currently supported.
	 * 
	 * @return Supported wizard pages
	 */
	public IInstallWizardPage[] getSupportedWizardPages();
	
	/**
	 * Returns the installed product to update with installation.
	 * 
	 * @return Installed product or <code>null</code> if the product is
	 * not installed.
	 */
	public IInstalledProduct getInstalledProduct();
	
	/**
	 * Sets the installed product to update with installation.  The location of
	 * the product will be used and the Install Folder wizard page will not be
	 * displayed.
	 * 
	 * @param product Installed product or <code>null</code>
	 * @throws CoreException on failure
	 */
	public void setInstalledProduct(IInstalledProduct product) throws CoreException;
	
	/**
	 * Returns an installed product by identifier.
	 * 
	 * @param id Product identifier.
	 * @return Installed product or <code>null</code> if product is not installed.
	 */
	public IInstalledProduct getInstalledProduct(String id);
	
	/**
	 * Returns all installed products found in the install registry.
	 * 
	 * @return Installed products
	 */
	public IInstalledProduct[] getInstalledProducts();
	
	/**
	 * Returns all installed products that match a given set of product version 
	 * ranges.  
	 *
	 * @param range Range of products to return.
	 * @param uniqueLocations <code>true</code> to return only products installed
	 * at different locations
	 * @return Installed products
	 */
	public IInstalledProduct[] getInstalledProducts(IProductRange[] range, boolean uniqueLocations);
	
	/**
	 * Returns the existing product in the install location.
	 * 
	 * @param manifest Install manifest to search
	 * @return Existing product or <code>null</code>
	 */
	public IInstallProduct getExistingProduct(IInstallManifest manifest);
}
