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

import org.eclipse.core.runtime.IPath;

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
	/** Product version */
	private String version;
	/** Product installation actions */
	private IInstallAction[] actions;
	/** Install location */
	private IPath location;
	/** P2 install location */
	private IPath installLocation;
	
	/**
	 * Constructor
	 * 
	 * @param id Product identifier
	 * @param name Product name
	 * @param version Product version
	 * @param location Product install location
	 * @param installLocation P2 install location
	 * @param actions Install actions
	 */
	public InstallProduct(String id, String name, String version, 
			IPath location, IPath installLocation, IInstallAction[] actions) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.actions = actions;
		this.location = location;
		this.installLocation = installLocation;
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
	public String getVersion() {
		return version;
	}

	@Override
	public IInstallAction[] getActions() {
		return actions;
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
}
