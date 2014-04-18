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
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.actions.EnvironmentAction;

/**
 * Action to install/uninstall paths into the PATH environment variable.
 * Note, this is just a wrapper for the more general environment variables
 * action, {@link com.codesourcery.installer.actions.EnvironmentAction}.
 * It also serves to handle the <code>com.codesourcery.installer.pathAction</code>
 * action that was available in previous versions.
 */
public class PathAction extends EnvironmentAction {
	/** Path environment variable name */
	private final static String PATH_VARIABLE_NAME = "PATH";
	/** Paths */
	private String[] paths;

	/**
	 * Constructor
	 */
	public PathAction() {
	}
	
	/**
	 * Constructor
	 * 
	 * @param productName Product name
	 * @param paths Paths to add in environment
	 */
	public PathAction(String[] paths) {
		super();

		setPaths(paths);
	}
	
	/**
	 * Sets the paths to add in the PATH environment.
	 * 
	 * @param paths Paths
	 */
	protected void setPaths(String[] paths) {
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

	@Override
	public void load(Element element) throws CoreException {
		// Attempt to load previous schema
		if (!loadPreviousSchema(element)) {
			super.load(element);
		}
	}
	
	/**
	 * Loads the action data from a previous schema.
	 * 
	 * @param element Element
	 * @return <code>true</code> if the action loaded data, <code>false</code>
	 * otherwise.
	 * @throws CoreException on failure
	 */
	public boolean loadPreviousSchema(Element element) throws CoreException {
		ArrayList<String> pathValues = new ArrayList<String>();
		
		NodeList pathsNodes = element.getElementsByTagName("paths");
		for (int pathsIndex = 0; pathsIndex < pathsNodes.getLength(); pathsIndex++) {
			Node pathsNode = pathsNodes.item(pathsIndex);
			if (pathsNode.getNodeType() == Node.ELEMENT_NODE) {
				Element pathsElement = (Element)pathsNode;
				NodeList pathNodes = pathsElement.getElementsByTagName("path");
				for (int pathIndex = 0; pathIndex < pathNodes.getLength(); pathIndex++) {
					Node pathNode = pathNodes.item(pathIndex);
					if (pathNode.getNodeType() == Node.ELEMENT_NODE) {
						Element pathElement = (Element)pathNode;
						String value = pathElement.getAttribute("value");
						pathValues.add(value);
					}
				}
			}
		}
		
		if (pathValues.size() > 0) {
			setPaths(pathValues.toArray(new String[pathValues.size()]));
			return true;
		}
		else {
			return false;
		}
	}
}
