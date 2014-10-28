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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.BrowseDefaultEditor;
import com.codesourcery.installer.ui.BrowseDirectoryDefaultEditor;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.ContributorRegistry;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * Page that prompts for the installation folder
 * This page supports the console.
 */
public class InstallFolderPage extends InstallWizardPage implements IInstallSummaryProvider, IInstallConsoleProvider {
	/** Folder text */
	private BrowseDefaultEditor valueEditor;
	/** Default installation folder */
	private String defaultFolder;
	/** Installation folder */
	private String folder;
	/** Whether all users is selected or not */
	private boolean allUsers;
	/** Install context */
	private IInstallDescription installDescription;
	/** Warning console prompter */
	private ConsoleYesNoPrompter warningConsolePrompter;
	
	/** All users vs Only this user buttons. */
	private Button allUsersBtn;
	private Button thisUserBtn;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param defaultFolder Default folder or <code>null</code>.
	 * @param installDescription Install description
	 */
	public InstallFolderPage(String pageName, String title, String defaultFolder, IInstallDescription installDescription) {
		super(pageName, title);
		this.defaultFolder = defaultFolder;
		if (this.defaultFolder == null) {
			this.defaultFolder = "";
		}
		setFolder(this.defaultFolder);
		this.installDescription = installDescription;
		this.allUsers = installDescription.getAllUsers();
	}
	
	/**
	 * Returns the install description.
	 * 
	 * @return Install description
	 */
	protected IInstallDescription getInstallDescription() {
		return installDescription;
	}

	/**
	 * Returns the default installation directory.
	 * 
	 * @return Default directory
	 */
	protected String getDefaultFolder() {
		return defaultFolder;
	}
	
	/**
	 * Returns the install folder.
	 * 
	 * @return Install folder
	 */
	protected String getFolder() {
		// If UI is available, get folder from it
		if (valueEditor != null) {
			folder = valueEditor.getText().trim();
		}
		return folder;
	}
	
	

	/**
	 * Sets the install folder.
	 * 
	 * @param folder Install folder
	 */
	protected void setFolder(String folder) {
		this.folder = folder.trim();
		// Update UI if available
		if (valueEditor != null) {
			valueEditor.setText(folder);
		}
	}

	public IStatus[] verifyInstallLocation() {
		return (ContributorRegistry.getDefault().verifyInstallLocation(new Path(getFolder())));
	}
	
	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		area.setLayout(new GridLayout(1, false));

		// Install folder label
		Label folderLabel = new Label(area, SWT.NONE);
		folderLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		folderLabel.setText(InstallMessages.InstallFolder);

		valueEditor = new BrowseDirectoryDefaultEditor(area, SWT.NONE, true, true, getFolder());
		valueEditor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		valueEditor.getEditor().addModifyListener(new ModifyListener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void modifyText(ModifyEvent e) {
				hideStatus();
				setPageComplete(true);
			}
		});
		valueEditor.getRestoreButton().addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});
		valueEditor.getBrowseButton().addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});
		valueEditor.setBrowseMessage(InstallMessages.SelectInstallFolderMessage);

		// Install for users label
		Label usersLabel = new Label(area, SWT.NONE);
		usersLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		usersLabel.setText(InstallMessages.InstallFor);
		
		allUsersBtn = new Button(area, SWT.RADIO);
		allUsersBtn.setText(InstallMessages.InstallForAllUsers);
		allUsersBtn.setSelection(allUsers);
		
		thisUserBtn = new Button(area, SWT.RADIO);
		thisUserBtn.setText(getThisUserStr());
		thisUserBtn.setSelection(!allUsers);
		
		Listener allUserUpdate = new Listener() {
			@Override
			public void handleEvent(Event event) {
				allUsers = allUsersBtn.getSelection();
				Installer.getDefault().getInstallManager().getInstallDescription().setAllUsers(allUsers);
				validate();
			}
		};
		allUsersBtn.addListener(SWT.Selection, allUserUpdate);
		thisUserBtn.addListener(SWT.Selection, allUserUpdate);
		
		validate();
		
		return area;
	}
	
	private String getThisUserStr() {
		return NLS.bind(InstallMessages.InstallForThisUser, System.getProperty("user.name"));
	}

	@Override
	public String getInstallSummary() {
		String userStr = allUsers ? InstallMessages.InstallForAllUsers : getThisUserStr();
		return NLS.bind(InstallMessages.SummaryInstallFolder, getFolder(), userStr);	
	}

	@Override
	public void saveInstallData(IInstallData data) {
		// save folder-related data
		data.setProperty(IInstallConstants.PROPERTY_INSTALL_FOLDER, getFolder());
		try {
			Installer.getDefault().getInstallManager().setInstallLocation(new Path(getFolder()));
		} catch (CoreException e) {
			Installer.log(e);
		}
		
		// and user-related data
		data.setProperty(IInstallConstants.PROPERTY_INSTALL_FOR_ALL_USERS, allUsers);
	}
	
	/**
	 * Returns if status has errors.
	 * 
	 * @param status Status
	 * @return <code>true</code> if any status is an error,
	 * <code>false</code> if all status are warning or information.
	 */
	private boolean statusContainsErrors(IStatus[] status) {
		boolean hasErrors = false;
		for (IStatus s : status) {
			if (s.getSeverity() == IStatus.ERROR) {
				hasErrors = true;
				break;
			}
		}
		
		return hasErrors;
	}
	
	@Override
	public boolean validate() {
		boolean valid = true;
		
		// Verify install location
		IStatus[] status = verifyInstallLocation();
		if (status.length > 0) {
			// Status has errors
			if (statusContainsErrors(status)) {
				valid = false;
				setPageComplete(false);
			}
			// If only warnings or information, the page is valid if has already
			// been displayed.
			else {
				valid = (getStatus() != null);
				setPageComplete(true);
				// Append an additional information status with a message to continue
				IStatus[] newStatus = new IStatus[status.length + 1];
				System.arraycopy(status, 0, newStatus, 0, status.length);
				newStatus[status.length] = new Status(IStatus.INFO, Installer.ID, InstallMessages.Error_NextToContinue);
				status = newStatus;
			}
			
			showStatus(status);
		}
		else {
			hideStatus();
			valid = true;
			setPageComplete(true);
		}
		
		return valid;
	}

	/**
	 * Returns the console message.
	 * 
	 * @return Message
	 */
	protected String getConsoleMessage() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(InstallMessages.InstallFolder);
		buffer.append('\n');
		buffer.append(NLS.bind(InstallMessages.InstallFolderPageConsoleInstallFolder, getFolder()));
		buffer.append("\n\n");
		buffer.append(InstallMessages.ConsolePressEnterOrChange);
		buffer.append('\n');
		
		return buffer.toString();
	}
	
	@Override
	public String getConsoleResponse(String input) throws IllegalArgumentException {
		String response = null;
		
		// Warning prompter active
		if (warningConsolePrompter != null) {
			response = warningConsolePrompter.getConsoleResponse(input);
			// Continue
			if (warningConsolePrompter.getResult()) {
				return null;
			}
			// Do not continue
			else {
				warningConsolePrompter = null;
				input = null;
			}
		}
		
		// Initial message
		if (input == null) {
			response = getConsoleMessage();
		}
		// Response
		else {
			// Continue with folder
			if (input.isEmpty()) {
				// Verify install location
				IStatus[] status = verifyInstallLocation();
				if (status.length > 0) {
					StringBuffer buffer = new StringBuffer();
					for (IStatus s : status) {
						if (buffer.length() == 0)
							buffer.append('\n');
						buffer.append(s.getMessage());
					}
					
					// Result contains errors
					if (statusContainsErrors(status)) {
						response = buffer.toString();
					}
					// Show warnings and/or information and prompt to continue
					else {
						warningConsolePrompter = new ConsoleYesNoPrompter(buffer.toString(), 
								InstallMessages.Continue, true);
						response = warningConsolePrompter.getConsoleResponse(null);
					}
				}
				else {
					response = null;
				}
			}
			// Change folder
			else {
				setFolder(input);
				response = getConsoleMessage();
			}
		}
		
		return response;
	}

	@Override
	public boolean isSupported() {
		// If an installed product has been set, don't prompt for install location
		if (Installer.getDefault().getInstallManager().getInstalledProduct() != null) {
			return false;
		}
		// Otherwise, show page for an installation that is not already set up 
		// for an update or upgrade to an existing product.
		else {
			IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
			return (mode.isInstall() && !mode.isUpdate() && !mode.isUpgrade());
		}
	}
}
