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
 * Update site description
 */
public class UpdateSite {
	/** Site name */
	private String name;
	/** Site location */
	private String location;
	
	/**
	 * Constructor
	 * 
	 * @param location Site location
	 * @param name Site name or <code>null</code>
	 */
	public UpdateSite(String location, String name) {
		this.name = name;
		this.location = location;
	}
	
	/**
	 * Returns the site name.
	 * 
	 * @return Site name or <code>null</code>
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the site location.
	 * 
	 * @return Site location
	 */
	public String getLocation() {
		return location;
	}
}
