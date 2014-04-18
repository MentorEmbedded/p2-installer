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
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallContext;
import com.codesourcery.internal.installer.InstallData;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallProduct;
import com.codesourcery.internal.installer.LocationsManager;
import com.codesourcery.internal.installer.ui.pages.InformationPage;
import com.codesourcery.internal.installer.ui.pages.ProductsPage;
import com.codesourcery.internal.installer.ui.pages.ProgressPage;
import com.codesourcery.internal.installer.ui.pages.ResultsPage;
import com.codesourcery.internal.installer.ui.pages.SummaryPage;

/**
 * Install wizard
 * This wizard shows pages that implement <code>IInstallWizardPage</code>.
 */
public class InstallWizard extends Wizard {
	/**
	 * Install context
	 */
	private IInstallContext installContext;
	/**
	 * Summary page
	 */
	private SummaryPage summaryPage;
	/**
	 * Progress page
	 */
	private ProgressPage progressPage;
	/**
	 * Result page
	 */
	private ResultsPage resultsPage;
	/**
	 * <code>true</code> if installation complete
	 */
	private boolean installed = false;
	/**
	 * Products page
	 */
	private ProductsPage productsPage;
	/**
	 * Install data
	 */
	private IInstallData installData;
	/**
	 * Status of install operation.
	 */
	private IStatus status;
	/**
	 * Title image
	 */
	private Image titleImage;

	/**
	 * Constructor
	 * 
	 * @param installContext Install context
	 */
	public InstallWizard(IInstallContext installContext) {
		this.installContext = installContext;
		setNeedsProgressMonitor(true);
		// Set window title
		String title;
		if (isInstall()) {
			title = installContext.getInstallDescription().getWindowTitle(); 
			if (title == null) {
				title = MessageFormat.format(InstallMessages.Title0, installContext.getInstallDescription().getProductName());
			}
		}
		else {
			title = InstallMessages.UninstallTitle;
		}
		setWindowTitle(title);
		
		// Load title image if available
		try {
			IPath titleImagePath = installContext.getInstallDescription().getTitleImage();
			if (titleImagePath != null) {
				if (titleImagePath.toFile().exists()) {
					ImageLoader imageLoader = new ImageLoader();
					ImageData[] imageDatas = imageLoader.load(titleImagePath.toOSString());
					if (imageDatas.length > 0) {
						titleImage = new Image(Display.getDefault(), imageDatas[0]);
					}
				}
				else {
					Installer.log("Missing title image file: " + titleImagePath.toOSString());
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
		
		installData = new InstallData();
	}
	
	/**
	 * Returns the title image specified in the installer properties.
	 * 
	 * @return Title image or <code>null</code>
	 */
	public Image getTitleImage() {
		return titleImage;
	}
	
	/**
	 * Returns the install data.
	 * 
	 * @return Install data
	 */
	public IInstallData getInstallData() {
		return installData;
	}
	
	/**
	 * Returns if the wizard is for
	 * an installation.
	 * 
	 * @return <code>true</code> if install
	 * <code>false</code> if uninstall
	 */
	public boolean isInstall() {
		return getInstallContext().isInstall();
	}

	/**
	 * Returns the install context.
	 * 
	 * @return Install context
	 */
	public IInstallContext getInstallContext() {
		return installContext;
	}
	
	/**
	 * Returns the status of the install operation.
	 * 
	 * @return Status
	 */
	public IStatus getStatus() {
		return status;
	}

	@Override
	public boolean performCancel() {
		// Show cancel confirmation
		if (showCancelConfirmation()) {
			status = new Status(IStatus.CANCEL, Installer.ID, 0, "Cancelled", null);
			// Cleanup any install files
			try {
				if (getInstallContext().isInstall())
					LocationsManager.getDefault().setInstallLocation(null);
			} catch (CoreException e) {
				Installer.log(e);
			}

			return super.performCancel();
		}
		else {
			return false;
		}
	}

	/**
	 * Shows a cancel confirmation dialog.
	 * 
	 * @return <code>true</code> to cancel
	 */
	public boolean showCancelConfirmation() {
		// Confirmation message
		String confirmationMessage;
		if (isInstall()) {
			confirmationMessage = MessageFormat.format(InstallMessages.CancelInstallConfirmation2, new Object[] {
					getInstallContext().getInstallDescription().getProductName(),
					InstallMessages.Resume,
					InstallMessages.Quit
			});
		}
		else {
			confirmationMessage = MessageFormat.format(InstallMessages.CancelUninstallConfirmation1, new Object[] {
					InstallMessages.Resume,
					InstallMessages.Quit
			});
		}
		// Confirm dialog
		MessageDialog confirmationDialog = new MessageDialog(
				getContainer().getShell(),
				InstallMessages.CancelSetupTitle,
				null,
				confirmationMessage,
				MessageDialog.QUESTION,
				new String[] { InstallMessages.Resume, InstallMessages.Quit },
				0
				);

		return (confirmationDialog.open() != 0);
	}

	@Override
	public boolean performFinish() {
		boolean complete = true;
		
		// First time - perform installation/uninstallation
		if (!installed) {
			complete = false;
			
			installed = true;
			
			// Show installing page 
			getContainer().showPage(progressPage);
			
			// Install
			if (isInstall()) {
				// Save page install data
				for (IWizardPage page : getPages()) {
					if (page instanceof IInstallWizardPage) {
						((IInstallWizardPage)page).saveInstallData(getInstallData());
					}
				}
				
				status = install(getInstallData());
			}
			// Uninstall
			else {
				status = uninstall();
			}
			
			// Set result
			if (resultsPage != null) {
				// Success
				if (status.isOK()) {
					String message = getInstallContext().isInstall() ?
							NLS.bind(InstallMessages.InstallationComplete0, getInstallContext().getInstallDescription().getProductName()) :
							InstallMessages.UninstallationComplete;
					resultsPage.setResult(message, true); 
				}
				// Cancel
				else if (status.getSeverity() == IStatus.CANCEL) {
					String message = getInstallContext().isInstall() ?
							NLS.bind(InstallMessages.InstallationCancelled0, getInstallContext().getInstallDescription().getProductName()) :
							InstallMessages.UninstallationCancelled;
					resultsPage.setResult(message, false); 
				}
				// Error
				else {
					resultsPage.setError(getStatusMessage(status));
				}
				
				// Show install complete page
				getContainer().showPage(resultsPage);
			}
		}
		// Second time - installation has
		// been completed
		else {
			// Launch selected product items if no errors at end of installation
			if (isInstall() && (resultsPage != null) && !resultsPage.hasError()) {
				LaunchItem[] launchItems = getInstallContext().getInstallDescription().getLaunchItems();
				if (launchItems != null) {
					for (LaunchItem launchItem : launchItems) {
						try {
							if (resultsPage.isItemChecked(launchItem)) {
								getInstallContext().launch(launchItem);
							}
						}
						catch (Exception e) {
							Installer.log(e);
						}
					}
				}
			}
		}
		
		return complete;
	}

	/**
	 * Returns a status message.
	 * 
	 * @param status Status
	 * @return Message
	 */
	private String getStatusMessage(IStatus status) {
		StringBuilder message = new StringBuilder();
		if (status != null) {
			message.append(status.getMessage());
			IStatus[] children = status.getChildren();
			if (children != null) {
				for (IStatus child : children) {
					message.append('\n');
					message.append(child.getMessage());
				}
			}
			Throwable exception = status.getException();
			if (exception instanceof InvocationTargetException) {
				exception = ((InvocationTargetException)exception).getTargetException();
			}
			
			if (exception != null) {
				message.append('\n');
				message.append(exception.getLocalizedMessage());
			}
		}
		
		return message.toString();
	}
	
	/**
	 * Performs the uninstallation.
	 *
	 * @return Status
	 * @throws CoreException on failure
	 */
	private IStatus uninstall() {
		Throwable error = null;
		final IStatus[] status = new IStatus[] { Status.OK_STATUS };
		
		final InstallProduct[] products = productsPage.getSelectedProducts();

		// Install
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						// Remove products
						getInstallContext().uninstall(products, monitor);
						
						if (monitor.isCanceled())
							status[0] = Status.CANCEL_STATUS;
					}
					catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (Exception e) {
			error = e;
		}
		
		// Install operation failed
		if (error != null) {
			status[0] = new Status(IStatus.ERROR, Installer.ID, 0, InstallMessages.UninstallationFailed, error);
		}
		
		return status[0];
	}

	/**
	 * Performs the installation
	 * 
	 * @param data Install data
	 * @return Status
	 * @throws CoreException on failure
	 */
	private IStatus install(IInstallData data) {
		final IStatus[] status = new IStatus[] { Status.OK_STATUS };
		try {
			// Install
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						getInstallContext().install(installData, monitor);
						
						if (monitor.isCanceled()) {
							status[0] = Status.CANCEL_STATUS;
						}
					} catch (CoreException e) {
						Installer.log(e);
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (Exception e) {
			status[0] = new Status(IStatus.ERROR, Installer.ID, 0, "", e);
		}
		
		return status[0];
	}

	/**
	 * Returns the progress page.
	 * 
	 * @return Progress page
	 */
	public ProgressPage getProgressPage() {
		return progressPage;
	}
	
	/**
	 * Adds install pages.
	 */
	private void addInstallPages() {
		// Add wizard pages
		IInstallWizardPage[] installPages = getInstallContext().getWizardPages();
		for (IInstallWizardPage installPage : installPages) {
			addPage(installPage);
		}
		
		// Summary page
		summaryPage = new SummaryPage("summaryPage", InstallMessages.SummaryPageTitle, getPages());
		addPage(summaryPage);
		
		// Installing page
		String installingMessage = MessageFormat.format(getInstallContext().isUpgrade() ? InstallMessages.UpgradingMessage0 :InstallMessages.InstallingMessage0, 
				new Object[] { getInstallContext().getInstallDescription().getProductName() });
		progressPage = new ProgressPage("progressPage", InstallMessages.InstallingPageTitle, installingMessage);
		addPage(progressPage);
		
		// Results page
		LaunchItem[] launchItems = getInstallContext().getInstallDescription().getLaunchItems();
		
		// There are items to launch at the end of installation
		if ((launchItems != null) && (launchItems.length > 0)) {
			resultsPage = new ResultsPage("resultsPage", InstallMessages.ResultsPageTitle, 
					launchItems);
		}
		// No items to launch at the end of installation
		else {
			resultsPage = new ResultsPage("resultsPage", InstallMessages.ResultsPageTitle);
		}
		addPage(resultsPage);
	}
	
	/**
	 * Adds uninstall pages.
	 */
	private void addUninstallPages() {
		// Welcome page
		InformationPage welcomePage = new InformationPage("welcomePage", InstallMessages.WelcomePageTitle, InstallMessages.uninstallMessage);
		addPage(welcomePage);

		// Products page
		productsPage = new ProductsPage("productsPage", InstallMessages.ProductsPageTitle, getInstallContext().getInstallManifest().getProducts(), InstallMessages.ProductsMessage);
		productsPage.setMessage(InstallMessages.SelectProductsToUninstall);
		addPage(productsPage);
		
		// Installing page
		progressPage = new ProgressPage("progressPage", InstallMessages.Uninstalling, InstallMessages.UninstallingMessage);
		addPage(progressPage);

		// Results page
		resultsPage = new ResultsPage("resultsPage", InstallMessages.ResultsPageTitle);
		addPage(resultsPage);
	}
	
	@Override
	public void addPages() {
		// Install
		if (isInstall()) {
			addInstallPages();
		}
		// Uninstall
		else {
			addUninstallPages();
		}
	}

	/**
	 * Runs a long operation and shows a wizard page as busy.
	 * 
	 * @param wizardPage Wizard page
	 * @param message Busy message
	 * @param runnable Runnable to execute
	 */
	public void runOperation(InstallWizardPage wizardPage, String message, Runnable runnable) {
		// Run job
		OperationJob job = new OperationJob(wizardPage, message, runnable);
		job.schedule();
		
		// Dispatch UI events until job is complete
		Display display = getShell().getDisplay();
		while ((getShell() != null) && !getShell().isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
			if (job.isDone())
				break;
		}
	}

	/**
	 * Job to perform long running operation.
	 */
	private class OperationJob extends Job {
		/** Wizard page */
		private InstallWizardPage installPage;
		/** Operation to perform */
		private Runnable runnable;
		/** Busy message */
		private String message;
		/** <code>true</code> if operation is complete */
		private boolean done = false;
		
		/**
		 * Constructor
		 * 
		 * @param installPage Install wizard page
		 * @param message Busy message
		 * @param runnable Operation to perform
		 */
		public OperationJob(InstallWizardPage installPage, String message, Runnable runnable) {
			super("Page Operation Job");
			this.installPage = installPage;
			this.message = message;
			this.runnable = runnable;
			setSystem(true);
		}

		/**
		 * Returns if the operation is complete.
		 * 
		 * @return <code>true</code> if operation is complete
		 */
		public boolean isDone() {
			return done;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				// Show page busy
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						((InstallWizardDialog)getContainer()).setButtonsEnabled(false);
						installPage.showBusy(message);
					}
				});
				
				// Run operation
				runnable.run();
				done = true;
				
				// Hide page busy
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						installPage.hideBusy();
						((InstallWizardDialog)getContainer()).setButtonsEnabled(true);
					}
				});
			}
			catch (Exception e) {
				Installer.log(e);
			}
			return Status.OK_STATUS;
		}
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		// Page navigation
		IInstallDescription.PageNavigation navigation = IInstallDescription.PageNavigation.NONE;
		if (getInstallContext().isInstall()) {
			navigation = getInstallContext().getInstallDescription().getPageNavigation();
		}
		
		// Set page navigation
		for (IWizardPage page : getPages()) {
			if (page instanceof InstallWizardPage) {
				InstallWizardPage installPage = (InstallWizardPage)page;
				if (navigation == IInstallDescription.PageNavigation.NONE)
					installPage.setPageNavigation(SWT.NONE);
				else if (navigation == IInstallDescription.PageNavigation.TOP)
					installPage.setPageNavigation(SWT.TOP);
				else if (navigation == IInstallDescription.PageNavigation.LEFT)
					installPage.setPageNavigation(SWT.LEFT);
			}
		}
		
		super.createPageControls(pageContainer);
	}
}
