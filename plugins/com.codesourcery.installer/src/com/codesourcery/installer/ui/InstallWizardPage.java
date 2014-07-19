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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.ContributorRegistry;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.ui.BusyAnimationControl;
import com.codesourcery.internal.installer.ui.InstallWizard;
import com.codesourcery.internal.installer.ui.SideBarComposite;
import com.codesourcery.internal.installer.ui.StepsControl;
import com.codesourcery.internal.installer.ui.pages.ProgressPage;
import com.codesourcery.internal.installer.ui.pages.AbstractSetupPage;

/**
 * Install wizard base page class.  All install wizard pages should subclass
 * this class.
 * Subclasses should provide an implementation of {@link #createContents(Composite)}.
 * A page can show a warning or error, a page can call {@link #showStatus(IStatus[])}.
 * A page can show a busy animation by calling {@link #showBusy(String)}.
 */
public abstract class InstallWizardPage extends WizardPage implements IInstallWizardPage {
	/** Default width */
	protected static final int DEFAULT_WIDTH = 120;
	/** Default height */
	protected static final int DEFAULT_HEIGHT = 23;
	/** Options indenting */
	protected static final int DEFAULT_INDENT = 10;
	/** Page side bar */
	private SideBarComposite sideBar;
	/** Page top bar */
	private StepsControl topBar;
	/** Page label */
	private String pageLabel;
	/** Page area */
	private Composite pageArea;
	/** Page client area */
	private Composite pageClientArea;
	/** Warning panel */
	private WarningPanel warningPanel;
	/** Waiting progress panel */
	private BusyPanel busyPanel;
	/** Status */
	private IStatus[] status;
	/** Bold font */
	private Font boldFont;
	/** Busy message or <code>null</code> if not busy */
	private String busyMessage = null;
	/** Page navigation */
	private int pageNavigation = SWT.NONE;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 */
	protected InstallWizardPage(String pageName, String title) {
		super(pageName, "", null); //$NON-NLS-1$
		setPageLabel(title);
		
		// Set default page navigation
		int navigation = SWT.NONE;
		// For install default to mode specified in installer properties
		if (Installer.getDefault().getInstallManager().getInstallMode().isInstall()) {
			navigation = Installer.getDefault().getInstallManager().getInstallDescription().getPageNavigation();
		}
		// For uninstall, default to top navigation
		else {
			navigation = SWT.TOP;
		}
		setPageNavigation(navigation);
	}
	
	/**
	 * Sets the page navigation.  This can be one of the following:
	 * <ul>
	 * <li>SWT.NONE	- No page navigation</li>
	 * <li>SWT.TOP	- Page navigation at the top</li>
	 * <li>SWT.LEFT	- Page navigation at the left</li>
	 * <ul>
	 * 
	 * @param pageNavigation Page navigation
	 */
	public void setPageNavigation(int pageNavigation) {
		this.pageNavigation = pageNavigation;
	}
	
	/**
	 * Returns the page navigation.
	 * 
	 * @return Page navigation
	 */
	public int getPageNavigation() {
		return pageNavigation;
	}
	
	@Override
	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		
		super.dispose();
	}

	/**
	 * Returns a bold font.
	 * 
	 * @return Bold font
	 */
	protected Font getBoldFont() {
		if (boldFont == null) {
			FontData[] fd = getFont().getFontData();
			fd[0].setStyle(SWT.BOLD);
			boldFont = new Font(getShell().getDisplay(), fd[0]);
		}
		
		return boldFont;
	}

	/**
	 * Returns the page label.
	 * 
	 * @return Page label
	 */
	public String getPageLabel() {
		return pageLabel;
	}
	
	/**
	 * Sets the page label.
	 * 
	 * @param pageLabel Page label
	 */
	public void setPageLabel(String pageLabel) {
		this.pageLabel = pageLabel;
	}
	
	@Override
	public void setActive(IInstallData data) {
		// If top bar, update current page so scrolling is adjusted
		if (topBar != null) {
			topBar.setCurrentStep(getPageLabel());
		}
	}

	/**
	 * Shows success for this page in the side bar.
	 * 
	 * @param success <code>true</code> to show success.
	 */
	protected void showSuccess(boolean success) {
		if (sideBar != null) {
			sideBar.setState(getPageLabel(), success ? 
					SideBarComposite.StepState.SUCCESS : SideBarComposite.StepState.NONE);
		}
	}
	
	/**
	 * Returns the default indention.
	 * 
	 * @return Indention
	 */
	public int getDefaultIndent() {
		return DEFAULT_INDENT;
	}
	
	@Override
	public String getTitle() {
		if (Installer.getDefault().getInstallManager().getInstallMode().isInstall()) {
			return Installer.getDefault().getInstallManager().getInstallDescription().getTitle();
		}
		return super.getTitle();
	}
	
	@Override
	public Image getImage() {
		Image image = null;
		
		// Use wizard title image if available
		try {
			image = ((InstallWizard)getWizard()).getTitleImage();
		}
		catch (Exception e) {
			Installer.log(e);
		}
		
		// Otherwise, use registered title image
		if (image == null) {
			image = ContributorRegistry.getDefault().getTitleIcon();
			
			if (image != null) {
				return image;
			}
			else {
				return Installer.getDefault().getImageRegistry().get(IInstallerImages.PAGE_BANNER);
			}
		}
		
		return image;
	}
	
	@Override
	public final void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite area = new Composite(parent, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return new Point(convertWidthInCharsToPixels(DEFAULT_WIDTH), convertHeightInCharsToPixels(DEFAULT_HEIGHT));
			}
		};
		GridData areaData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		area.setLayoutData(areaData);
		GridLayout layout = new GridLayout(getPageNavigation() == SWT.LEFT ? 3 : 2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		area.setLayout(layout);

		// Top navigation bar
		if (getPageNavigation() == SWT.TOP) { 
			topBar = new StepsControl(area, SWT.NONE);
			topBar.setFont(getFont());
			topBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		}
		// Left navigation bar
		else if (getPageNavigation() == SWT.LEFT) { 
			sideBar = new SideBarComposite(area, SWT.NONE);
			sideBar.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, true, 1, 1));
			// Vertical separator
			Label verticalSeparator = new Label(area, SWT.SEPARATOR | SWT.VERTICAL);
			verticalSeparator.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, true, 1, 1));
		}

		// Page area
		pageArea = new Composite(area, SWT.NONE);
		pageArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		pageArea.setLayout(layout);
		
		// Page client area
		pageClientArea = new Composite(pageArea, SWT.NONE);
		pageClientArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		pageClientArea.setLayout(layout);
		
		// Create page contents
		createContents(pageClientArea);

		// Warning pane (initially hidden unless a status is available)
		warningPanel = new WarningPanel(pageArea, SWT.BORDER);
		GridData warningPanelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		warningPanelData.exclude = (status == null);
		warningPanel.setLayoutData(warningPanelData);
		if (status != null) {
			warningPanel.setStatus(status);
		}
		
		// Waiting pane (initially hidden)
		busyPanel = new BusyPanel(pageArea, SWT.NONE);
		GridData busyPanelData = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);
		busyPanelData.exclude = true;
		busyPanel.setLayoutData(busyPanelData);
		if (busyMessage != null) {
			showBusy(busyMessage);
		}
		
		// Set page control
		setControl(area);
	}
	
	/**
	 * Returns the page container.
	 * 
	 * @return Page container
	 */
	protected Composite getPageContainer() {
		return pageArea;
	}
	
	/**
	 * Returns the page area.
	 * 
	 * @return Page area
	 */
	protected Composite getPageArea() {
		return pageClientArea;
	}

	/**
	 * Sets the current status for the page.
	 * 
	 * @param status Status
	 */
	protected void setStatus(IStatus[] status) {
		this.status = status;
	}
	
	/**
	 * Returns the current status for the page.
	 *  
	 * @return Status or <code>null</code>
	 */
	protected IStatus[] getStatus() {
		return status;
	}
	
	/**
	 * Returns if the page status has errors.
	 * 
	 * @return <code>true</code> if status has errors
	 */
	protected boolean hasStatusError() {
		boolean isError = false;
		if (status != null) {
			for (IStatus stat : status) {
				if (stat.getSeverity() == IStatus.ERROR) {
					isError = true;
					break;
				}
			}
		}
		
		return isError;
	}
	
	/**
	 * Returns the current page status as a console message.
	 * 
	 * @param status Status for message
	 * @return Console message
	 */
	protected String getStatusMessage(IStatus[] status) {
		String message = null;
		if (status != null) {
			StringBuilder buffer = new StringBuilder();
			for (IStatus stat : status) {
				// Information
				if (stat.getSeverity() == IStatus.INFO) {
					buffer.append(stat.getMessage());
				}
				// Error
				else if (stat.getSeverity() == IStatus.ERROR) {
					buffer.append(NLS.bind(InstallMessages.ConsoleError0, stat.getMessage()));
				}
				// Warning
				else if (stat.getSeverity() == IStatus.WARNING) {
					buffer.append(NLS.bind(InstallMessages.ConsoleWarning0, stat.getMessage()));
				}
				buffer.append('\n');
			}
			message = buffer.toString();
		}
		
		return message;
	}
	
	/**
	 * Shows a status pane at the button of the page.
	 * 
	 * @param status Status to show
	 */
	protected void showStatus(IStatus[] status) {
		setStatus(status);
		
		if (warningPanel != null) {
			// Update status
			warningPanel.setStatus(status);
			// Show pane
			GridData data = (GridData)warningPanel.getLayoutData();
			data.exclude = false;
			getPageContainer().layout(true);

			if (sideBar != null) {
				sideBar.setState(getPageLabel(), hasStatusError() ? 
						SideBarComposite.StepState.ERROR : SideBarComposite.StepState.NONE);
			}
		}
	}
	
	/**
	 * Hides the status pane.
	 */
	protected void hideStatus() {
		setStatus(null);
		
		if (warningPanel != null) {
			// Clear the status
			warningPanel.clearStatus();
			// Hide the status panel
			GridData data = (GridData)warningPanel.getLayoutData();
			data.exclude = true;
			warningPanel.setSize(0, 0);
			getPageContainer().layout(true);

			if (sideBar != null) {
				sideBar.setState(getPageLabel(), SideBarComposite.StepState.NONE);
			}
		}
	}

	/**
	 * Hides the page client area and shows a busy message.
	 * 
	 * @param message Message to display
	 */
	public void showBusy(String message) {
		GridData data;
		this.busyMessage = message;

		if (busyPanel != null) {
			// Hide the page contents
			data = (GridData)getPageArea().getLayoutData();
			data.exclude = true;
			getPageArea().setSize(0, 0);
			// Show the busy panel
			busyPanel.startBusy(message);
			data = (GridData)busyPanel.getLayoutData();
			data.exclude = false;
			
			getPageContainer().layout(true);
		}
	}
	
	/**
	 * Hides the busy message and shows he page client area.
	 * If the page is not busy, this method does nothing.
	 */
	public void hideBusy() {
		GridData data;

		if (busyMessage != null) {
			this.busyMessage = null;
			
			// Show the page contents
			data = (GridData)getPageArea().getLayoutData();
			data.exclude = false;
			// Hide the status panel
			busyPanel.stopBusy();
			data = (GridData)busyPanel.getLayoutData();
			data.exclude = true;
			busyPanel.setSize(0, 0);
			
			getPageContainer().layout(true);
		}
	}

	/**
	 * Returns if the page is running in console mode.
	 * 
	 * @return <code>true</code> if console mode, <code>false</code> if GUI mode.
	 */
	public boolean isConsoleMode() {
		return (getShell() == null);
	}
	
	/**
	 * Returns if the page is currently busy.
	 * 
	 * @return <code>true</code> if page is busy.
	 */
	public boolean isBusy() {
		return (busyMessage != null);
	}
	
	/**
	 * Initializes the page navigation steps.
	 */
	public void setupNavigation() {
		IWizardPage[] pages = getWizard().getPages();
		for (IWizardPage page : pages) {
			// Skip any setup page
			if (page instanceof AbstractSetupPage)
				continue;
			
			InstallWizardPage installPage = (InstallWizardPage)page;
			// Page is supported
			if (installPage.isSupported()) {
				if (sideBar != null)
					sideBar.addStep(installPage.getPageLabel());
				if (topBar != null)
					topBar.addStep(installPage.getPageLabel());
			}
		}
		if (sideBar != null) {
			sideBar.layout(true);
			sideBar.getParent().layout(true);
			sideBar.setCurrentStep(getPageLabel());
		}
		if (topBar != null) {
			topBar.layout(true);
			topBar.getParent().layout(true);
			topBar.setCurrentStep(getPageLabel());
		}
	}

	/**
	 * Called to create the page contents.
	 * Subclasses should override.
	 * 
	 * @param parent Parent
	 */
	public abstract Control createContents(Composite parent);

	@Override
	public IWizardPage getPreviousPage() {
		IWizardPage previousPage = super.getPreviousPage();
		// Don't allow moving back to a setup page if present
		if (previousPage instanceof AbstractSetupPage) {
			return null;
		}
		else {
			return previousPage;
		}
	}

	@Override
	public boolean canFlipToNextPage() {
		IWizardPage nextPage = getWizard().getNextPage(this);
		// The installing page is only shown when
		// the install button is selected.
		if (nextPage instanceof ProgressPage) {
			return false;
		}
		else {
			return super.canFlipToNextPage();
		}
	}
	
	@Override
	public void saveInstallData(IInstallData data) {
	}

	@Override
	public void setErrorMessage(String newMessage) {
		super.setErrorMessage(newMessage);
		
		// Set sidebar state
		if (sideBar != null) {
			sideBar.setState(getPageLabel(), newMessage == null ? 
					SideBarComposite.StepState.NONE : SideBarComposite.StepState.ERROR);
		}
	}
	
	@Override
	public boolean validate() {
		return true;
	}
	
	@Override
	public void setDescription(String description) {
		throw new IllegalArgumentException("Description is not allowed for installer pages.");
	}

	/**
	 * Performs a long running operation.  A waiting animation is displayed
	 * instead of the page contents until the operation is complete.
	 * 
	 * @param message Waiting message to display
	 * @param runnable Operation to run
	 */
	protected void runOperation(String message, Runnable runnable) {
		InstallWizard wizard = (InstallWizard)getWizard();
		wizard.runOperation(this, message, runnable);
	}
	
	/**
	 * Removes any formatting from text.
	 * 
	 * @param text Formatted text
	 * @return Text with formatting removed
	 */
	protected String removeFormatting(String text) {
		text = text.replace("<b>", "");
		text = text.replace("</b>", "");
		text = text.replace("<i>", "");
		text = text.replace("</i>", "");
		
		return text;
	}

	@Override
	public boolean isSupported() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		
		// By default, pages are supported in a new install only
		return (mode.isInstall() && !mode.isUpdate() && !mode.isPatch());
	}

	/**
	 * Composite to show status.
	 */
	private class WarningPanel extends Composite {
		/**
		 * Constructor
		 * 
		 * @param parent Parent
		 * @param style Style
		 */
		public WarningPanel(Composite parent, int style) {
			super(parent, style);
			setLayout(new GridLayout(2, false));
		}

		/**
		 * Sets the panel status.
		 * 
		 * @param statuses Status to show
		 */
		public void setStatus(IStatus[] statuses) {
			clearStatus();
			
			for (IStatus status : statuses) {
				Label iconLabel = new Label(this, SWT.NONE);
				iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false, 1, 1));
				if (status.getSeverity() == IStatus.WARNING) {
					iconLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.WARNING));
				}
				else if (status.getSeverity() == IStatus.ERROR) {
					iconLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.ERROR));
				}
				else {
					iconLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.INFO));
				}
				StringBuilder buffer = new StringBuilder();
				getStatusMessage(buffer, status);
				Label messageLabel = new Label(this, SWT.WRAP);
				messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				messageLabel.setFont(getBoldFont());
				messageLabel.setText(buffer.toString());
			}
			
			layout(true);
		}
		
		/**
		 * Combines all status messages.
		 * 
		 * @param buffer Buffer for messages.  Each status message will be terminated
		 * with a new line. 
		 * @param status Status to combine.
		 */
		private void getStatusMessage(StringBuilder buffer, IStatus status) {
			buffer.append(status.getMessage());
			buffer.append('\n');
			IStatus[] children = status.getChildren();
			for (IStatus child : children) {
				getStatusMessage(buffer, child);
			}
		}

		/**
		 * Clears the panel status.
		 */
		public void clearStatus() {
			Control[] children = getChildren();
			for (Control child : children) {
				child.dispose();
			}
		}
	}

	/**
	 * Composite to show page busy status.
	 */
	private class BusyPanel extends Composite {
		private BusyAnimationControl waitCtrl;
		
		/**
		 * Constructor
		 * 
		 * @param parent Parent
		 * @param style Style
		 */
		public BusyPanel(Composite parent, int style) {
			super(parent, style);
			setLayout(new GridLayout(1, false));
			
			// Create animation control
			waitCtrl = new BusyAnimationControl(this, SWT.NONE);
			waitCtrl.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false, 1, 1));
			waitCtrl.setFont(getBoldFont());
		}
		
		/**
		 * Starts the busy animation.
		 * 
		 * @param message Message to display
		 */
		public void startBusy(String message) {
			waitCtrl.setText(message);
			waitCtrl.animate(true);
			
			layout(true);
		}
		
		/**
		 * Stops the busy animation.
		 */
		public void stopBusy() {
			waitCtrl.animate(false);
		}
	}
}
