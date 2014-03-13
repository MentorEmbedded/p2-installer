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
	 * Sets if the component is an add-on.
	 * 
	 * @param addon <code>true</code> if add-on
	 */
	public void setAddon(boolean addon);
	
	/**
	 * Returns if the component is an add-on.
	 * 
	 * @return <code>true</code> if add-on
	 */
	public boolean isAddon();
	
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
}
