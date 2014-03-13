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

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.ui.InstallWizard;
import com.codesourcery.internal.installer.ui.UIUtils;

/**
 * A page that shows summary information from all pages that support
 * {@link IInstallSummaryProvider}.
 * This page supports console.
 */
public class SummaryPage extends InformationPage implements IInstallConsoleProvider {
	/** Wizard pages */
	private IWizardPage[] pages;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param pages Wizard pages
	 */
	public SummaryPage(String pageName, String title, IWizardPage[] pages) {
		super(pageName, title);
		
		// Set information title
		setInformationTitle(InstallMessages.SummaryMessage);
		// Enable scrolling
		setScrollable(true);
		
		this.pages = pages;
	}

	/**
	 * Returns the install summary.
	 * 
	 * @return Install summary
	 */
	private String getInstallSummary() {
		StringBuffer buffer = new StringBuffer();
		
		// Get the wizard pages
		for (IWizardPage page : pages) {
			// If the page provides summary information
			if (page instanceof IInstallSummaryProvider) {
				IInstallSummaryProvider provider = (IInstallSummaryProvider)page;
				buffer.append(provider.getInstallSummary());
			}
		}
		
		return buffer.toString();
	}
	
	/**
	 * Updates the summary.
	 */
	private void updateSummary() {
		checkDiskSpace();
		setInformation(getInstallSummary());
	}

	/**
	 * Warn the user if the free disk space appears to be insufficient 
	 * for the install.
	 */
	private void checkDiskSpace() {
		IWizard wizard = getWizard();
		if (!(wizard instanceof InstallWizard)) {
			return;
		}
		IInstallData data = ((InstallWizard) wizard).getInstallData();
		String installFolder = (String)data.getProperty(IInstallConstants.PROPERTY_INSTALL_FOLDER);
		if (installFolder != null) {
			File installDirectory = new File(installFolder);
			while (!installDirectory.exists()) {
				installDirectory = installDirectory.getParentFile();
			}
			long bytesFree = installDirectory.getUsableSpace();
			
			long bytesRequired = (Long)data.getProperty(IInstallConstants.PROPERTY_INSTALL_SIZE);
			if (bytesRequired >= bytesFree) {
				String msg = MessageFormat.format(
						InstallMessages.SummaryPage_0,
						new Object[] { UIUtils.formatBytes(bytesRequired),
								UIUtils.formatBytes(bytesFree) });

				showStatus(new IStatus[] {
						new Status(IStatus.WARNING, Installer.ID, msg)
				});
			}
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		// Update summary with information from pages
		if (visible) {
			updateSummary();
		}
	}

	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {

		// Replace non-console elements
		String summary = InstallMessages.SummaryConsoleMessage + getInstallSummary();
		summary = summary.replace("<b>", "");
		summary = summary.replace("</b>", "");
		summary = summary.replace("<i>", "");
		summary = summary.replace("</i>", "");
		
		ConsoleYesNoPrompter prompter = new ConsoleYesNoPrompter(summary, InstallMessages.SummaryPage_ProceedPrompt, true);
		String response = prompter.getConsoleResponse(input);
		if (response == null) {
			// Installation cancelled
			if (!prompter.getResult()) {
				throw new IllegalArgumentException(InstallMessages.SummaryPage_InstallationCancelled);
			}
		}
		
		return response;
	}
}
