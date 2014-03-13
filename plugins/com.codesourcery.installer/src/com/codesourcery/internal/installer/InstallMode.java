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
	/** <code>true</code> if install, <code>false</code> if uninstall */
	private boolean install;
	/** <code>true</code> if product upgrade */
	private boolean upgrade;
	/** <code>true</code> if root uninstall </code> */
	private boolean rootUninstall;

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
	 * 
	 * @param upgrade <code>true</code> if upgrade
	 */
	public void setUpgrade(boolean upgrade) {
		if (isInstall()) {
			this.upgrade = upgrade;
		}
	}
	
	/**
	 * Sets root uninstall.
	 * 
	 * @param rootUninstall <code>true</code> if roo uninstall
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
		return upgrade;
	}

	@Override
	public boolean isRootUninstall() {
		return rootUninstall;
	}
}
