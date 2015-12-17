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
package com.codesourcery.internal.installer.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.widgets.Button;

import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.ui.pages.ProgressPage;

/**
 * Install wizard dialog
 */
public class InstallWizardDialog extends WizardDialog {
	/** Install wizard */
	private InstallWizard installWizard;
	/** <code>true</code> if dialog should bring to front on activation */
	private boolean bringToFront;
	
	/**
	 * Constructor
	 * 
	 * @param newWizard Install wizard
	 * @param brintToFront <code>true</code> if dialog should bring to the front
	 * on activation
	 */
	public InstallWizardDialog(InstallWizard newWizard, boolean bringToFront) {
		super(null, newWizard);
		this.installWizard = newWizard;
		this.bringToFront = bringToFront;
	}
	
	/**
	 * Returns the install wizard.
	 * 
	 * @return Install wizard
	 */
	private InstallWizard getInstallWizard() {
		return installWizard;
	}
	
	@Override
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {
		// Set focus to cancel button
		this.getButton(IDialogConstants.CANCEL_ID).setFocus();
		
		super.run(fork, cancelable, runnable);
	}

	@Override
	public void create() {
		super.create();

		// Set window icon
		getShell().setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.TITLE_ICON));
		// Force activation of dialog
		if (bringToFront) {
			Installer.getDefault().getInstallPlatform().bringShellToFront(getShell());
		}
	}

	/**
	 * Validates the current install page.
	 * 
	 * @return <code>true</code> if the page is valid,
	 * <code>false</code> otherwise
	 */
	private boolean validateCurrentPage() {
		boolean valid = true;
		
		IWizardPage current = getCurrentPage();
		if (current instanceof IInstallWizardPage) {
			IInstallWizardPage installPage = (IInstallWizardPage)current;
			if (!installPage.validate()) {
				valid = false;
			}
		}
		
		return valid;
	}

	@Override
	protected void finishPressed() {
		// Validate current page
		if (!validateCurrentPage())
			return;

		if (saveCurrentPage()) {
			super.finishPressed();
		}
	}

	@Override
	public void nextPressed() {
		// Validate the install page
		if (!validateCurrentPage()) {
			return;
		}
		
		if (saveCurrentPage()) {
			super.nextPressed();
		}
	}
	
	/**
	 * Saves the current page.
	 * 
	 * @return <code>true</code> if save was successful
	 */
	private boolean saveCurrentPage() {
		if (getCurrentPage() instanceof InstallWizardPage) {
			InstallWizardPage wizardPage = (InstallWizardPage)getCurrentPage();
			try {
				wizardPage.saveInstallData(getInstallWizard().getInstallData());

				IWizardPage nextPage = getCurrentPage().getNextPage();
				// If page is last in the setup pages
				if ((wizardPage instanceof ISetupWizardPage) && !(nextPage instanceof ISetupWizardPage)) {
					// Update install button text
					updateButtons();
					
					// Setup page navigation
					((InstallWizard)getWizard()).setupPageNavigation();
				}
			}
			catch (CoreException e) {
				wizardPage.showStatus(new IStatus[] { e.getStatus() });
				wizardPage.setPageComplete(false);
				return false;
			}
		}
		
		return true;
	}

	@Override
	public void showPage(IWizardPage page) {
		// Set new page
		super.showPage(page);
		
		// Set page active
		if (page instanceof IInstallWizardPage) {
			try {
				((IInstallWizardPage)getCurrentPage()).setActive(getInstallWizard().getInstallData());
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}

		IWizardPage[] pages = getInstallWizard().getPages();
		// If final page, update buttons so that only
		// OK is enabled.
		if ((pages.length > 0) && (page == pages[pages.length - 1])) {
			Button button = getButton(IDialogConstants.BACK_ID);
			if (button != null) {
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.NEXT_ID);
			if (button != null) {
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.CANCEL_ID);
			if (button != null) {
				button.setEnabled(false);
			}
			getButton(IDialogConstants.FINISH_ID).setText(IDialogConstants.OK_LABEL);
		}
	}

	/**
	 * Returns the page progress monitor part to
	 * use for long operations.
	 * 
	 * @return Progress monitor part
	 */
	protected ProgressMonitorPart getPageProgressMonitor() {
		ProgressPage installPage = getInstallWizard().getProgressPage();
		return installPage.getProgressMonitorPart();
	}

	@Override
	public void updateButtons() {
		super.updateButtons();
		
		// Rename 'Finish' button
		getButton(IDialogConstants.FINISH_ID).setText("&" + getInstallWizard().getFinishText()); 

		// Override the wizard dialog default behavior to set the finish button
		// as default.  Always set the next button as default.
		IWizardPage currentPage = getCurrentPage();
		if ((currentPage != null) && currentPage.canFlipToNextPage()) {
			getShell().setDefaultButton(this.getButton(IDialogConstants.NEXT_ID));
		}
	}

	/**
	 * Sets wizard buttons enabled or disabled.
	 * 
	 * @param enable <code>true</code> to enable,
	 * <code>false</code> to disable
	 */
	public void setButtonsEnabled(boolean enable) {
		// Restore enabled state
		if (enable) {
			Button button = getButton(IDialogConstants.CANCEL_ID);
			if (button != null) {
				button.setEnabled(true);
			}
			updateButtons();
		}
		// Disable
		else {
			Button button = getButton(IDialogConstants.BACK_ID);
			if (button != null) {
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.NEXT_ID);
			if (button != null) {
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.CANCEL_ID);
			if (button != null) {
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.FINISH_ID);
			if (button != null) {
				button.setEnabled(false);
			}
		}
	}
}
