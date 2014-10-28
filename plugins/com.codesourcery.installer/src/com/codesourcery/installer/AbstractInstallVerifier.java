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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
/**
 * Abstract install verifier.
 * @see {@link com.codesourcery.installer.IInstallVerifier}
 */
public abstract class AbstractInstallVerifier implements IInstallVerifier {
	@Override
	public IStatus[] verifyInstallLocation(IPath installLocation) {
		return new IStatus[0];
	}

	@Override
	public IStatus[] verifyCredentials(String username, String password) {
		return new IStatus[0];
	}
}
