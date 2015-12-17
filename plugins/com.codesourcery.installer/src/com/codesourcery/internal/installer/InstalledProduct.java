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

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.Version;

import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.Installer;

/**
 * Installed product information
 */
public class InstalledProduct implements IInstalledProduct {
	/** Product identifier */
	private String id;
	/** Product name */
	private String name;
	/** Product version text */
	private String versionText;
	/** Product version */
	private Version version;
	/** Product installed location */
	private IPath installLocation;
	/** Product category */
	private String category;
	
	/**
	 * Constructor
	 * 
	 * @param id Product identifier
	 * @param name Product name
	 * @param version Product version
	 * @param installLocation Product install location
	 * @param category or <code>null</code>
	 */
	public InstalledProduct(String id, String name, String version, IPath installLocation, String category) {
		this.id = id;
		this.name = name;
		this.versionText = version;
		this.installLocation = installLocation;
		this.version = InstallUtils.createVersion(version);
		this.category = category;
	}

	@Override
	public String getId() {
		return id;
	}


	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVersionText() {
		return versionText;
	}

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public String getCategory() {
		return category;
	}

	@Override
	public IPath getInstallLocation() {
		return installLocation;
	}

	@Override
	public IPath getUninstaller() {
		// Uninstaller path
		IPath uninstallerPath = null;
		String uninstallerName = Installer.getDefault().getInstallManager().getInstallDescription().getUninstallerName();
		if (uninstallerName != null) {
			// On Windows, the uninstaller executable requires a 'c' appended for silent or console mode
			if (Installer.isWindows() && 
				(Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_CONSOLE) || 
				Installer.getDefault().hasCommandLineOption(IInstallConstants.COMMAND_LINE_INSTALL_SILENT))) {
				uninstallerName += "c";
			}
			
			uninstallerPath = getInstallLocation().append(IInstallConstants.UNINSTALL_DIRECTORY);
			uninstallerPath = uninstallerPath.append(uninstallerName);
			if (Installer.isWindows())
				uninstallerPath = uninstallerPath.addFileExtension(IInstallConstants.EXTENSION_EXE);
			// Check that uninstall exists
			if (!uninstallerPath.toFile().exists())
				uninstallerPath = null;
		}
		
		return uninstallerPath;
	}
}
