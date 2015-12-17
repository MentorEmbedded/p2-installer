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
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Provides information about a product that is installed.
 */
public interface IInstalledProduct {
	/**
	 * Returns the product identifier.
	 * 
	 * @return Identifier
	 */
	public String getId();
	
	/**
	 * Returns the product name.
	 * 
	 * @return Product name
	 */
	public String getName();
	
	/**
	 * Returns the product version text.
	 * 
	 * @return Version
	 */
	public String getVersionText();

	/**
	 * Returns the product version.
	 * 
	 * @return Version
	 */
	public Version getVersion();

	/**
	 * Returns the install location.
	 * 
	 * @return Install location
	 */
	public IPath getInstallLocation();
	
	/**
	 * Returns the product category.
	 * 
	 * @return Category or <code>null</code>
	 */
	public String getCategory();
	
	/**
	 * Returns the path to the product uninstaller.
	 * 
	 * @return Uninstaller path or <code>null</code> if product installation
	 * does not include an uninstaller.
	 */
	public IPath getUninstaller();
}
