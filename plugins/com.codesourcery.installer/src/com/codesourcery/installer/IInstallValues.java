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
 * Install data properties
 */
public interface IInstallValues {
	/**
	 * Property to specify if system PATH should be set.
	 * "true" to set PATH.
	 */
	public static final String SET_PATH = "setPATH";

	/**
	 * Property to specify if desktop short-cuts should be created.
	 * "true" to create desktop short-cuts.
	 */
	public static final String CREATE_DESKTOP_SHORTCUTS = "createDesktopShortcuts";

	/**
	 * Property to specify if program short-cuts should be created.
	 * "true" to create program short-cuts.
	 */
	public static final String CREATE_PROGRAM_SHORTCUTS = "createProgramShortcuts";

	/**
	 * Property to specify if launcher short-cuts should be created.
	 * "true" to create launcher short-cuts.
	 */
	public static final String CREATE_LAUNCHER_SHORTCUTS = "createLauncherShortcuts";

	/**
	 * Property to specify the folder for program short-cuts.
	 */
	public static final String PROGRAM_SHORTCUTS_FOLDER = "programShortcutsFolder";

	/**
	 * Property to specify the installation size.
	 */
	public static final String INSTALL_SIZE = "installSize";

}
