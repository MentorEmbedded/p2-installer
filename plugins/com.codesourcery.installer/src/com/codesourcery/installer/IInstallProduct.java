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

/**
 * Provides information for an installed product.
 */
public interface IInstallProduct {
	/**
	 * Returns the unique product identifier.
	 * 
	 * @return Product identifier
	 */
	public String getId();
	
	/**
	 * Returns the product name.
	 * 
	 * @return Name
	 */
	public String getName();
	
	/**
	 * Returns the version of the product.
	 * 
	 * @return Version
	 */
	public String getVersion();
	
	/**
	 * Returns the product install location.
	 * 
	 * @return Product install location
	 */
	public IPath getLocation();
	
	/**
	 * Returns the P2 install location.
	 * 
	 * @return P2 install location
	 */
	public IPath getInstallLocation();
	
	/**
	 * Returns the install actions to perform for this product.
	 * 
	 * @return Actions
	 */
	public IInstallAction[] getActions();
}
