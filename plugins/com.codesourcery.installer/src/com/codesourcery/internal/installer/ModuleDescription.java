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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.codesourcery.installer.IInstallModule;
import com.codesourcery.installer.Installer;

/**
 * Description of an installer module.
 */
public class ModuleDescription {
	/** Module identifier attribute */
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	/** Module class attribute */
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	/** Module identifier */
	private String id = null;
	/** Configuration element */
	private IConfigurationElement configurationElement;

	/**
	 * Constructor
	 */
	public ModuleDescription(IConfigurationElement configurationElement)
	{
		this.configurationElement = configurationElement;
		this.id = configurationElement.getAttribute(ATTRIBUTE_ID);
	}
	
	/**
	 * Creates an installer module.
	 * 
	 * @return Install module
	 * @throws CoreException on failure
	 */
	public IInstallModule createModule() throws CoreException {
		IInstallModule module = null;

		// Create the module
		Object extension = getConfigurationElement().createExecutableExtension(ATTRIBUTE_CLASS);
		// Ensure extension implements required class
		if (extension instanceof IInstallModule)
		{
			module = (IInstallModule)extension;
		}
		else
		{
			Installer.fail("Install module class does not implement IInstallModule interface.");
		}
		
		return module;
	}

	/**
	 * Returns the ID
	 * 
	 * @return ID
	 */
	public String getId()
	{
		return id;
	}
	
	/**
	 * Returns the configuration element
	 * 
	 * @return Configuration element
	 */
	private IConfigurationElement getConfigurationElement()
	{
		return configurationElement;
	}
}
