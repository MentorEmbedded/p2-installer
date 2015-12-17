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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallProduct;

/**
 * Install product
 */
public class InstallProduct implements IInstallProduct {
	/** Product identifier */
	private String id;
	/** Product name */
	private String name;
	/** Uninstall name */
	private String uninstallName;
	/** Product version string */
	private String versionText;
	/** Product version */
	private Version version;
	/** Product installation actions */
	private ArrayList<IInstallAction> actions = new ArrayList<IInstallAction>();
	/** Install location */
	private IPath location;
	/** P2 install location */
	private IPath installLocation;
	/** Installed units */
	private ArrayList<IVersionedId> units = new ArrayList<IVersionedId>();
	/** Product installation properties */
	private HashMap<String, String> properties = new HashMap<String, String>();
	
	/**
	 * Constructor
	 * 
	 * @param id Product identifier
	 * @param name Product name
	 * @param version Product version
	 * @param uninstallName Uninstall Name of product
	 * @param location Product install location
	 * @param installLocation P2 install location
	 */
	public InstallProduct(String id, String name, String version, 
			String uninstallName, IPath location, IPath installLocation) {
		this.id = id;
		this.name = name;
		this.versionText = version;
		this.uninstallName = (uninstallName != null) ? uninstallName : name;
		this.location = location;
		this.installLocation = installLocation;
		this.version = InstallUtils.createVersion(version);
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getUninstallName() {
		return uninstallName;
	}

	@Override
	public String getVersionString() {
		return versionText;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public IInstallAction[] getActions() {
		return actions.toArray(new IInstallAction[actions.size()]);
	}
	
	@Override
	public void addAction(IInstallAction action) {
		actions.add(action);
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InstallProduct) {
			return ((InstallProduct)other).getId().equals(getId());
		}
		
		return false;
	}

	@Override
	public IPath getLocation() {
		return location;
	}
	
	@Override
	public IPath getInstallLocation() {
		return installLocation;
	}

	@Override
	public void addInstallUnit(IVersionedId unit) {
		if (!units.contains(unit))
			units.add(unit);
	}

	@Override
	public void removeInstallUnit(IVersionedId unit) {
		units.remove(unit);
	}

	@Override
	public IVersionedId[] getInstallUnits() {
		return units.toArray(new IVersionedId[units.size()]);
	}

	@Override
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}

	@Override
	public String getProperty(String name) {
		return properties.get(name);
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(properties);
	}
}
