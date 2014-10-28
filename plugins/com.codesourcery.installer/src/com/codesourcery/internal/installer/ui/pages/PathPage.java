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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.actions.PathAction;

/**
 * Install wizard page for path options.
 * This page supports the console.
 */
public class PathPage extends InstallWizardPage implements IInstallSummaryProvider, IInstallConsoleProvider {
	/** Product name */
	private String productName;
	/** Modify PATH button */
	private Button modifyButton;
	/** Do not modify PATH button */
	private Button doNotModifyButton;
	/** <code>true</code> to modify PATH */
	private boolean modifyPath = true;
	/** Paths */
	private String[] paths;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param productName Product name
	 * @param modifyDefault <code>true</code> to set paths by default
	 * @param paths Paths
	 */
	public PathPage(String pageName, String title, String productName, boolean modifyDefault, String[] paths) {
		super(pageName, title);
		this.productName = productName;
		// Set default
		setModifyPath(modifyDefault);
		this.paths = paths;
	}

	/**
	 * Returns the paths.
	 * 
	 * @return Paths
	 */
	public String[] getPaths() {
		return paths;
	}
	
	/**
	 * Returns the product name.
	 * 
	 * @return Product name
	 */
	private String getProductName() {
		return productName;
	}
	
	/**
	 * Sets whether the PATH environment should be modified.
	 * 
	 * @param modifyPath <code>true</code> to modify the PATH
	 */
	protected void setModifyPath(boolean modifyPath) {
		this.modifyPath = modifyPath;
	}
	
	/**
	 * Returns if the PATH environment should be modified.
	 * 
	 * @return <code>true</code> to modify the PATH
	 */
	protected boolean getModifyPath() {
		return modifyPath;
	}

	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Message label
		FormattedLabel messageLabel = new FormattedLabel(area, SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1);
		messageLabel.setLayoutData(data);
		String message = MessageFormat.format(InstallMessages.PathPage_MessageLabel0, 
				new Object[] { getProductName() });
		messageLabel.setText(message);

		// Spacing
		Label spacing = new Label(area, SWT.NONE);
		spacing.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
		
		// Do not modify PATH button
		doNotModifyButton = new Button(area, SWT.RADIO);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
		data.horizontalIndent = getDefaultIndent();
		doNotModifyButton.setLayoutData(data);
		doNotModifyButton.setText(InstallMessages.PathPage_DoNotModifyPath);
		doNotModifyButton.setSelection(!getModifyPath());
		doNotModifyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setModifyPath(false);
				hideStatus();
			}
		});
		
		// Modify PATH button
		modifyButton = new Button(area, SWT.RADIO);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
		data.horizontalIndent = getDefaultIndent();
		modifyButton.setLayoutData(data);
		modifyButton.setText(InstallMessages.PathPage_ModifyPath);
		modifyButton.setSelection(getModifyPath());
		modifyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setModifyPath(true);
			}
		});

		return area;
	}

	@Override
	public boolean validate() {
		boolean valid = checkPaths();

		// Paths can be added
		if (valid) {
			hideStatus();
		}
		// Paths can't be added
		else {
			showStatus(new IStatus[] { 
				new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_PathTooLarge)
			});
		}
		
		return valid;
	}
	
	/**
	 * Returns if product paths can be added to the PATH environment.
	 *  
	 * @return <code>true</code> if the paths can be added
	 */
	private boolean checkPaths() {
		boolean valid = true;
		
		// Check that the required paths can be added to the PATH environment
		if (getModifyPath()) {
			if ((getPaths() != null) && (getPaths().length > 0)) {
				try {
					boolean allUsers = Installer.getDefault().getInstallManager().getInstallDescription().getAllUsers();
					valid = PathAction.checkPaths(allUsers, paths);
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
		}
		
		return valid;
	}

	@Override
	public String getInstallSummary() {
		return MessageFormat.format(getModifyPath() ? 
				InstallMessages.PathPageSummaryModified0 : 
					InstallMessages.PathPageSummaryNotModified0, 
					new Object[] { InstallMessages.PathPageSummaryLabel }) + "\n\n";
	}

	@Override
	public void saveInstallData(IInstallData data) {
		data.setProperty(IInstallConstants.PROPERTY_MODIFY_PATHS, new Boolean(getModifyPath()));
	}

	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String message = MessageFormat.format(InstallMessages.PathPage_ConsoleMessage, 
				new Object[] { getProductName() });
		ConsoleYesNoPrompter prompt = new ConsoleYesNoPrompter(message, InstallMessages.PathPage_ConsolePrompt, true);
		String response = prompt.getConsoleResponse(input);
		if (response == null) {
			setModifyPath(prompt.getResult());
			if (prompt.getResult() && !checkPaths()) {
				System.out.println(InstallMessages.Error_PathTooLarge);
				response = prompt.getConsoleResponse(null);
			}
		}
		
		return response;
	}
}
