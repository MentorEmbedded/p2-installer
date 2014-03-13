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

/**
 * Information for a product license.
 */
public class LicenseDescriptor {
	/** License name */
	private String licenseName;
	/** License text */
	private String licenseText;
	/** <code>true</code> if license is enabled **/
	private boolean enabled = true;
	
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
	 * Returns the license content text.
	 * 
	 * @return License text
	 */
	public String getLicenseText() {
		return licenseText;
	}
	
	/**
	 * Returns the license name.
	 * 
	 * @return License name or <code>null</code>
	 */
	public String getLicenseName() {
		return licenseName;
	}
	
	/**
	 * Returns if the license is enabled.
	 * 
	 * @return <code>true</code> if the license is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the license enabled or disabled.
	 * 
	 * @param enable <code>true</code> to enable license
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
