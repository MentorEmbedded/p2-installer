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
	 * Sets a property.
	 * 
	 * @param name Property name
	 * @param value Property value
	 */
	public void setProperty(String name, Object value);
	
	/**
	 * Gets a property.  The property must be cast to the appropriate data
	 * type.
	 * 
	 * @param name Property name
	 * @return Property value or <code>null</code> if the property has not
	 * been set.
	 */
	public Object getProperty(String name);
}
