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

import org.eclipse.swt.widgets.Shell;

/**
 * The <code>com.mentor.installer.platformActionsProvider</code> extension point
 * can be used to contribute platform specific operations.
 */
public interface IInstallPlatformActions {
	/**
	 * Called to bring a shell to the front (top) above all other shells.
	 * 
	 * @param shell Shell to bring to front
	 */
	public void bringToFront(Shell shell);
}
