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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;

import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallProduct;

/**
 * This page presents installed products
 * for selection.
 * This page does not support console.
 */
public class ProductsPage extends InstallWizardPage {
	private static final String[] COLUMN_NAMES = new String[] { 
		InstallMessages.ProductsPage_NameColumn, 
		InstallMessages.ProductsPage_VersionColumn 
	};
	/** Components table column widths */
	private static final int[] COLUMN_WIDTHS = new int[] { 350, 100 };

	/** Installed products */
	protected IInstallProduct[] products;
	/** Message label */
	protected Label messageLabel;
	/** Message to show */
	protected String message;
	/** Products table */
	protected CheckboxTableViewer viewer;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param products Installed products
	 * @param message Message to show
	 */
	public ProductsPage(String pageName, String title, IInstallProduct[] products, String message) {
		super(pageName, title);
		
		this.products = products;
		this.message = message;
		setPageComplete(true);
	}
	
	/**
	 * Returns the installed products.
	 * 
	 * @return Products
	 */
	public IInstallProduct[] getProducts() {
		return products;
	}

	/**
	 * Returns the selected products.
	 * 
	 * @return Selected products
	 */
	public InstallProduct[] getSelectedProducts() {
		Object[] checkedElements = viewer.getCheckedElements();
		InstallProduct[] products = new InstallProduct[checkedElements.length];
		for (int index = 0; index < checkedElements.length; index++) {
			products[index] = (InstallProduct)checkedElements[index];
		}
		
		return products;
	}
	
	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Message label
		messageLabel = new Label(area, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
		messageLabel.setText(message);
		
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
		viewer.setInput(getProducts());
		viewer.setAllChecked(true);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				onProductSelected();
			}
		});
		
		return area;
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
					return product.getName();
				else if (columnIndex == 1)
					return product.getVersion();
			}
			
			return null;
		}
	}
}
