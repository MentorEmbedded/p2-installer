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
package com.codesourcery.installer;

import org.eclipse.swt.widgets.Shell;

/**
 * Default implementation of <code>IInstallPlatformActions</code>.
 */
public class AbstractInstallPlatformActions implements
		IInstallPlatformActions {

	/**
	 * Required default constructor.
	 */
	public AbstractInstallPlatformActions() {
	}
	
	@Override
	public void bringToFront(Shell shell) {
		shell.forceActive();
	}
}
