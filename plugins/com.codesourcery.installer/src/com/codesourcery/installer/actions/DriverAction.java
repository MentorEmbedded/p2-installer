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
package com.codesourcery.installer.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallPlatform;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
	
/**
 * This action will install digitally signed Windows drivers using the
 * Microsoft Driver Package Installer (DPInst).
 * This action will require administrative privileges to run on Windows.
 */
public class DriverAction extends AbstractInstallAction {
	/** Action identifier */
	public static final String ID = "com.codesourcery.installer.DriverAction";
	/** Directory containing drivers files */
	private IPath driverPath;
	
	/**
	 * Constructor
	 */
	protected DriverAction() {
		super(ID);
	}
	
	@Override
	public boolean isSupported(String platform, String arch) {
		// Only supported on Windows platforms
		return isWindows(platform);
	}
	
	/**
	 * Constructor
	 * 
	 * @param driverPath Path to driver directory
	 */
	protected DriverAction(IPath driverPath) {
		super(ID);
	}
	
	/**
	 * Returns the path to the driver directory.
	 * 
	 * @return Driver path
	 */
	public IPath getDriverPath() {
		return driverPath;
	}

	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product,
			IInstallMode mode, IProgressMonitor monitor) throws CoreException {
		if (mode.isInstall()) {
			IInstallPlatform installPlatform = Installer.getDefault().getInstallPlatform();
			// If Vista or greater, install signed drivers
			installPlatform.installWindowsDriver(getDriverPath().toOSString());
		}
	}
}
