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

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.codesourcery.installer.IInstallComponent;

/**
 * A component that can be installed.
 */
public class InstallComponent implements IInstallComponent {
	/** Constant for no members */
	private static final IInstallComponent[] NO_MEMBERS = new IInstallComponent[0];
	/** Install unit */
	private IInstallableUnit installUnit;
	/** Existing install unit */
	private IInstallableUnit installedUnit;
	/** <code>true</code> if component is optional */
	private boolean optional;
	/** <code>true</code> if component should be installed */
	private boolean install = false;
	/** <code>true</code> if component should be installed by default */
	private boolean defaultInstall = false;
	/** <code>true</code> if the component is included */
	private boolean included = true;
	/** Component properties */
	private HashMap<String, Object> properties;
	/** Parent component or <code>null</code> */
	private IInstallComponent parent;
	/** Group components */
	private ArrayList<IInstallComponent> members;

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
	 * Sets the parent component for this component.
	 * 
	 * @param parent Parent or <code>null</code> for no parent
	 */
	public void setParent(IInstallComponent parent) {
		this.parent = parent;
	}
	
	/**
	 * Adds a component to the group.
	 * 
	 * @param component Component to add
	 */
	public void addComponent(IInstallComponent component) {
		if (members == null) {
			members = new ArrayList<IInstallComponent>();
		}
		if (!members.contains(component)) {
			members.add(component);
		}
	}
	
	/**
	 * Sets the component optional.
	 * 
	 * @param optional <code>true</code> if component is optional.
	 */
	public void setOptional(boolean optional) {
		this.optional = optional;
		// If required and has members, set all members as required
		if (hasMembers()) {
			IInstallComponent[] members = getMembers();
			for (IInstallComponent member : members) {
				((InstallComponent)member).setOptional(optional);
			}
		}
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
		// If group component, set install for members also
		if (hasMembers()) {
			IInstallComponent[] members = getMembers();
			for (IInstallComponent member : members) {
				((InstallComponent)member).setInstall(install);
			}
		}
		RepositoryManager.getDefault().fireComponentChanged(this);
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
		// If group component, set default for members also
		if (defaultInstall && hasMembers()) {
			IInstallComponent[] members = getMembers();
			for (IInstallComponent member : members) {
				((InstallComponent)member).setDefault(defaultInstall);
			}
		}
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
		RepositoryManager.getDefault().fireComponentChanged(this);
	}

	@Override
	public boolean isIncluded() {
		return included;
	}

	@Override
	public boolean hasMembers() {
		return (members != null);
	}

	@Override
	public IInstallComponent[] getMembers() {
		if (members == null) {
			return NO_MEMBERS;
		}
		else {
			return members.toArray(new IInstallComponent[members.size()]);
		}
	}

	@Override
	public boolean isMemberOf(IInstallComponent component) {
		boolean member = false;
		IInstallComponent parent = getParent();
		while (parent != null) {
			if (parent.equals(component)) {
				member = true;
				break;
			}
			parent = parent.getParent();
		}
		
		return member;
	}

	@Override
	public IInstallComponent getParent() {
		return parent;
	}
}
