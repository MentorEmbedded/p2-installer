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
import java.util.ArrayList;
import java.util.List;
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

	/** Returns an error caused by the installation path.  Null means no error. */
	private void checkLocation(List<IStatus> problems, IPath location) {
		if (location.isEmpty()) {
			problems.add(new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_PleaseSpecifyLocation));
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
				problems.add(new Status(IStatus.ERROR, Installer.ID, InstallMessages.NoWritePermissions));
			}
		}
	}
	
	/** Returns an error caused by problems in the product version or required products.  Null means no error. */
	private void checkProductVersions(List<IStatus> problems, IPath location) {
		try {
			IInstallDescription installDescription = Installer.getDefault().getInstallManager().getInstallDescription();
			InstallManifest manifest = InstallManifest.loadManifest(location);
			
			// Requirement on existing products
			IProductRange[] ranges = installDescription.getRequires();
			if (ranges != null) {
				IInstallProduct[] matchedProducts = null;
				if (manifest != null) {
					matchedProducts = manifest.getProducts(ranges);
				}
				// No products found that match required ranges
				if ((matchedProducts == null) || (matchedProducts.length == 0)) {
					problems.add(new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_ProductsNotFound));
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
						problems.add(new Status(IStatus.ERROR, Installer.ID, errorMessage));
					}
					// Older version installed
					else if (product.getVersion().compareTo(installDescription.getProductVersion()) < 0) {
						String errorMessage = MessageFormat.format(InstallMessages.Error_Upgrade2, new Object[] {
								installDescription.getProductName(),
								product.getVersionString(),
								installDescription.getProductVersionString()
							});
						problems.add(new Status(IStatus.WARNING, Installer.ID, errorMessage));
					}
				}
			}
		} catch (CoreException e) {
			Installer.log(e);
		}
	}
	
	/** Returns an error caused by problems in the product version or required products.  Null means no error. */
	private void checkAdminIfAllUsers(List<IStatus> problems) {
		IInstallDescription installDescription = Installer.getDefault().getInstallManager().getInstallDescription();
		if (installDescription.getAllUsers() && !Installer.getDefault().getInstallPlatform().isRunningAsAdmin()) {
			problems.add(new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_AllUsersNeedsAdmin));
		}
	}
	
	@Override
	public IStatus[] verifyInstallLocation(IPath location) {
		List<IStatus> statuses = new ArrayList<IStatus>();
		checkLocation(statuses, location);
		checkProductVersions(statuses, location);
		checkAdminIfAllUsers(statuses);
		return statuses.toArray(new IStatus[statuses.size()]);
	}
}
