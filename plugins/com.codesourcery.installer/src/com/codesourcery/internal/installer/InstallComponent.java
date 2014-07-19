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

import java.util.HashMap;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.codesourcery.installer.ui.IInstallComponent;

/**
 * A component that can be installed.
 */
public class InstallComponent implements IInstallComponent {
	/** Install unit */
	private IInstallableUnit installUnit;
	/** Existing install unit */
	private IInstallableUnit installedUnit;
	/** <code>true</code> if component is optional */
	private boolean optional;
	/** Other required components */
	private IInstallComponent[] requiredComponents;
	/** <code>true</code> if component should be installed */
	private boolean install = false;
	/** <code>true</code> if component should be installed by default */
	private boolean defaultInstall = false;
	/** <code>true</code> if the component is included */
	private boolean included = true;
	/** Component properties */
	private HashMap<String, Object> properties;

	/**
	 * Constructor
	 * 
	 * @param installUnit Install unit for this component
	 */
	public InstallComponent(IInstallableUnit installUnit) {
		this.installUnit = installUnit;
	}

	@Override
	public String getName() {
		return getInstallUnit().getProperty(IInstallableUnit.PROP_NAME, null);
	}
	
	@Override
	public String getDescription() {
		return getInstallUnit().getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
	}
	
	@Override
	public IInstallableUnit getInstallUnit() {
		return installUnit;
	}
	
	/**
	 * Sets required components.
	 * 
	 * @param requiredComponents Required components
	 */
	public void setRequiredComponents(IInstallComponent[] requiredComponents) {
		this.requiredComponents = requiredComponents;
	}
	
	@Override
	public IInstallComponent[] getRequiredComponents() {
		return requiredComponents;
	}

	/**
	 * Sets the component optional.
	 * 
	 * @param optional <code>true</code> if component is optional.
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
	}
	
	@Override
	public boolean isOptional() {
		return optional;
	}

	@Override
	public String toString() {
		return getName() + " - " + getInstallUnit().getVersion().toString();
	}

	@Override
	public void setInstall(boolean install) {
		this.install = install;
	}

	@Override
	public boolean getInstall() {
		return install;
	}

	/**
	 * Sets the component to be installed by default.
	 * 
	 * @param defaultInstall <code>true</code> if component should be installed
	 * by default
	 */
	public void setDefault(boolean defaultInstall) {
		this.defaultInstall = defaultInstall;
	}
	
	@Override
	public boolean isDefault() {
		return defaultInstall;
	}

	/**
	 * Sets the installed unit.
	 * 
	 * @param installedUnit Installed unit or <code>null</code>
	 */
	public void setInstalledUnit(IInstallableUnit installedUnit) {
		this.installedUnit = installedUnit;
	}
	
	@Override
	public IInstallableUnit getInstalledUnit() {
		return installedUnit;
	}

	@Override
	public void setProperty(String name, String value) {
		if (properties == null) {
			properties = new HashMap<String, Object>();
		}
		properties.put(name, value);
	}

	@Override
	public Object getProperty(String name) {
		if (properties == null) {
			return null;
		}
		else {
			return properties.get(name);
		}
	}

	@Override
	public void setIncluded(boolean included) {
		this.included = included;
	}

	@Override
	public boolean isIncluded() {
		return included;
	}
}
