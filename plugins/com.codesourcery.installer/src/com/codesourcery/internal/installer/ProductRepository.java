/*******************************************************************************
 *  Copyright (c) 2015 Mentor Graphics and others.
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.metadata.BasicVersion;
import org.eclipse.equinox.internal.p2.metadata.OSGiVersion;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import com.codesourcery.installer.Installer;

/**
 * This class manages a temporary repository containing an installable unit (IU) for the product.  The IU is set with
 * requirements on the IU's corresponding to installation components and is used to provision the product.
 */
@SuppressWarnings("restriction") // P2 internal OSGiVersion used
public class ProductRepository {
	/** Meta-data repository manager */
	private IMetadataRepositoryManager metadataRepositoryManager;
	/** Product meta-data repository */
	private IMetadataRepository metadataRepository;
	/** Repository directory */
	private File repositoryDirectory;
	/** Product IU */
	private IInstallableUnit productIu;

	/**
	 * Constructs a product repository.
	 * 
	 * @param agent Provisioning agent
	 */
	public ProductRepository(IProvisioningAgent agent) {
		metadataRepositoryManager = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
	}
	
	/**
	 * @return The meta-data repository manager.
	 */
	protected IMetadataRepositoryManager getMetadataRepositoryManager() {
		return metadataRepositoryManager;
	}

	/**
	 * @return The product meta-data repository.
	 */
	protected IMetadataRepository getMetadataRepository() {
		return metadataRepository;
	}

	/**
	 * @return The product name.
	 */
	protected String getProductName() {
		return Installer.getDefault().getInstallManager().getInstallDescription().getProductName();
	}
	
	/**
	 * @return The product identifier.
	 */
	protected String getProductId() {
		return Installer.getDefault().getInstallManager().getInstallDescription().getProductId();
	}

	/**
	 * @return The product version.
	 */
	protected Version getProductVersion() {
		return Installer.getDefault().getInstallManager().getInstallDescription().getProductVersion();
	}
	
	/**
	 * @return Returns the product vendor.
	 */
	protected String getProductVendor() {
		return Installer.getDefault().getInstallManager().getInstallDescription().getProductVendor();
	}
	
	/**
	 * Creates the product meta-data repository.
	 * Clients should call {@link #dispose()} when the repository is no longer needed.
	 * 
	 * @throws CoreException on failure
	 * @see #dispose()
	 */
	public IMetadataRepository createRepository() throws CoreException {
		try {
			// Create temporary directory for product repository
			repositoryDirectory = Files.createTempDirectory(null).toFile();
			
			// Create the P2 repository for meta-data information
			metadataRepository = getMetadataRepositoryManager().createRepository(repositoryDirectory.toURI(), getProductName(),
					IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			
			Installer.log("Created product repository at: " + repositoryDirectory.getAbsolutePath());
		}
		catch (Exception e) {
			Installer.fail(e);
		}
		
		return metadataRepository;
	}
	
	/**
	 * Creates the product IU and adds it to the product repository.  The product IU will be created with the same
	 * name and identifier as specified for the product in the installer description.  The product IU version will be
	 * set to the major, minor, and service version for the product specified in the installer description, unless if 
	 * already exists in the profile.  In that case, the version of the product IU will be set to the version of the 
	 * existing profile IU but with the qualifier segment incremented.
	 * 
	 * @param units IU's to be installed
	 * @return Product IU in the first element and the existing installed product IU if available in the second element.
	 * @throws CoreException on failure
	 */
	public IInstallableUnit[] createProductIu(IProfile profile, List<IInstallableUnit> units) throws CoreException {
		IInstallableUnit[] productUnits = new IInstallableUnit[2];
		try {
			// Check if there is an existing product IU installed in the profile
			IInstallableUnit existingProductIu = getProductIu(profile);
			
			// Remove old IU
			if (productIu != null) {
				ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
				ius.add(productIu);
				getMetadataRepository().removeInstallableUnits(ius);
			}
			
			// Product IU properties
			HashMap<String, String> properties = new HashMap<String, String>();
			properties.put(IInstallableUnit.PROP_NAME, getProductName());
			if (getProductVendor() != null) {
				properties.put(IInstallableUnit.PROP_PROVIDER, getProductVendor());
			}
		
			Version productVersion = null;
			// If updating the install, increment the qualifier part of the product IU version
			if (Installer.getDefault().getInstallManager().getInstallMode().isUpdate() && (existingProductIu != null)) {
				final Version existingProductVersion = existingProductIu.getVersion();
				if (existingProductVersion instanceof BasicVersion) {
					OSGiVersion version = (OSGiVersion)existingProductVersion;
					String qualifierSpec = version.getQualifier();
					int qualifier = ((qualifierSpec == null) || qualifierSpec.isEmpty()) ? 
							0 : 
							Integer.parseInt(version.getQualifier());
					
					// Create new product IU version
					productVersion = OSGiVersion.createOSGi(
							version.getMajor(), 
							version.getMinor(), 
							version.getMicro(), 
							String.format("%06d", qualifier + 1));
				}
			}
			// Else use all parts of the product version except for the qualifier.  The qualifier will be used to update
			// the product IU when the install is updated.
			else {
				if (getProductVersion() instanceof OSGiVersion) {
					OSGiVersion version = (OSGiVersion)getProductVersion();
					productVersion = OSGiVersion.createOSGi(version.getMajor(), version.getMinor(), version.getMicro());
				}
			}
			// This shouldn't happen
			if (productVersion == null) {
				Installer.fail("Product version is not in OSGI format.");
			}
			
			// Create product IU description
			InstallableUnitDescription productIuDesc = InstallUtils.createIuDescription(
					getProductId(), 
					productVersion, 
					true, 
					properties);
	
			// Add requirements for the units
			InstallUtils.addInstallableUnitRequirements(productIuDesc, units, false);
			
			// Remove previous product IU
			if (productIu != null) {
				ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
				ius.add(productIu);
				getMetadataRepository().removeInstallableUnits(ius);
			}
			
			// Create and add product IU
			ArrayList<IInstallableUnit> ius = new ArrayList<IInstallableUnit>();
			productIu = MetadataFactory.createInstallableUnit(productIuDesc);
			ius.add(productIu);
			getMetadataRepository().addInstallableUnits(ius);
			
			// New product IU
			productUnits[0] = productIu;
			// Existing product IU
			productUnits[1] = existingProductIu;
		}
		catch (Exception e) {
			Installer.fail(e);
		}
		
		return productUnits;
	}
	
	/**
	 * Returns an existing product IU if available.
	 *  
	 * @param profile Profile
	 * @return Product IU or <code>null</code>
	 */
	public IInstallableUnit getProductIu(IProfile profile) {
		ProfileAdapter adapter = new ProfileAdapter(profile);
		return adapter.findUnit(getProductId());
	}
	
	/**
	 * Disposes of the product repository. 
	 */
	public void dispose() {
		getMetadataRepositoryManager().removeRepository(metadataRepository.getLocation());
		try {
			FileUtils.deleteDirectory(repositoryDirectory.toPath());
			Installer.log("Deleted product repository at: " + repositoryDirectory.getAbsolutePath());
		}
		// Could not delete the temporary repository
		catch (Exception e) {
			Installer.log(e);;
		}
	}
}
