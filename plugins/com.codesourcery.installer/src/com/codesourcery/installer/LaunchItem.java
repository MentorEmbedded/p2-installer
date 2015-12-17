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
		HTML("html"), 
		EXECUTABLE("exe"), 
		FILE("file"),
		RESTART("restart"),
		LOGOUT("logout");
		
		private String name;
		private LaunchItemType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
	};
	
	/** Presentation Type */
	public enum LaunchItemPresentation {
		CHECKED("checked"),
		UNCHECKED("unchecked"),
		LINK("link");
		
		private String name;
		private LaunchItemPresentation(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		/**
		 * Return a LaunchItemPresentation object from string
		 * 
		 * @param type of presentation
		 * @return LaunchItemPresentation set to type or <code>null</code> if type is not matched
		 */
		public static LaunchItemPresentation fromString(String type) {
			LaunchItemPresentation presentation = null;
			for (LaunchItemPresentation value : LaunchItemPresentation.values()) {
				if (value.getName().equals(type)) {
					presentation = value;
					break;
				}
			}
			return presentation;
		}
	}
	
	/** Name of item */
	private String name;
	/** Path of item */
	private String path;
	/** Type of item */
	private LaunchItemType type;
	/** Presentation type */
	private LaunchItemPresentation presentation;
	
	/**
	 * Constructor
	 * 
	 * @param type Launch item type
	 * <code>LaunchItemType.HTML</code> - URL of web content to open in browser
	 * <code>LaunchItemType.EXECUTABLE</code>  - Executable to run
	 * <code>LaunchItemType.FILE</code>  - File to open
	 * <code>LaunchItemType.RESTART</code>  - Cause computer to restart when installer closes
	 * <code>LaunchItemType.LOGOUT</code>  - Cause logout to occur when the installer closes
	 * @param name Displayable name for item
	 * @param path Path to item
	 * @param presentation Launch item presentation
	 * <code>LaunchItemPresentation.CHECKED</code>  - display a checkbox which is checked by default
	 * <code>LaunchItemPresentation.UNCHECKED</code>  - display a checkbox which is unchecked by default
	 * <code>LaunchItemPresentation.LINK</code>  - display a link which the user can click on
	 */
	public LaunchItem(LaunchItemType type, String name, String path, LaunchItemPresentation presentation) {
		this.type = type;
		this.name = name;
		this.path = path;
		this.presentation = presentation;
		
		/*
		 *  RESTART and LOGOUT types are only supported with checkboxes, and assumed to be checked by default
		 *  A LINK does not support RESTART or LOGOUT
		 */
		if ((type == LaunchItemType.RESTART || type == LaunchItemType.LOGOUT) && presentation == LaunchItemPresentation.LINK) {
			this.presentation = LaunchItemPresentation.CHECKED;
		}
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
	 * Returns the presentation type
	 * 
	 * @return Presentation
	 */
	public LaunchItemPresentation getPresentation() {
		return presentation;
	}
	
	/**
	 * Returns if the item should be launched by default.
	 * 
	 * @return <code>true</code> if item should be launched
	 */
	public boolean isDefault() {
		if (presentation == LaunchItemPresentation.CHECKED) {
			return true;
		}
		return false;
	}
}
