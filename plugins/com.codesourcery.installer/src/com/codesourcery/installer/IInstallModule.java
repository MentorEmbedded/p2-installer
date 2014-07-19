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
	 * Initializes the install module.  This is the first method called.
	 * 
	 * @param description Install description
	 */
	public void init(IInstallDescription description);
	
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
}
