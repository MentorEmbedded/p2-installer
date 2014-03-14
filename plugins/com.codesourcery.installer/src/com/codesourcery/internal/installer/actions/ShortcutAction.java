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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallPlatform;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.actions.AbstractInstallAction;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;

/**
 * Action to install and uninstall a shortcut.
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
	 * @param iconPath Path to the shortcut icon
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

		SubMonitor monitor = SubMonitor.convert(pm, InstallMessages.CreatingShortcuts, 100);

		IPath shortcutFolder = getPath();
		String fileName = InstallUtils.makeFileNameSafe(getName());
		
		// Install
		if (mode.isInstall()) {
			monitor.setTaskName(NLS.bind(InstallMessages.CreatingLink0, fileName));
			// Create short-cut
			platform.createShortcut(shortcutFolder, fileName, getTarget(), null, getWorkingDirectory());
		}
		// Uninstall
		else {
			monitor.setTaskName(NLS.bind(InstallMessages.RemovingLink0, shortcutFolder.append(fileName).toOSString()));
			// Remove short-cut
			try  {
				platform.deleteShortcut(shortcutFolder, fileName);
			}
			catch (Exception e) {
				// Do not fail if short-cut can't be removed, just log the error.
				Installer.log(e);
			}
			// Remove short-cut directory (if empty)
			try {
				IPath removeFolder = getRemovePath();
				if ((removeFolder != null) && !removeFolder.isEmpty())
					platform.deleteDirectory(removeFolder.toOSString(), true);
			}
			catch (Exception e) {
				// Do not fail if short-cut directory can't be removed, just
				// log the error.
				Installer.log(e);
			}
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
}
