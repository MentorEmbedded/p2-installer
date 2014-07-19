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
package com.codesourcery.installer.ui;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * A component that can be installed.
 */
public interface IInstallComponent {
	/** Property indicating that component is an add-on */
	public static final String PROPERTY_ADDON = "addon";
	
	/**
	 * Returns the name of the component.
	 * 
	 * @return Name
	 */
	public String getName();

	/**
	 * Returns the description of the component.
	 * 
	 * @return Description
	 */
	public String getDescription();
	
	/**
	 * Returns the install unit for this component.
	 * 
	 * @return Install unit
	 */
	public IInstallableUnit getInstallUnit();
	
	/**
	 * Returns the existing install unit for this component.
	 * 
	 * @return Existing install unit or <code>null</code> if not install unit
	 * is installed already
	 */
	public IInstallableUnit getInstalledUnit();

	/**
	 * Returns other components that this component must be installed with.
	 * 
	 * @return Required components or <code>null</code>
	 */
	public IInstallComponent[] getRequiredComponents();
	
	/**
	 * Returns if this component is optional.
	 * 
	 * @return <code>true</code> if component is optional,
	 * <code>false</code> if component is required.
	 */
	public boolean isOptional();
	
	/**
	 * Sets whether the component should be installed.
	 * 
	 * @param install <code>true</code> to install
	 */
	public void setInstall(boolean install);
	
	/**
	 * Returns if the component should be installed.
	 * 
	 * @return <code>true</code> to install
	 */
	public boolean getInstall();
	
	/**
	 * Returns if the component should be installed by default.
	 * 
	 * @return <code>true</code> if installed by default
	 */
	public boolean isDefault();

	/**
	 * Sets the component included.
	 * 
	 * @param included <code>true</code> if included
	 */
	public void setIncluded(boolean included);
	
	/**
	 * Returns if the component is included.
	 * 
	 * @return <code>true</code> if included
	 */
	public boolean isIncluded();
	
	/**
	 * Sets a component property value.
	 * 
	 * @param name Name of property
	 * @param value Property value or <code>null</code>
	 */
	public void setProperty(String name, String value);
	
	/**
	 * Returns a component property value.
	 * 
	 * @param name Name of property
	 * @return Property value or <code>null</code>
	 */
	public Object getProperty(String name);
}
