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

import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * Install page that displays a welcome message.
 * This page supports console.
 */
public class WelcomePage extends InformationPage implements IInstallConsoleProvider {
	/** Console message */
	private String consoleMessage;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param productName Product name
	 */
	public WelcomePage(String pageName, String productName) {
		super(pageName, InstallMessages.WelcomePageTitle);
		
		// Set welcome information
		String welcomeMessage = NLS.bind(InstallMessages.welcomeMessage0, productName );
		setInformation(welcomeMessage);
		// Console message
		consoleMessage = NLS.bind(InstallMessages.welcomeMessageConsole0, productName);
	}

	@Override
	public String getConsoleResponse(String input) throws IllegalArgumentException {
		if ((input == null) || (input.length() > 0)) {
			return consoleMessage;
		}
		else {
			return null;
		}
	}
}
