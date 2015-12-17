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

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * The install manifest records information for products in an installation.
 */
public interface IInstallManifest {
	/**
	 * Returns the products contained in the
	 * installation.
	 * 
	 * @return Products
	 */
	public IInstallProduct[] getProducts();

	/**
	 * Adds a product to the installation.
	 * 
	 * @param product Product to add
	 */
	public void addProduct(IInstallProduct product);

	/**
	 * Removes a product from the installation.
	 * 
	 * @param product Product to remove
	 */
	public void removeProduct(IInstallProduct product);

	/**
	 * Returns a product.
	 * 
	 * @param productId Product identifier
	 * @return Product or <code>null</code> if no product with the specified
	 * identifier exists in the manifest
	 */
	public IInstallProduct getProduct(String productId);

	/**
	 * Returns all products that are included in a set of ranges.
	 * 
	 * @param ranges Product ranges
	 * @return Products matching the ranges (if any)
	 */
	public IInstallProduct[] getProducts(IProductRange[] ranges);

	/**
	 * Returns the path to the installer data directory.
	 * 
	 * @return Path to data directory
	 */
	public IPath getDataPath();
	
	/**
	 * Saves the manifest to a file.
	 * 
	 * @param path File
	 * @throws CoreException on failure
	 */
	public void save(File file) throws CoreException;

	/**
	 * Convenience method to save the manifest
	 * to an existing file.
	 * 
	 * @throws CoreException on failure
	 */
	public void save() throws CoreException;
	
	/**
	 * Loads the manifest from a file.
	 * 
	 * @param path File
	 * @throws CoreException on failure
	 */
	public void load(File file) throws CoreException;

	/**
	 * Returns the install location.
	 * 
	 * @return Install location or <code>null</code> if manifest has not been saved or loaded.
	 */
	public IPath getInstallLocation();
}
