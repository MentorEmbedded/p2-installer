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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallManifest;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * This page presents installed products
 * for selection.
 */
public class ProductsPage extends InformationPage implements IInstallConsoleProvider {
	/** Product column names */
	private static final String[] COLUMN_NAMES = new String[] { 
		InstallMessages.ProductsPage_NameColumn, 
		InstallMessages.ProductsPage_VersionColumn 
	};
	/** Components table column widths */
	private static final int[] COLUMN_WIDTHS = new int[] { 350, 100 };

	/** Installed products that can be selected for uninstallation */
	protected IInstallProduct[] selectableProducts;
	/** 
	 * Internal installed products that will automatically be uninstalled when 
	 * all other products are uninstalled.
	 */
	protected IInstallProduct[] internalProducts;
	/** All installed products */
	protected IInstallProduct[] allProducts;
	/** Selected products */
	protected IInstallProduct[] selectedProducts = null;
	/** Message label */
	protected Label messageLabel;
	/** Message to show */
	protected String message;
	/** Products table */
	protected CheckboxTableViewer viewer;
	/** Console list prompter */
	protected ConsoleListPrompter<IInstallProduct> consoleList;
	/** Main area */
	protected Composite mainArea;
	/** Products information area */
	protected Control informationArea;
	/** Products list area */
	protected Control productsArea;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param message Message to show
	 */
	public ProductsPage(String pageName, String title, String message) {
		super(pageName, title);
		
		this.message = message;
		setPageComplete(true);
		
		// Set title
		setInformationTitle(InstallMessages.ProductsPage_InformationTitle);
		// Set information about closing running products
		setStatus(new IStatus[] { new Status(IStatus.INFO, Installer.ID, InstallMessages.ProductsPage_Info) });
	}
	
	/**
	 * Returns the prompt message.
	 * 
	 * @return Message
	 */
	private String getPromptMessage() {
		return message;
	}
	
	/**
	 * Returns the selected products.
	 * 
	 * @return Selected products
	 */
	public IInstallProduct[] getSelectedProducts() {
		return selectedProducts;
	}
	
	/**
	 * Sets the selected products.
	 * 
	 * @param products Selected products
	 */
	private void setSelectedProducts(IInstallProduct[] products) {
		this.selectedProducts = products;
	}
	
	@Override
	public Control createContents(Composite parent) {
		// Main area holding normal information and products list
		// (only one will be shown)
		mainArea = new Composite(parent, SWT.NONE);
		mainArea.setLayout(new GridLayout(1, false));
		mainArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		// Normal information area
		informationArea = super.createContents(mainArea);
		GridData data = (GridData)informationArea.getLayoutData();
		data.exclude = true;
		
		// Products list
		productsArea = createProductsContent(mainArea);
		productsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		return mainArea;
	}
	
	/**
	 * Creates the product list area.
	 * 
	 * @param parent Parent
	 * @return Product list area
	 */
	public Control createProductsContent(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Message label
		messageLabel = new Label(area, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
		messageLabel.setText(getPromptMessage());
		
		viewer = CheckboxTableViewer.newCheckList(area, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		viewer.getTable().setHeaderVisible(true);
		viewer.setLabelProvider(new ProductLabelProvider());
		// Components table columns
		for (int i = 0; i < COLUMN_NAMES.length; i++) {
			TableColumn column = new TableColumn(viewer.getTable(), SWT.LEFT);
			column.setText(COLUMN_NAMES[i]);
			column.setWidth(COLUMN_WIDTHS[i]);
		}
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				onProductSelected();
			}
		});
		
		return area;
	}

	/**
	 * Show list to select products for uninstallation or information for 
	 * products to be uninstalled.
	 * 
	 * @param show <code>true</code> to show selectable products list
	 * <code>false</code> to show products information.
	 */
	private void showProducts(boolean show) {
		GridData data;
		data = (GridData)informationArea.getLayoutData();
		data.exclude = show;
		data = (GridData)productsArea.getLayoutData();
		data.exclude = !show;
		if (show) {
			informationArea.setSize(0, 0);
		}
		else {
			productsArea.setSize(0, 0);
		}
		mainArea.layout(true);		
	}

	/**
	 * Initializes the products.
	 */
	private void initializeProducts() {
		ArrayList<IInstallProduct> selectable = new ArrayList<IInstallProduct>();
		ArrayList<IInstallProduct> automatic = new ArrayList<IInstallProduct>();

		// Get the products available for uninstall
		allProducts = Installer.getDefault().getInstallManager().getInstallManifest().getProducts();
		for (IInstallProduct installedProduct : allProducts) {
			final String TRUE_PROPERTY = Boolean.TRUE.toString();
			// Product can be selected to uninstall
			String showProperty = installedProduct.getProperty(IInstallProduct.PROPERTY_SHOW_UNINSTALL);
			if ((showProperty != null) && TRUE_PROPERTY.equals(showProperty.toLowerCase())) {
				selectable.add(installedProduct);
			}
			// Product will automatically be uninstalled
			else {
				automatic.add(installedProduct);
			}
		}
		selectableProducts = selectable.toArray(new IInstallProduct[selectable.size()]);
		internalProducts = automatic.toArray(new IInstallProduct[automatic.size()]);
	}

	/**
	 * Returns if there are installed products that can be selected for
	 * uninstallation.
	 * 
	 * @return <code>true</code> if products can be selected
	 */
	private boolean hasSelectableProducts() {
		return (selectableProducts.length != 0);
	}
	
	/**
	 * Returns installed products that can be selected.
	 * 
	 * @return Selectable installed products
	 */
	private IInstallProduct[] getSelectableProducts() {
		return selectableProducts;
	}
	
	/**
	 * Returns all installed products.
	 * 
	 * @return Installed products
	 */
	private IInstallProduct[] getAllProducts() {
		return allProducts;
	}
	
	/**
	 * Returns internal installed products that can't be selected for
	 * uninstallation.
	 * 
	 * @return Internal installed products
	 */
	private IInstallProduct[] getInternalProducts() {
		return internalProducts;
	}
	
	/**
	 * @return The formatted message text all products.
	 */
	private String getAllProductsMessage() {
		StringBuilder message = new StringBuilder();
		for (IInstallProduct installedProduct : getAllProducts()) {
			message.append("\n\t<b>");
			message.append(installedProduct.getName());
			message.append("</b> ");
			message.append(installedProduct.getVersionString());
		}
		
		// Add note that the product directory will be removed
		IInstallManifest manifest = Installer.getDefault().getInstallManager().getInstallManifest();
		if (manifest != null) {
			IPath installLocation = manifest.getInstallLocation();
			if (installLocation != null) {
				message.append("\n\n");
				message.append(NLS.bind(InstallMessages.UninstallDirectoryNotice0, installLocation.toOSString()));
				message.append('\n');
			}
		}
		
		return message.toString();
	}
	
	@Override
	public void setActive(IInstallData data) {
		super.setActive(data);

		if (getAllProducts() == null) {
			initializeProducts();
			
			// UI mode
			if (!isConsoleMode()) {
				// If there are no products to select, show that all products
				// will be uninstalled.
				if (!hasSelectableProducts()) {
					showProducts(false);

					setInformation(getAllProductsMessage());
				}
				// Show products to select for uninstall.
				else  {
					showProducts(true);
					
					viewer.setInput(getSelectableProducts());
	
					// Find installed product if available
					IInstallProduct uninstallProduct = null;
					IInstalledProduct installedProduct = Installer.getDefault().getInstallManager().getInstalledProduct();
					if (installedProduct != null) {
						IInstallProduct[] products = Installer.getDefault().getInstallManager().getInstallManifest().getProducts();
						for (IInstallProduct product : products) {
							if (product.getId().equals(installedProduct.getId())) {
								uninstallProduct = product;
								break;
							}
						}
					}
	
					// If installed product has been set, check only it
					if (uninstallProduct != null) {
						viewer.setAllChecked(false);
						viewer.setChecked(uninstallProduct, true);
					}
					// Else check all products
					else {
						viewer.setAllChecked(true);
					}
				}
			}
		}
	}

	/**
	 * Called when the products selection has changed.
	 */
	private void onProductSelected() {
		Object[] checkedElements = viewer.getCheckedElements();
		if (checkedElements.length == 0) {
			setErrorMessage(InstallMessages.ProductsPageSelectionError);
			setPageComplete(false);
		}
		else {
			setErrorMessage(null);
			setPageComplete(true);
		}
		
		getContainer().updateButtons();
	}

	/**
	 * Product label provider
	 */
	private class ProductLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (element instanceof IInstallProduct) {
				if (columnIndex == 0)
					return Installer.getDefault().getImageRegistry().get(IInstallerImages.COMPONENT);
			}
			
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof IInstallProduct) {
				IInstallProduct product = (IInstallProduct)element;
				if (columnIndex == 0)
					return product.getUninstallName();
				else if (columnIndex == 1)
					return product.getVersionString();
			}
			
			return null;
		}
	}

	@Override
	public boolean isSupported() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		return mode.isUninstall();
	}
	
	@Override
	public void saveInstallData(IInstallData data) {
		if (!isConsoleMode()) {
			Object[] checkedElements = viewer.getCheckedElements();
			IInstallProduct[] products = new IInstallProduct[checkedElements.length];
			for (int index = 0; index < checkedElements.length; index++) {
				products[index] = (IInstallProduct)checkedElements[index];
			}

			// All remaining products selected, remove all products
			// (including automatic uninstalled products)
			if (products.length + getInternalProducts().length == getAllProducts().length) {
				setSelectedProducts(getAllProducts());
			}
			// Remove selected products
			else {
				setSelectedProducts(products);
			}
		}
	}

	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;
		
		// Has products to select for uninstall
		if (hasSelectableProducts()) {
			if (input == null) {
				// Create prompter
				consoleList = new ConsoleListPrompter<IInstallProduct>(getPromptMessage(), false);
				// Add prompts
				for (IInstallProduct installedProduct : getSelectableProducts()) {
					consoleList.addItem(installedProduct.getName(), installedProduct, true, true);
				}
			}
			
			// Get response
			response = consoleList.getConsoleResponse(input);
			if (response == null) {
				ArrayList<IInstallProduct> products = new ArrayList<IInstallProduct>();
				consoleList.getSelectedData(products);
				IInstallProduct[] selectedProducts = products.toArray(new IInstallProduct[products.size()]);
				if (selectedProducts.length + getInternalProducts().length == getAllProducts().length) {
					setSelectedProducts(getAllProducts());
				}
				else {
					setSelectedProducts(selectedProducts);
				}
			}
		}
		// Uninstall all products
		else {
			String message = getPromptMessage() + "\n" + formatConsoleMessage(getAllProductsMessage());
			ConsoleYesNoPrompter prompter = new ConsoleYesNoPrompter(message.toString(), InstallMessages.Continue, true);
			response = prompter.getConsoleResponse(input);
			if (response == null) {
				if (prompter.getResult()) {
					setSelectedProducts(getAllProducts());
				}
				else {
					setSelectedProducts(new IInstallProduct[0]);
				}
			}
		}
		
		return response;
	}
}
