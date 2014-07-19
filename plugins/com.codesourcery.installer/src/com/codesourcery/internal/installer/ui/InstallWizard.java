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
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallManager;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.InstallData;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.RepositoryManager;
import com.codesourcery.internal.installer.ui.pages.ProductsPage;
import com.codesourcery.internal.installer.ui.pages.ProgressPage;
import com.codesourcery.internal.installer.ui.pages.ResultsPage;
import com.codesourcery.internal.installer.ui.pages.AbstractSetupPage;
import com.codesourcery.internal.installer.ui.pages.SummaryPage;

/**
 * Install wizard
 * This wizard shows pages that implement <code>IInstallWizardPage</code>.
 */
public class InstallWizard extends Wizard {
	/**
	 * Delay before displaying busy animation for long operations (ms)
	 */
	private final static int BUSY_DELAY = 2000;
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
	 */
	public InstallWizard() {
		setNeedsProgressMonitor(true);
		// Set window title
		String title;
		if (isInstall()) {
			title = getInstallManager().getInstallDescription().getWindowTitle(); 
			if (title == null) {
				title = MessageFormat.format(InstallMessages.Title0, getInstallManager().getInstallDescription().getProductName());
			}
		}
		else {
			title = InstallMessages.UninstallTitle;
		}
		setWindowTitle(title);
		
		// Load title image if available
		try {
			if (isInstall()) {
				IPath titleImagePath = getInstallManager().getInstallDescription().getTitleImage();
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
		}
		catch (Exception e) {
			Installer.log(e);
		}
		
		installData = new InstallData();
	}
	
	/**
	 * Returns the install manager.
	 * 
	 * @return Install manager
	 */
	protected IInstallManager getInstallManager() {
		return Installer.getDefault().getInstallManager();
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
		return Installer.getDefault().getInstallManager().getInstallMode().isInstall();
	}

	/**
	 * Returns if the wizard is for an upgrade.
	 * @return <code>true</code> if upgrade
	 */
	public boolean isUpgrade() {
		return Installer.getDefault().getInstallManager().getInstallMode().isUpgrade();
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
				if (isInstall())
					Installer.getDefault().getInstallManager().setInstallLocation(null);
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
					Installer.getDefault().getInstallManager().getInstallDescription().getProductName(),
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
			
			
			// Install
			if (isInstall()) {
				// Save page install data
				for (IInstallWizardPage page : getSupportedPages()) {
					page.saveInstallData(getInstallData());
				}
				
				// Show the summary page and validate the installation
				if (summaryPage != null) {
					getContainer().showPage(summaryPage);
					// Installation not valid, don't proceed
					if (!summaryPage.validate()) {
						return false;
					}
				}
				
				// Show installing page 
				getContainer().showPage(progressPage);

				status = install(getInstallData());
			}
			// Uninstall
			else {
				// Show uninstalling page 
				getContainer().showPage(progressPage);

				status = uninstall();
			}
			
			installed = true;
			
			// Set result
			if (resultsPage != null) {
				// Success
				if (status.isOK()) {
					String message = isInstall() ?
							NLS.bind(InstallMessages.InstallationComplete0, getInstallManager().getInstallDescription().getProductName()) :
							InstallMessages.UninstallationComplete;
					resultsPage.setResult(message, true); 
				}
				// Cancel
				else if (status.getSeverity() == IStatus.CANCEL) {
					String message = isInstall() ?
							NLS.bind(InstallMessages.InstallationCancelled0, getInstallManager().getInstallDescription().getProductName()) :
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
				LaunchItem[] launchItems = getInstallManager().getInstallDescription().getLaunchItems();
				if (launchItems != null) {
					for (LaunchItem launchItem : launchItems) {
						try {
							if (resultsPage.isItemChecked(launchItem)) {
								getInstallManager().launch(launchItem);
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
		
		final IInstallProduct[] products = productsPage.getSelectedProducts();

		// Install
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						// Remove products
						getInstallManager().uninstall(products, monitor);
						
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
						getInstallManager().install(installData, monitor);
						
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
	 * Sets up page navigation.
	 */
	public void setupPageNavigation() {
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			if (page instanceof InstallWizardPage) {
				InstallWizardPage wizardPage = (InstallWizardPage)page;
				wizardPage.setupNavigation();
			}
		}
	}
	
	@Override
	public void addPages() {
		// Add install wizard pages from modules
		IInstallWizardPage[] installPages = getInstallManager().getWizardPages();
		for (IInstallWizardPage installPage : installPages) {
			addPage(installPage);
		}
		
		// Install summary page
		summaryPage = new SummaryPage("summaryPage", InstallMessages.SummaryPageTitle);
		addPage(summaryPage);

		// Uninstall products page
		productsPage = new ProductsPage("productsPage", InstallMessages.ProductsPageTitle, InstallMessages.ProductsMessage);
		productsPage.setMessage(InstallMessages.SelectProductsToUninstall);
		addPage(productsPage);
		
		// Progress page
		progressPage = new ProgressPage("progressPage");
		addPage(progressPage);
		
		// Results page
		resultsPage = new ResultsPage("resultsPage", InstallMessages.ResultsPageTitle);
		addPage(resultsPage);
	}

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		// Initialize the wizard pages navigation unless the Setup page is
		// present as it will do it.
		if ((getPageCount() > 0) && !(getPages()[0] instanceof AbstractSetupPage)) {
			setupPageNavigation();
		}
	}

	/**
	 * Returns is a page is currently supported.
	 * 
	 * @param page Page
	 * @return <code>true</code> if page is supported
	 */
	private boolean isPageSupported(IWizardPage page) {
		boolean supported = true;
		if (page instanceof IInstallWizardPage) {
			supported = ((IInstallWizardPage)page).isSupported();
		}
		
		return supported;
	}

	@Override
    public IWizardPage getStartingPage() {
		IWizardPage startPage = null;
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			if (isPageSupported(page)) {
				startPage = page;
				break;
			}
		}
		if (startPage instanceof IInstallWizardPage) {
			((IInstallWizardPage)startPage).setActive(getInstallData());
		}
		
		return startPage;
    }
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage nextPage = super.getNextPage(page);
		// If page is not supported, get next supported page
		while (!isPageSupported(nextPage)) {
			nextPage = super.getNextPage(nextPage);
		}
		
		return nextPage;
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
				// Disable buttons
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						((InstallWizardDialog)getContainer()).setButtonsEnabled(false);
					}
				});
				// Show busy animation if operation takes too long
				Timer busyTimer = new Timer();
				busyTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						// Show page busy
						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								if (!installPage.isBusy()) {
									installPage.showBusy(message);
								}
							}
						});
					}
				}, BUSY_DELAY);
				
				// Run operation
				runnable.run();
				busyTimer.cancel();
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

	/**
	 * Returns all wizard pages that are currently supported.
	 * 
	 * @return Supported wizard pages
	 */
	public IInstallWizardPage[] getSupportedPages() {
		ArrayList<IInstallWizardPage> supportedPages = new ArrayList<IInstallWizardPage>();
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			if (page instanceof IInstallWizardPage) {
				IInstallWizardPage wizardPage = (IInstallWizardPage)page;
				if (wizardPage.isSupported()) {
					supportedPages.add(wizardPage);
				}
			}
		}
		
		return supportedPages.toArray(new IInstallWizardPage[supportedPages.size()]);
	}
	
	@Override
	public boolean canFinish() {
		// Check if all pages report can finish
		boolean finish = true;
		for (IInstallWizardPage page : getSupportedPages()) {
			if (!page.isPageComplete()) {
				finish = false;
				break;
			}
		}
		
		if (finish && isInstall()) {
			// Allow install only if there is an install plan
			finish = RepositoryManager.getDefault().hasInstallUnits();
		}
		
		return finish;
	}
}
