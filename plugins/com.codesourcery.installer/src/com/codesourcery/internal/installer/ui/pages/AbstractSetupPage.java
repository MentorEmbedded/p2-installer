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
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.ui.InfoButton;
import com.codesourcery.installer.ui.InfoList;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.ui.InstallWizard;
import com.codesourcery.internal.installer.ui.InstallWizardDialog;

/**
 * Abstract page for setup options.  Subclasses should provide {@link #createOptions()}
 * to create the options for selection.
 */
public abstract class AbstractSetupPage extends InstallWizardPage implements IInstallConsoleProvider {
	/** Title font */
	private Font titleFont;
	/** Currently selection */
	private Option selection;
	/** Console list prompter */
	private ConsoleListPrompter<Option> consoleList;
	/** Options list */
	private InfoList optionsList;
	/** Title message */
	private String titleMessage;
	/** Prompt message */
	private String prompt;
	/** <code>true</code> to draw border around options */
	private boolean border = false;
	/** Options */
	private ArrayList<Option> options = new ArrayList<Option>();
	/** Option buttons */
	private HashMap<Option, InfoButton> buttons = new HashMap<Option, InfoButton>();
	/** <code>true</code> if page has been saved */
	private boolean saved = false;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param titleMessage or <code>null</code>
	 * @param prompt or <code>null</code>
	 */
	protected AbstractSetupPage(String pageName, String titleMessage, String prompt) {
		super(pageName, "");
		
		this.titleMessage = titleMessage;
		this.prompt = prompt;

		setPageNavigation(SWT.NONE);
		setPageComplete(false);
	}

	/**
	 * This method is called to create all options.
	 * Subclasses should call {@link #addOption(Option)} to add a new option.
	 */
	protected abstract void createOptions();

	/**
	 * This method is called to save the selected option.
	 * 
	 * @param selectedOption Selected option
	 */
	protected abstract void saveOption(Option selectedOption);
	
	/**
	 * Sets a border around the options area.
	 * 
	 * @param border <code>true</code> to border
	 */
	protected void setBorder(boolean border) {
		this.border = border;
	}
	
	/**
	 * Returns if there is a border around the options area.
	 * 
	 * @return <code>true</code> if border
	 */
	protected boolean getBorder() {
		return border;
	}
	
	/**
	 * Returns the title message.
	 * 
	 * @return Title message or <code>null</code>
	 */
	protected String getTitleMessage() {
		return titleMessage;
	}
	
	/**
	 * Returns the prompt message.
	 * 
	 * @return Prompt message or <code>null</code>
	 */
	protected String getPrompt() {
		return prompt;
	}
	
	@Override
	public void dispose() {
		if (titleFont != null) {
			titleFont.dispose();
			titleFont = null;
		}
		
		super.dispose();
	}

	/**
	 * Returns the current selection.
	 * 
	 * @return Selection or <code>null</code>
	 */
	protected Option getSelection() {
		return selection;
	}
	
	/**
	 * Sets the current selection.
	 * 
	 * @param selection Selection or <code>null</code>
	 */
	protected void setSelection(Option selection) {
		this.selection = selection;
		if (selection != null) {
			setPageComplete(true);
		}
	}
	
	/**
	 * Returns a plug-in image.
	 * 
	 * @param id Image identifier
	 * @return Image or <code>null</code> if id is <code>null</code> or installer
	 * is running console mode
	 */
	protected Image getImage(String id) {
		if ((id != null) && !isConsoleMode()) {
			return Installer.getDefault().getImageRegistry().get(id);
		}
		else {
			return null;
		}
	}
	
	@Override
	public Control createContents(Composite parent) {
		// Main area
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, true));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Create title font
		FontData fontData = getFont().getFontData()[0];
		fontData.setHeight(fontData.getHeight() + 2);
		titleFont = new Font(getShell().getDisplay(), fontData);

		// Title
		if (getTitleMessage() != null) {
			Label titleLabel = new Label(area, SWT.NONE);
			titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			//titleLabel.setFont(titleFont);
			titleLabel.setText(getTitleMessage());
		}
		
		// Spacing after title
		Label spacing = new Label(area, SWT.NONE);
		spacing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		// Button area
		final Composite buttonArea = new Composite(area, SWT.NONE);
		GridLayout buttonLayout = new GridLayout(1, true);
		buttonLayout.verticalSpacing = 0;
		buttonArea.setLayout(buttonLayout);
		buttonArea.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

		// Prompt
		if (getPrompt() != null) {
			Label promptLabel = new Label(buttonArea, SWT.NONE);
			promptLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
			promptLabel.setText(getPrompt());
			promptLabel.setFont(getBoldFont());
		}
		
		// Spacing after prompt
		spacing = new Label(buttonArea, SWT.NONE);
		spacing.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Create items area
		int listStyle = SWT.V_SCROLL;
		if (getBorder())
			listStyle |= SWT.BORDER;
		optionsList = new InfoList(buttonArea, listStyle);
		optionsList.setLabelFont(titleFont);
		optionsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// Set option on selection
		optionsList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				InfoButton button = (InfoButton)e.widget;
				setSelection((Option)button.getData());
			}
		});
		// Move to next page on double-click
		optionsList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				((InstallWizardDialog)getContainer()).nextPressed();
			}
		});
		
		// Create choices
		createOptions();

		return area;
	}
	
	/**
	 * Blends two RGB values using the provided ratio. 
	 * 
	 * @param c1 First RGB value
	 * @param c2 Second RGB value
	 * @param ratio Percentage of the first RGB to blend with 
	 * second RGB (0-100)
	 * 
	 * @return The RGB value of the blended color
	 */
	public static RGB blendRGB(RGB c1, RGB c2, int ratio) {
		ratio = Math.max(0, Math.min(255, ratio));

		int r = Math.max(0, Math.min(255, (ratio * c1.red + (100 - ratio) * c2.red) / 100));
		int g = Math.max(0, Math.min(255, (ratio * c1.green + (100 - ratio) * c2.green) / 100));
		int b = Math.max(0, Math.min(255, (ratio * c1.blue + (100 - ratio) * c2.blue) / 100));
		
		return new RGB(r, g, b);
	}

	/**
	 * Returns the options.
	 * 
	 * @return Options
	 */
	protected Option[] getOptions() {
		return options.toArray(new Option[options.size()]);
	}
	
	/**
	 * Adds an option.
	 * 
	 * @param option Option to add
	 */
	protected void addOption(Option option) {
		// Add option
		options.add(option);
		if (!isConsoleMode()) {
			InfoButton item = optionsList.addItem(option.getImage(), option.getLabel(), option.getDescription());
			item.setData(option);
		}
	}
	
	/**
	 * Selects an option.
	 * 
	 * @param option Option
	 */
	protected void selectOption(Option option) {
		if (buttons.containsKey(option)) {
			setSelection(option);
			buttons.get(option).setSelection(true);
		}
	}

	@Override
	public boolean isSupported() {
		return true;
	}
	
	@Override
	public void saveInstallData(IInstallData data) {
		// Setup page only saves once
		if (!saved) {
			saved = true;
			
			Option selection = getSelection();
			if (selection != null) {
				saveOption(selection);
			}
			// Update UI if not in console mode
			if (!isConsoleMode()) {
				// Update install button text
				getContainer().updateButtons();
				
				// Setup page navigation
				((InstallWizard)getWizard()).setupPageNavigation();
			}
		}
	}
	
	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;
		
		// Initial prompt
		if (input == null) {
			consoleList = new ConsoleListPrompter<Option>(getTitleMessage(), true);
			
			// Add choices
			createOptions();
			for (Option option : options) {
				StringBuffer itemName = new StringBuffer();
				itemName.append(option.getLabel());
				itemName.append(" (");
				itemName.append(option.getDescription());
				itemName.append(')');
				consoleList.addItem(itemName.toString(), option);
			}
		}
		
		// Get response
		response = consoleList.getConsoleResponse(input);
		if (response == null) {
			ArrayList<Option> selection = new ArrayList<Option>();
			consoleList.getSelectedData(selection);
			if (selection.size() > 0) {
				setSelection(selection.get(0));
			}
		}
		
		return response;
	}

	/**
	 * Page option
	 */
	protected class Option {
		/** Option data */
		private Object data;
		/** Option image */
		private Image image;
		/** Option label */
		private String label;
		/** Option description */
		private String description;
		
		/**
		 * Constructor
		 * 
		 * @param data Option data
		 * @param image Image or <code>null</code>
		 * @param label Image label
		 * @param description Image description or <code>null</code>
		 */
		public Option(Object data, Image image, String label, String description) {
			this.data = data;
			this.image = image;
			this.label = label;
			this.description = description;
		}
		
		/**
		 * Returns the choice data.
		 * 
		 * @return Data
		 */
		public Object getData() {
			return data;
		}
		
		/**
		 * Returns the image.
		 * 
		 * @return Identifier
		 */
		public Image getImage() {
			return image;
		}
		
		/**
		 * Returns the label.
		 * 
		 * @return Label
		 */
		public String getLabel() {
			return label;
		}
		
		/**
		 * Returns the description.
		 * 
		 * @return Description
		 */
		public String getDescription() {
			return description;
		}
	}
}
