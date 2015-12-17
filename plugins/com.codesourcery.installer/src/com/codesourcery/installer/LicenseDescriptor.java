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

import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * Information for a product license.
 */
public class LicenseDescriptor {
	/** License name */
	private String licenseName;
	/** License text or <code>null</code> */
	private String licenseText;
	/** Installable unit for license or <code>null</code> */
	private IVersionedId id;
	
	/**
	 * Constructor
	 * 
	 * @param licenseText License text
	 * @param licenseName License name or <code>null</code>
	 */
	public LicenseDescriptor(String licenseText, String licenseName) {
		this.licenseText = licenseText;
		this.licenseName = licenseName;
	}
	
	/**
	 * Constructor
	 * 
	 * @param id Installable unit
	 * @param licenseName License name or <code>null</code>
	 */
	public LicenseDescriptor(IVersionedId iu, String licenseName) {
		this.id = iu;
		this.licenseName = licenseName;
	}
	
	/**
	 * Sets the license content text.
	 * 
	 * @param licenseText License text or <code>null</code>
	 */
	public void setLicenseText(String licenseText) {
		this.licenseText = licenseText;
	}
	
	/**
	 * Returns the license content text.
	 * 
	 * @return License text or <code>null</code>
	 */
	public String getLicenseText() {
		return licenseText;
	}
	
	/**
	 * Returns the installable unit for the license.
	 * 
	 * @return Installable unit or <code>null</code>
	 */
	public IVersionedId getUnit() {
		return id;
	}
	
	/**
	 * Returns the license name.
	 * 
	 * @return License name or <code>null</code>
	 */
	public String getLicenseName() {
		return licenseName;
	}
}
