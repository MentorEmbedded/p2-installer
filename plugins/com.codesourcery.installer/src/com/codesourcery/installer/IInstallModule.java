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

import java.util.Map;

import org.eclipse.equinox.p2.core.IProvisioningAgent;

/**
 * An install module can contribute install actions and/or install wizard 
 * pages.
 * Modules can be contributed using the 
 * <code>com.codesourcery.installer.modules</code> extension point.
 * All modules will be included in an installation unless the 
 * <code>eclipse.p2.modules</code> install property is set.
 */
public interface IInstallModule {
	/**
	 * Returns the module identifier.
	 * 
	 * @return Identifier
	 */
	public String getId();
	
	/**
	 * Called to initialize the install module.
	 * 
	 * @param description Installation description
	 */
	public void init(IInstallDescription description);
	
	/**
	 * Called to give the install module the opportunity to register services with a new provisioning agent.
	 * 
	 * @param agent Provisioning agent
	 */
	public void initAgent(IProvisioningAgent agent);
	
	/**
	 * Called to set defaults for any install data.  Modules should set default
	 * values for data used in wizard pages or during silent installation.
	 * 
	 * @param data Install data
	 */
	public void setDataDefaults(IInstallData data);
	
	/**
	 * Returns pages to add to the install wizard.  All pages should extend
	 * <code>InstallWizardPage</code>.
	 * Modules can be contributed using the 
	 * <code>com.codesourcery.installer.modules</code> extension point.
	 * 
	 * @param installMode Installation mode
	 * @return Install wizard pages or <code>null</code>
	 */
	public IInstallWizardPage[] getInstallPages(IInstallMode installMode);
	
	/**
	 * Returns the actions to perform for an installation
	 * of a product.  An existing product will be provided in the case of an
	 * upgrade.
	 * 
	 * @param P2 provisioning agent for the installation
	 * @param data Install data
	 * @param installMode Installation mode
	 * @return Actions to perform
	 */
	public IInstallAction[] getInstallActions(IProvisioningAgent agent, IInstallData data, 
			IInstallMode installMode);
	
	/**
	 * Called to let the module set any environment variables that are needed
	 * for items launched at the end of an installation.
	 * 
	 * @param Map of environment variable names to environment variable values.
	 * The module can set new variable values.
	 */
	public void setEnvironmentVariables(Map<String, String> environmentVariables);
}
