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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.AbstractInstallAction;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * Action to install/uninstall paths into the environment.
 */
public class PathAction extends AbstractInstallAction {
	/** Action identifier */
	private static final String ID = "com.codesourcery.installer.pathAction";
	/** Profile file names. */  
	private static final String PROFILE_FILENAMES[] = {
		".bash_profile",
		".bash_login",
		".profile"
	};
	/** Index of PROFILE_FILENAMES to use when creating a new profile. */
	private static final int PROFILE_DEFAULT_INDEX = 2;
	/** Windows user environment registry key */
	private static final String REG_USER_ENVIRONMENT = "HKEY_CURRENT_USER\\Environment";
	/** Windows environment path variable */
	private static final String REG_PATH_VAR = "PATH";
	/** Paths element */
	private static final String ELEMENT_PATHS = "paths";
	/** Path element */
	private static final String ELEMENT_PATH = "path";
	/** Path value element */
	private static final String ATTRIBUTE_VALUE = "value";
	
	/** Paths */
	private String[] paths;
	
	/**
	 * Constructor
	 */
	public PathAction() {
		super(ID);
	}
	
	/**
	 * Constructor
	 * 
	 * @param productName Product name
	 * @param paths Paths to add in environment
	 */
	public PathAction(String[] paths) {
		super(ID);
		this.paths = paths;
	}
	
	/**
	 * Returns the paths.
	 * 
	 * @return Paths
	 */
	private String[] getPaths() {
		return paths;
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor pm) throws CoreException {
		SubMonitor monitor = SubMonitor.convert(pm, InstallMessages.SettingUpPaths, 100);

		try {
			// Windows
			if (Installer.isWindows()) {
				runWindows(mode, monitor);
			}
			// Linux
			else {
				runLinux(product, mode, monitor);
			}
		}
		finally {
			monitor.done();
		}
	}
	
	/**
	 * Handles Windows path environment.
	 * 
	 * @param mode Mode
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	private void runWindows(IInstallMode mode, SubMonitor monitor)
			throws CoreException {
		// Get current path value
		String pathVariable = null;
		
		try {
			pathVariable = Installer.getDefault().getInstallPlatform().getWindowsRegistryValue(REG_USER_ENVIRONMENT, REG_PATH_VAR);
		}
		catch (CoreException e) {
			// Ignore
		}
		// No value read
		if ((pathVariable !=null) && pathVariable.isEmpty())
			pathVariable = null;

		// Install
		if (mode.isInstall()) {
			// Build extended path value
			StringBuffer pathExtension = new StringBuffer();
			String[] paths = getPaths();
			for (int index = 0; index < paths.length; index++) {
				if (index != 0)
					pathExtension.append(File.pathSeparator);
				pathExtension.append(paths[index]);
			}

			if (pathVariable != null) {
				pathVariable += File.pathSeparator + pathExtension.toString();
			}
			else {
				pathVariable = pathExtension.toString();
			}
			monitor.setTaskName(NLS.bind(InstallMessages.SettingPaths, pathVariable));
			
			// Set new path value
			Installer.getDefault().getInstallPlatform().setWindowsRegistryValue(REG_USER_ENVIRONMENT, REG_PATH_VAR, pathVariable);
		}
		// Uninstall
		else {
			if (pathVariable != null) {
				// Get paths
				String[] envPaths = pathVariable.split(File.pathSeparator);
				ArrayList<IPath> newPaths = new ArrayList<IPath>();
				for (String envPath : envPaths) {
					newPaths.add(new Path(envPath));
				}
				// Remove paths being uninstalled
				for (String path : getPaths()) {
					newPaths.remove(new Path(path));
				}
				// Build new path value
				StringBuffer buffer = new StringBuffer();
				for (IPath path : newPaths) {
					if (buffer.length() != 0)
						buffer.append(File.pathSeparator);
					buffer.append(path.toOSString());
				}
				String newPath = buffer.toString();
				monitor.setTaskName(NLS.bind(InstallMessages.SettingPaths, newPath));
				
				// Set new path value
				Installer.getDefault().getInstallPlatform().setWindowsRegistryValue(REG_USER_ENVIRONMENT, REG_PATH_VAR, newPath);
			}
		}
	}

	/**
	 * Returns the marker for beginning or ending of profile path block.
	 * 
	 * @param product Product
	 * @param start <code>true</code> for start of block marker,
	 * <code>false</code> for end of block marker.
	 * @return Marker
	 */
	private String getProfileMarker(IInstallProduct product, boolean start) {
		if (start)
			return MessageFormat.format("# Product Begin: {0}", new Object[] { product.getName() });
		else
			return MessageFormat.format("# Product End: {0}.", new Object[] { product.getName() });
	}
	
	/**
	 * Handles Linux path environment.
	 * 
	 * @param product Product
	 * @param mode Mode
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	private void runLinux(IInstallProduct product, IInstallMode mode, IProgressMonitor monitor)
			throws CoreException {
		// Path to .profile
		String homeDir = System.getProperty("user.home");
		if (homeDir == null)
			Installer.fail("Failed to get user home directory.");
		IPath homePath = new Path(homeDir);

		// Check for profile
		String profileFilename = null;
		File profileFile = null;
		for (String name : PROFILE_FILENAMES) {
			IPath profilePath = homePath.append(name);
			profileFile = profilePath.toFile();
			if (profileFile.exists()) {
				profileFilename = name;
				break;
			}
			else {
				Installer.log(name + " not found, skipping.");
			}
		}
		if (profileFilename == null) {
			// Create a new profile.
			profileFilename = PROFILE_FILENAMES[PROFILE_DEFAULT_INDEX];
			IPath newProfilePath = homePath.append(profileFilename);
			try {
				profileFile = newProfilePath.toFile();
				profileFile.createNewFile();
			} catch (IOException e) {
				Installer.log("Could not create profile " + newProfilePath);
				return;
			}
		}
		
		// Do not modify read-only profile
		if (!profileFile.canWrite()) {
			Installer.log("Profile was not modified because it is read-only.");
			return;
		}

		// File date suffix
		SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyDDDHHmmss");
		String fileDateDesc = fileDateFormat.format(new Date());
		// Backup file path
		String backupName = profileFilename + fileDateDesc;
		IPath backupPath = homePath.append(backupName);
		File backupFile = backupPath.toFile();

		String line;
		// Install
		if (mode.isInstall()) {
			// Date description
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
			String dateDesc = dateFormat.format(new Date());

			// Make backup of .profile
			try {
				org.apache.commons.io.FileUtils.copyFile(profileFile, backupFile);
			} catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToBackupProfile, e);
			}
			
			// Write path extensions
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(profileFile, true));
				writer.newLine();
				writer.append(getProfileMarker(product, true));
				writer.newLine();
				writer.append("# Do NOT modify these lines; they are used to uninstall.");
				writer.newLine();
				line = MessageFormat.format("# New environment added by {0} on {1}.", new Object[] {
					product.getName(),
					dateDesc
				});
				writer.append(line);
				writer.newLine();
				line = MessageFormat.format("# The unmodified version of this file is saved in {0}.", backupPath.toOSString());
				writer.append(line);
				writer.newLine();
				
				StringBuilder buffer = new StringBuilder();
				for (String path : getPaths()) {
					buffer.append(path);
					buffer.append(File.pathSeparatorChar);
				}
				buffer.append("${PATH}");
				String path = buffer.toString();
				line = MessageFormat.format("PATH=\"{0}\"", new Object[] { path });
				monitor.setTaskName(NLS.bind(InstallMessages.SettingPaths, path));
				writer.append(line);
				writer.newLine();
				writer.append("export PATH");
				writer.newLine();
				
				writer.append(getProfileMarker(product, false));
				writer.newLine();
				
			} catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToUpdateProfile, e);
			}
			finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}
		// Uninstall
		else {
			BufferedReader reader = null;
			BufferedWriter writer = null;
			boolean inProductBlock = false;
			try {
				reader = new BufferedReader(new FileReader(profileFile));
				writer = new BufferedWriter(new FileWriter(backupFile));
				while ((line = reader.readLine()) != null) {
					// Start of product path block
					if (line.startsWith(getProfileMarker(product, true))) {
						inProductBlock = true;
					}
					// End of product path block
					else if (line.startsWith(getProfileMarker(product, false))) {
						inProductBlock = false;
					}
					// If not in product path block, copy lines
					else if (!inProductBlock) {
						writer.write(line);
						writer.newLine();
					}
				}
			}
			catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToUpdateProfile, e);
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// Ignore
					}
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}

			// Copy new profile
			try {
				org.apache.commons.io.FileUtils.copyFile(backupFile, profileFile);
			} catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToUpdateProfile, e);
			}
			
			backupFile.delete();
		}
	}

	@Override
	public void save(Document document, Element node) throws CoreException {
		Element element = document.createElement(ELEMENT_PATHS);
		node.appendChild(element);
		for (String path : getPaths()) {
			Element pathElement = document.createElement(ELEMENT_PATH);
			element.appendChild(pathElement);
			pathElement.setAttribute(ATTRIBUTE_VALUE, path);
		}
	}

	@Override
	public void load(Element element) throws CoreException {
		ArrayList<String> pathValues = new ArrayList<String>();
		
		NodeList pathsNodes = element.getElementsByTagName(ELEMENT_PATHS);
		for (int pathsIndex = 0; pathsIndex < pathsNodes.getLength(); pathsIndex++) {
			Node pathsNode = pathsNodes.item(pathsIndex);
			if (pathsNode.getNodeType() == Node.ELEMENT_NODE) {
				Element pathsElement = (Element)pathsNode;
				NodeList pathNodes = pathsElement.getElementsByTagName(ELEMENT_PATH);
				for (int pathIndex = 0; pathIndex < pathNodes.getLength(); pathIndex++) {
					Node pathNode = pathNodes.item(pathIndex);
					if (pathNode.getNodeType() == Node.ELEMENT_NODE) {
						Element pathElement = (Element)pathNode;
						String value = pathElement.getAttribute(ATTRIBUTE_VALUE);
						pathValues.add(value);
					}
				}
			}
		}
		
		this.paths = pathValues.toArray(new String[pathValues.size()]);
	}
}
