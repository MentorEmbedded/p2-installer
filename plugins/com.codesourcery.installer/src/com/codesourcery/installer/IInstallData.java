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
 * Install data.  This interface provides install wizard pages a mechanism to
 * set properties that can be used by install actions or other wizard pages.
 */
public interface IInstallData {
	/**
	 * Returns if the data has a property value.
	 * 
	 * @param name Property name
	 * @return <code>true</code> if property has a value
	 */
	public boolean hasProperty(String name);
	
	/**
	 * Sets a property.
	 * 
	 * @param name Property name
	 * @param value Property value
	 */
	public void setProperty(String name, String value);
	
	/**
	 * Gets a property.
	 * 
	 * @param name Property name
	 * @return Property value or <code>null</code> if the property has not
	 * been set.
	 */
	public String getProperty(String name);
	
	/**
	 * Sets a boolean property.
	 * 
	 * @param name Property name
	 * @param value Property value
	 */
	public void setProperty(String name, boolean value);
	
	/**
	 * Returns a boolean property.
	 * 
	 * @param name Property name
	 * @return Property value or <code>false</code> if value has not been set
	 */
	public boolean getBooleanProperty(String name);
}
