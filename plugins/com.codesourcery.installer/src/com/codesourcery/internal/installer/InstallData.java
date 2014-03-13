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
	private HashMap<String, Object> properties = new HashMap<String, Object>();
	
	@Override
	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}

	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}
}
