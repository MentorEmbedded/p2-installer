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

import com.codesourcery.installer.IInstallMode;

/**
 * Installation mode
 */
public class InstallMode implements IInstallMode {
	/** Update modes */
	private enum UpdateMode {
		/** No update */
		NONE,
		/** Updating installation */
		UPDATE,
		/** Upgrading installation */
		UPGRADE,
	}
	
	/** <code>true</code> if install, <code>false</code> if uninstall */
	private boolean install;
	/** Update mode */
	private UpdateMode updateMode = UpdateMode.NONE; 
	/** <code>true</code> if root uninstall </code> */
	private boolean rootUninstall;
	/** <code>true</code> if patch */
	private boolean patch = false;
	/** Run mode */
	private InstallerRunMode runMode = InstallerRunMode.GUI;

	/**
	 * Constructor
	 * 
	 * @param install <code>true</code> if install, <code>false</code> if
	 * uninstall
	 */
	public InstallMode(boolean install) {
		this.install = install;
	}

	/**
	 * Sets upgrade.
	 */
	public void setUpgrade() {
		if (isInstall()) {
			this.updateMode = UpdateMode.UPGRADE;
		}
	}
	
	/**
	 * Sets update.
	 */
	public void setUpdate() {
		if (isInstall()) {
			this.updateMode = UpdateMode.UPDATE;
		}
	}
	
	/**
	 * Sets patch install.
	 */
	public void setPatch() {
		this.patch = true;
	}

	/**
	 * Sets the run mode.
	 * 
	 * @param runMode Run mode
	 */
	public void setRunMode(InstallerRunMode runMode) {
		this.runMode = runMode;
	}
	
	/**
	 * Sets root uninstall.
	 * 
	 * @param rootUninstall <code>true</code> if root uninstall
	 */
	public void setRootUninstall(boolean rootUninstall) {
		if (isUninstall()) {
			this.rootUninstall = rootUninstall;
		}
	}
	
	@Override
	public boolean isInstall() {
		return install;
	}

	@Override
	public boolean isUninstall() {
		return !install;
	}

	@Override
	public boolean isUpgrade() {
		return (updateMode == UpdateMode.UPGRADE);
	}

	@Override
	public boolean isUpdate() {
		return (updateMode == UpdateMode.UPDATE);
	}

	@Override
	public boolean isRootUninstall() {
		return rootUninstall;
	}

	@Override
	public boolean isPatch() {
		return patch;
	}

	@Override
	public InstallerRunMode getRunMode() {
		return runMode;
	}
}
