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

import org.apache.commons.io.FileUtils;
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

import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.InstallWizardPage;
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
	/** Option buttons */
	private Button[] optionButtons;
	/** Launch items */
	private LaunchItem[] launchItems;
	/** <code>true</code> to show option buttons */
	private boolean showOptions = true;

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
		
		if (optionButtons == null) {
			checked = false;
		}
		else if (!showOptions) {
			checked = false;
		}
		else {
			for (Button optionButton : optionButtons) {
				if (optionButton.getData() == item) {
					checked = optionButton.getSelection();
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
		if (optionButtons != null) {
			for (Button optionButton : optionButtons) {
				if (optionButton.getData() == item) {
					optionButton.setSelection(checked);
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
		// If installing, create launch items
		if (visible && 
			Installer.getDefault().getInstallManager().getInstallMode().isInstall() && 
			(launchItems == null)) {
			launchItems = Installer.getDefault().getInstallManager().getInstallDescription().getLaunchItems();
			// Create launch item buttons
			if (showOptions && (launchItems != null) && (launchItems.length > 0)) {
				optionButtons = new Button[launchItems.length];
				for (int index = 0; index < launchItems.length; index++) {
					optionButtons[index] = new Button(resultsPane, SWT.CHECK);
					optionButtons[index].setText(launchItems[index].getName());
					optionButtons[index].setSelection(launchItems[index].isDefault());
					optionButtons[index].setData(launchItems[index]);
					GridData buttonData = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
					buttonData.horizontalIndent = getDefaultIndent();
					optionButtons[index].setLayoutData(buttonData);
				}
				
				resultsPane.layout(true);
				area.layout(true);
			}
		}
		
		super.setVisible(visible);
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
						String  configContents = FileUtils.readFileToString(logFile);
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
		
		IPath logDirPath = Installer.getDefault().getLogPath();
		try {
			// Copy the platform log file
			if (Installer.getDefault().isCopyLog()) {
				File originalLogFile = Platform.getLogFileLocation().toFile();
				logFile = logDirPath.append(originalLogFile.getName()).toFile();
				Files.copy(originalLogFile.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			// Use the platform log file
			else {
				logFile = Platform.getLogFileLocation().toFile();
			}
		} catch (IOException e) {
//			Installer.log(e);
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
