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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents an action that must be performed during
 * installation.  Actions are saved to the installation manifest and
 * uninstalled when a product is uninstalled.
 * 
 * All clients implementing this interface should provide a zero argument
 * constructor so that the action can be instantiated.
 */
public interface IInstallAction {
	
	/** Default progress weight */
	public static final int DEFAULT_PROGRESS_WEIGHT = 100;
	
	/** Install phase allows clients control over the order of the install
	 * actions.
	 */
	public enum InstallPhase {
		PRE_INSTALL,
		INSTALL,
		POST_INSTALL
	}
	
	/**
	 * Returns the identifier for the action used in the 
	 * <code>com.codesourcery.installer.actions</code> extension.
	 * 
	 * @return Identifier
	 */
	public String getId();
	
	/**
	 * Returns if the action is supported for a platform. 
	 *  
	 * @param platform Operating system
	 * @param arch Architecture
	 * @return <code>true</code> if supported
	 * @see {@link org.eclipse.core.runtime.Platform} for operating system
	 * and architecture identifiers.
	 */
	public boolean isSupported(String platform, String arch);

	/**
	 * Returns if the action should be uninstalled on an upgrade.  Some actions
	 * may not require to be uninstalled during an upgrade and can be handled
	 * by the installation of a new action.
	 * 
	 * @return <code>true</code> if the action should be uninstalled
	 */
	public boolean uninstallOnUpgrade();
	
	/**
	 * Returns the progress weight for this action.  The default weight is 100.
	 * Actions that take a long time to perform can report a larger value.
	 * 
	 * @return Progress weight or <code>DEFAULT_PROGRESS_WEIGHT</code>
	 */
	public int getProgressWeight();
	
	/**
	 * Runs the action.  The action is provided with the mode describing the
	 * type of action to perform.
	 *
	 * @param agent Provisioning agent
	 * @param product Product
	 * @param existingProduct Existing product if upgrade or <code>null</code>
	 * for a new installation.
	 * @param installData Install data
	 * @param installDescription Install description
	 * @param mode IInstallMode mode
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor monitor) throws CoreException;

	/**
	 * Saves the action to a document.
	 * The action should save any data required
	 * for uninstallation.
	 * 
	 * @param document Document
	 * @param element Document element for action data
	 * @throws CoreException on failure
	 * @see #load(Element)
	 */
	public void save(Document document, Element element) throws CoreException;
	
	/**
	 * Loads an action for a document element.
	 * 
	 * @param element Document element for action data
	 * @throws CoreException on failure
	 * @see #save(Document, Element)
	 */
	public void load(Element element) throws CoreException;
	
	/**
	 * Specifies the install phase at which this action should run.
	 * 
	 * @return The install phase
	 */
	public InstallPhase getInstallPhase();
}
