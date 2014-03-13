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
package com.codesourcery.installer.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

/**
 * Displays a text field and optional buttons to restore the 
 * default value.and browse for a file.
 * 
 */
public class BrowseFileDefaultEditor extends BrowseDefaultEditor {
	/** List of file extension filters **/
	private String[] filterExtensions;
	/** List of filter names **/
	private String[] filterNames;
	
	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags
	 * @param includeRestore <code>true</code> to show restore button
	 * @param includeBrowse <code>true</code> to show browse button
	 * @param defaultValue Default value or <code>null</code>
	 */
	public BrowseFileDefaultEditor(Composite parent, int style,
			boolean includeRestore, boolean includeBrowse, String defaultValue) {
		this(parent, style, includeRestore, includeBrowse, defaultValue, null, null);
	}

	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags
	 * @param includeRestore <code>true</code> to show restore button
	 * @param includeBrowse <code>true</code> to show browse button
	 * @param defaultValue Default value or <code>null</code>
	 * @param filterExtensions the file extension filter
	 * @param filterNames the list of filter names, or null for no filter names
	 */
	public BrowseFileDefaultEditor(Composite parent, int style,
			boolean includeRestore, boolean includeBrowse, String defaultValue,
			String[] filterExtensions, String[] filterNames) {
		super(parent, style, includeRestore, includeBrowse, defaultValue);
		
		this.filterExtensions = filterExtensions;
		this.filterNames = filterNames;
	}
	
	@Override
	public void openBrowseDialog() {
		FileDialog dialog = new FileDialog(getShell());
		dialog.setFileName(getText());
		dialog.setFilterExtensions(filterExtensions);
		dialog.setFilterNames(filterNames);
		
		if (getBrowseMessage() != null)
			dialog.setText(getBrowseMessage());

		String file = dialog.open();
		if (file != null)
			setText(file);
	}
}
