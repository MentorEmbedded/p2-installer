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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.ui.pages.ProgressPage;

/**
 * Install wizard dialog
 */
public class InstallWizardDialog extends WizardDialog {
	/** Install wizard */
	private InstallWizard installWizard;
	/** Back button state */
	private boolean backButtonState;
	/** Next button state */
	private boolean nextButtonState;
	/** Cancel button state */
	private boolean cancelButtonState;
	/** Finish button state */
	private boolean finishButtonState;
	/** Current wizard page */
	private IInstallWizardPage currentPage;
	
	/**
	 * Constructor
	 * 
	 * @param newWizard Install wizard
	 */
	public InstallWizardDialog(InstallWizard newWizard) {
		super(null, newWizard);
		this.installWizard = newWizard;
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
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		// Rename 'Finish' button to 'Install or 'Uninstall'
		getButton(IDialogConstants.FINISH_ID).setText(getInstallWizard().isInstall() ? 
				InstallMessages.Install : InstallMessages.Uninstall);
	}
	
	@Override
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {
		// Set focust to cancel button
		this.getButton(IDialogConstants.CANCEL_ID).setFocus();
		
		super.run(fork, cancelable, runnable);
	}

	@Override
	public void create() {
		super.create();

		// Set window icon
		getShell().setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.TITLE_ICON));
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
		
		super.finishPressed();
	}

	@Override
	protected void nextPressed() {
		// Validate the install page
		if (!validateCurrentPage()) {
			return;
		}

		super.nextPressed();
	}

	@Override
	public void showPage(IWizardPage page) {
		// Set page active
		if (page instanceof IInstallWizardPage) {
			// Save current page data
			if ((currentPage != null) && !page.equals(currentPage)) {
				currentPage.saveInstallData(getInstallWizard().getInstallData());
			}
			// Set page active
			currentPage = (IInstallWizardPage)page;
			currentPage.setActive(getInstallWizard().getInstallData());
		}
		
		super.showPage(page);

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
		
		// Override the wizard dialog default behavior to set the finish button
		// as default.  Always set the next button as default.
		IWizardPage currentPage = getCurrentPage();
		if ((currentPage != null) && currentPage.canFlipToNextPage()) {
			getShell().setDefaultButton(this.getButton(IDialogConstants.NEXT_ID));
		}
	}

	public void setButtonsEnabled(boolean enable) {
		// Restore enabled state
		if (enable) {
			Button button = getButton(IDialogConstants.BACK_ID);
			if (button != null) {
				button.setEnabled(backButtonState);
			}
			button = getButton(IDialogConstants.NEXT_ID);
			if (button != null) {
				button.setEnabled(nextButtonState);
			}
			button = getButton(IDialogConstants.CANCEL_ID);
			if (button != null) {
				button.setEnabled(cancelButtonState);
			}
			button = getButton(IDialogConstants.FINISH_ID);
			if (button != null) {
				button.setEnabled(finishButtonState);
			}
		}
		// Disable
		else {
			Button button = getButton(IDialogConstants.BACK_ID);
			if (button != null) {
				backButtonState = button.isEnabled();
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.NEXT_ID);
			if (button != null) {
				nextButtonState = button.isEnabled();
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.CANCEL_ID);
			if (button != null) {
				cancelButtonState = button.isEnabled();
				button.setEnabled(false);
			}
			button = getButton(IDialogConstants.FINISH_ID);
			if (button != null) {
				finishButtonState = button.isEnabled();
				button.setEnabled(false);
			}
		}
	}
}
