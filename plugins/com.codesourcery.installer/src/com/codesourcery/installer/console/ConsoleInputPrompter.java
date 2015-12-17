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
package com.codesourcery.installer.console;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * This class can be used to provide a simple input prompt in the console.
 */
public class ConsoleInputPrompter implements IInstallConsoleProvider {
	/** Prompt text */
	private String prompt;
	/** Prompt result */
	private String result;

	/**
	 * Constructs the prompter.
	 * 
	 * @param prompt Text for the prompt.
	 * @param defaultValue Default value to return if ENTER is pressed at the prompt.
	 */
	public ConsoleInputPrompter(String prompt, String defaultValue) {
		this.prompt = prompt;
		this.result = defaultValue;
	}
	
	/**
	 * @return Returns the prompt text.
	 */
	public String getPrompt() {
		return prompt;
	}
	
	/**
	 * @return Returns the prompt result.
	 */
	public String getResult() {
		return result;
	}

	/**
	 * @return The prompt text.
	 */
	private String getPromptText() {
		StringBuilder buffer = new StringBuilder(getPrompt());
		buffer.append('\n');
		buffer.append(getResult());
		buffer.append("\n\n");
		buffer.append(InstallMessages.ConsolePressEnterOrChange);
		buffer.append("\n");
		
		return buffer.toString();
	}
	
	@Override
	public String getConsoleResponse(String input) throws IllegalArgumentException {
		String response = null;
		
		if (input == null) {
			response = getPromptText();
		}
		else if (!input.isEmpty()) {
			result = input;
			response = getPromptText();
		}
		
		return response;
	}
}
