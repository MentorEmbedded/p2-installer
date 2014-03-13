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
 * Installation mode
 */
public interface IInstallMode {
	/**
	 * Returns if installing.
	 * 
	 * @return <code>true</code> if installing
	 */
	public boolean isInstall();
	
	/**
	 * Returns if uninstalling.
	 * 
	 * @return <code>true</code> if uninstalling
	 */
	public boolean isUninstall();
	
	/**
	 * Returns if upgrading.  This is only valid if installing.
	 * 
	 * @return <code>true</code> if upgrading
	 * @see #isInstall()
	 */
	public boolean isUpgrade();
	
	/**
	 * Returns if the entire installation is being removed.  This is only valid
	 * if uninstalling.  If an actions artifacts will be removed when the
	 * installation directory is removed, the action can choose not to run.
	 * 
	 * @return <code>true</code> if the entire installation is being removed,
	 * <code>false</code> if only a single product is being removed.
	 */
	public boolean isRootUninstall();
}
