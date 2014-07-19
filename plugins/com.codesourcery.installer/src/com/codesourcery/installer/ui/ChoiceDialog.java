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

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.installer.ui.InfoButton;
import com.codesourcery.installer.ui.InfoButton.ElementColor;


/**
 * A dialog that displays a list of choices.  This dialog also provides
 * console support.
 */
public class ChoiceDialog extends Dialog implements IInstallConsoleProvider {
	/** Dialog element colors */
	private Color[] colors = new Color[ElementColor.values().length];
	/** Dialog background color */
	private Color backgroundColor;
	/** Prompt foreground color*/
	private Color promptColor;
	/** Dialog title font */
	private Font titleFont;
	/** Choices to display */
	private ArrayList<Option> options = new ArrayList<Option>();
	/** Dialog title */
	private String title;
	/** Dialog title text */
	private String titleText;
	/** Dialog prompt text */
	private String promptText;
	/** Default option or <code>null</code>*/
	private Option defaultOption;
	/** Identifier of currently selected option or <code>null</code>*/
	private String selectedOption;
	/** Console list prompter */
	private ConsoleListPrompter<String> consoleList;
	/** Choice widths */
	private int width = -1;

	/**
	 * Constructor
	 * 
	 * @param parentShell Parent shell or <code>null</code> to use for console
	 * @param title Title for dialog
	 */
	public ChoiceDialog(Shell parentShell, String title) {
		super(parentShell);
		this.title = title;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	/**
	 * Sets the text displayed in the title.
	 * 
	 * @param titleText Title text or <code>null</code>
	 */
	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}
	
	/**
	 * Returns the text displayed in the title.
	 * 
	 * @return Title text or <code>null</code>
	 */
	public String getTitleText() {
		return titleText;
	}
	
	/**
	 * Sets the prompt text.
	 * 
	 * @param promptText Prompt text
	 */
	public void setPromptText(String promptText) {
		this.promptText = promptText;
	}
	
	/**
	 * Returns the prompt text.
	 * 
	 * @return Prompt text
	 */
	public String getPromptText() {
		return promptText;
	}

	/**
	 * Sets the maximum width of the dialog.
	 * 
	 * @param width Width
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	
	/**
	 * Returns the maximum width of the dialog.
	 * 
	 * @return Width
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * Adds a new option to display on the dialog.
	 * 
	 * @param id Identifier for the option
	 * @param image Image for the option or <code>null</code>
	 * @param label Label for the option or <code>null</code>
	 * @param description Description for the option or <code>null</code>
	 */
	public void addOption(String id, Image image, String label, String description) {
		options.add(new Option(id, image, label, description));
	}
	
	/**
	 * Returns an option.
	 * 
	 * @param id Option identifier
	 * @return Option or <code>null</code>
	 */
	private Option getOption(String id) {
		Option foundOption = null;
		for (Option option : options) {
			if (option.getId().equals(id)) {
				foundOption = option;
				break;
			}
		}
		
		return foundOption;
	}
	
	/**
	 * Sets the default option.
	 * 
	 * @param id Identifier of option to set as default
	 */
	public void setDefaultOption(String id) {
		defaultOption = getOption(id);
	}
	
	/**
	 * Returns the selected option.
	 * 
	 * @return Option identifier or <code>null</code>
	 */
	public String getSelection() {
		return selectedOption;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		// Set dialog icon
		newShell.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.TITLE_ICON));
		// Set title
		newShell.setText(title);
	}

	@Override
	protected Control createContents(Composite parent) {
		GridLayout layout;
		GridData data;

		if (width == -1) {
			GC gc = new GC(parent);
			gc.setFont(JFaceResources.getDialogFont());
			FontMetrics fontMetrics = gc.getFontMetrics();
			gc.dispose();
			width = convertHorizontalDLUsToPixels(fontMetrics, 185);
		}
		
		// Initialize element colors
		initColors();

		// Main area
		Composite area = new Composite(parent, SWT.NONE);
		layout = new GridLayout(1, true);
		layout.marginHeight = 5;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		area.setLayout(layout);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Title text
		if (getTitleText() != null) {
			// Title text font
			FontData fontData = JFaceResources.getDialogFont().getFontData()[0];
			fontData.setHeight(fontData.getHeight() + 2);
			titleFont = new Font(getShell().getDisplay(), fontData);
	
			// Title text area
			Composite titleArea = new Composite(area, SWT.NONE);
			layout = new GridLayout(1, true);
			titleArea.setLayout(layout);
			titleArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			// Title text label
			Label label = new Label(titleArea, SWT.WRAP);
			data = new GridData(SWT.FILL, SWT.CENTER, true, false);
			data.widthHint = getWidth();
			label.setLayoutData(data);
			label.setText(getTitleText());
			label.setFont(titleFont);
			
			// Separator
			label = new Label(titleArea, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			// Prompt
			if (getPromptText() != null) {
				label = new Label(titleArea, SWT.WRAP);
				label.setForeground(promptColor);
				data = new GridData(SWT.FILL, SWT.CENTER, true, false);
				data.widthHint = getWidth();
				label.setLayoutData(data);
				label.setText(getPromptText());
				label.setFont(titleFont);
				label.setForeground(promptColor);
			}
		}

		// Add choices
		for (Option option : options) {
			InfoButton button = new InfoButton(area, SWT.RADIO);
			button.setData(option);
			button.setImage(option.getImage());
			button.setText(option.getLabel());
			button.setDescription(option.getDescription());
			data = new GridData(SWT.FILL, SWT.CENTER, true, false);
			data.widthHint = getWidth();
			button.setLayoutData(data);
			// Set label colors
			for (int index = 0; index < colors.length; index++) {
				button.setColor(ElementColor.values()[index], colors[index]);
			}
			
			// Default option
			if (option.equals(defaultOption)) {
				button.setSelection(true);
				selectedOption = option.getId();
			}
			
			// Selected option listener
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					InfoButton button = (InfoButton)e.widget;
					selectedOption = ((Option)button.getData()).getId();
					updateButtons();
				}
			});
			// Close dialog on option double-click
			button.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					InfoButton button = (InfoButton)e.widget;
					selectedOption = ((Option)button.getData()).getId();
					updateButtons();
					okPressed();
				}
			});
		}

		applyDialogFont(area);

		// Set dialog background color
		Composite contents = (Composite)super.createContents(parent);
		setBackground(parent, backgroundColor);
		return contents;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (getSelection() != null) {
			// Select OK by default
			getButton(IDialogConstants.OK_ID).setFocus();
		}
		else {
			// Select Cancel by default
			getButton(IDialogConstants.CANCEL_ID).setFocus();
		}
		updateButtons();
	}

	/**
	 * Updates the state of buttons.
	 */
	private void updateButtons() {
		// Set OK enabled only if an option is selected
		getButton(IDialogConstants.OK_ID).setEnabled(getSelection() != null);
	}

	/**
	 * Initializes dialog colors.
	 */
	private void initColors() {
		promptColor = new Color(getShell().getDisplay(), new RGB(45, 137, 239));
		// Background
		backgroundColor = getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE);
		Color selectBackground = new Color(getShell().getDisplay(), blendRGB(new RGB(45, 137, 239), backgroundColor.getRGB(), 20));
		// Option selection
		colors[ElementColor.selectBackground.ordinal()] = selectBackground;
		// Option hover
		colors[ElementColor.hoverBackground.ordinal()] = new Color(getShell().getDisplay(), blendRGB(selectBackground.getRGB(), backgroundColor.getRGB(), 15));
		// Option label
		Color foreground = new Color(getShell().getDisplay(), new RGB(0, 0, 0));
		colors[ElementColor.label.ordinal()] = foreground;
		colors[ElementColor.selectedLabel.ordinal()] = foreground;
		// Option description
		Color descriptionForeground = new Color(getShell().getDisplay(), blendRGB(foreground.getRGB(), backgroundColor.getRGB(), 50));
		colors[ElementColor.description.ordinal()] = descriptionForeground;
		colors[ElementColor.selectedDescription.ordinal()] = descriptionForeground;
	}
	
	@Override
	public boolean close() {
		// Dispose of element colors
		if (colors != null) {
			for (Color color : colors) {
				color.dispose();
			}
			colors = null;
		}
		// Dispose of title color
		if (promptColor != null) {
			promptColor.dispose();
			promptColor = null;
		}
		// Dispose of title font
		if (titleFont != null) {
			titleFont.dispose();
			titleFont = null;
		}
		return super.close();
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
	
	@Override
	protected Control createButtonBar(Composite parent) {
		Composite bar = (Composite)super.createButtonBar(parent);
		// Change bar to center on dialog
		bar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER
				| GridData.VERTICAL_ALIGN_CENTER));
		
		return bar;
	}
	
	/**
	 * Sets the background color for a composite and it's children.
	 * 
	 * @param area Composite
	 * @param color Background color
	 */
	private void setBackground(Control area, Color color) {
		area.setBackground(color);
		if (area instanceof Composite) {
			Control[] children = ((Composite)area).getChildren();
			for (Control child : children) {
				setBackground(child, color);
			}
		}
	}

	/**
	 * Dialog option
	 */
	private class Option {
		/** Option identifier */
		private String id;
		/** Image for the option */
		private Image image;
		/** Label for the option */
		private String label;
		/** Description for the label */
		private String description;
		
		/**
		 * Constructor
		 * 
		 * @param id Option identifier
		 * @param image Option image
		 * @param label Option label
		 * @param description Option description
		 */
		public Option(String id, Image image, String label, String description) {
			this.id = id;
			this.image = image;
			this.label = label;
			this.description = description;
		}
		
		/**
		 * Returns the option identifier.
		 * 
		 * @return Identifier
		 */
		public String getId() {
			return id;
		}
		
		/**
		 * Returns the option image.
		 * 
		 * @return Image
		 */
		public Image getImage() {
			return image;
		}
		
		/**
		 * Return the option label.
		 * 
		 * @return Label
		 */
		public String getLabel() {
			return label;
		}
		
		/**
		 * Returns the option description.
		 * 
		 * @return Description
		 */
		public String getDescription() {
			return description;
		}
	}

	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;
		
		// Initial prompt
		if (input == null) {
			consoleList = new ConsoleListPrompter<String>(getTitleText(), true);
			
			// Add options
			for (Option option : options) {
				StringBuffer itemName = new StringBuffer();
				if (option.getLabel() != null)
					itemName.append(option.getLabel());
				if (option.getDescription() != null) {
					if (itemName.length() > 0)
						itemName.append(" - ");
					itemName.append(option.getDescription());
				}
				consoleList.addItem(itemName.toString(), option.getId());
			}
		}
		// Get response
		response = consoleList.getConsoleResponse(input);
		// Handle selection
		if (response == null) {
			ArrayList<String> selection = new ArrayList<String>();
			consoleList.getSelectedData(selection);
			if (selection.size() > 0) {
				selectedOption = selection.get(0);
			}
		}		
		
		return response;
	}
}
