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
import org.eclipse.swt.widgets.DirectoryDialog;

/**
 * Displays a text field and optional buttons to restore the 
 * default value.and browse for a folder.
 * 
 * @author taimoor
 *
 */
public class BrowseDirectoryDefaultEditor extends BrowseDefaultEditor {

	/**
	 * Constructor 
	 * 
	 * @param parent Parent 
	 * @param style Style flags
	 * @param includeRestore <code>true</code> to show restore button
	 * @param includeBrowse <code>true</code> to show browse button
	 * @param defaultValue Default value or <code>null</code>
	 */
	public BrowseDirectoryDefaultEditor(Composite parent, int style,
			boolean includeRestore, boolean includeBrowse, String defaultValue) {
		super(parent, style, includeRestore, includeBrowse, defaultValue);
	}

	@Override
	public void openBrowseDialog() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(getText());
		if (getBrowseMessage() != null)
			dialog.setMessage(getBrowseMessage());
		String directory = dialog.open();
		if (directory != null) {
			setText(directory);
		}
	}
}
