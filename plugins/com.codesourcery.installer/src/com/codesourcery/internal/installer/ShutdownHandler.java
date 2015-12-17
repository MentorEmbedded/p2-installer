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

import java.io.IOException;
import java.util.ArrayList;

import com.codesourcery.installer.Installer;

/**
 * This class handles shutdown operations.
 */
public class ShutdownHandler implements Runnable {
	/** Default instance */
	private static ShutdownHandler instance = null;
	
	/** Shutdown operations */
	private ArrayList<Runnable> operations = new ArrayList<Runnable>();
	/** Directories to remove on shutdown */
	private ArrayList<String> directoriesToRemove = new ArrayList<String>();
	/** Directories to remove on shutdown only if they are empty */
	private ArrayList<String> emptyDirectoriesToRemove = new ArrayList<String>();

	protected ShutdownHandler() {
	}
	
	/**
	 * Returns the default instance.
	 * 
	 * @return Default instance
	 */
	public static ShutdownHandler getDefault() {
		if (instance == null) {
			instance = new ShutdownHandler();
		}
		
		return instance;
	}
	
	/**
	 * Adds a directory to be removed after the installer has
	 * shutdown.
	 * 
	 * @param path Path to directory
	 * @param onlyIfEmpty <code>true</code> to only remove directory
	 * if it is empty.
	 */
	public void addDirectoryToRemove(String path, boolean onlyIfEmpty) {		
		if (onlyIfEmpty) {
			emptyDirectoriesToRemove.add(path);
		}
		else {
			// Set all files write-able so they can be removed.
			try {
				FileUtils.setWritable(java.nio.file.Paths.get(path));
			} catch (IOException e) {
				Installer.log(e);
			}

			directoriesToRemove.add(path);			
		}
	}
	
	/**
	 * Adds an operation to perform on shutdown.
	 * 
	 * @param operation Operation to perform
	 */
	public void addOperation(Runnable operation) {
		operations.add(operation);
	}

	@Override
	public void run() {
		try {
			// Remove directories
			Installer.getDefault().getInstallPlatform().dispose(
					directoriesToRemove.toArray(new String[directoriesToRemove.size()]), 
					emptyDirectoriesToRemove.toArray(new String[emptyDirectoriesToRemove.size()])
					);
			
			// Run operations
			for (Runnable operation : operations) {
				operation.run();
			}
		} catch (Exception e) {
			Installer.log(e);
		}
	}
}
