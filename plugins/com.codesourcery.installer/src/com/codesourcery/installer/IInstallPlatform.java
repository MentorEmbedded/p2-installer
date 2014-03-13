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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * The installer platform is used to perform operating specific native
 * operations.
 */
public interface IInstallPlatform {
	/** Short-cut folder */
	public enum ShortcutFolder {
		/** Desktop short-cut */
		DESKTOP,
		/** Programs short-cut */
		PROGRAMS, 
		/** Unity short-cut */
		UNITY_DASH 
	};
	
	/**
	 * Schedules directories to removed after the installer has closed.
	 * This method can be used to remove directories that are normally locked
	 * by the operating system when the installer is running.
	 * Supported on all platforms.
	 * 
	 * @param directories Directories to remove or <code>null</code>.
	 * @param emptyDirectories Directories to remove only if they are empty
	 * or <code>null</code>.
	 * @throws CoreException on failure
	 */
	public void scheduleDirectoriesToBeRemoved(String[] directories, 
			String[] emptyDirectories) throws CoreException;
	
	/**
	 * Sets a Windows registry string value.
	 * Only supported on Windows.
	 * 
	 * @param key Full path to registry key.  For example,
	 * <code>"HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows"</code>
	 * @param name Value name
	 * @param value Value
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException If operation is not supported
	 * by operating system.
	 * @see #setRegistryValue(String, String, int)
	 */
	public void setWindowsRegistryValue(String key, String name, String value) 
			throws CoreException, UnsupportedOperationException;

	/**
	 * Sets a Windows registry integer value.
	 * Only supported on Windows.
	 * 
	 * @param key Full path to registry key
	 * @param name Value name
	 * @param value Value
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException If operation is not supported
	 * by operating system.
	 * @see #setWindowsRegistryValue(String, String, String)
	 */
	public void setWindowsRegistryValue(String key, String name, int value) 
			throws CoreException, UnsupportedOperationException;

	/**
	 * Returns a Windows registry value.
	 * Only supported on Windows.
	 * 
	 * @param key Full path to registry key
	 * @param name Value name
	 * @return Key value
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException If operation is not supported
	 * by operating system.
	 */
	public String getWindowsRegistryValue(String key, String name) 
			throws CoreException, UnsupportedOperationException;
	
	/**
	 * Deletes a Windows registry key.
	 * Only supported on Windows.
	 * 
	 * @param key Full path to the registry key
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException If operation is not supported
	 * by operating system.
	 */
	public void deleteWindowsRegistryKey(String key) throws CoreException, UnsupportedOperationException;
	
	/**
	 * Deletes a Windows registry value.
	 * Only supported on Windows.
	 * 
	 * @param key Full path to the registry key
	 * @param name Value name
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException If operation is not supported
	 * by operating system.
	 */
	public void deleteWindowsRegistryValue(String key, String name) throws CoreException, UnsupportedOperationException;
	
	/**
	 * Creates a short-cut.  
	 * 
	 * @param path Path to the short-cut.  The directories will be created if 
	 * they do not exist.
	 * @param linkName Short-cut link name
	 * @param targetFile Path to the short-cut target file.
	 * @param arguments Command line arguments or <code>null</code>
	 * @param workingDirectory Working directory or <code>null<code>
	 * (Only used on Windows)
	 * @throws CoreException on failure
	 * @see #getSpecialFolderPath(ShortcutFolder)
	 */
	public void createShortcut(IPath path, String linkName, 
		IPath targetFile, String arguments, IPath workingDirectory) 
		throws CoreException;
	
	/**
	 * Deletes a short-cut.
	 * 
	 * @param path Path to the short-cut.
	 * @param linkName Link name
	 * @throws CoreException on failure
	 */
	public void deleteShortcut(IPath path, String linkName) throws CoreException;
	
	/**
	 * Deletes a directory and child directories.
	 * Supported on all platforms.
	 * 
	 * @param path Full path to the directory
	 * @param onlyIfEmpty <code>true</code> to delete directory
	 * and child directories only if they are emtpy.
	 * @throws CoreException
	 */
	public void deleteDirectory(String path, boolean onlyIfEmpty) 
		throws CoreException;
	
	/**
	 * Returns the path to a short-cut folder.
	 * 
	 * @return Short-cut folder
	 * @throws CoreException on failure
	 */
	public IPath getShortcutFolder(ShortcutFolder folder) throws CoreException;
	
	/**
	 * Returns the Windows System folder. A Typical path is C:\Windows\System32.
	 * 
	 * Only supported on Windows.
	 * 
	 * @return Folder path
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException if operation is not supported by
	 * the operating system.
	 */
	public IPath getWindowsSystemFolder() throws CoreException, UnsupportedOperationException;
	
	/**
	 * Detects if desktop session is Unity.
	 * 
	 * @return true if the current desktop session is detected as being some version of Ubuntu Unity
	 */
	public boolean desktopIsUnity();
	
	/**
	 * Returns the Windows major version.
	 * Only supported on Windows.
	 * 
	 * @return Major version
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException if operation is not supported by
	 * the operation system.
	 */
	public int getWindowsMajorVersion() throws CoreException, UnsupportedOperationException;
	
	/**
	 * Returns the Windows minor version.
	 * Only supported on Windows.
	 * 
	 * @return Minor version
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException if operation is not supported by
	 * the operation system.
	 */
	public int getWindowsMinorVersion() throws CoreException, UnsupportedOperationException;
	
	/**
	 * Launches an external program and returns the output.
	 * 
	 * @param path Path to program
	 * @param arguments Program arguments
	 * @return Program output
	 * @throws CoreException on failure
	 */
	public String launchProgram(String path, String[] arguments)
			throws CoreException;

	/**
	 * Installs Windows drivers in the specified directory using the
	 * Microsoft DPInst redistributable driver installer.
	 * For information on creating a DPInst driver installation package,
	 * refer to 
	 * <a href="http://msdn.microsoft.com/en-us/library/windows/hardware/ff540184%28v=vs.85%29.aspx">Creating a DPInst Installation Package</a>
	 * 
	 * @param path Path to directory containing drivers to be installed
	 * @throws CoreException on failure
	 * @throws UnsupportedOperationException if operation is not supported by
	 * the operation system.
	 */
	public void installWindowsDriver(String path) throws CoreException, UnsupportedOperationException;
}
