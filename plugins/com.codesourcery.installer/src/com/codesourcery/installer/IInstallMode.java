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
	/** Installer run mode */
	public enum InstallerRunMode {
		/** Running GUI wizard */
		GUI,
		/** Running in console */
		CONSOLE,
		/** Running silently */
		SILENT
	}
	
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
	 * Returns whether product is being upgraded.  This is only valid if 
	 * installing.
	 * 
	 * @return <code>true</code> if earlier version of the product was found.
	 * @see #isInstall()
	 */
	public boolean isUpgrade();
	
	/**
	 * Returns whether product is being updated.  This is only valid if 
	 * installing.
	 * 
	 * @return <code>true</code> if same version of the product was found.
	 * @see #isInstall()
	 */
	public boolean isUpdate();
	
	/**
	 * Returns if the installation is a patch to an existing product.
	 * This is only valid if installing.
	 * 
	 * @return <code>true</code> if patch
	 * @see #isInstall()
	 */
	public boolean isPatch();
	
	/**
	 * @return <code>true</code> if performing a mirror operation
	 */
	public boolean isMirror();

	/**
	 * Returns if the installer run mode.
	 * 
	 * @return Run mode
	 */
	public InstallerRunMode getRunMode();
}
