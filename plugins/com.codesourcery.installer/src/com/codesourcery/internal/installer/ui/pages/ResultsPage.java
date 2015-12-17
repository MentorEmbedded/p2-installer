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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.LaunchItem.LaunchItemPresentation;
import com.codesourcery.installer.LaunchItem.LaunchItemType;
import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.FileUtils;
import com.codesourcery.internal.installer.InstallManager;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * Page to show installation results.
 * This page does not support console.
 */
public class ResultsPage extends InstallWizardPage {
	/** Default width of log dialog contents */
	private static final int DEFAULT_LOG_DIALOG_WIDTH = 640;
	/** Default height of log dialog contents */
	private static final int DEFAULT_LOG_DIALOG_HEIGHT = 480;
	
	/** Results message */
	private String resultMessage;
	/** Error message */
	private String errorMessage = null;
	/** Message label */
	private FormattedLabel messageLabel;
	/** Error text */
	private Text errorText;
	/** Page contents area */
	private Composite area;
	/** Results pane */
	private Composite resultsPane;
	/** Error pane */
	private Composite errorPane;
	/** Launch items */
	private HashMap<LaunchItem, Control> launchItems;
	/** <code>true</code> to show option buttons */
	private boolean showOptions = true;
	/** <code>true</code> to show reset/logout option */
	private boolean showResetOrLogoutOption = true;

	/** Log file copy location */
	private File logFile;
	
	/** Log file UI link */
	private Link logLink;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 */
	public ResultsPage(String pageName, String title) {
		super(pageName, title);
		launchItems = new HashMap<LaunchItem, Control>();
	}

	/**
	 * Returns an option checked state.
	 * 
	 * @param index Index of option
	 * @return <code>true</code> is option
	 * is selected.
	 */
	public boolean isItemChecked(LaunchItem item) {
		boolean checked = false;
		
		if (launchItems.keySet() == null) {
			checked = false;
		}
		else if (!showOptions) {
			checked = false;
		}
		else if ((item.getType() == LaunchItemType.RESTART || item.getType() == LaunchItemType.LOGOUT)
				&& !showResetOrLogoutOption) {
				checked = false;
		}
		else {
			for (Control control : launchItems.values()) {
				if (control instanceof Button && control.getData() == item) {
					checked = ((Button)control).getSelection() && control.isEnabled();
					break;
				}
			}
		}
		
		return checked;
	}
	
	/**
	 * Sets the check state of an option.
	 * 
	 * @param index Index of option
	 * @param checked <code>true</code> to
	 * check option
	 */
	public void setItemChecked(LaunchItem item, boolean checked) {
		if (launchItems.values() != null) {
			for (Control control : launchItems.values()) {
				if (control.getData() == item) {
					((Button)control).setSelection(checked);
					break;
				}
			}
		}
	}

	/**
	 * Sets the result message.
	 * 
	 * @param message Result message
	 * @param showOptions <code>true</code> to show option buttons
	 */
	public void setResult(String message, boolean showOptions) {
		this.resultMessage = message;
		this.showOptions = showOptions;

		copyLog();
		updateInformation();
	}
	
	/**
	 * Returns the result message.
	 * 
	 * @return Result message
	 */
	public String getResult() {
		return resultMessage;
	}
	
	/**
	 * Sets an error message.
	 * 
	 * @param error Error message or
	 * <code>null</code> to clear.
	 */
	public void setError(String error) {
		this.errorMessage = error;
		copyLog();
		updateInformation();
	}
	
	/**
	 * Returns if the result has an error.
	 * 
	 * @return <code>true</code> if error
	 * @see #setError(String)
	 * @see #getError()
	 */
	public boolean hasError() {
		return (errorMessage != null);
	}
	
	/**
	 * Returns the error message.
	 * 
	 * @return Error message or 
	 * <code>null</code>.
	 */
	public String getError() {
		return errorMessage;
	}
	
	/**
	 * Updates the page information.
	 */
	protected void updateInformation() {
		if ((area != null) && !area.isDisposed()) {
			// Successful result
			if (errorMessage == null) {
				setErrorMessage(null);
				showSuccess(true);
				
				if (getResult() != null) {
					messageLabel.setText(getResult());
				}
				resultsPane.setVisible(true);
				((GridData)resultsPane.getLayoutData()).exclude = false;
				errorPane.setVisible(false);
				((GridData)errorPane.getLayoutData()).exclude = true;
				
				if (launchItems.keySet() != null) {
					for (LaunchItem launchItem: launchItems.keySet()) {
						Control control = launchItems.get(launchItem);
						if (launchItem.getType() == LaunchItemType.RESTART 
								|| launchItem.getType() == LaunchItemType.LOGOUT) {
							control.setVisible(showResetOrLogoutOption);
							// Initial enablement
							if (control instanceof Button && ((Button)control).getSelection() && showResetOrLogoutOption)
								restartOrLogoutSelectionChanged((Button)control);
						}
						else
							control.setVisible(showOptions);
					}
				}
			}
			// Error in result
			else {
				setErrorMessage(InstallMessages.SetupError);
				
				if (logFile != null) {
					String linkMsg = MessageFormat.format(
							InstallMessages.ResultsPage_LogLink,
							new Object[] { logFile.getAbsolutePath() });
					logLink.setText(linkMsg);
				}

				messageLabel.setText(InstallMessages.SetupErrors);
				resultsPane.setVisible(false);
				((GridData)resultsPane.getLayoutData()).exclude = true;
				errorPane.setVisible(true);
				((GridData)errorPane.getLayoutData()).exclude = false;
				
				errorText.setText(getError());
			}
			
			area.layout();
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		boolean restartOrLogoutPresent = false;
		LaunchItem []launchItemsArray;
		// If installing, create launch items
		if (visible && 
			Installer.getDefault().getInstallManager().getInstallMode().isInstall() && 
			(launchItems.keySet().isEmpty())) {
			launchItemsArray = Installer.getDefault().getInstallManager().getInstallDescription().getLaunchItems();
			
			if (launchItemsArray != null) {
				ArrayList<LaunchItem> launchItemList = new ArrayList<LaunchItem>();
				for (LaunchItem item : launchItemsArray) {
					if (Installer.getDefault().getInstallManager().isLaunchItemAvailable(item)) {
						if (item.getType() == LaunchItemType.RESTART
								|| item.getType() == LaunchItemType.LOGOUT) {
							restartOrLogoutPresent = true;
							break;
						}
						launchItemList.add(item);	
					}
				}
				// Add restart or logout launch item If it is not provided in installer.properties.
				if (!restartOrLogoutPresent)
					addRestartOrLogoutItems(launchItemList);
				// Create launch item buttons
				createOptionButtons(launchItemList);
			}
		}
		
		super.setVisible(visible);
	}


	@Override
	public void setActive(IInstallData data) {
		super.setActive(data);
		// Get property to check whether we need to show reset or logout option 
		this.showResetOrLogoutOption = ((InstallManager)Installer.getDefault().getInstallManager()).needsRestartOrRelogin();
		if (showOptions) {
			updatePageMessage();
			updateInformation();
		}
	}

	/**
	 * Add RestartOrLogout launch item
	 */
	protected void addRestartOrLogoutItems(ArrayList<LaunchItem>items) {
		if (Installer.isWindows())
		{
			LaunchItem item = new LaunchItem(LaunchItemType.RESTART, InstallMessages.ResultsPage_RestartMessage, "", LaunchItemPresentation.CHECKED);
			items.add(item);
			// Update launch items
			Installer.getDefault().getInstallManager().getInstallDescription().setLaunchItems(items.toArray(new LaunchItem[items.size()]));
		}
	}
	
	/**
	 * Create launch item option buttons
	 * @param items
	 */
	private void createOptionButtons(ArrayList<LaunchItem>items) {
		if (showOptions && (items != null) && (items.size() > 0)) {
			ArrayList <LaunchItem>buttonLaunchItems = new ArrayList <LaunchItem>();
			ArrayList <LaunchItem>linkLaunchItems = new ArrayList <LaunchItem>();
			for (LaunchItem item: items) {
				if (item.getPresentation() != LaunchItemPresentation.LINK) {
					buttonLaunchItems.add(item);
				}
			}
			for (LaunchItem item: items) {
				if (item.getPresentation() == LaunchItemPresentation.LINK) {
					linkLaunchItems.add(item);
				}
			}
			items.clear();
			items.addAll(buttonLaunchItems);
			items.addAll(linkLaunchItems);
			FormattedLabel linkSectionLabel = null;
			for (LaunchItem item: items) {
				Control control = null;
				GridData controlGridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
				if (item.getPresentation() == LaunchItemPresentation.LINK) {
					if (linkSectionLabel == null) {
						Label spacer = new Label(resultsPane, SWT.NONE);
						spacer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
						
						linkSectionLabel = new FormattedLabel(resultsPane, SWT.NONE);
						linkSectionLabel.setText(InstallMessages.ResultsPage_Link_Label);
						GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
						gridData.horizontalIndent = getDefaultIndent();
						linkSectionLabel.setLayoutData(gridData);
					}
					control = new Link(resultsPane, SWT.NONE);
					((Link)control).setText("<A>"+item.getName()+"</A>");
					controlGridData.horizontalIndent = getDefaultIndent() * 3;
				}
				else {
					control = new Button(resultsPane, SWT.CHECK);
					((Button)control).setText(item.getName());
					((Button)control).setSelection(item.isDefault());
					controlGridData.horizontalIndent = getDefaultIndent();
				}
				launchItems.put(item, control);
				control.setData(item);
				control.setLayoutData(controlGridData);
				
				// Create Restart or logout option button
				if (item.getType() == LaunchItemType.RESTART
						|| item.getType() == LaunchItemType.LOGOUT) {
					
					// Add selection listener
					if (control instanceof Button) {
						((Button)control).addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								Button restartLogoutButton = (Button) e.widget;
								restartOrLogoutSelectionChanged(restartLogoutButton);
							}
						});	
					} 
				}
				if (item.getPresentation() == LaunchItemPresentation.LINK) {
					if (control instanceof Link) {
						((Link)control).addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent event) {
								Link link = (Link) event.widget;
								LaunchItem launchItem = (LaunchItem)link.getData();
								try {
									Installer.getDefault().getInstallManager().launch(launchItem);
								} catch (CoreException e) {
									Installer.log(e);
								}
							}
						});
					}
				}
			}

			resultsPane.layout(true);
			area.layout(true);
		}
	}
	
	/**
	 * Restart or Logout launch item selection changed. Disable all other
	 * launch items if restart or logout item is checked.
	 * 
	 * @param resetOrLogoutButton Reset or Logout button
	 */
	protected void restartOrLogoutSelectionChanged(Button resetOrLogoutButton) {
		// Restart or Logout launch item is mutually exclusive to other launch items. If
		// this item is checked, all other launch items should be disabled.
		for (Control control : launchItems.values()) {
			if (control != resetOrLogoutButton && (control instanceof Button))
				control.setEnabled(!resetOrLogoutButton.getSelection());
		}
	}
	
	protected void updatePageMessage() {
		if (this.showResetOrLogoutOption)
			this.resultMessage = this.resultMessage + "\n\n" + getRestartOrLogoutText();
	}
	
	/**
	 * Returns text of Restart or Logout option depending on which one is present.
	 * 
	 * @return text of Restart or Logout option or <code>null</code> if Restart of 
	 * logout option item is not found.
	 */
	protected String getRestartOrLogoutText() {
		String text = null;
		
		for (LaunchItem item : launchItems.keySet()) {
			if (item.getType() == LaunchItemType.RESTART) {
				text =  InstallMessages.ResultsPage_RestartText;
				break;
			}
			else if (item.getType() == LaunchItemType.LOGOUT) {
				text = InstallMessages.ResultsPage_ReloginText;
				break;
			}
		}
		if (text == null) {
			text = Installer.isWindows() ? InstallMessages.ResultsPage_RestartText : InstallMessages.ResultsPage_ReloginText; 
		}

		return text;
	}
	
	@Override
	public Control createContents(Composite parent) {
		area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		GridLayout layout;
		
		// Message label
		messageLabel = new FormattedLabel(area, SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
		messageLabel.setLayoutData(data);

		// Spacing
		Label spacer = new Label(area, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		// Results pane
		resultsPane = new Composite(area, SWT.NONE);
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		resultsPane.setLayout(layout);
		resultsPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		// Error pane
		errorPane = new Composite(area, SWT.NONE);
		layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		errorPane.setLayout(layout);
		errorPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Error text
		errorText = new Text(errorPane, SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		errorText.setLayoutData(data);
		
		// Log file link
		logLink = new Link(area, SWT.WRAP);
		logLink.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, false, 1, 1));
		logLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Read from the log file in the original location, to avoid 
				// unlikely scenario that the log file copy failed. 
				File logFile = Platform.getLogFileLocation().toFile();
				if (logFile.exists()) {
					try {
						String  configContents = FileUtils.readFile(logFile);
						LogDialog dialog = new LogDialog(getShell(), configContents);
						dialog.open();
					} catch (IOException e1) {
						// Ignore - skip configuration log
					}
				}
				else {
					MessageDialog.openError(getShell(), InstallMessages.ResultsPage_NoLogFile, InstallMessages.Error_LogFileNotFound);
				}
			}
		});

		updateInformation();
		
		return area;
	}
	
	/**
	 * Copy the log file to a location outside of the default configuration 
	 * area so that it's accessible after the installer has exited and self-deleted.
	 */
	private void copyLog() {
		
		if (logFile != null) {
			// Already copied log.
			return;
		}
		
		try {
			IPath logDirPath = Installer.getDefault().getLogPath();
			
			// Copy the platform log file
			if (Installer.getDefault().isCopyLog()) {
				File originalLogFile = Platform.getLogFileLocation().toFile();
				logFile = logDirPath.append(originalLogFile.getName()).toFile();
				if (logFile.exists()) {
					Files.copy(originalLogFile.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			// Use the platform log file
			else {
				logFile = Platform.getLogFileLocation().toFile();
			}
		} catch (Exception e) {
			Installer.log(e);
		}		
	}
	
	/**
	 * Dialog to display log results.
	 */
	private class LogDialog extends Dialog {
		/** Log contents */
		private String contents;

		/**
		 * Constructor
		 * 
		 * @param parentShell Parent shell
		 * @param contents Log contents
		 */
		protected LogDialog(Shell parentShell, String contents) {
			super(parentShell);
			
			this.contents = contents;
			setShellStyle(getShellStyle() | SWT.RESIZE); 
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			
			newShell.setText(InstallMessages.ResultsPage_Log);
		}
		
		@Override
		protected Control createButtonBar(Composite parent) {
			Composite composite = (Composite)super.createButtonBar(parent);
			composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | 
					GridData.VERTICAL_ALIGN_CENTER));
			
			return composite;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
					true);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite)super.createDialogArea(parent);
			
			// Log text
			Text text = new Text(area, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
			data.heightHint = DEFAULT_LOG_DIALOG_WIDTH;
			data.widthHint = DEFAULT_LOG_DIALOG_HEIGHT;
			text.setLayoutData(data);
			text.setEditable(false);
			text.setText(contents);
			
			// Copy link
			Link copyLink = new Link(area, SWT.NONE);
			data = new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 1, 1);
			copyLink.setLayoutData(data);
			copyLink.setText(InstallMessages.ResultsPage_Copy);
			copyLink.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					copyContents();
				}
			});
			
			return area;
		}

		/**
		 * Copies the log contents to the clipboard
		 */
		private void copyContents() {
			Clipboard clipboard = new Clipboard(getShell().getDisplay());
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[] { contents },
					new Transfer[] { textTransfer });		
		}
	}

	@Override
	public boolean isSupported() {
		// Show for all modes
		return true;
	}
}
