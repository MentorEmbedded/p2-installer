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
 * Uninstaller modes.
 */
public class UninstallMode {
	/** <code>true</code> to show product in uninstaller */
	private boolean showUninstall;
	/** <code>true</code> to create add/remove entry */
	private boolean createAddRemove;
	/** <code>true</code> to remove created directories on uninstall */
	private boolean removeDirectories;
	
	/**
	 * Constructor
	 * 
	 * @param showUninstall <code>true</code> to show product in uninstaller
	 * @param createAddRemove <code>true</code> to create add/remove entry on
	 * windows platforms.
	 * @param removeDirectories <code>true</code> to remove directories created
	 * during installation.
	 */
	public UninstallMode(boolean showUninstall, boolean createAddRemove, 
			boolean removeDirectories) {
		this.showUninstall = showUninstall;
		this.createAddRemove = createAddRemove;
		this.removeDirectories = removeDirectories;
	}

	/**
	 * Returns if the product should be shown in the uninstaller.
	 * 
	 * @return <code>true</code> to show product in uninstaller
	 */
	public boolean getShowUninstall() {
		return showUninstall;
	}
	
	/**
	 * Returns if an add/remove entry should be created on Windows platforms.
	 * 
	 * @return <code>true</code> to create add/remove entry
	 */
	public boolean getCreateAddRemove() {
		return createAddRemove;
	}
	
	/**
	 * Returns if directories created during installation should be removed
	 * during uninstallation.
	 * 
	 * @return <code>true</code> to remove created directories.
	 */
	public boolean getRemoveDirectories() {
		return removeDirectories;
	}
}
