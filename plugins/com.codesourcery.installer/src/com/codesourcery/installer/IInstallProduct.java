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

import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Provides information for a manifest installed product.
 */
public interface IInstallProduct {
	/** Property indicating product install directories will be removed on uninstall */
	public final static String PROPERTY_REMOVE_DIRS = "removeDirectories";
	/** 
	 * Property indicating whether this product will be shown in the uninstaller.
	 */
	public final static String PROPERTY_SHOW_UNINSTALL = "showUninstall";
	/** 
	 * Property indicating additional text to be displayed when the product is uninstalled.
	 */
	public final static String PROPERTY_UNINSTALL_TEXT = "uninstallText";
	
	/**
	 * Sets an installation property.
	 * 
	 * @param name Name of property
	 * @param value Value of property
	 */
	public void setProperty(String name, String value);
	
	/**
	 * Returns an installation property.
	 * 
	 * @param name Name of property
	 * @return Value for property
	 */
	public String getProperty(String name);
	
	/**
	 * Returns the product properties.
	 * 
	 * @return Properties
	 */
	public Map<String, String> getProperties();
	
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
	 * Returns the Uninstall name of the product.
	 * 
	 * @return Uninstall name of the product
	 */
	public String getUninstallName();
	
	/**
	 * Returns the version of the product.
	 * 
	 * @return Version string
	 */
	public String getVersionString();
	
	/**
	 * Returns the version of the product.
	 * 
	 * @return Version
	 */
	public Version getVersion();
	
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
	
	/**
	 * Adds an install action to perform for this product.
	 * 
	 * @param action Action to add
	 */
	public void addAction(IInstallAction action);
	
	/**
	 * Adds a new install unit to the product.
	 * If the unit has already been added, this method does nothing.
	 * 
	 * @param unit Unit to add
	 */
	public void addInstallUnit(IVersionedId unit);

	/**
	 * Removes an install unit from the product.
	 * 
	 * @param unit Unit to remove
	 */
	public void removeInstallUnit(IVersionedId unit);
	
	/**
	 * Returns the units installed for the product.
	 * 
	 * @return Install units
	 */
	public IVersionedId[] getInstallUnits();
}
