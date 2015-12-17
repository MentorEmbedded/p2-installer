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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * A Setup page that displays a list of installed products. 
 */
public class SetupInstalledProductsPage extends AbstractListSetupPage {
	/** Installed products for selection */
	private IInstalledProduct[] products;
	/** <code>true</code> to show locations */
	private boolean showLocations;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param pageTitle Page title
	 * @param products Installed products for selection
	 * @param <code>true</code> to show unique install locations in a list 
	 * along with the products installed in the location.
	 * <code>false</code> to show products in a list along with the product 
	 * installed location.
	 */
	public SetupInstalledProductsPage(String pageName, String pageTitle, IInstalledProduct[] products, boolean showLocations) {
		super(pageName, 
				pageTitle, 
				InstallMessages.SetupPage_Prompt);
		this.products = products;
		this.showLocations = showLocations;
		
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
		// Show installations by unique locations
		if (showLocations) {
			// Get unique locations
			HashMap<IPath, ArrayList<IInstalledProduct>> installations = new HashMap<IPath, ArrayList<IInstalledProduct>>();
			for (IInstalledProduct product : products) {
				ArrayList<IInstalledProduct> locationProducts = installations.get(product.getInstallLocation());
				if (locationProducts == null) {
					locationProducts = new ArrayList<IInstalledProduct>();
					installations.put(product.getInstallLocation(), locationProducts);
				}
				locationProducts.add(product);
			}
			// Add options for locations with installed products
			for (Entry<IPath, ArrayList<IInstalledProduct>> entry : installations.entrySet()) {
				ArrayList<IInstalledProduct> products = entry.getValue();
				StringBuffer description = new StringBuffer();
				for (int index = 0; index < products.size(); index ++) {
					IInstalledProduct product = products.get(index);
					
					if (index > 0)
						description.append("\n");
					description.append("  ");
					description.append(product.getName());
					description.append(" (");
					description.append(product.getVersionText());
					description.append(')');
				}
				
				Option choice = new Option(
						products.get(0),
						getImage(IInstallerImages.UPDATE_INSTALL),
						entry.getKey().toOSString(),
						description.toString());
				addOption(choice);
			}
		}
		// Show installations by products
		else {
			IInstalledProduct[] products = getProducts();
			for (IInstalledProduct product : products) {
				Option choice = new Option(
						product,
						getImage(IInstallerImages.UPDATE_INSTALL),
						product.getName() + " " + product.getVersionText(),
						product.getInstallLocation().toOSString());
				addOption(choice);
			}
		}
		
		// Add option to choose other product
		addOption(new Option(
				null,
				getImage(IInstallerImages.UPDATE_FOLDER),
				InstallMessages.InstalledProductsPage_OtherLabel,
				InstallMessages.InstalledProductsPage_OtherDescription));
		
		// Select first
		if (getOptions().length > 1) {
			selectOption(getOptions()[0]);
		}
	}

	@Override
	protected void saveOption(Option selectedChoice) throws CoreException {
		final IInstalledProduct product = (IInstalledProduct)selectedChoice.getData();
		setProduct(product);
	}

	@Override
	public boolean isSupported() {
		return (super.isSupported() && !getInstallMode().isMirror());
	}
}
