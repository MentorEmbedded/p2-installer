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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import com.codesourcery.installer.IInstallDescription.WizardNavigation;
import com.codesourcery.internal.installer.ContributorRegistry;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.ui.BusyAnimationControl;
import com.codesourcery.internal.installer.ui.ISetupWizardPage;
import com.codesourcery.internal.installer.ui.InstallWizard;
import com.codesourcery.internal.installer.ui.SideBarComposite;
import com.codesourcery.internal.installer.ui.StepsControl;
import com.codesourcery.internal.installer.ui.pages.ProgressPage;

/**
 * Install wizard base page class.  All install wizard pages should subclass
 * this class.
 * Subclasses should provide an implementation of {@link #createContents(Composite)}.
 * A page can show a warning or error, a page can call {@link #showStatus(IStatus[])}.
 * A page can show a busy animation by calling {@link #showBusy(String)}.
 */
public abstract class InstallWizardPage extends WizardPage implements IInstallWizardPage {
	/** Auto update delay (ms) */
	public static final int DEFAULT_UPDATE_DELAY = 2000;
	/** Default width in characters */
	protected static final int DEFAULT_WIDTH = 120;
	/** Default height in characters */
	protected static final int DEFAULT_HEIGHT = 20;
	/** Options indenting */
	protected static final int DEFAULT_INDENT = 10;
	/** Maximum number of lines to display in warning panel */
	protected static final int MAX_WARNING_LINES = 3;
	/** Page side bar */
	private SideBarComposite sideBar;
	/** Page top bar */
	private StepsControl topBar;
	/** Page label */
	private String pageLabel;
	/** Page container */
	private Composite pageContainer;
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
	private WizardNavigation pageNavigation = WizardNavigation.TOP;
	/** <code>true</code> if page is auto updating */
	private boolean autoUpdate = false;
	/** Auto-update job */
	private AutoUpdateJob updateJob;
	/** Auto-update delay */
	private int autoUpdateDelay;
	/** Maximum height of warning panel */
	private int maxWarningHeight;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 */
	protected InstallWizardPage(String pageName, String title) {
		super(pageName, "", null); //$NON-NLS-1$
		setPageLabel(title);
		
		// For install default to mode specified in installer properties
		if (Installer.getDefault().getInstallManager().getInstallMode().isInstall()) {
			setPageNavigation(Installer.getDefault().getInstallManager().getInstallDescription().getPageNavigation());
		}
	}
	
	/**
	 * Returns the install data.
	 * 
	 * @return Install data
	 */
	protected IInstallData getInstallData() {
		return Installer.getDefault().getInstallManager().getInstallData();
	}
	
	/**
	 * Sets the page navigation.  This can be one of the following:
	 * 
	 * @param pageNavigation Page navigation
	 */
	public void setPageNavigation(WizardNavigation pageNavigation) {
		this.pageNavigation = pageNavigation;
	}
	
	/**
	 * Returns the page navigation.
	 * 
	 * @return Page navigation
	 */
	public WizardNavigation getPageNavigation() {
		return pageNavigation;
	}
	
	@Override
	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		// Stop any update thread
		if (updateJob != null) {
			updateJob.cancel();
			try {
				updateJob.join();
			} catch (InterruptedException e) {
				// Ignore
			}
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
	 * Called to give the page a chance to make any label adjustments
	 * based on installation mode.
	 */
	public void initPageLabel() {
		// Base does nothing
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

		// Compute maximum height of warning panel
		maxWarningHeight = convertHeightInCharsToPixels(MAX_WARNING_LINES);
		
		boolean leftNavigation = (getPageNavigation() == WizardNavigation.LEFT) || (getPageNavigation() == WizardNavigation.LEFT_MINIMAL);
		
		Composite area = new Composite(parent, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				Point defaultSize = super.computeSize(wHint, hHint, changed);
				int defaultHeight = convertHeightInCharsToPixels(DEFAULT_HEIGHT);
				// Limit the width of the page to a default so that labels wrap instead of extending the wizard dialog.
				// Limit the height to the page contents or default height (whichever is largest) plus the maximum
				// width of the warning panel.
				return new Point(
						convertWidthInCharsToPixels(DEFAULT_WIDTH), 
						Math.max(defaultSize.y, defaultHeight) + maxWarningHeight);
			}
		};
		GridData areaData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		area.setLayoutData(areaData);
		GridLayout layout = new GridLayout(leftNavigation ? 3 : 2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		area.setLayout(layout);

		// Top navigation bar
		if (getPageNavigation() == WizardNavigation.TOP) { 
			topBar = new StepsControl(area, SWT.NONE);
			topBar.setFont(getFont());
			topBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		}
		// Left navigation bar
		else if (leftNavigation) { 
			if (getPageNavigation() == WizardNavigation.LEFT) {
				sideBar = new SideBarComposite(
						area, 
						SWT.NONE,
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_EMPTY),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_SOLID),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_CHECKED),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_ARROW),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_ERROR),
						true);
			}
			else {
				sideBar = new SideBarComposite(
						area, 
						SWT.NONE,
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_EMPTY2),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_EMPTY2),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_CHECKED2),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_ARROW2),
						Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_ERROR2),
						false);
			}
			sideBar.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, true, 1, 1));
			// Vertical separator
			Label verticalSeparator = new Label(area, SWT.SEPARATOR | SWT.VERTICAL);
			verticalSeparator.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, true, 1, 1));
		}

		// Page container
		pageContainer = new Composite(area, SWT.NONE);
		pageContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		pageContainer.setLayout(layout);
		
		// Page area
		pageArea = new Composite(pageContainer, SWT.NONE);
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
		layout.marginWidth = 4;
		pageClientArea.setLayout(layout);
		
		// Create page contents
		createContents(pageClientArea);

		// Warning area
		final Composite warningArea = new Composite(pageArea, SWT.NONE);
		warningArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		GridLayout warningAreaLayout = new GridLayout(1, true);
		warningAreaLayout.marginTop = 0;
		warningAreaLayout.marginBottom = 2;
		warningAreaLayout.marginLeft = 0;
		warningAreaLayout.marginRight = 2;
		warningArea.setLayout(warningAreaLayout);
		
		// Warning pane (initially hidden unless a status is available)
		warningPanel = new WarningPanel(warningArea, SWT.NONE);
		GridData warningPanelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		warningPanelData.exclude = (status == null);
		warningPanel.setLayoutData(warningPanelData);
		if (status != null) {
			warningPanel.setStatus(status);
		}
		
		// Waiting pane (initially hidden)
		busyPanel = new BusyPanel(pageContainer, SWT.NONE);
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
	 * @return The page container
	 */
	protected Composite getPageContainer() {
		return pageContainer;
	}
	
	/**
	 * Returns the page area.
	 * 
	 * @return Page area
	 */
	protected Composite getPageArea() {
		return pageArea;
	}
	
	/**
	 * Returns the page client area.
	 * 
	 * @return Page area
	 */
	protected Composite getPageClientArea() {
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
	public IStatus[] getStatus() {
		return status;
	}
	
	/**
	 * Returns if the page status has errors.
	 * 
	 * @return <code>true</code> if status has errors
	 */
	public boolean hasStatusError() {
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
	public void showStatus(IStatus[] status) {
		setStatus(status);
		
		if (warningPanel != null) {
			// Update status
			warningPanel.setStatus(status);
			// Show pane
			GridData data = (GridData)warningPanel.getLayoutData();
			data.exclude = false;
			getPageArea().layout(true);

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
			getPageArea().layout(true);

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
	 * Prints a line of console output.
	 * 
	 * @param output Output
	 */
	protected void printConsole(String output) {
		System.out.println(output);
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
			if (page instanceof ISetupWizardPage)
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
		if (previousPage instanceof ISetupWizardPage) {
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
	public void saveInstallData(IInstallData data) throws CoreException {
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
	 * Performs a long running operation.  If the installer is running in GUI mode, a busy animation is 
	 * displayed instead of the page contents until the operation is complete.  If the insaller is running
	 * in console mode, the runnable is called directly.
	 * 
	 * @param message Busy message to display
	 * @param runnable Operation to run
	 */
	protected void runOperation(String message, Runnable runnable) {
		// Console mode
		if (isConsoleMode()) {
			runnable.run();
		}
		// GUI mode
		else {
			InstallWizard wizard = (InstallWizard)getWizard();
			wizard.runOperation(this, message, runnable);
		}
	}
	
	/**
	 * Performs a long running operation.  A busy animation is displayed
	 * instead of the page contents if the operation takes longer than the
	 * specified time.
	 * 
	 * @param message Busy message to display
	 * @param runnable Operation to run
	 * @param delay Delay (ms) before showing busy animation
	 */
	protected void runOperation(String message, Runnable runnable, int delay) {
		InstallWizard wizard = (InstallWizard)getWizard();
		wizard.runOperation(this, message, runnable, delay);
	}
	
	/**
	 * @return Returns the current install mode.
	 */
	protected IInstallMode getInstallMode() {
		return Installer.getDefault().getInstallManager().getInstallMode();
	}

	/**
	 * Formats a message for the console.  Any special formatting tags are 
	 * removed.  Copyright and trademark symbols are replaced with suitable
	 * substitutes for the console.
	 * 
	 * @param text Formatted text
	 * @return Text with formatting removed
	 */
	public static String formatConsoleMessage(String text) {
		// Remove formatting
		text = text.replace("<b>", "");
		text = text.replace("</b>", "");
		text = text.replace("<i>", "");
		text = text.replace("</i>", "");
		text = text.replace("<u>", "");
		text = text.replace("</u>", "");
		text = text.replace("<strike>", "");
		text = text.replace("</strike>", "");
		text = text.replace("<small>", "");
		text = text.replace("</small>", "");
		text = text.replace("<big>", "");
		text = text.replace("</big>", "");
		// Replace any trademarks
		text = replaceTrademarks(text);
		
		return text;
	}
	
	/**
	 * Replaces copyright and trademark symbols substitutes.
	 * 
	 * @param text Text to replace
	 * @return Replaced text
	 */
	public static String replaceTrademarks(String text) {
		text = text.replaceAll("(?i)\\u00a9", "Copyright");
		text = text.replaceAll("(?i)\\u00ae", "(R)");
		text = text.replaceAll("(?i)\\u2122", "(TM)");
		
		return text;
	}

	@Override
	public boolean isSupported() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		
		// By default, pages are supported in a new install only
		return (mode.isInstall() && !mode.isUpdate() && !mode.isPatch() && !mode.isMirror());
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		// If auto-updating
		if (getAutoUpdate()) {
			// Start update thread if page is visible
			if (visible) {
				runUpdateJob(true);
			}
			// else stop update thread when page is not visible
			else {
				runUpdateJob(false);
			}
		}
	}

	/**
	 * Sets the page to start automatically updating.  If auto-update is enabled, the {@link #autoUpdate()}
	 * method will be called periodically to update the page.
	 * 
	 * @see #autoUpdate()
	 * @see #stopAutoUpdate()
	 */
	protected void startAutoUpdate() {
		startAutoUpdate(DEFAULT_UPDATE_DELAY);
	}
	
	/**
	 * Sets the page to start automatically updating.  If auto-update is enabled, the {@link #autoUpdate()}
	 * method will be called periodically to update the page.
	 * 
	 * @param delay Delay in milliseconds.
	 * @see #autoUpdate()
	 * @see #stopAutoUpdate()
	 */
	protected void startAutoUpdate(int delay) {
		this.autoUpdate = true;
		
		if (delay < 0) {
			delay = DEFAULT_UPDATE_DELAY;
		}
		this.autoUpdateDelay = delay;
		
		runUpdateJob(true);
	}
	
	/**
	 * Stops the page auto-updating.
	 */
	protected void stopAutoUpdate() {
		this.autoUpdate = false;
		runUpdateJob(false);
	}
	
	/**
	 * @return <code>true</code> if automatic updating is enabled.
	 */
	protected boolean getAutoUpdate() {
		return autoUpdate;
	}

	/**
	 * Starts or stops the automatic update job.
	 * 
	 * @param start <code>true</code> to start, <code>false</code> to stop.
	 */
	private void runUpdateJob(boolean start) {
		if (start) {
			if (updateJob == null) {
				updateJob = new AutoUpdateJob(autoUpdateDelay);
				updateJob.schedule();
			}
		}
		else {
			if (updateJob != null) {
				updateJob.cancel();
				updateJob = null;
			}
		}
	}
	
	/**
	 * Called periodically if auto-updating is enabled.  This method should be overridden to perform required periodic
	 * updates the page.
	 * 
	 * @see {@link #startAutoUpdate()}
	 * @see {@link #stopAutoUpdate()}
	 */
	protected void autoUpdate() {
		// Base does nothing
	}
	
	/**
	 * Composite to show status.
	 */
	private class WarningPanel extends ScrolledComposite {
		/** Panel area */
		private Composite panelArea;
		
		/**
		 * Constructor
		 * 
		 * @param parent Parent
		 * @param style Style
		 */
		public WarningPanel(Composite parent, int style) {
			super(parent, style | SWT.V_SCROLL);
			panelArea = new Composite(this, SWT.NONE);
			GridLayout panelLayout = new GridLayout(2, false);
			panelLayout.marginHeight = panelLayout.marginWidth = 0;
			panelArea.setLayout(panelLayout);
			setContent(panelArea);
			setExpandHorizontal(true);
			parent.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					updateLayout();
				}
			});
		}
		
		/**
		 * Returns the panel area.
		 * 
		 * @return Panel area
		 */
		private Composite getPanelArea() {
			return panelArea;
		}

		/**
		 * Updates the layout of the panel.
		 */
		private void updateLayout() {
			if ((getPanelArea() != null) && !getPanelArea().isDisposed()) {
				int width = getParent().getSize().x;
				Point panelSize = getPanelArea().computeSize(width > 0 ? width : SWT.DEFAULT, SWT.DEFAULT);
				getPanelArea().setSize(panelSize);
				getPanelArea().layout(true);
				// Limit height of warning panel
				GridData data = (GridData)getLayoutData();
				data.heightHint = (panelSize.y > maxWarningHeight) ? maxWarningHeight : SWT.DEFAULT;
				setLayoutData(data);
	
				getPageArea().layout(true);
			}
		}
		
		/**
		 * Sets the panel status.
		 * 
		 * @param statuses Status to show
		 */
		public void setStatus(IStatus[] statuses) {
			clearStatus();
			
			// Skip warnings and information if the status has errors
			boolean onlyErrors = false;
			for (IStatus status : statuses) {
				if (status.getSeverity() == IStatus.ERROR) {
					onlyErrors = true;
				}
			}
			
			for (IStatus status : statuses) {
				// Show only errors
				if (onlyErrors && (status.getSeverity() != IStatus.ERROR))
					continue;
				
				Label iconLabel = new Label(getPanelArea(), SWT.NONE);
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
				Label messageLabel = new Label(getPanelArea(), SWT.WRAP);
				messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				messageLabel.setText(buffer.toString());
			}

			updateLayout();
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
			Control[] children = getPanelArea().getChildren();
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
	
	/**
	 * Job that periodically calls the auto-update method on the UI thread.
	 */
	private class AutoUpdateJob extends Job {
		/** Update delay */
		private int delay;
		
		/**
		 * Constructs auto-update thread.
		 * 
		 * @param delay Delay in milliseconds.
		 */
		public AutoUpdateJob(int delay) {
			super("WizardPageAutoUpdateJob");
			this.delay = delay;
			setSystem(true);
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			try {
				while (!monitor.isCanceled()) {
					Thread.sleep(delay);

					synchronized(this) {
						if (!monitor.isCanceled()) {
							// Run in UI thread
							getShell().getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									try {
										autoUpdate();
									}
									catch (Exception e) {
										Installer.log(e);
										monitor.setCanceled(true);
									}
								}
							});
						}
					}
				}
			} catch (Exception e) {
				Installer.log(e);
			}
			
			return Status.OK_STATUS;
		}
	}
}
