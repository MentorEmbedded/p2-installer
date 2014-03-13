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

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.Installer;

/**
 * This class describes a registered install action.
 */
public class ActionDescription {
	/** Action identifier configuration attribute */
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	/** Action class configuration attribute */
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	/** Action identifier */
	private String id = null;
	/** Action class */
	private String className;
	/** Action configuration element */
	private IConfigurationElement configurationElement;

	/**
	 * Constructor
	 */
	public ActionDescription(IConfigurationElement configurationElement)
	{
		this.configurationElement = configurationElement;
		this.id = configurationElement.getAttribute(ATTRIBUTE_ID);
		this.className = configurationElement.getAttribute(ATTRIBUTE_CLASS);
	}

	/**
	 * Creates the install action.
	 * 
	 * @return Action
	 * @throws CoreException on failure
	 */
	public IInstallAction createAction() throws CoreException {
		IInstallAction action = null;
		
		// Create the module
		Object extension = getConfigurationElement().createExecutableExtension(ATTRIBUTE_CLASS);
		// Ensure extension implements required class
		if (extension instanceof IInstallAction)
		{
			action = (IInstallAction)extension;
		}
		else
		{
			Installer.fail("Install action class does not implement IInstallAction interface.");
		}

		return action;
	}

	/**
	 * Returns the action identifier.
	 * 
	 * @return ID
	 */
	public String getId()
	{
		return id;
	}
	
	/**
	 * Returns the action class name.
	 * 
	 * @return Class name
	 */
	public String getClassName() {
		return className;
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
