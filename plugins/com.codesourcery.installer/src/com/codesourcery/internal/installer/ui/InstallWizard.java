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
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallManager;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.InstallMessages;
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
	 * Status of install operation.
	 */
	private IStatus status;
	/**
	 * Title image
	 */
	private Image titleImage;
	/**
	 * Products that were uninstalled or <code>null</code>.
	 */
	private IInstallProduct[] uninstalledProducts;

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
	 * @return The text used for finish.
	 */
	public String getFinishText() {
		IInstallMode mode = getInstallManager().getInstallMode();
		
		if (mode.isInstall()) {
			IInstallDescription desc = getInstallManager().getInstallDescription();
			return mode.isMirror() ? 
					desc.getText(IInstallDescription.TEXT_FINISH_MIRROR, InstallMessages.Save) :
					desc.getText(IInstallDescription.TEXT_FINISH_INSTALL, InstallMessages.Install);
		}
		else {
			return InstallMessages.Uninstall;
		}
	}
	
	/**
	 * Returns the install data.
	 * 
	 * @return Install data
	 */
	public IInstallData getInstallData() {
		return Installer.getDefault().getInstallManager().getInstallData();
	}
	
	/**
	 * Returns if the wizard is for
	 * an installation.
	 * 
	 * @return <code>true</code> if install
	 * <code>false</code> if uninstall
	 */
	public boolean isInstall() {
		return getInstallManager().getInstallMode().isInstall();
	}

	/**
	 * Returns if the wizard is for a mirror.
	 * 
	 * @return <code>true</code> if mirror
	 */
	public boolean isMirror() {
		return getInstallManager().getInstallMode().isMirror();
	}
	
	/**
	 * Returns if the wizard is for an upgrade.
	 * @return <code>true</code> if upgrade
	 */
	public boolean isUpgrade() {
		return getInstallManager().getInstallMode().isUpgrade();
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
		boolean confirm = true;
		
		// If not showing Results page, confirm the cancel
		if (getContainer().getCurrentPage() != resultsPage) {
			confirm = showCancelConfirmation();
		}
		
		// Show cancel confirmation
		if (confirm) {
			status = new Status(IStatus.CANCEL, Installer.ID, 0, "Cancelled", null);
			// Cleanup any install files
			try {
				if (isInstall())
					Installer.getDefault().getInstallManager().setInstallLocation(null, null);
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
			// Install
			if (isInstall()) {
				// Save page install data
				for (IInstallWizardPage page : getSupportedPages()) {
					try {
						page.saveInstallData(getInstallData());
					}
					catch (CoreException e) {
						InstallWizardPage wizardPage = (InstallWizardPage)page;
						// If page has error, show it and return
						wizardPage.showStatus(new IStatus[] { e.getStatus() });
						wizardPage.setPageComplete(false);
						getContainer().showPage(page);
						return false;
					}
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

				status = install();
			}
			// Uninstall
			else {
				// Show uninstalling page 
				getContainer().showPage(progressPage);

				status = uninstall();
			}
			
			complete = false;
			installed = true;
			
			// Set result
			if (resultsPage != null) {
				// Show results page launch options
				boolean showOptions = false;
				
				// Success
				if (status.isOK()) {
					String message;
					// Install message
					if (isInstall()) {
						IInstallDescription desc = Installer.getDefault().getInstallManager().getInstallDescription();
						
						StringBuilder buffer = new StringBuilder();
						// Mirror operation
						if (getInstallManager().getInstallMode().isMirror()) {
							String resultMessage = desc.getText(IInstallDescription.TEXT_RESULT_MIRROR, 
									NLS.bind(InstallMessages.MirrorComplete0, getInstallManager().getInstallDescription().getProductName()));
							buffer.append(resultMessage);
						}
						// Install
						else {
							String resultMessage = desc.getText(IInstallDescription.TEXT_RESULT_INSTALL,
									NLS.bind(InstallMessages.InstallationComplete0, getInstallManager().getInstallDescription().getProductName()));
							buffer.append(resultMessage);
							String installText = getInstallManager().getInstallDescription().getText(IInstallDescription.TEXT_INSTALL_ADDENDUM, null);
							if (installText != null) {
								buffer.append("\n\n");
								buffer.append(installText);
							}
						}
						buffer.append("\n\n");
						buffer.append(InstallMessages.ClickClose);
						message = buffer.toString();
						// Show options if not mirror operation
						showOptions = !getInstallManager().getInstallMode().isMirror();
					}
					// Uninstall message
					else {
						StringBuilder buffer = new StringBuilder(InstallMessages.UninstallationComplete);

						IInstallProduct[] uninstalledProducts = getUninstalledProducts();
						if (uninstalledProducts != null) {
							buffer.append("\n\n");

							// Single product that specifies uninstall text
							if (uninstalledProducts.length == 1) {
								String uninstallText = uninstalledProducts[0].getProperty(IInstallProduct.PROPERTY_UNINSTALL_TEXT);
								if (uninstallText != null) {
									buffer.append(uninstallText);
								}
							}
							// Multiple products that specify uninstall text
							else {
								for (IInstallProduct uninstalledProduct : uninstalledProducts) {
									String uninstallText = uninstalledProduct.getProperty(IInstallProduct.PROPERTY_UNINSTALL_TEXT);
									if (uninstallText != null) {
										buffer.append("<b>" + uninstalledProduct.getName() + "</b>\n");
										buffer.append(uninstallText);
										buffer.append("\n\n");
									}
								}
							}
						}
						
						message = buffer.toString();
						showOptions = false;
					}
					resultsPage.setResult(message, showOptions); 
				}
				// Cancel
				else if (status.getSeverity() == IStatus.CANCEL) {
					String message;
					if (isInstall()) {
						message = Installer.getDefault().getInstallManager().getInstallMode().isUpdate() ?
								NLS.bind(InstallMessages.UpdateCancelled0, getInstallManager().getInstallDescription().getProductName()) :
								NLS.bind(InstallMessages.InstallationCancelled0, getInstallManager().getInstallDescription().getProductName());
									
					}
					else {
						message = InstallMessages.UninstallationCancelled;
					}
							
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

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						// Remove products
						getInstallManager().uninstall(products, monitor);
						
						if (monitor.isCanceled())
							status[0] = Status.CANCEL_STATUS;
						
						uninstalledProducts = products;
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
	 * @return Status
	 * @throws CoreException on failure
	 */
	private IStatus install() {
		final IStatus[] status = new IStatus[] { Status.OK_STATUS };
		try {
			// Install
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						getInstallManager().install(monitor);
						
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

		// Initialize page labels
		for (IWizardPage page : pages) {
			if (page instanceof InstallWizardPage) {
				InstallWizardPage wizardPage = (InstallWizardPage)page;
				wizardPage.initPageLabel();
			}
		}
		
		// Setup page navigation
		for (IWizardPage page : pages) {
			if (page instanceof InstallWizardPage) {
				InstallWizardPage wizardPage = (InstallWizardPage)page;
				wizardPage.initPageLabel();
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
		if (!setupPageAvailable()) {
			setupPageNavigation();
		}
	}

	/**
	 * @return Returns if a setup page is available.
	 */
	private boolean setupPageAvailable() {
		boolean available = false;
		IWizardPage[] pages = getPages();
		for (IWizardPage page : pages) {
			if (page instanceof ISetupWizardPage) {
				IInstallWizardPage installPage = (IInstallWizardPage)page;
				if (installPage.isSupported()) {
					available = true;
					break;
				}
			}
		}
		return available;
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

	/**
	 * Returns the products that were uninstalled.
	 * 
	 * @return Products that were uninstalled or <code>null</code> if the uninstall operation has not been performed
	 * yet or the operation performed was an installation.
	 */
	private IInstallProduct[] getUninstalledProducts() {
		return uninstalledProducts;
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
    public IWizardPage getPreviousPage(IWizardPage page) {
		IWizardPage previousPage = super.getPreviousPage(page);
		// Skip pages that are not supported in current mode
		while ((previousPage != null) && !isPageSupported(previousPage)) {
			previousPage = super.getPreviousPage(previousPage);
		}

		return previousPage;
    }

    @Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage nextPage = super.getNextPage(page);
		// Skip pages that are not supported in current mode
		while ((nextPage != null) && !isPageSupported(nextPage)) {
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
		internalRunOperation(wizardPage, message, runnable, 0);
	}

	/**
	 * Runs a long operation and shows a wizard page as busy if the operation
	 * takes longer than the specified time.
	 * 
	 * @param wizardPage Wizard page
	 * @param message Busy message
	 * @param runnable Runnable to execute
	 * @param delay Delay (ms) before showing busy
	 */
	public void runOperation(InstallWizardPage wizardPage, String message, Runnable runnable, int delay) {
		internalRunOperation(wizardPage, message, runnable, delay);
	}

	/**
	 * Runs a long operation and shows a wizard page as busy if the operation
	 * takes more than the specified time.
	 * 
	 * @param wizardPage Wizard page
	 * @param message Busy message
	 * @param runnable Runnable to execute
	 * @param delay Delay before busy is shown
	 */
	private void internalRunOperation(InstallWizardPage wizardPage, String message, Runnable runnable, int delay) {
		// Run job
		OperationJob job = new OperationJob(wizardPage, message, runnable, delay);
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
		/** Delay before showing busy animation */
		private int delay;
		
		/**
		 * Constructor
		 * 
		 * @param installPage Install wizard page
		 * @param message Busy message
		 * @param runnable Operation to perform
		 * @param delay Delay before showing busy animation (ms) or <code>0</code>
		 * to display immediately.
		 */
		public OperationJob(InstallWizardPage installPage, String message, Runnable runnable, int delay) {
			super("Page Operation Job");
			this.installPage = installPage;
			this.message = message;
			this.runnable = runnable;
			this.delay = delay;
			setSystem(true);
		}
		

		/**
		 * Returns the delay before the busy animation will be displayed.
		 * 
		 * @return Delay before busy animation is displayed (ms)
		 */
		public int getDelay() {
			return delay;
		}

		/**
		 * Returns if the operation is complete.
		 * 
		 * @return <code>true</code> if operation is complete
		 */
		public boolean isDone() {
			return done;
		}
		
		/**
		 * Sets the operation complete.
		 */
		private void setDone() {
			done = true;
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
				if (getDelay() == 0) {
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
				else {
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
					}, getDelay());
				}
				
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						getShell().update();
					}
				});
				
				// Run operation
				try {
					runnable.run();
				}
				catch (Exception e) {
					Installer.log(e);
				}
				busyTimer.cancel();
				
				// Hide page busy
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						installPage.hideBusy();
						((InstallWizardDialog)getContainer()).setButtonsEnabled(true);
						// Update wizard to allow outstanding paints to occur.  This is needed when the busy operation
						// is during a page transition to avoid unwanted artifacts.
						getShell().update();
					}
				});
			}
			catch (Exception e) {
				Installer.log(e);
			}
			finally {
				setDone();
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
		
		return finish;
	}
}
