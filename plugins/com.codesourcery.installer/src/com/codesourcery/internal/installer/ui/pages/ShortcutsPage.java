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
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LinkDescription;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.ui.BrowseDefaultEditor;
import com.codesourcery.installer.ui.BrowseDirectoryDefaultEditor;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;

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
	/** <code>true</code> to create desktop shortcuts */
	private boolean isDesktopShortcuts;
	/** <code>true</code> to create program shortcuts */
	private boolean isProgramShortcuts;
	/** Program shortcuts location */
	private String programsLocation;
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
	 * @param linkPath Directory for short-cuts
	 */
	public ShortcutsPage(String pageName, String title, LinkDescription[] shortcuts, IPath linkPath) {
		super(pageName, title);
		
		showProgramShortcuts = false;
		showDesktopShortcuts = false;
		isDesktopShortcuts = false;
		isProgramShortcuts = false;
		consoleState = 0;
		
		this.shortcuts = shortcuts;
		setProgramsLocation(linkPath.toOSString());
		
		init();
	}

	/**
	 * Initializes the page.
	 */
	private void init() {
		setProgramShortcuts(false);
		setDesktopShortcuts(false);
		
		for (LinkDescription shortcut : getShortcuts()) {
			if (shortcut.getFolder() == ShortcutFolder.PROGRAMS) {
				showProgramShortcuts = true;
				if (shortcut.isDefault())
					setProgramShortcuts(true);
			}
			else if (shortcut.getFolder() == ShortcutFolder.DESKTOP) {
				showDesktopShortcuts = true;
				if (shortcut.isDefault())
					setDesktopShortcuts(true);
			}
		}
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
		this.isProgramShortcuts = isProgramShortcuts;
	}
	
	/**
	 * Returns if shortcuts in programs
	 * will be created.
	 * 
	 * @return <code>true</code> if shortcuts in programs
	 * will be created.
	 */
	protected boolean isProgramShortcuts() {
		return isProgramShortcuts;
	}
	
	/**
	 * Sets if shortcuts on desktop will be created.
	 * 
	 * @param isDesktopShortcuts <code>true</code> if shortcuts on desktop
	 * will be created.
	 */
	protected void setDesktopShortcuts(boolean isDesktopShortcuts) {
		this.isDesktopShortcuts = isDesktopShortcuts;
	}
	
	/**
	 * Returns if shortcuts on desktop
	 * will be created.
	 * 
	 * @return <code>true</code> if shortcuts on desktop
	 * will be created.
	 */
	protected boolean isDesktopShortcuts() {
		return isDesktopShortcuts;
	}

	/**
	 * Returns the programs short-cut label.
	 * 
	 * @return Label
	 */
	private String getProgramsLabel() {
		return Installer.isWindows() ? 
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
		messageLabel.setText(InstallMessages.ShortcutsPage_MessageLabel);

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
			boolean includeBrowse = !Installer.isWindows();
			
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
	public void saveInstallData(IInstallData data) {
		data.setProperty(IInstallConstants.PROPERTY_DESKTOP_SHORTCUTS, new Boolean(isDesktopShortcuts()));
		data.setProperty(IInstallConstants.PROPERTY_PROGRAM_SHORTCUTS, new Boolean(isProgramShortcuts()));
		data.setProperty(IInstallConstants.PROPERTY_PROGRAM_SHORTCUTS_FOLDER, getProgramsLocation());
	}

	@Override
	public boolean validate() {
		boolean valid = true;
		
		setErrorMessage(null);
		
		if (getProgramsLocation().isEmpty()) {
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
			consolePrompter = new ConsoleListPrompter<String>(InstallMessages.ShortcutsPage_MessageLabel);
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
