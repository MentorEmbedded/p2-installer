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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.codesourcery.installer.IInstallManifest;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;

/**
 * An install operation that runs silently and uses default options.
 */
public class SilentInstallOperation extends InstallOperation {
	/**
	 * Constructor
	 */
	public SilentInstallOperation() {
	}

	@Override
	public void run() {
		IStatus status = Status.OK_STATUS;
		
		try {
			// Install
			if (getInstallManager().getInstallMode().isInstall()) {
				// Installation data
				InstallData installData = new InstallData();
	
				checkStatus(ContributorRegistry.getDefault().verifyInstallLocation(getInstallManager().getInstallDescription()));
				
				// Set default install location
				getInstallManager().setInstallLocation(getInstallManager().getInstallDescription().getRootLocation());
				// Wait for repository load
				RepositoryManager.getDefault().waitForLoad();
				
				// Perform installation.
				getInstallManager().install(installData, new NullProgressMonitor());
			}
			// Uninstall
			else {
				IInstallManifest manifest = getInstallManager().getInstallManifest();
				
				// Get the installed products
				IInstallProduct[] products = manifest.getProducts();
				// Uninstall all products
				getInstallManager().uninstall(products, new NullProgressMonitor());
			}
		}
		// Install aborted
		catch (IllegalArgumentException e) {
			status = new Status(IStatus.CANCEL, Installer.ID, 0, e.getLocalizedMessage(), null);
			System.out.println(e.getMessage());
		}
		catch (Exception e) {
			status = new Status(IStatus.ERROR, Installer.ID, 0, e.getLocalizedMessage(), e);
			Installer.log(e);
			System.err.println(e.getLocalizedMessage());
		}
		
		// Write status
		writeStatus(status);
	}

	/**
	 * Check to see if a status object contains an error. If so, throw an exception.
	 * 
	 * @param status
	 * @throws CoreException
	 */
	private void checkStatus(IStatus[] status) throws CoreException {
		for (IStatus s : status) {
			if (s.getSeverity() == IStatus.ERROR) {
				Installer.fail(s.getMessage(), new CoreException(s));
			}
		}
		
	}
}
