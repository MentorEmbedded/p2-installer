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

import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * A Setup page that displays a list of installed products. 
 */
public class SetupInstalledProductsPage extends AbstractSetupPage {
	/** Installed products for selection */
	private IInstalledProduct[] products;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param products Installed products for selection
	 */
	public SetupInstalledProductsPage(String pageName, IInstalledProduct[] products) {
		super(pageName, 
				NLS.bind(InstallMessages.SetupPage_Title0, Installer.getDefault().getInstallManager().getInstallDescription().getProductName()), 
				InstallMessages.SetupPage_Prompt);
		this.products = products;
		
		setBorder(true);
	}

	/**
	 * Returns the installed products.
	 * 
	 * @return Installed products
	 */
	public IInstalledProduct[] getProducts() {
		return products;
	}

	@Override
	protected void createOptions() {
		// Add installed products
		IInstalledProduct[] products = getProducts();
		for (IInstalledProduct product : products) {
			Option choice = new Option(
					product,
					getImage(IInstallerImages.UPDATE_INSTALL),
					product.getName() + " " + product.getVersionText(),
					product.getInstallLocation().toOSString());
			addOption(choice);
		}
		
		// Add option to choose other product
		addOption(new Option(
				null,
				getImage(IInstallerImages.UPDATE_FOLDER),
				InstallMessages.InstalledProductsPage_OtherLabel,
				InstallMessages.InstalledProductsPage_OtherDescription));
		
		// Select first
		if (products.length > 0) {
			selectOption(getOptions()[0]);
		}
	}

	@Override
	protected void saveOption(Option selectedChoice) {
		try {
			IInstalledProduct product = (IInstalledProduct)selectedChoice.getData();
			if (product != null) {
				Installer.getDefault().getInstallManager().setInstalledProduct(product);
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
}
