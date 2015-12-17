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

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.swt.widgets.Label;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.IInstallValues;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LinkDescription;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.ui.BrowseDefaultEditor;
import com.codesourcery.installer.ui.BrowseDirectoryDefaultEditor;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;

/**
 * Install wizard page for shortcut options.
 * This page supports console.
 */
public class ShortcutsPage extends InstallWizardPage implements IInstallSummaryProvider, IInstallConsoleProvider {
	/** Programs shortcuts */
	public static final int PROGRAM_SHORTCUTS	= 0x0001;
	/** Desktop shortcuts */
	public static final int DESKTOP_SHORTCUTS	= 0x0002;
	/** Short-cuts */
	private LinkDescription[] shortcuts;
	/** Programs shortcuts option button */
	private Button programsOptionButton;
	/** Programs shortcuts location text */
	private BrowseDefaultEditor programsLocationEditor;
	/** Desktop shortcuts option button */
	private Button desktopOptionButton;
	/** <code>true</code> to show program short-cuts option */
	private boolean showProgramShortcuts;
	/** <code>true</code> to show desktop short-cuts option */
	private boolean showDesktopShortcuts;
	/** Program shortcuts location */
	private String programsLocation;
	/** Saved install location */
	private IPath savedInstallLocation;
	/** Console prompter */
	private ConsoleListPrompter<String> consolePrompter;
	/** Current console state */
	private int consoleState;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param shortcuts Short-cuts
	 */
	public ShortcutsPage(String pageName, String title, LinkDescription[] shortcuts) {
		super(pageName, title);
		
		showProgramShortcuts = false;
		showDesktopShortcuts = false;
		consoleState = 0;
		
		this.shortcuts = shortcuts;
		
		init();
	}

	/**
	 * Initializes the page.
	 */
	private void init() {
		for (LinkDescription shortcut : getShortcuts()) {
			// If program short-cut defined, show program short-cuts option
			if (shortcut.getFolder() == ShortcutFolder.PROGRAMS) {
				showProgramShortcuts = true;
			}
			// If desktop short-cut defined, show desktop short-cuts option
			else if (shortcut.getFolder() == ShortcutFolder.DESKTOP) {
				showDesktopShortcuts = true;
			}
		}
	}
	
	/**
	 * Returns if the page is being displayed on a Windows platform.
	 * 
	 * @return <code>true</code> if running on Windows
	 */
	private boolean isWindows() {
		return Installer.isWindows();
	}

	/**
	 * Returns the short-cuts.
	 * 
	 * @return Short-cut types
	 */
	public LinkDescription[] getShortcuts() {
		return shortcuts;
	}
	
	/**
	 * Sets if shortcuts in programs will be created.
	 * 
	 * @param isProgramShortcuts <code>true</code> if shortcuts in programs
	 * will be created.
	 */
	protected void setProgramShortcuts(boolean isProgramShortcuts) {
		getInstallData().setProperty(IInstallValues.CREATE_PROGRAM_SHORTCUTS, isProgramShortcuts);
	}
	
	/**
	 * Returns if shortcuts in programs
	 * will be created.
	 * 
	 * @return <code>true</code> if shortcuts in programs
	 * will be created.
	 */
	protected boolean isProgramShortcuts() {
		return getInstallData().getBooleanProperty(IInstallValues.CREATE_PROGRAM_SHORTCUTS);
	}
	
	/**
	 * Sets if shortcuts on desktop will be created.
	 * 
	 * @param isDesktopShortcuts <code>true</code> if shortcuts on desktop
	 * will be created.
	 */
	protected void setDesktopShortcuts(boolean isDesktopShortcuts) {
		getInstallData().setProperty(IInstallValues.CREATE_DESKTOP_SHORTCUTS, isDesktopShortcuts);
	}
	
	/**
	 * Returns if shortcuts on desktop
	 * will be created.
	 * 
	 * @return <code>true</code> if shortcuts on desktop
	 * will be created.
	 */
	protected boolean isDesktopShortcuts() {
		return getInstallData().getBooleanProperty(IInstallValues.CREATE_DESKTOP_SHORTCUTS);
	}

	/**
	 * Returns the programs short-cut label.
	 * 
	 * @return Label
	 */
	private String getProgramsLabel() {
		return isWindows() ? 
				InstallMessages.ShortcutsPage_ProgramsOptionWin : 
				InstallMessages.ShortcutsPage_ProgramsOptionOther;
	}
	
	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(2, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Message label
		Label messageLabel = new Label(area, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		messageLabel.setText(
				Installer.getDefault().getInstallManager().getInstallDescription().getText(IInstallDescription.TEXT_SHORTCUTS_PAGE_MESSAGE, 
				InstallMessages.ShortcutsPage_MessageLabel));

		// Spacing
		Label spacing = new Label(area, SWT.NONE);
		spacing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// Button to select shortcuts in programs
		GridData data;
		if (showProgramShortcuts) {
			// Programs shortcut button
			programsOptionButton = new Button(area, SWT.CHECK);
			data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
			data.horizontalIndent = getDefaultIndent();
			programsOptionButton.setLayoutData(data);
			programsOptionButton.setText(getProgramsLabel() + ":"); 
			programsOptionButton.setSelection(isProgramShortcuts());
			programsOptionButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setProgramShortcuts(programsOptionButton.getSelection());
					updateControls();
				}
			});
			
			// Include browse button?
			boolean includeBrowse = !isWindows();
			
			// Programs shortcut location
			programsLocationEditor = new BrowseDirectoryDefaultEditor(area, SWT.NONE, true, includeBrowse, getProgramsLocation());
			programsLocationEditor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			programsLocationEditor.setBrowseMessage(InstallMessages.ChooseShortcutDirectory);
			programsLocationEditor.getEditor().addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					setProgramsLocation(programsLocationEditor.getText());
					validate();
				}
			});
		}
		
		// Button to select shortcuts on desktop
		if (showDesktopShortcuts) {
			desktopOptionButton = new Button(area, SWT.CHECK);
			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
			data.horizontalIndent = getDefaultIndent();
			desktopOptionButton.setLayoutData(data);
			desktopOptionButton.setText(InstallMessages.ShortcutsPage_DesktopOption);
			desktopOptionButton.setSelection(isDesktopShortcuts());
			desktopOptionButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setDesktopShortcuts(desktopOptionButton.getSelection());
				}
			});
		}
		
		updateControls();
		
		return area;
	}
	
	/**
	 * Sets the program links location.
	 * 
	 * @param programsLocation Location
	 */
	private void setProgramsLocation(String programsLocation) {
		this.programsLocation = programsLocation;
	}
	
	/**
	 * Returns the programs links location.
	 * 
	 * @return Location
	 */
	private String getProgramsLocation() {
		return programsLocation;
	}
	
	@Override
	public void setActive(IInstallData data) {
		// Current install location
		IPath installLocation = Installer.getDefault().getInstallManager().getInstallLocation();
		// Program links location
		IPath linksLocation = Installer.getDefault().getInstallManager().getInstallDescription().getLinksLocation();

		// Initialize the programs location
		if (getProgramsLocation() == null) {
			String programsLocation;
			// For windows, the programs location is fixed to Start Menu
			if (isWindows()) {
				programsLocation = linksLocation.toOSString();
			}
			// For Linux, the programs location is relative to the install location
			else {
				programsLocation = installLocation.append(linksLocation).toOSString();
				// Save the install location.  If it changes and the user has not
				// changed the program short-cuts location, we will update relative
				// to the new install location.
				savedInstallLocation = installLocation;
			}
			
			setProgramsLocation(programsLocation);
			// Update the GUI
			if (!isConsoleMode() && (programsLocationEditor != null)) {
				programsLocationEditor.setDefaultValue(programsLocation);
				programsLocationEditor.setText(programsLocation);
			}
		}
		// For Linux, check if the install location has changed
		else if (!isWindows()) {
			// Install location has changed
			if (!savedInstallLocation.equals(installLocation)) {
				String programsLocation = installLocation.append(linksLocation).toOSString();
				if (programsLocationEditor != null) {
					programsLocationEditor.setDefaultValue(programsLocation);
					// If the user didn't change the default program short-cuts location
					// update to new install location
					if (getProgramsLocation().equals(savedInstallLocation.append(linksLocation).toOSString())) {
						programsLocationEditor.setText(programsLocation);
					}
				}
				savedInstallLocation = installLocation;
			}
		}
		
		if (!isConsoleMode() && (programsOptionButton != null)) {
			programsOptionButton.setSelection(isProgramShortcuts());
		}

		if (!isConsoleMode() && (desktopOptionButton != null)) {
			desktopOptionButton.setSelection(isDesktopShortcuts());
		}

		String programShortcutsFolder = data.getProperty(IInstallValues.PROGRAM_SHORTCUTS_FOLDER);
		if (programShortcutsFolder != null) {
			setProgramsLocation(programShortcutsFolder);
			if (!isConsoleMode() && (programsLocationEditor != null)) {
				programsLocationEditor.setText(getProgramsLocation());
			}
		}

		super.setActive(data);
	}

	/**
	 * Updates the enabled/disabled state of controls.
	 */
	private void updateControls() {
		if (showProgramShortcuts) {
			boolean selected = programsOptionButton.getSelection();
			programsLocationEditor.setEnabled(selected);	
		}
	}
	
	@Override
	public String getInstallSummary() {
		// Short-cuts
		if (isProgramShortcuts() || isDesktopShortcuts()) {
			StringBuffer buffer = new StringBuffer();
			
			buffer.append(InstallMessages.ShortcutsPageSummaryTitle);
			if (isProgramShortcuts()) {
				buffer.append("\n\t");
				buffer.append(getProgramsLabel()); 
			}
			if (isDesktopShortcuts()) {
				buffer.append("\n\t");
				buffer.append(InstallMessages.ShortcutsPage_DesktopOption);
			}
			
			return buffer.toString() + "\n\n";
		}
		// No short-cuts
		else {
			return InstallMessages.ShortcutsPage_NoShortcuts + "\n\n";
		}
	}

	@Override
	public void saveInstallData(IInstallData data) throws CoreException {
		data.setProperty(IInstallValues.CREATE_DESKTOP_SHORTCUTS, isDesktopShortcuts());
		data.setProperty(IInstallValues.CREATE_PROGRAM_SHORTCUTS, isProgramShortcuts());
		if (getProgramsLocation() != null) {
			IPath shortcutsPath = InstallUtils.resolvePath(getProgramsLocation());
			data.setProperty(IInstallValues.PROGRAM_SHORTCUTS_FOLDER, shortcutsPath.toOSString());
		}
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		
		setErrorMessage(null);
		
		if ((getProgramsLocation() != null) && getProgramsLocation().isEmpty()) {
			setErrorMessage(InstallMessages.Error_PleaseSpecifyLocation);
			valid = false;
		}
		
		setPageComplete(valid);
		
		return valid;
	}

	protected String getConsoleMessage() {
		StringBuffer buffer = new StringBuffer();

		if (showProgramShortcuts) {
			
		}
		
		return buffer.toString();
	}
	
	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;
		// Initial response
		if (input == null) {
			consoleState = 0;
			// Show options to create program and/or desktop shortcuts
			consolePrompter = new ConsoleListPrompter<String>(
					Installer.getDefault().getInstallManager().getInstallDescription().getText(IInstallDescription.TEXT_SHORTCUTS_PAGE_MESSAGE, 
					InstallMessages.ShortcutsPage_MessageLabel));
			if (showProgramShortcuts) {
				consolePrompter.addItem(getProgramsLabel(), "PROGRAMS", true, true);
			}
			if (showDesktopShortcuts) {
				consolePrompter.addItem(InstallMessages.ShortcutsPage_DesktopOption, "DESKTOP", true, true);
			}
		}

		// Shortcut selection
		if (consoleState == 0) {
			response = consolePrompter.getConsoleResponse(input);
			if (response == null) {
				setProgramShortcuts(false);
				setDesktopShortcuts(false);
				
				ArrayList<String> selections = new ArrayList<String>();
				consolePrompter.getSelectedData(selections);
				for (String selection : selections) {
					if (selection.equals("PROGRAMS")) {
						consoleState = 1;
						setProgramShortcuts(true);
						
						// If programs selected, prompt for location
						StringBuffer buffer = new StringBuffer();
						buffer.append(InstallMessages.ShortcutsPageConsoleProgramsLocation);
						buffer.append('\n');
						buffer.append(getProgramsLabel());
						buffer.append(": ");
						buffer.append(getProgramsLocation());
						buffer.append("\n\n");
						buffer.append(InstallMessages.ConsolePressEnterOrChange);
						response = buffer.toString(); 
					}
					else if (selection.equals("DESKTOP")) {
						setDesktopShortcuts(true);
					}
				}
			}
		}
		// Programs location
		else if (consoleState == 1) {
			if (!input.isEmpty())
				setProgramsLocation(input);
			response = null;
		}
		
		return response;
	}
}
