/*******************************************************************************
 *  Copyright (c) 2015 Mentor Graphics and others.
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
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleInputPrompter;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.ui.BrowseDirectoryDefaultEditor;
import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;

/**
 * Wizard page that shows three options:
 * <ul>
 * <li>Option to install normally</li>
 * <li>Option to install and update mirror</li>
 * <li>Option to install from mirror</li>
 * </ul>
 */
public class MirrorPage extends AbstractSetupPage implements IInstallConsoleProvider, IInstallSummaryProvider {
	/** Install option */
	private final static int OPTION_INSTALL = 0;
	/** Install from saved data option */
	private final static int OPTION_LOAD = 1;
	/** Save option */
	private final static int OPTION_SAVE = 2;
	
	/** Main content area */
	private Composite area;
	/** Description label */
	private FormattedLabel descriptionLabel;
	/** Install button */
	private Button installButton;
	/** Install from save button */
	private Button installFromSaveButton;
	/** Save button */
	private Button saveButton;
	
	/** Install from save editor group */
	private BrowseDirectoryDefaultEditor installFromSaveEditor;
	/** Save editor group */
	private BrowseDirectoryDefaultEditor saveEditor;

	/** Currently selected option */
	private int selectedOption;
	/** Save path */
	private String savePath;
	/** Load path*/
	private String loadPath;
	
	/** Status error text */
	private String errorText;

	/** Console option prompter */
	private ConsoleListPrompter<Integer> consoleOptionPrompter;
	/** Console location prompter */
	private ConsoleInputPrompter consoleLocationPrompter;
	/** Current console state */
	private int consoleState = 0;
	
	/**
	 * Constructs a mirror page.
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 */
	public MirrorPage(String pageName, String title) {
		super(pageName, title);
		
		String defaultSavePath = getDefaultSavePath();
		setSavePath(defaultSavePath);
		setLoadPath(defaultSavePath);
		setSelectedOption(0);
	}
	
	/**
	 * @return Returns the default save path
	 */
	protected String getDefaultSavePath() {
		StringBuilder buffer = new StringBuilder();

		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String homeDir = System.getProperty("user.home");
		
		// Mirror download directory 
		String defaultDirectory = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_DEFAULT_DIRECTORY, null);
		if (defaultDirectory != null) {
			defaultDirectory = defaultDirectory.replace("~", homeDir);
			buffer.append(defaultDirectory);
		}
		// Default mirror download directory name
		else {
			buffer.append(homeDir);
			buffer.append(System.getProperty("file.separator"));
			buffer.append("Downloads");
			buffer.append(System.getProperty("file.separator"));

			buffer.append(InstallUtils.makeFileNameSafe(description.getProductId()));
			buffer.append('_');
			buffer.append(description.getProductVersionString());
		}
		
		Path path = new Path(buffer.toString());
		return path.toOSString();
	}
	
	/**
	 * Sets the selected option.
	 * <ul>
	 * <li>0 = Install</li>
	 * <li>1 = Install and save</li>
	 * <li>2 = Install from saved</li>
	 * </ul>
	 * 
	 * @param option Index of selected option
	 */
	public void setSelectedOption(int option) {
		this.selectedOption = option;
	}
	
	/**
	 * @return The index of the selected option
	 */
	public int getSelectedOption() {
		return selectedOption;
	}
	
	/**
	 * Sets the save path.
	 * 
	 * @param savePath Save path.
	 */
	protected void setSavePath(String savePath) {
		this.savePath = savePath;
	}
	
	/**
	 * @return The save path.
	 */
	public String getSavePath() {
		return savePath;
	}
	
	/**
	 * Sets the load path.
	 * 
	 * @param loadPath Load path.
	 */
	protected void setLoadPath(String loadPath) {
		this.loadPath = loadPath;
	}
	
	/**
	 * @return The load path.
	 */
	public String getLoadPath() {
		return loadPath;
	}

	/**
	 * @return The page description text.
	 */
	protected String getPageDescription() {
		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String descriptionText = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_MESSAGE,
				InstallMessages.MirrorPage_Description);
		
		return descriptionText;
	}

	/**
	 * @return The install section text.
	 */
	protected String getInstallSectionText() {
		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String installText = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_SECTION_INSTALL,
				InstallMessages.MirrorPage_InstallSection);
		
		return installText;
	}

	/**
	 * @return The install option text.
	 */
	protected String getInstallOptionText() {
		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String installText = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_INSTALL,
				InstallMessages.MirrorPage_Install);
		
		return installText;
	}
	
	/**
	 * @return The save section text.
	 */
	protected String getSaveSectionText() {
		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String installSaveText = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_SECTION_SAVE,
				InstallMessages.MirrorPage_SaveSection);

		return installSaveText;
	}

	/**
	 * @return The save option text.
	 */
	protected String getSaveOptionText() {
		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String installSaveText = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_SAVE,
				InstallMessages.MirrorPage_Save);

		return installSaveText;
	}

	/**
	 * @return The load option text.
	 */
	protected String getLoadOptionText() {
		IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
		String installLoadText = description.getText(IInstallDescription.TEXT_MIRROR_PAGE_LOAD,
				InstallMessages.MirrorPage_Load);

		return installLoadText;
	}

	@Override
	public Control createContents(Composite parent) {
		String defaultSavePath = getDefaultSavePath();
		final int OPTION_INDENT = 
				Installer.getDefault().getImageRegistry().get(IInstallerImages.UPDATE_INSTALL).getImageData().width;
		final int EDITOR_INDENT = OPTION_INDENT + getDefaultIndent() + getDefaultIndent();

		// Main area
		area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, true));

		// Description
		descriptionLabel = new FormattedLabel(area, SWT.WRAP);
		descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		descriptionLabel.setText(getPageDescription());
		
		// Install section
		createSpacer(area);
		createSeparator(area,
				Installer.getDefault().getImageRegistry().get(IInstallerImages.UPDATE_INSTALL),
				getInstallSectionText(),
				0);
		
		// Install option
		installButton = createOptionButton(area, getInstallOptionText(), OPTION_INDENT);
		installButton.setSelection(getSelectedOption() == OPTION_INSTALL);
		installButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSelectedOption(OPTION_INSTALL);
				updateControls();
			}
		});
		
		// Install from save option
		installFromSaveButton = createOptionButton(area, getLoadOptionText(), OPTION_INDENT);
		installFromSaveButton.setSelection(getSelectedOption() == OPTION_LOAD);
		installFromSaveEditor = createOptionEditor(area, installFromSaveButton, OPTION_LOAD, defaultSavePath, EDITOR_INDENT);

		// Save section
		createSpacer(area);
		createSeparator(area,
				Installer.getDefault().getImageRegistry().get(IInstallerImages.UPDATE_FOLDER),
				getSaveSectionText(),
				0);
		
		// Save option
		saveButton = createOptionButton(area, getSaveOptionText(), OPTION_INDENT);
		saveButton.setSelection(getSelectedOption() == OPTION_SAVE);
		saveEditor = createOptionEditor(area, saveButton, OPTION_SAVE, defaultSavePath, EDITOR_INDENT);

		// Since the page contents can grow or shrink depending on what option editors are currently displayed, fix
		// the height to that with all editors visible.
		GridData areaData = new GridData(SWT.FILL, SWT.FILL, true, true);
		areaData.heightHint = area.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		area.setLayoutData(areaData);
		
		updateControls();
		
		return area;
	}

	/**
	 * Creates a button for an option.
	 * 
	 * @param parent Parent for button
	 * @param text Text for option
	 * @param indent Horizontal indent
	 * @return Option button
	 */
	protected Button createOptionButton(Composite parent, String text, int indent) {
		Button button = new Button(parent, SWT.RADIO);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = indent;
		button.setLayoutData(data);
		button.setText(text);
		
		return button;
	}
	
	/**
	 * Creates a separator line.
	 * 
	 * @param parent Parent for separator
	 * @param image Image or <code>null</code>
	 * @param text Text for separator
	 * @param indent Horizontal indentation
	 * @return Separator line
	 */
	protected Composite createSeparator(Composite parent, Image image, String text, int indent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(image == null ? 2 : 3, false);
		layout.marginHeight = layout.marginWidth = 0;
		area.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.horizontalIndent = indent;
		area.setLayoutData(data);
		
		if (image != null) {
			Label iconLabel = new Label(area, SWT.NONE);
			iconLabel.setImage(image);
			iconLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		}
		
		// Label
		FormattedLabel label = new FormattedLabel(area, SWT.NONE);
		label.setText("<big><big><big>" + text + "</big></big></big>");
		label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		
		// Separator
		Label separator = new Label(area, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		return area;
	}
	
	/**
	 * Creates an path editor group for an option.
	 * 
	 * @param parent Parent
	 * @param selectButton Button to show the editor for
	 * @param option Option index
	 * @param defaultText Default text for editor
	 * @param indent Horizontal indention
	 * @return Editor group
	 */
	protected BrowseDirectoryDefaultEditor createOptionEditor(Composite parent, Button selectButton, final int option, 
			String defaultText, int indent) {
		GridData data;
		
		final BrowseDirectoryDefaultEditor editor = new BrowseDirectoryDefaultEditor(parent, SWT.NONE, true, true, defaultText);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = indent;
		editor.setLayoutData(data);
		
		// Show the editor when the option button is selected
		selectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSelectedOption(option);
				updateControls();
				clearError();
				editor.getEditor().setFocus();
			}
		});
		editor.getEditor().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setSavePath(saveEditor.getEditor().getText());
				setLoadPath(installFromSaveEditor.getEditor().getText());

				clearError();
				updateStatus();
			}
		});
		
		return editor;
	}

	/**
	 * Creates spacing.
	 * 
	 * @param parent Parent
	 */
	protected void createSpacer(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	}

	/**
	 * Shows or hides an option editor.
	 * 
	 * @param editor Editor
	 * @param show <code>true</code> to show, <code>false</code> to hide
	 * @return <code>true</code> if the layout requires an update
	 */
	protected boolean showOptionEditor(BrowseDirectoryDefaultEditor editor, boolean show) {
		GridData data = (GridData)editor.getLayoutData();
		boolean result = (data.exclude == !show);
		data.exclude = !show;
		editor.setLayoutData(data);
		editor.setSize(0, 0);
		
		return result;
	}
	
	/**
	 * Updates the page controls.
	 */
	protected void updateControls() {
		boolean updateLayout = false;

		// Show save option editor?
		if (showOptionEditor(installFromSaveEditor, installFromSaveButton.getSelection())) {
			updateLayout = true;
		}
		// Show save option editor?
		if (showOptionEditor(saveEditor, saveButton.getSelection())) {
			updateLayout = true;
		}
		
		updateStatus();
		
		// Update layout if required
		if (updateLayout) {
			area.layout(true);
		}
	}

	/**
	 * Updates the page status with any warning or error.
	 */
	protected void updateStatus() {
		if (!isConsoleMode()) {
			ArrayList<IStatus> status = new ArrayList<IStatus>();
			// There is an error
			if (errorText != null) {
				// Auto-update in case the error condition is corrected
				startAutoUpdate();
				status.add(new Status(IStatus.ERROR, Installer.ID, errorText));
			}
			// There may be warnings
			else {
				stopAutoUpdate();
				String[] warnings = getWarnings();
				for (String warning : warnings) {
					status.add(new Status(IStatus.WARNING, Installer.ID, warning));
				}
			}
			
			if (status.size() > 0) {
				showStatus(status.toArray(new IStatus[status.size()]));
			}
			else {
				hideStatus();
			}
			
			setPageComplete(errorText == null);
		}
	}

	@Override
	protected void autoUpdate() {
		// Re-validate to possibly clear any errors for conditions that have been corrected
		validate();
	}

	/**
	 * Clears any error.
	 */
	protected void clearError() {
		errorText = null;
		updateStatus();
	}
	
	/**
	 * @return <code>true</code>if the save directory does not exist or is empty.
	 */
	protected boolean checkSaveDirectoryEmpty() {
		boolean empty = true;
		File saveFile = new File(getSavePath());
		if (saveFile.exists()) {
			empty = saveFile.listFiles().length == 0;
		}
		
		return empty;
	}
	
	/**
	 * @return Any error for the current settings or <code>null</code>.
	 */
	protected String getError() {
		String error = null;

		int option = getSelectedOption();
		if (option == OPTION_LOAD) {
			File loadFile = new File(getLoadPath());
			if (!loadFile.exists()) {
				error = InstallMessages.Error_DirectoryNotExist;
			}
		}
		else if (option == OPTION_SAVE) {
			if (!checkSaveDirectoryEmpty()) {
				error = InstallMessages.Error_OverwriteDirectory;
			}
		}
		
		return error;
	}
	
	/**
	 * @return Any warnings for the current settings or <code>null</code>.
	 */
	protected String[] getWarnings() {
		ArrayList<String> warnings = new ArrayList<String>();
		int option = getSelectedOption();
		
		if (option == OPTION_LOAD) {
			warnings.add(InstallMessages.Error_MirrorSelectedLoad);
		}
		else if (option == OPTION_SAVE) {
			warnings.add(InstallMessages.Error_MirrorSelectedSave);
		}
		
		return warnings.toArray(new String[warnings.size()]);
	}
	
	@Override
	public boolean validate() {
		setSavePath(saveEditor.getEditor().getText());
		setLoadPath(installFromSaveEditor.getEditor().getText());

		errorText = getError();
		
		updateStatus();
		
		return (errorText == null);
	}

	@Override
	protected void saveSetup(IInstallData data) throws CoreException {
		if (!isConsoleMode()) {
			if (installButton.getSelection()) {
				setSelectedOption(OPTION_INSTALL);
			}
			else if (installFromSaveButton.getSelection()) {
				setSelectedOption(OPTION_LOAD);
			}
			else if (saveButton.getSelection()) {
				setSelectedOption(OPTION_SAVE);
			}
		}

		final Exception[] error = new Exception[] { null };
		
		// Handle selected option
		int option = getSelectedOption();
		// Install from mirror
		if (option == OPTION_LOAD) {
			Installer.getDefault().getInstallManager().setSourceLocation(new Path(getLoadPath()));
		}
		// Save to mirror
		else if (option == OPTION_SAVE) {
			runOperation(InstallMessages.PreparingInstall, new Runnable() {
				@Override
				public void run() {
					try {
						Installer.getDefault().getInstallManager().setMirrorLocation(new Path(getSavePath()), null);
					} catch (Exception e) {
						error[0] = e;
					}
				}
			});
			
			if (error[0] != null) {
				Installer.fail(error[0]);
			}
			
			// Update the container buttons to show mirror operation instead of install
			if (!isConsoleMode()) {
				getContainer().updateButtons();
			}
		}
	}

	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {

		String response = null;
		
		// Initial prompt
		if (input == null) {
			consoleState = 0;

			String prompt = formatConsoleMessage(getPageDescription()) + "\n" + InstallMessages.ConsoleDoPrompt;
			consoleOptionPrompter = new ConsoleListPrompter<Integer>(prompt, true);
			// Install option
			consoleOptionPrompter.addItem(getInstallOptionText(), OPTION_INSTALL);
			// Load option
			consoleOptionPrompter.addItem(getLoadOptionText(), OPTION_LOAD);
			// Save option
			consoleOptionPrompter.addItem(getSaveOptionText(), OPTION_SAVE);
			// Default to install
			consoleOptionPrompter.setDefaultItem(0);
		}

		// Prompt to update or use mirror
		if (consoleState == 0) {
			response = consoleOptionPrompter.getConsoleResponse(input);
			if (response == null) {
				ArrayList<Integer> options = new ArrayList<Integer>();
				consoleOptionPrompter.getSelectedData(options);
				if (options.size() > 0) {
					int option = options.get(0);
					setSelectedOption(option);

					// Display any warnings
					String[] warnings = getWarnings();
					if (warnings.length > 0) {
						for (String warning : warnings) {
							printConsole(warning);
						}
					}
					
					// Move to next prompt if required
					if (option != OPTION_INSTALL) {
						consoleState ++;
						input = null;
						
						// Location prompter
						consoleLocationPrompter = new ConsoleInputPrompter(
								getSelectedOption() == OPTION_SAVE ? 
										InstallMessages.MirrorPage_MirrorPathSavePrompt : 
										InstallMessages.MirrorPage_MirrorPathLoadPrompt,
								getSelectedOption() == OPTION_SAVE ? 
										getSavePath() : 
										getLoadPath());
					}
				}
			}
		}
		// Prompt for mirror save or load location
		if (consoleState == 1) {
			response = consoleLocationPrompter.getConsoleResponse(input);
			
			if (response == null) {
				if (getSelectedOption() == OPTION_SAVE) {
					setSavePath(consoleLocationPrompter.getResult());
				}
				else {
					setLoadPath(consoleLocationPrompter.getResult());
				}
				
				String error = getError();
				if (error != null) {
					printConsole(error);
					response = consoleLocationPrompter.getConsoleResponse(null);
				}
			}
		}
		
		return response;
	}

	@Override
	public String getInstallSummary() {
		if (getSelectedOption() == OPTION_SAVE) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("<b>");
			buffer.append(getSaveOptionText());
			buffer.append("</b>\n");
			buffer.append(getSavePath());
			buffer.append("\n\n");
			
			return buffer.toString();
		}
		else {
			return null;
		}
	}
}
