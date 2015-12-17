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
	 * This method does nothing if {@link #setMirrorLocation(IPath, IProgressMonitor)} has been called.
	 * 
	 * @param path Install location
	 * @param monitor Progress monitor or <code>null</code>
	 * @throws CoreException on failure
	 * @see #setMirrorLocation(IPath, IProgressMonitor)
	 */
	public void setInstallLocation(IPath path, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Sets the location to create a mirror repository.
	 * 
	 * This method does nothing if {@link #setInstallLocation(IPath, IProgressMonitor)} has been called.
	 * 
	 * @param path
	 * @param monitor
	 * @throws CoreException
	 * @see {@link #setInstallLocation(IPath, IProgressMonitor)}
	 */
	public void setMirrorLocation(IPath path, IProgressMonitor monitor) throws CoreException;
	
	/**
	 * Sets the directory for a source P2 repository to use for installation.  Only install components present
	 * in the repository will be available for installation.
	 * 
	 * @param path Path to repository directory
	 * @param CoreException if the directory is empty or made with a different installer
	 */
	public void setSourceLocation(IPath path) throws CoreException;
	
	/**
	 * Returns the install data.
	 * 
	 * @return Install data or <code>null</code> for an uninstallation.
	 */
	public IInstallData getInstallData();
	
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
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void install(IProgressMonitor monitor) throws CoreException;

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
	 * Determine if launch item is available on file system
	 * 
	 * @param item Launch item
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean isLaunchItemAvailable(LaunchItem item);
	
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
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void setInstalledProduct(IInstalledProduct product, IProgressMonitor monitor) throws CoreException;
	
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
	public IInstalledProduct[] getInstalledProductsByRange(IProductRange[] range, boolean uniqueLocations);
	
	/**
	 * Returns all installed products that match a category.
	 * 
	 * @param category Category
	 * @param uniqueLocations <code>true</code> to return only products installed
	 * at different locations.
	 * @return Installed products
	 */
	public IInstalledProduct[] getInstalledProductsByCategory(String category, boolean uniqueLocations);
	
	/**
	 * Returns the existing product in the install location.
	 * 
	 * @param manifest Install manifest to search
	 * @return Existing product or <code>null</code>
	 */
	public IInstallProduct getExistingProduct(IInstallManifest manifest);
	
	/**
	 * Adds a new install verifier.  The verifier will be called to validate install information.
	 * This method does nothing if the verifier has already been added.
	 * 
	 * @param verifier Verifier to add
	 */
	public void addInstallVerifier(IInstallVerifier verifier);
	
	/**
	 * Removes a install verifier.
	 * 
	 * @param verifier Verifier to remove
	 */
	public void removeInstallVerifier(IInstallVerifier verifier);
}
