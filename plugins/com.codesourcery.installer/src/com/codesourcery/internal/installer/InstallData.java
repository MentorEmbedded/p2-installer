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

import com.codesourcery.installer.IInstallData;

/**
 * Default implementation of installation data.
 */
public class InstallData implements IInstallData {
	/** Properties */
	private HashMap<String, String> properties = new HashMap<String, String>();

	/**
	 * Constructor
	 */
	public InstallData() {
	}
	
	@Override
	public boolean hasProperty(String name) {
		return properties.containsKey(name);
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
	public void setProperty(String name, boolean value) {
		properties.put(name, Boolean.toString(value).toLowerCase());
	}

	@Override
	public boolean getBooleanProperty(String name) {
		String value = properties.get(name);
		if (value == null) {
			return false;
		}
		else {
			return Boolean.TRUE.toString().toLowerCase().equals(value.toLowerCase());
		}
	}
}
