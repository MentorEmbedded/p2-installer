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
	/** <code>true</code> if patch */
	private boolean patch = false;
	/** Run mode */
	private InstallerRunMode runMode = InstallerRunMode.GUI;
	/** <code>true</code> if creating a mirror */
	private boolean mirror;

	/**
	 * Constructs an install mode.
	 * 
	 * @param install <code>true</code> if install, <code>false</code> if
	 * uninstall
	 */
	public InstallMode(boolean install) {
		this.install = install;
	}
	
	/**
	 * Constructs an install mode from another install mode.
	 * 
	 * @param other Install mode to copy.
	 */
	public InstallMode(InstallMode other) {
		this.install = other.install;
		this.updateMode = other.updateMode;
		this.patch = other.patch;
		this.runMode = other.runMode;
		this.mirror = other.mirror;
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
	 * Sets mirror operation.
	 */
	public void setMirror() {
		this.mirror = true;
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
	 * Sets installing or uninstalling.
	 * 
	 * @param install <code>true</code> for installing.
	 */
	public void setInstall(boolean install) {
		this.install = install;
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
	public boolean isPatch() {
		return patch;
	}

	@Override
	public boolean isMirror() {
		return mirror;
	}

	@Override
	public InstallerRunMode getRunMode() {
		return runMode;
	}
}
