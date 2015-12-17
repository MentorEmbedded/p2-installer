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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.codesourcery.installer.AbstractInstallVerifier;
import com.codesourcery.installer.IInstallComponent;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
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
	
	/**
	 * Checks if the install location is empty.
	 * 
	 * @param location Install location.
	 * @param manifest Install manifest.
	 * @return <code>IStatus.OK</code> if location is empty.
	 */
	private IStatus checkEmpty(IPath location, InstallManifest manifest) {
		IStatus status = Status.OK_STATUS;
		
		boolean productsFound = ((manifest != null) && (manifest.getProducts().length > 0));
		boolean installationFound = false;
		if (RepositoryManager.getDefault().getAgent() != null) {
			if (location.isPrefixOf(RepositoryManager.getDefault().getAgentLocation())) {
				installationFound = true;
			}
			
		}
		
		// If no existing products are found at the location and installation has not been already initialized
		// in location.
		if (!productsFound && !installationFound) {
			if ((location != null) && location.toFile().exists() && (location.toFile().list().length > 0)) {
				String errorMessage = MessageFormat.format(InstallMessages.Error_NonEmptyInstallLocation0, location.toOSString());
				status = new Status(IStatus.ERROR, Installer.ID, errorMessage);
			}
		}
		
		return status;
	}

	/**
	 * Checks permissions for the install location.
	 * 
	 * @param location Location.
	 * @return <code>IStatus.OK</code> if location is writable.
	 */
	private IStatus checkPermissions(IPath location) {
		IStatus status = Status.OK_STATUS;
		
		File directory = location.toFile();
		if (directory.exists()) {
			IInstallMode installMode = Installer.getDefault().getInstallManager().getInstallMode();
			if (installMode.isInstall() && !installMode.isUpdate() && !installMode.isUpgrade()) {
				// Check write permissions
				if (!Files.isWritable(directory.toPath())) {
					status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.NoWritePermissions);
				}
				// Check read permissions
				else if (!Files.isReadable(directory.toPath())) {
					status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.NoWritePermissions);
				}
				// If that succeeds, check if a file really can be written (Windows permissions).
				else {
					// File.canWrite() can't be used as it does not check Windows security permissions
					// Files.isWriteable() can't be used.  Although it checks Windows security permissions, it
					// is unreliable (fixed only in Java 8 (b84) and back-ported to Java 7u40).
					File tempFile = location.append(UUID.randomUUID().toString()).toFile();
					try {
						if (tempFile.createNewFile()) {
							tempFile.delete();
						}
					} catch (IOException e) {
						status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.NoWritePermissions);
					}
				}
			}
		}

		return status;
	}

	/**
	 * Checks for required installed products.
	 * 
	 * @param manifest Install manifest.
	 * @param ranges Ranges for installed products.
	 * @return <code>IStatus.ERROR</code> if no product is installed for the required range.
	 */
	private IStatus checkProductRanges(InstallManifest manifest, IProductRange[] ranges) {
		IStatus status = Status.OK_STATUS;
		
		IInstallProduct[] matchedProducts = null;
		if (manifest != null) {
			matchedProducts = manifest.getProducts(ranges);
		}
		// No products found that match required ranges
		if ((matchedProducts == null) || (matchedProducts.length == 0)) {
			status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_ProductsNotFound);
		}

		return status;
	}
	
	/**
	 * Checks for an existing installed version of the product.
	 * 
	 * @param installDescription Install description
	 * @param manifest Install manifest
	 * @return <code>IStatus.ERROR</code> if an existing version of the product is already installed.
	 */
	private IStatus checkExistingVersion(IInstallDescription installDescription, InstallManifest manifest) {
		IStatus status = Status.OK_STATUS;

		IInstallMode installMode = Installer.getDefault().getInstallManager().getInstallMode();
		if ((manifest != null) && !installMode.isUpdate()) {
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
					boolean upgradeSupported = true;
					
					// Upgrade supported
					if (installDescription.getSupportsUpgrade()) {
						if (installDescription.getMinimumUpgradeVersion() != null) {
							upgradeSupported = product.getVersion().compareTo(installDescription.getMinimumUpgradeVersion()) >= 0;
						}
					}
					// Upgrade not supported
					else {
						upgradeSupported = false;
					}
					
					if (upgradeSupported) {
						String errorMessage = MessageFormat.format(InstallMessages.Error_Upgrade2, new Object[] {
								installDescription.getProductName(),
								product.getVersionString(),
								installDescription.getProductVersionString()
							});
						status = new Status(IStatus.WARNING, Installer.ID, errorMessage);
					}
					else {
						String errorMessage = MessageFormat.format(InstallMessages.Error_NoUpgrade1, new Object[] {
								installDescription.getProductName(),
								product.getVersionString()
						});
						status = new Status(IStatus.ERROR, Installer.ID, errorMessage);
					}
				}
			}
		}
		
		return status;
	}
	
	@Override
	public IStatus verifyInstallLocation(IPath location) {
		IStatus status = Status.OK_STATUS;
		
		try {
			IInstallDescription installDescription = Installer.getDefault().getInstallManager().getInstallDescription();
			InstallManifest manifest = InstallManifest.loadManifest(location);
			
			// No location specified
			if (location.isEmpty()) {
				status = new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_PleaseSpecifyLocation);
			}

			// Check if install location is required to be empty
			if (status.isOK() && installDescription.getRequireEmptyInstallDirectory()) {
				status = checkEmpty(location, manifest);
			}
			
			// Requirement on existing products
			if (status.isOK()) {
				IProductRange[] ranges = installDescription.getRequires();
				if (ranges != null) {
					status = checkProductRanges(manifest, ranges);
				}
				// Check for existing version of install product
				else {
					status = checkExistingVersion(installDescription, manifest);
				}
			}
			
			// Check that the install location is writable
			if (status.isOK()) {
				status = checkPermissions(location);
			}
		} catch (CoreException e) {
			Installer.log(e);
		}

		return status;
	}

	@Override
	public IStatus verifyInstallComponentSelection(
			IInstallComponent[] selectedComponents) {
		// For new installation, at least one component must be selected
		if (!Installer.getDefault().getInstallManager().getInstallMode().isUpdate() && (selectedComponents.length == 0)) {
			return new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_SelectAtLeastOneComponent);
		}
		
		return Status.OK_STATUS;
	}
}
