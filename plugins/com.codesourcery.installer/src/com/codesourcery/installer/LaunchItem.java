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
 * This class provides information for an item to be launched
 * at the end of an installation.
 */
public class LaunchItem {
	/** Launch item type */
	public enum LaunchItemType { 
		HTML, 
		EXECUTABLE, 
		FILE };
	
	/** Name of item */
	private String name;
	/** Path of item */
	private String path;
	/** Type of item */
	private LaunchItemType type;
	/** <code>true</code> if item should be launched by default */
	private boolean isDefault;
	
	/**
	 * Constructor
	 * 
	 * @param type Launch item type
	 * LAUNCH_HTML - URL of web content to open in browser
	 * LAUNCH_EXECUTABLE - Executable to run
	 * LAUNCH_FILE - File to open
	 * @param name Displayable name for item
	 * @param path Path to item
	 * @param isDefault <code>true</code> if item should be launched by default
	 */
	public LaunchItem(LaunchItemType type, String name, String path, boolean isDefault) {
		this.type = type;
		this.name = name;
		this.path = path;
		this.isDefault = isDefault;
	}

	/**
	 * Returns the item type.
	 * 
	 * @return Type
	 */
	public LaunchItemType getType() {
		return type;
	}
	
	/**
	 * Returns the item name.
	 * 
	 * @return Name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the path to the item.
	 * 
	 * @return Path
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * Returns if the item should be launched by default.
	 * 
	 * @return <code>true</code> if item should be launched
	 */
	public boolean isDefault() {
		return isDefault;
	}
}
