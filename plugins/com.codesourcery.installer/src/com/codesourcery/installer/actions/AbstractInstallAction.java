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
package com.codesourcery.installer.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.codesourcery.installer.IInstallAction;

/**
 * Abstract install action
 * @see {@link com.codesourcery.installer.IInstallAction}
 */
public abstract class AbstractInstallAction implements IInstallAction {
	/** Action identifier */
	private String id;
	
	/**
	 * Constructor
	 * 
	 * @param id Action identifier
	 */
	protected AbstractInstallAction(String id) {
		this.id = id;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isSupported(String platform, String arch) {
		// Default supports all platforms
		return true;
	}
	
	/**
	 * Returns if the platform is Windows.
	 * 
	 * @param platform Platform
	 * @return <code>true</code> if platform is Windows.
	 */
	protected boolean isWindows(String platform) {
		return platform.equals(Platform.OS_WIN32);
	}
	
	/**
	 * Returns if the platform is Linux.
	 * 
	 * @param platform Platform
	 * @return <code>true</code> if platform is Linux.
	 */
	protected boolean isLinux(String platform) {
		return platform.equals(Platform.OS_LINUX);
	}

	/**
	 * Returns if the platform is Mac OSX
	 * 
	 * @param platform Platform
	 * @return <code>true</code> if platform is Mac OSX.
	 */
	protected boolean isMac(String platform) {
		return platform.equals(Platform.OS_MACOSX);
	}

	@Override
	public int getProgressWeight() {
		return DEFAULT_PROGRESS_WEIGHT;
	}

	@Override
	public void save(Document document, Element element) throws CoreException {
		// Default saves nothing to the installation manifest
	}

	@Override
	public void load(Element element) throws CoreException {
		// Default loads nothing from the installation manifest
	}

	@Override
	public boolean uninstallOnUpgrade() {
		// Default will remove the action on an upgrade
		return true;
	}
	
	@Override
	public InstallPhase getInstallPhase() {
		// Default runs during install phase
		return InstallPhase.INSTALL;
	}
}
