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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * Base class that displays a text field and optional buttons to restore the 
 * default value.
 */
public abstract class BrowseDefaultEditor extends Composite {
	/** Value editor */
	private Text fieldEditor;
	/** Restore default button */
	private Button restoreButton;
	/** Browse button */
	private Button browseButton;
	/** Default value */
	private String defaultValue;
	/** Browse dialog message */
	private String browseMessage;
	
	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags
	 * @param includeRestore <code>true</code> to show restore button
	 * @param includeBrowse <code>true</code> to show browse button
	 * @param defaultValue Default value or <code>null</code>
	 */
	public BrowseDefaultEditor(Composite parent, int style, boolean includeRestore, boolean includeBrowse, final String defaultValue) {
		super(parent, style);
		
		this.defaultValue = defaultValue;
		
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		
		// Value editor
		fieldEditor = new Text(this, SWT.BORDER);
		fieldEditor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		if ((defaultValue != null) && !defaultValue.isEmpty()) {
			fieldEditor.setText(defaultValue);
			// Scroll to end of location
			int end = defaultValue.length();
			fieldEditor.setSelection(end, end);
			if (defaultValue != null) {
				setText(defaultValue);
			}
		}
		
		// Button area
		Composite buttonArea = new Composite(this, SWT.NONE);
		layout = new GridLayout((includeRestore ? 1 : 0) + (includeBrowse ? 1: 0), false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttonArea.setLayout(layout);
		buttonArea.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		
		// Restore default button
		if (includeRestore) {
			restoreButton = new Button(buttonArea, SWT.NONE);
			restoreButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
			restoreButton.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.REFRESH));
			restoreButton.setText(InstallMessages.RestoreDefaultFolder);
			restoreButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fieldEditor.setText(getDefaultValue());
				}
			});
		}

		// Browse button
		if (includeBrowse) {
			browseButton = new Button(buttonArea, SWT.NONE);
			browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
			browseButton.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.FOLDER));
			browseButton.setText(InstallMessages.Browse);
			browseButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					openBrowseDialog();
				}
			});
		}
	}
	
	/**
	 * Returns the value editor.
	 * 
	 * @return Editor
	 */
	public Text getEditor() {
		return fieldEditor;
	}
	
	/**
	 * Returns the restore default button.
	 * 
	 * @return Restore button
	 */
	public Button getRestoreButton() {
		return restoreButton;
	}
	
	/**
	 * Returns the browse button.
	 * 
	 * @return Browse button
	 */
	public Button getBrowseButton() {
		return browseButton;
	}
	
	/**
	 * Returns the value text.
	 * 
	 * @return Value
	 */
	public String getText() {
		return fieldEditor.getText();
	}
	
	/**
	 * Sets the value text.
	 * 
	 * @param text Value
	 */
	public void setText(String text) {
		fieldEditor.setText(text);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		fieldEditor.setEnabled(enabled);
		if (restoreButton != null)
			restoreButton.setEnabled(enabled);
		if (browseButton != null)
			browseButton.setEnabled(enabled);
	}

	/**
	 * Sets the browse dialog message.
	 * 
	 * @param browseMessage Browse message
	 */
	public void setBrowseMessage(String browseMessage) {
		this.browseMessage = browseMessage;
	}
	
	/**
	 * Returns the browse dialog message.
	 * 
	 * @return Browse message
	 */
	public String getBrowseMessage() {
		return browseMessage;
	}
	
	/**
	 * Sets the default value.
	 * 
	 * @param defaultValue Default value or <code>null</code>
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	/**
	 * Returns the default value.
	 * 
	 * @return Default vaue
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Opens the browse dialog.
	 * Derived classes should override.
	 */
	public abstract void openBrowseDialog();

}
