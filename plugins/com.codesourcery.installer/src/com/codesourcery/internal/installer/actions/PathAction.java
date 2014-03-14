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
package com.codesourcery.internal.installer.actions;

import java.io.File;

import com.codesourcery.installer.actions.EnvironmentAction;

/**
 * Action to install/uninstall paths into the PATH environment variable.
 */
public class PathAction extends EnvironmentAction {
	/** Path environment variable name */
	private final static String PATH_VARIABLE_NAME = "PATH";
	/** Paths */
	private String[] paths;
	
	/**
	 * Constructor
	 * 
	 * @param productName Product name
	 * @param paths Paths to add in environment
	 */
	public PathAction(String[] paths) {
		super();
		
		this.paths = paths;
		addVariable(PATH_VARIABLE_NAME, paths, EnvironmentOperation.PREPEND, Character.toString(File.pathSeparatorChar));
	}
	
	/**
	 * Returns the paths.
	 * 
	 * @return Paths
	 */
	public String[] getPaths() {
		return paths;
	}
}
