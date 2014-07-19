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
import java.io.PrintStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;

/**
 * Action to install and uninstall Unity launcher (.desktop) files. 
 */
public class UnityShortcutAction extends ShortcutAction {
	/** Action identifier */
	private static final String ID = "com.codesourcery.installer.unityShortcutAction";
	/** Dash attribute */
	private static final String ATTRIBUTE_LAUNCHER = "launcher";
	
	/** <code>true</code> if Ubuntu Unity Dash short-cut, <code>false</code>
	 * if desktop short-cut.
	 */
	protected boolean isDashShortcut;

	/**
	 * Constructor
	 */
	public UnityShortcutAction() {
	}
	
	/**
	 * Constructor
	 * 
	 * @param path Short-cut path
	 * @param removePath Remove path
	 * @param name Short-cut name
	 * @param target Short-cut target
	 * @param iconPath Short-cut icon
	 * @param workingDirectory Short-cut working directory
	 * @param args Short-cut arguments
	 * @param isPanelShortcut <code>true</code> if panel short-cut
	 */
	public UnityShortcutAction(IPath path, IPath removePath, String name,
			IPath target, IPath iconPath, IPath workingDirectory, String[] args, boolean isPanelShortcut) {
		super(path, removePath, name, target, iconPath, workingDirectory, args);
		
		this.isDashShortcut = isPanelShortcut;
	}

	/**
	 * Returns the contents of the launcher file.
	 **/
	protected String getLauncherContents(IInstallProduct product) {
		
		String cmd = getTarget().toOSString();
		
		for (String arg : getArguments()) { 
			cmd += " " + arg;
		}
		
		String fileContents = "[Desktop Entry]\n" //$NON-NLS-1$
				+ "Type=Application\n" //$NON-NLS-1$
				+ "Name=" + getName() + "\n"  //$NON-NLS-1$ //$NON-NLS-2$
				+ "Exec=" + cmd + "\n"//$NON-NLS-1$
				+ "Icon=" + getIconPath() + "\n" //$NON-NLS-1$
				+ "StartupWMClass=" + InstallUtils.makeFileNameSafe(getName()); //$NON-NLS-1$
		
		return fileContents;
	}

	/**
	 * Generates and writes the launcher file to path.
	 */
	protected void writeLauncherFile(IPath path, IInstallProduct product) throws CoreException {
		try {
			File launcherFile = new File(path.toOSString());
			
			if (launcherFile.exists()) {
				// Should we error here?
				launcherFile.delete();
			}
			launcherFile.createNewFile();
			launcherFile.setExecutable(true);
			PrintStream stream = new PrintStream(launcherFile);
			stream.write(getLauncherContents(product).getBytes());
			stream.close();
			
		} catch (Exception e) {
			Installer.fail(InstallMessages.UnityShortcutAction_0, e);
		}
	}

	/**
	 * Deletes the file at path.
	 */
	protected void deleteLauncherFile(IPath path) {
		File launcherFile = new File(path.toOSString());
		launcherFile.delete();
	}
	
	/**
	 * Returns the launcher name.
	 * 
	 * @return Launcher name
	 */
	protected String getLauncherName() {
		return InstallUtils.makeFileNameSafe(getName() + ".desktop"); //$NON-NLS-1$
	}

	/**
	 * Returns the default Unity launcher path ~/.local/share/applications/
	 **/
	protected IPath getDashRegistrationPath() {
		IPath path = new Path(System.getProperty("user.home"));
		path = path.append(".local");
		path = path.append("share");
		path = path.append("applications");
		return path;
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor pm)
			throws CoreException {
		
		try {
			// Install
			if (mode.isInstall()) {
				if (isDashShortcut) {
					// Register application with Unity by creating a .desktop file in the default 
					// launcher path. This means that the application will show up in the Unity Dash.
					
					// TODO: Also install desktop file to launcher panel using DBUS.
					if (!new File (getDashRegistrationPath().append(getLauncherName()).toOSString()).exists()) {
						new File (getDashRegistrationPath().toOSString()).mkdirs();
						writeLauncherFile(getDashRegistrationPath().append(getLauncherName()), product);	
					}
				} else {
					// This is a desktop link - write .desktop file to ~/Desktop.
					String taskName = NLS.bind(InstallMessages.CreatingLink0, getName());
					pm.beginTask(taskName, 1);
					pm.setTaskName(taskName);
					writeLauncherFile(getPath().append(getLauncherName()), product);
					pm.worked(1);
				}
			}
			// Uninstall
			else {
				if (isDashShortcut) {
					if (new File (getDashRegistrationPath().append(getLauncherName()).toOSString()).exists()) {
						String taskName = NLS.bind(InstallMessages.RemovingLink0, getDashRegistrationPath().append(getLauncherName()));
						pm.beginTask(taskName, 1);
						pm.setTaskName(taskName);
						deleteLauncherFile(getDashRegistrationPath().append(getLauncherName()));
						pm.worked(1);
					}
				} else {
					String taskName = NLS.bind(InstallMessages.RemovingLink0, getPath().append(getLauncherName()));
					pm.beginTask(taskName, 1);
					pm.setTaskName(taskName);
					deleteLauncherFile(getPath().append(getLauncherName()));
					pm.worked(1);
				}
			}
		}
		finally {
			pm.done();
		}
	}
	
	@Override
	public void save(Document document, Element element) throws CoreException {
		super.save(document, element);
		element.setAttribute(ATTRIBUTE_LAUNCHER, new Boolean(isDashShortcut).toString());
	}
	
	@Override
	public void load(Element element) throws CoreException {
		super.load(element);
		String value = element.getAttribute(ATTRIBUTE_LAUNCHER);
		isDashShortcut = Boolean.parseBoolean(value);
	}
	
	@Override
	public String getId() {
		return ID;
	}
}
