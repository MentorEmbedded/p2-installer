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
import com.codesourcery.installer.console.ConsoleYesNoPrompter;

/**
 * An abstract wizard install page that displays a choice of 
 * two options.
 * The choice is stored as a Boolean (true or false) in the property
 * specified.
 * The page supports console mode.
 */
public class AbstractYesNoInstallPage extends InstallWizardPage implements
IInstallSummaryProvider, IInstallConsoleProvider {
	/** Message label */
	private String messageLabel;
	/** Yes message */
	private String yesMessage;
	/** Yes summary */
	private String yesSummary;
	/** Yes button */
	private Button yesButton;
	/** No message */
	private String noMessage;
	/** No summary */
	private String noSummary;
	/** No button */
	private Button noButton;
	/** Name of property to store chosen value */
	private String propertyName;
	/** Console prompt message */
	private String consolePrompt;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param messageLabel Message label
	 * @param yesMessage Message to display for yes selection
	 * @param noMessage Message to display for no selection
	 * @param yesSummary Summary message if yes chosen or <code>null</code> to
	 * not display a summary
	 * @param noSummary Summary message if no chosen or <code>null</code> to
	 * not display a summary
	 * @param consolePrompt Console prompt message or <code>null</code> to not
	 * support console interaction
	 * @param propertyName Name of property to store chosen value in 
	 * <code>IInstallData</code>
	 */
	protected AbstractYesNoInstallPage(String pageName, String title, 
			String messageLabel, String yesMessage, String noMessage,
			String yesSummary, String noSummary, String consolePrompt, 
			String propertyName) {
		super(pageName, title);
		
		this.messageLabel = messageLabel;
		this.yesMessage = yesMessage;
		this.yesSummary = yesSummary;
		this.noMessage = noMessage;
		this.noSummary = noSummary;
		this.consolePrompt = consolePrompt;
		this.propertyName = propertyName;
	}

	/**
	 * Returns the message label.
	 * 
	 * @return Message label
	 */
	protected String getMessageLabel() {
		return messageLabel;
	}
	
	/**
	 * Returns the yes message.
	 * 
	 * @return Yes message
	 */
	protected String getYesMessage() {
		return yesMessage;
	}
	
	/**
	 * Returns the yes summary message.
	 * 
	 * @return Yes summary message or <code>null</code>
	 */
	protected String getYesSummary() {
		return yesSummary;
	}
	
	/**
	 * Returns the no message.
	 * 
	 * @return No message
	 */
	protected String getNoMessage() {
		return noMessage;
	}
	
	/**
	 * Returns the no summary message.
	 * 
	 * @return No summary message or <code>null<code>
	 */
	protected String getNoSummary() {
		return noSummary;
	}
	
	/**
	 * Returns the console prompt.
	 * 
	 * @return Console prompt or <code>null</code>
	 */
	protected String getConsolePrompt() {
		return consolePrompt;
	}

	/**
	 * Returns the name of the property to store the chosen value in.
	 * 
	 * @return Property name
	 */
	protected String getPropertyName() {
		return propertyName;
	}
	
	/**
	 * Returns the chosen value.
	 * 
	 * @return Chosen value
	 */
	protected boolean getValue() {
		return getInstallData().getBooleanProperty(getPropertyName());
	}
	
	/**
	 * Sets the chosen value.
	 * 
	 * @param value Chosen value
	 */
	protected void setValue(boolean value) {
		getInstallData().setProperty(getPropertyName(), value);
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
		messageLabel.setText(getMessageLabel());

		// Spacing
		Label spacing = new Label(area, SWT.NONE);
		spacing.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
		
		// No button
		noButton = new Button(area, SWT.RADIO);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
		data.horizontalIndent = getDefaultIndent();
		noButton.setLayoutData(data);
		noButton.setText(getNoMessage());
		noButton.setSelection(!getValue());
		noButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (noButton.getSelection()) {
					setValue(false);
				}
			}
		});
		
		// Yes button
		yesButton = new Button(area, SWT.RADIO);
		data = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1);
		data.horizontalIndent = getDefaultIndent();
		yesButton.setLayoutData(data);
		yesButton.setText(getYesMessage());
		yesButton.setSelection(getValue());
		yesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (yesButton.getSelection()) {
					setValue(true);
				}
			}
		});

		return area;
	}

	
	
	@Override
	public void setActive(IInstallData data) {
		super.setActive(data);
		
		if (!isConsoleMode()) {
			noButton.setSelection(!getValue());
			yesButton.setSelection(getValue());
		}
	}

	@Override
	public void saveInstallData(IInstallData data) throws CoreException {
		// Save value in property
		data.setProperty(getPropertyName(), getValue());
	}
	
	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;
		// Show console prompt
		if (getConsolePrompt() != null) {
			ConsoleYesNoPrompter prompt = new ConsoleYesNoPrompter(formatConsoleMessage(getMessageLabel()), 
					getConsolePrompt(), true);
			response = prompt.getConsoleResponse(input);
			if (response == null) {
				setValue(prompt.getResult());
			}
		}
		
		return response;
	}

	@Override
	public String getInstallSummary() {
		// Show summary information
		if ((getYesSummary() != null) && (getNoSummary() != null)) {
			return (getValue() ? getYesSummary() : getNoSummary()) + "\n\n";
		}
		else {
			return null;
		}
	}
}
