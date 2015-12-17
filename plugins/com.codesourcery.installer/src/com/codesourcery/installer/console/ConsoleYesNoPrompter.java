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
package com.codesourcery.installer.console;

import java.text.MessageFormat;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * This class can be used to provide a yes/no prompt in the console.
 * 
 * Example:
 *   public String getConsoleResponse(String input) throws IllegalArgumentException {
 *     ConsoleYesNoPrompter prompter = 
 *       new ConsoleYesNoPrompter("An option can be enabled.", 
 *       "DO YOU WANT TO ENABLE THE OPTION?", false);
 *     String response = prompter.getResponse(input);
 *     if (response == null) {
 *       if (response.getResult()) {
 *         // Handle result...
 *       }
 *     }
 *     
 *     return response;
 *   }
 *   
 */
public class ConsoleYesNoPrompter implements IInstallConsoleProvider {
	/** Description message */
	private String message;
	/** Prompt message */
	private String prompt;
	/** Result of prompt */
	private boolean result;
	/** Default value or <code>null</code> if there is no default value */
	private Boolean defaultValue;
	/** First letter of Yes input */
	private static final String YES = InstallMessages.Yes.substring(0, 1);
	/** First letter of No input */
	private static final String NO = InstallMessages.No.substring(0, 1);
	
	/**
	 * Constructor
	 * No default is supplied so the user must enter Yes or No and will not
	 * be allowed to press ENTER.
	 * 
	 * @param message Message to display
	 * @param prompt Prompt to display or <code>null</code>
	 */
	public ConsoleYesNoPrompter(String message, String prompt) {
		this.message = message;
		this.prompt = prompt;
		this.result = false;
	}

	/**
	 * Constructor
	 * The user is prompted to enter Yes or No or press ENTER to use the default
	 * value.
	 * 
	 * @param message Message to display
	 * @param prompt Prompt to display or <code>null</code>
	 * @param defaultValue Default value
	 */
	public ConsoleYesNoPrompter(String message, String prompt, boolean defaultValue) {
		this(message, prompt);
		this.result = defaultValue;
		this.defaultValue = new Boolean(defaultValue);
	}
	
	/**
	 * Returns the message.
	 * 
	 * @return Message
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * Returns the prompt.
	 * 
	 * @return Prompt
	 */
	public String getPrompt() {
		return prompt;
	}
	
	/**
	 * Returns the result of the prompt.
	 * 
	 * @return <code>true</code> if yes was selected, <code>false</code> if
	 * no was selected.
	 */
	public boolean getResult() {
		return result;
	}
		
	/**
	 * Returns the prompt message.  A different message is returned depending
	 * on if a default value is available and the user can press ENTER.
	 * 
	 * @return Prompt message
	 */
	protected String getPromptMessage() {
		// Default available
		if (defaultValue != null) {
			return MessageFormat.format(InstallMessages.ConsolePressEnterYesNo2, 
					defaultValue ? InstallMessages.Yes : InstallMessages.No,
					YES,
					NO);
		}
		// Default not available
		else {
			return MessageFormat.format(InstallMessages.ConsolePressYesNo1, 
					YES,
					NO);
		}
	}

	@Override
	public String getConsoleResponse(String input) throws IllegalArgumentException {
		String response = null;
		
		if (input == null) {
			response = toString();
		}
		else if (input.isEmpty()) {
			// If not default is available, the user must enter yes or no
			response = (defaultValue == null) ? getPromptMessage() : null;
		}
		else {
			input = input.toUpperCase();
			// Yes
			if (input.equals(YES) || (input.compareToIgnoreCase(InstallMessages.Yes) == 0)) {
				result = true;
			}
			// No
			else if (input.equals(NO) || (input.compareToIgnoreCase(InstallMessages.No) == 0)) {
				result = false;
			}
			// Invalid input
			else {
				response = getPromptMessage();
			}			
		}
		
		return response;
	}
	
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		if (getMessage() != null) {
			buffer.append(getMessage());
			buffer.append('\n');
		}
		if (getPrompt() != null) {
			buffer.append('\n');
			buffer.append(getPrompt());
			buffer.append('\n');
		}
		String promptMessage = getPromptMessage();
		buffer.append(promptMessage);
		
		return buffer.toString();
	}
}
