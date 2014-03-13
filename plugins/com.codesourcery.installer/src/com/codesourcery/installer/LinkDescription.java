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

import org.eclipse.core.runtime.Path;

import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;

/**
 * Describes a short-cut
 */
public class LinkDescription {
	/** Link folder */
	private ShortcutFolder linkFolder;
	/** Link name */
	private String linkName;
	/** Link path */
	private String linkPath;
	/** Icon path */
	private String iconPath;
	/** Invocation arguments */
	private String[] args;
	/** Link target */
	private String linkTarget;
	/** <code>true</code> if link is installed by default */
	private boolean defaultSelection = false;
	
	/**
	 * Constructor
	 * 
	 * @param linkFolder Link folder
	 * @param linkPath Link path
	 * @param linkName Link name
	 * @param linkTarget Link target
	 * @param iconPath Link icon path
	 * @param args Link command-line arguments
	 * @param defaultSelection <code>true</code> if the link should be installed
	 * by default.
	 */
	public LinkDescription(ShortcutFolder linkFolder, String linkPath, String linkName, 
			String linkTarget, String iconPath, String[] args, boolean defaultSelection) {
		this.linkFolder = linkFolder;
		this.linkPath = (linkPath == null) ? null : new Path(linkPath).toOSString();
		this.linkName = linkName;
		this.linkTarget = linkTarget;
		this.iconPath = iconPath;
		this.args = args;
		this.defaultSelection = defaultSelection;
	}
	
	/**
	 * Returns the link folder
	 * 
	 * @return Link folder
	 */
	public ShortcutFolder getFolder() {
		return linkFolder;
	}
	
	/**
	 * Returns the link path
	 * 
	 * @return Link path
	 */
	public String getPath() {
		return linkPath;
	}

	/**
	 * Returns the icon path
	 * 
	 * @return Icon path
	 */
	public String getIconPath() {
		return iconPath;
	}

	/**
	 * Returns the invocation arguments
	 * 
	 * @return Args
	 */
	public String[] getArguments() {
		return args;
	}
	
	/**
	 * Returns the link name
	 * 
	 * @return Link name
	 */
	public String  getName() {
		return linkName;
	}
	
	/**
	 * Returns the link target
	 * 
	 * @return Link target
	 */
	public String getTarget() {
		return linkTarget;
	}
	
	/**
	 * Returns if the link is installed by default.
	 * 
	 * @return <code>true</code> if installed by default.
	 */
	public boolean isDefault() {
		return defaultSelection;
	}
}
