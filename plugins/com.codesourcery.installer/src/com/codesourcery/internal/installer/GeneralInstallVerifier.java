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
import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.codesourcery.installer.AbstractInstallVerifier;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IProductRange;
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
			// Note: 
			// File.canWrite() can't be used as it does not check Windows security permissions
			// Files.isWriteable() can't be used.  Although it checks Windows security permissions, it
			// is unreliable (fixed only in Java 8 (b84) and back-ported to Java 7u40).
			path = path.append(UUID.randomUUID().toString());
			try {
				if (path.toFile().createNewFile()) {
					path.toFile().delete();
				}
			} catch (IOException e) {
				status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.NoWritePermissions);
			}
		}

		try {
			InstallManifest manifest = InstallManifest.loadManifest(installDescription);
			
			// Requirement on existing products
			IProductRange[] ranges = installDescription.getRequires();
			if (ranges != null) {
				IInstallProduct[] matchedProducts = null;
				if (manifest != null) {
					matchedProducts = manifest.getProducts(ranges);
				}
				// No products found that match required ranges
				if ((matchedProducts == null) || (matchedProducts.length == 0)) {
					status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_ProductsNotFound);
				}
			}
			// Check for existing version of install product
			else if (manifest != null) {
				IInstallProduct product = Installer.getDefault().getInstallManager().getExistingProduct(manifest);
				if (product != null) {
					// Same or newer version installed
					if ((product.getVersion().compareTo(installDescription.getProductVersion()) == 0) ||
						(product.getVersion().compareTo(installDescription.getProductVersion()) > 0)) {
						String errorMessage = MessageFormat.format(InstallMessages.Error_AlreadyInstalled1, new Object[] {
								installDescription.getProductName(),
								product.getVersionString()
							});
						status = new Status(IStatus.ERROR, Installer.ID, errorMessage);
					}
					// Older version installed
					else if (product.getVersion().compareTo(installDescription.getProductVersion()) < 0) {
						String errorMessage = MessageFormat.format(InstallMessages.Error_Upgrade2, new Object[] {
								installDescription.getProductName(),
								product.getVersionString(),
								installDescription.getProductVersionString()
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
