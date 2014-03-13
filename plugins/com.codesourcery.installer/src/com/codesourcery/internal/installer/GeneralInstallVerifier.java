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

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.codesourcery.installer.AbstractInstallVerifier;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;

/**
 * General installation verifier.
 */
public class GeneralInstallVerifier extends AbstractInstallVerifier {
	/**
	 * Constructor
	 */
	public GeneralInstallVerifier() {
	}

	@Override
	public IStatus verifyInstallLocation(IInstallDescription installDescription) {
		IStatus status = Status.OK_STATUS;
		
		IPath location = installDescription.getRootLocation();
		if (location.isEmpty()) {
			status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_PleaseSpecifyLocation);
		}
		else {
			// Find the parent folder
			IPath path = location;
			while (!path.toFile().exists() && (path.segmentCount() > 0)) {
				path = path.removeLastSegments(1);
			}
			
			// Verify the folder has write permissions
			if (!path.toFile().canWrite()) {
				status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.NoWritePermissions);
			}
		}

		// Check for existing version of product
		try {
			InstallManifest manifest = InstallManifest.loadManifest(installDescription);
			if (manifest != null) {
				IInstallProduct product = manifest.getProduct(installDescription.getProductId());
				// Product found
				if (product != null) {
					// Same version
					if (product.getVersion().equals(installDescription.getProductVersion())) {
						String errorMessage = MessageFormat.format(InstallMessages.Error_AlreadyInstalled1, new Object[] {
								installDescription.getProductName(),
								installDescription.getProductVersion()
							});
						status = new Status(IStatus.ERROR, Installer.ID, errorMessage);
					}
					// Different version
					else {
						String errorMessage = MessageFormat.format(InstallMessages.Error_Upgrade2, new Object[] {
								installDescription.getProductName(),
								product.getVersion(),
								installDescription.getProductVersion()
							});
						status = new Status(IStatus.WARNING, Installer.ID, errorMessage);
					}
				}
			}
		} catch (CoreException e) {
			Installer.log(e);
		}

		return status;
	}
}
