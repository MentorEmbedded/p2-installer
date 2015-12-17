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
package com.codesourcery.internal.installer.ui.pages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallManager;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallMode;

/**
 * The Setup page displays options to update, upgrade, or uninstall an existing
 * product. 
 */
public class SetupPage extends AbstractListSetupPage {
	/** Update options */
	private enum UpdateOption {
		/** Update installation */
		UPDATE,
		/** Uninstall */
		UNINSTALL,
		/** Install */
		INSTALL,
		/** Upgrade installation */
		UPGRADE
	}
	/** Installed product */
	private IInstalledProduct installedProduct;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param pageTitle Page title
	 * @param product Installed product for update
	 */
	public SetupPage(String pageName, String pageTitle, IInstalledProduct product) {
		super(pageName,
				pageTitle,
				InstallMessages.SetupPrompter_Prompt);
		
		this.installedProduct = product;
	}

	/**
	 * Returns the installed product.
	 * 
	 * @return Product
	 */
	public IInstalledProduct getProduct() {
		return installedProduct;
	}
	
	@Override
	protected void createOptions() {
		// Product version
		Version productVersion = Installer.getDefault().getInstallManager().getInstallDescription().getProductVersion();
		
		// Update installation
		if (productVersion.equals(getProduct().getVersion())) {
			// Change
			addOption(new Option(
					UpdateOption.UPDATE, 
					getImage(IInstallerImages.UPDATE_ADD), 
					InstallMessages.SetupPrompter_Change,
					InstallMessages.SetupPrompter_ChangeDescription));
			
			// Remove (only if uninstaller found)
			if (getProduct().getUninstaller() != null) {
				addOption(new Option(
						UpdateOption.UNINSTALL,
						getImage(IInstallerImages.UPDATE_REMOVE), 
						InstallMessages.SetupPrompter_Remove, 
						InstallMessages.SetupPrompter_RemoveDescription));
			}
		}
		// Upgrade installation
		else {
			String upgradeDescription = NLS.bind(InstallMessages.SetupPrompter_UpgradeDescription0, productVersion);
			addOption(new Option(
					UpdateOption.UPGRADE,
					getImage(IInstallerImages.UPDATE_INSTALL), 
					InstallMessages.SetupPrompter_Upgrade, 
					upgradeDescription));
		}
		// Install to a different location
		addOption(new Option(
				UpdateOption.INSTALL,
				getImage(IInstallerImages.UPDATE_FOLDER), 
				InstallMessages.SetupPrompter_Install, 
				InstallMessages.SetupPrompter_InstallDescription));
	}

	@Override
	protected void saveOption(Option selectedChoice) throws CoreException {
		try {
			UpdateOption option = (UpdateOption)selectedChoice.getData();

			// If uninstall, replace the install mode
			// This must be done before setting the product for uninstallation
			if (option == UpdateOption.UNINSTALL) {
				((InstallManager)Installer.getDefault().getInstallManager()).setInstallMode(new InstallMode(false));
			}
			// If update, upgrade, or uninstall then set the installed product.
			// This will set the install location, upgrade installation mode,
			// and hide the Install Folder wizard page.
			if ((option == UpdateOption.UPDATE) || (option == UpdateOption.UPGRADE) || (option == UpdateOption.UNINSTALL)) {
				setProduct(getProduct());
			}
			// Install to a different location
			else if (option == UpdateOption.INSTALL) {
				setProduct(null);
			}
		}
		catch (CoreException e) {
			throw e;
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}

	@Override
	public boolean isSupported() {
		return (super.isSupported() && !getInstallMode().isMirror());
	}
}
