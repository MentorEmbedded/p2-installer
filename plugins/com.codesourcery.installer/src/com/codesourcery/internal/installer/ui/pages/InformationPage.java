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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.InstallWizardPage;

/**
 * A general wizard page that shows information.
 * This page does not support console.
 */
public class InformationPage extends InstallWizardPage {
	/** Message */
	private String information;
	/** Message title */
	private String informationTitle = null;
	/** Message label */
	private FormattedLabel informationLabel;
	/** Message title label*/
	private FormattedLabel informationTitleLabel;
	/** <code>true</code> to enable scrolling */
	private boolean scrollable = false;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 */
	public InformationPage(String pageName, String title) {
		super(pageName, title);
		information = ""; //$NON-NLS-1$
	}
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param information Page information
	 */
	public InformationPage(String pageName, String title, String information) {
		this(pageName, title);

		this.information = information;
	}

	/**
	 * Returns the page information
	 * 
	 * @return Page information
	 */
	public String getInformation() {
		return information;
	}

	/**
	 * Sets the page information
	 * 
	 * @param text Page information
	 */
	public void setInformation(String text) {
		this.information = text;
		if (informationLabel != null) {
			informationLabel.setText(text);
		}
	}

	/**
	 * Enables/disables scrolling of information.
	 * 
	 * @param scrollable <code>true</code> to enable scrolling
	 */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}
	
	/**
	 * Returns if scrolling is enabled.
	 * 
	 * @return <code>true</code> if scrolling is enabled
	 */
	public boolean isScrollable() {
		return scrollable;
	}

	/**
	 * Sets the title to display above the information area
	 * 
	 * @param title Title or <code>null</code> for no title
	 */
	public void setInformationTitle(String title) {
		this.informationTitle = title ;
		if (informationTitleLabel != null) {
			informationTitleLabel.setText(informationTitle);
		}
	}
	
	/**
	 * Returns the title to display above the information area.
	 * 
	 * @return Title
	 */
	public String getInformationTitle() {
		return informationTitle;
	}

	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Create title area if required
		if (getInformationTitle() != null) {
			informationTitleLabel = new FormattedLabel(area, SWT.WRAP);
			informationTitleLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
			informationTitleLabel.setText(getInformationTitle());
		}
		
		// Create information area
		createInformationArea(area);
		
		return area;
	}
	
	/**
	 * Sets the background color.
	 * 
	 * @param background Background color
	 */
	public void setBackground(Color background) {
		informationLabel.setBackground(background);
	}
	
	/**
	 * Sets the foreground color.
	 * 
	 * @param foreground Foreground color
	 */
	public void setForeground(Color foreground) {
		informationLabel.setForeground(foreground);
	}
	
	/**
	 * Creates the information area.
	 * 
	 * @param area Parent
	 * @return Information area
	 */
	protected Control createInformationArea(Composite area) {
		int flags = SWT.WRAP;
		// Enable scrolling
		if (isScrollable())
			flags |= SWT.BORDER | SWT.V_SCROLL;
		
		// Information area
		informationLabel = new FormattedLabel(area, flags);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		informationLabel.setLayoutData(data);
		informationLabel.setText(information);
		if (isScrollable())
			informationLabel.setEnabled(true);

		return informationLabel;
	}
}
