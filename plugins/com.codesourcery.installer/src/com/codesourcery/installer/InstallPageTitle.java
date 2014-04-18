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
package com.codesourcery.installer;

/**
 * Installer wizard page title.
 */
public class InstallPageTitle {
	/** Page name */
	private String pageName;
	/** Page title */
	private String pageTitle;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param pageTitle Title to display for the page
	 */
	public InstallPageTitle(String pageName, String pageTitle) {
		this.pageName = pageName;
		this.pageTitle = pageTitle;
	}
	
	/**
	 * Returns the page name.
	 * 
	 * @return Name
	 */
	public String getPageName() {
		return pageName;
	}
	
	/**
	 * Returns the page title.
	 * 
	 * @return Page title
	 */
	public String getPageTitle() {
		return pageTitle;
	}
}
