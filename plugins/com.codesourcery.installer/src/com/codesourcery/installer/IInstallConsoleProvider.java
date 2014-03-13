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
package com.codesourcery.installer;

/**
 * Install pages that support console mode should implement this interface.
 */
public interface IInstallConsoleProvider {
	/**
	 * Provides page text for console installation.
	 * This method should process the input and return the text to display
	 * in the console.
	 * This method will be called initially with <code>null</code> for the input.
	 * This method should return the initial information for display along with
	 * a prompt.
	 * Subsequent calls will provide the text entered by the user.
	 * This method should return <code>null</code> when the page is complete and
	 * no further input is required.
	 * If the page returns more text than can be displayed, it will be 
	 * paginated.
	 * Helper classes in the <code>com.codesourcery.installer.console</code> can
	 * be used to build console responses.
	 * 
	 * @param input <code>null</code> on first call.  Text entered by user
	 * on subsequent calls.
	 * @return Text to display in console.
	 * @throws IllegalArgumentException when the provided input is not valid.
	 * The installer will print the exception message and terminate
	 * .
	 * @see {@link com.codesourcery.installer.console.ConsoleListPrompter}
	 * @see {@link com.codesourcery.installer.console.ConsoleYesNoPrompter}
	 */
	public String getConsoleResponse(String input) throws IllegalArgumentException;
}
