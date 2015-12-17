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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;

import com.codesourcery.installer.IInstallData;

/**
 * A wizard page shown in the installer.
 * Modules contribute wizard pages through the
 * {@link IInstallModule#getInstallPages(IInstallDescription)} method.
 * If the wizard page will contribute to the installer summary information,
 * it should implement {@link IInstallSummaryProvider}.
 * If the wizard page should be shown in console mode it should implement
 * {@link IInstallConsoleProvider}.
 */
public interface IInstallWizardPage extends IWizardPage {
	/**
	 * Creates the page contents.
	 * Clients should override and create the install wizard page contents.
	 * This method will not be called in console mode.
	 * 
	 * @param parent Parent
	 * @return Page Page control
	 */
	public Control createContents(Composite parent);
	
	/**
	 * Called when the page is shown.  The page is passed install data that
	 * it can use to retrieve known data from other pages.
	 * If a page saves any install data, it should try to load the values here
	 * as they can be set by other pages or in the installer properties.
	 * 
	 * @param data Install data
	 */
	public void setActive(IInstallData data);
	
	/**
	 * Saves the page data to the install data.
	 * 
	 * @param data Install data
	 * @throws CoreException on failure to save page data.  The page will be
	 * set incomplete and the exception information will be shown in the status.
	 */
	public void saveInstallData(IInstallData data) throws CoreException;
	
	/**
	 * Called to validate the page data.
	 * This method will not be called in console mode.
	 * 
	 * @return <code>true</code> if the page data is valid,
	 * <code>false</code> if the page contains invalid data.
	 */
	public boolean validate();
	
	/**
	 * Returns if the page is supported.  A page that is not supported will
	 * be skipped in the install wizard.
	 * 
	 * @return <code>true</code> if page is supported.
	 */
	public boolean isSupported();
}
