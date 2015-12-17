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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallPlatform;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.actions.AbstractInstallAction;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;

/**
 * Action to install and uninstall a shortcut. can be used to create short-cuts 
 * to any files installed via P2 or files that are part of the uninstaller
 */
public class ShortcutAction extends AbstractInstallAction {
	/** Action identifier */
	private static final String ID = "com.codesourcery.installer.shortcutAction";
	
	/** Path attribute */
	private static final String ATTRIBUTE_PATH = "path";
	/** Name attribute */
	private static final String ATTRIBUTE_NAME = "name";
	/** Remove folder attribute */
	private static final String ATTRIBUTE_REMOVE_PATH = "removePath";
	
	/** Short-cut path */
	private IPath path;
	/** Short-cut path to remove on uninstallation or <code>null</code> */
	private IPath removePath;
	/** Shortcut name */
	private String name;
	/** Shortcut target */
	private IPath target;
	/** Shortcut icon */
	private IPath iconPath;
	/** Arguments */
	private String[] args;
	/** Shortcut working directory */
	private IPath workingDirectory;
	
	/**
	 * Constructor
	 */
	public ShortcutAction() {
		super(ID);
	}
	
	/**
	 * Constructor
	 * 
	 * @param path Path to short-cut directory
	 * @param removePath Path to remove when short-cut is installed or
	 * <code>null</code>.
	 * @param name Name for shortcut
	 * @param target Target for shortcut
	 * @param iconPath Path to the shortcut icon or <code>null</code> to use
	 * target file for icon if needed.
	 * @param workingDirectory Working directory for shortcut
	 * @param args Command-line arguments for the shortcut
	 * folder on uninstallation.
	 */
	public ShortcutAction(IPath path, IPath removePath,  
			String name, IPath target, IPath iconPath, IPath workingDirectory, String[] args) {
		super(ID);
		this.path = path;
		this.removePath = removePath;
		this.name = name;
		this.target = target;
		this.iconPath = iconPath;
		this.args = args;
		this.workingDirectory = workingDirectory;
	}
	
	/**
	 * Returns the path of the short-cut directory.
	 * 
	 * @return Base path
	 */
	protected IPath getPath() {
		return path;
	}
	
	/**
	 * Returns the path to remove for the short-cut.
	 * 
	 * @return Remove path
	 */
	protected IPath getRemovePath() {
		return removePath;
	}
	
	/**
	 * Returns the name of the shortcut.
	 * 
	 * @return Name
	 */
	protected String getName() {
		return name;
	}
	
	/**
	 * Returns the target for the shortcut.
	 * 
	 * @return Target
	 */
	protected IPath getTarget() {
		return target;
	}
	
	/**
	 * Returns the icon path for the shortcut.
	 * 
	 * @return Icon path
	 */
	protected IPath getIconPath() {
		return iconPath;
	}

	/**
	 * Returns the invocation arguments for the shortcut.
	 * 
	 * @return Args
	 */
	protected String[] getArguments() {
		return args;
	}
	
	/**
	 * Returns the working directory for the
	 * shortcut.
	 * 
	 * @return Working directory
	 */
	protected IPath getWorkingDirectory() {
		return workingDirectory;
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor pm) throws CoreException {
		IInstallPlatform platform = Installer.getDefault().getInstallPlatform();		

		IPath shortcutFolder = getPath();
		String fileName = InstallUtils.makeFileNameSafe(getName());
		
		try {
			// Install
			if (mode.isInstall()) {
				pm.beginTask(InstallMessages.CreatingShortcut, 1);
				pm.setTaskName(InstallMessages.CreatingShortcut);
				
				// Target of short-cut exist?
				if (doesTargetExist()) {
					StringBuffer argsBuffer = new StringBuffer();
					if (args != null)
					for (int i = 0; i < args.length; i++) {
						String arg = args[i];
						
						/// Handle {installFolder} variable.
						if (arg.contains("{installFolder}")) {
							String installFolder = product.getLocation().toOSString();
							arg = arg.replace("{installFolder}", installFolder);
						}
						
						argsBuffer.append(arg);
						if (i < args.length - 1) {
							argsBuffer.append(" ");
						}
					}

					// Create short-cut
					platform.createShortcut(
							shortcutFolder, 
							fileName, 
							getTarget(), 
							argsBuffer.toString(), 
							getWorkingDirectory(), 
							(getIconPath() != null) ? getIconPath() : getTarget(), 
							0);
				}
				else {
					Installer.log("Short-cut target does not exist: " + getTarget());
				}
				
				pm.worked(1);
			}
			// Uninstall
			else {
				pm.beginTask(InstallMessages.RemovingShortcut, 1);
				pm.setTaskName(InstallMessages.RemovingShortcut);
				// Remove short-cut
				try  {
					platform.deleteShortcut(shortcutFolder, fileName);
				}
				catch (Exception e) {
					// Do not fail if short-cut can't be removed, just log the error.
					Installer.log(e);
				}
				// Remove short-cut directories (if empty)
				try {
					// Root short-cut folder
					IPath baseShortcutFolder = getRemovePath();
					while (!shortcutFolder.isRoot()) {
						// Do not attempt to remove desktop folder
						if (shortcutFolder.equals(platform.getShortcutFolder(ShortcutFolder.DESKTOP)))
							break;
						// Do not attempt to remove Programs folder on Windows
						if (Installer.isWindows() && shortcutFolder.equals(platform.getShortcutFolder(ShortcutFolder.PROGRAMS)))
							break;
						// Do not attempt to remove installation folder on Linux
						if (Installer.isLinux() && shortcutFolder.equals(product.getLocation()))
							break;
						// Delete short-cut directory
						platform.deleteDirectory(shortcutFolder.toOSString(), true);
						// If we removed last short-cut folder then exit
						if (shortcutFolder.equals(baseShortcutFolder))
							break;
						// Move to parent short-cut folder
						shortcutFolder = shortcutFolder.removeLastSegments(1);
					}
				}
				catch (Exception e) {
					// Do not fail if short-cut directory can't be removed, just
					// log the error.
					Installer.log(e);
				}
				pm.worked(1);
			}
		}
		finally {
			pm.done();
		}
	}

	@Override
	public void save(Document document, Element element) throws CoreException {
		element.setAttribute(ATTRIBUTE_PATH, getPath().toOSString());
		String fileName = InstallUtils.makeFileNameSafe(getName());
		element.setAttribute(ATTRIBUTE_NAME, fileName);
		if (getRemovePath() != null)
			element.setAttribute(ATTRIBUTE_REMOVE_PATH, getRemovePath().toOSString());
	}

	@Override
	public void load(Element element) throws CoreException {
		this.path = new Path(element.getAttribute(ATTRIBUTE_PATH));
		this.name = element.getAttribute(ATTRIBUTE_NAME);
		String value = element.getAttribute(ATTRIBUTE_REMOVE_PATH);
		if (value != null)
			this.removePath = new Path(value);
	}
	
	/**
	 * Determine if the target file exists. Typically used to decide whether or 
	 * not to create the shortcut. The notable exception is if the target file
	 * is contained with the "uninstall" directory; if so, true is always
	 * returned. 
	 *
	 * @return <code>true</code> if target exists
	 */
	protected boolean doesTargetExist() {
		IPath uninstallDir = Installer.getDefault().getInstallManager().getInstallDescription().getRootLocation().append(IInstallConstants.UNINSTALL_DIRECTORY);
		if (uninstallDir.isPrefixOf(getTarget())){
			return true;
		}
		else {
			return (getTarget().toFile().exists());
		}
	}

	/**
	 * Returns if the target is a directory.
	 * 
	 * @return <code>true</code> if target is directory
	 */
	protected boolean isTargetDirectory() {
		File targetFile = getTarget().toFile();
		return targetFile.exists() && targetFile.isDirectory();
	}
}
