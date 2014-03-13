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
package com.codesourcery.internal.installer;

import java.security.cert.Certificate;

import org.eclipse.equinox.p2.core.UIServices;

/**
 * This class provides all p2 certificate handling and authentication.
 * For signed install units, p2 with fail without a prompter installed.
 * "One or more certificates rejected.  Cannot proceed with installation".
 * 
 * This service can be installed on the provisioning agent using,
 * <code>
 * IProvisioningAgent.registerService(UIServices.SERVICE_NAME, AthenticationService.getDefault());
 * </code>
 */
public class AuthenticationService extends UIServices {
	/** Service instance */
	private static AuthenticationService service = new AuthenticationService();
	/** Password */
	private String password;
	/** User name */
	private String userName;
	
	/**
	 * Constructor
	 */
	private AuthenticationService() {
	}
	
	/**
	 * Returns the default service.
	 * 
	 * @return Service
	 */
	public static AuthenticationService getDefault() {
		return service;
	}
	
	/**
	 * Sets the user name for authentication.
	 * 
	 * @param userName User name
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	/**
	 * Returns the user name.
	 * 
	 * @return User name
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Sets the password for authentication.
	 * 
	 * @param password Password
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * Returns the password.
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return password;
	}
	
	@Override
	public AuthenticationInfo getUsernamePassword(String location) {
		return new AuthenticationInfo(getUserName(), getPassword(), false);
	}

	@Override
	public AuthenticationInfo getUsernamePassword(String location,
			AuthenticationInfo previousInfo) {
		return new AuthenticationInfo(getUserName(), getPassword(), false);
	}

	@Override
	public TrustInfo getTrustInfo(Certificate[][] untrustedChains,
			String[] unsignedDetail) {
		final Certificate[] trusted;
		if (untrustedChains == null) {
			trusted = null;
		} else {
			trusted = new Certificate[untrustedChains.length];
			for (int i = 0; i < untrustedChains.length; i++) {
				trusted[i] = untrustedChains[i][0];
			}
		}
		return new TrustInfo(trusted, false, true);
	}
}
