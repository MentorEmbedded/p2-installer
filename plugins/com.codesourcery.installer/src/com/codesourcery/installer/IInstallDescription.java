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
package com.codesourcery.installer;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.equinox.p2.metadata.IVersionedId;

/**
 * Information about a product installation.
 * This information is read from the installer properties file.
 * Methods are provided for reading defined data.
 * The {@link #getProperty(String)} method can be used to read custom
 * property values.
 */
public interface IInstallDescription {
	/** Wizard page navigation */
	public enum PageNavigation {
		/** No page navigation */
		NONE,
		/** Page navigation on the top */
		TOP,
		/** Page navigation on the left */
		LEFT
	}
	
	/**
	 * Sets an install property.
	 * 
	 * @param name Property name
	 * @param value Property value
	 */
	public void setProperty(String name, String value);

	/**
	 * Returns an install property.
	 * 
	 * @param name Property name
	 * @return Property or <code>null</code> if property is not
	 * available.
	 */
	public String getProperty(String name);

	/**
	 * Sets the title displayed in the installer window title area.
	 * 
	 * @param title Title to display
	 */
	public void setWindowTitle(String title);

	/**
	 * Returns the title to use for the installer window title.
	 * 
	 * @return Window title or <code>null</code>
	 */
	public String getWindowTitle();
	
	/**
	 * Sets the installer title.
	 * 
	 * @param value Window title
	 */
	public void setTitle(String value);

	/**
	 * Returns the title to use in the installer message area.
	 * 
	 * @return Title or <code>null</code>
	 */
	public String getTitle();
	
	/**
	 * Supplies a set of profile properties to be added when the profile is created.
	 * 
	 * @param properties the profile properties to be added
	 */
	public void setProfileProperties(Map<String, String> properties);

	/**
	 * Returns the P2 profile properties.
	 * 
	 * @return Properties
	 */
	public Map<String, String> getProfileProperties();
	
	/**
	 * Sets environment paths.
	 * 
	 * @param paths Paths
	 */
	public void setEnvironmnetPaths(String[] paths);
	
	/**
	 * Returns environment paths.
	 * 
	 * @return Environment paths or <code>null</code>
	 */
	public String[] getEnvironmentPaths();
	
	/**
	 * Sets the meta-data repositories.
	 * 
	 * @param value Repositories
	 */
	public void setMetadataRepositories(URI[] value);

	/**
	 * Returns the locations of the meta-data repositories to install from
	 * 
	 * @return a list of meta-data repository URLs
	 */
	public URI[] getMetadataRepositories();

	/**
	 * Sets the artifact repositories.
	 * 
	 * @param value Repositories
	 */
	public void setArtifactRepositories(URI[] value);
	
	/**
	 * Returns the locations of the artifact repositories to install from
	 * 
	 * @return a list of artifact repository URLs
	 */
	public URI[] getArtifactRepositories();
	
	/**
	 * Returns the locations any add-on repositories to query for
	 * additional components.
	 * 
	 * @return Add-on repository locations or <code>null</code>
	 */
	public URI[] getAddonRepositories();
	
	/**
	 * Sets the locations of add-on repositories to query for additional
	 * components.
	 * 
	 * @param repositories Add-on repositories
	 */
	public void setAddonRepositories(URI[] repositories);
	
	/**
	 * Sets the add-ons description.
	 * 
	 * @param addonDescription Add-ons description
	 */
	public void setAddonDescription(String addonDescription);
	
	/**
	 * Returns the description for add-ons.
	 * 
	 * @return Add-ons description or <code>null</code>
	 */
	public String getAddonDescription();

	/**
	 * Sets if add-ons require a login.
	 * 
	 * @param requiresLogin <code>true</code> if add-ons require a log-in
	 */
	public void setAddonsRequireLogin(boolean requiresLogin);
	
	/**
	 * Returns if add-ons require a login.
	 * 
	 * @return <code>true</code> if add-ons require a login
	 */
	public boolean getAddonsRequireLogin();
	
	/**
	 * Sets the update site locations.
	 * 
	 * @param updateSites Update site locations
	 */
	public void setUpdateSites(UpdateSite[] updateSites);
	
	/**
	 * Returns the update site locations to add.
	 * 
	 * @return Update site locations or <code>null</code>
	 */
	public UpdateSite[] getUpdateSites();
	
	/**
	 * Sets the install location relative to the product location.
	 * 
	 * @param location Install location
	 */
	public void setInstallLocation(IPath location);
	
	/**
	 * Returns the local file system location for installation.
	 * 
	 * @return a local file system location
	 */
	public IPath getInstallLocation();
	
	/**
	 * Sets the root install location.
	 * Note: This overrides the default set in the installer properties.
	 * 
	 * @param location Root location
	 */
	public void setRootLocation(IPath location);

	/**
	 * Returns the local file system root install location.
	 * 
	 * @return Root location
	 */
	public IPath getRootLocation();

	/**
	 * Sets the items to launch.
	 * 
	 * @param name Launch items
	 */
	public void setLaunchItems(LaunchItem[] value);
	
	/**
	 * Returns items to launch.
	 * 
	 * @return Launch items
	 */
	public LaunchItem[] getLaunchItems();
	
	/**
	 * Set the product identifier.
	 * 
	 * @param value Identifier for the product
	 */
	public void setProductId(String value);
	
	/**
	 * Returns the identifier for the product.
	 * 
	 * @return Product identifier
	 */
	public String getProductId();
	
	/**
	 * Set the name of the product being installed.
	 * 
	 * @param value the new name of the product to install
	 */
	public void setProductName(String value);
	
	/**
	 * Returns a human-readable name for this install.
	 * 
	 * @return the name of the product
	 */
	public String getProductName();
	
	/**
	 * Sets the product vendor.
	 * 
	 * @param value Vendor
	 */
	public void setProductVendor(String value);
	
	/**
	 * Returns the product vendor.
	 * 
	 * @return Vendor or <code>null</code>.
	 */
	public String getProductVendor();
	
	/**
	 * Sets the product version.
	 * @param value Version
	 */
	public void setProductVersion(String value);
	
	/**
	 * Returns the product version.
	 * 
	 * @return Version or <code>null</code>.
	 */
	public String getProductVersion();
	
	/**
	 * Sets the product help URL.
	 * 
	 * @param value Help URL
	 */
	public void setProductHelp(String value);
	
	/**
	 * Returns the product help URL.
	 * 
	 * @return Help URL or <code>null</code>.
	 */
	public String getProductHelp();
	
	/**
	 * Sets the profile name.
	 * 
	 * @param value Profile name
	 */
	public void setProfileName(String value);
	
	/**
	 * Returns the profile name.
	 * 
	 * @return Profile name
	 */
	public String getProfileName();

	/**
	 * Sets if all installable units in the profile should be removed on an
	 * uninstall or upgrade.
	 * 
	 * @param removeProfile <code>true</code> to remove all installabe units
	 * in profile.  <code>false</code> to remove only those installable units
	 * that were installed.
	 */
	public void setRemoveProfile(boolean removeProfile);
	
	/**
	 * Returns if all installable units in the profile should be removed
	 * on an uninstall or upgrade.  If this method returns <code>false</code>
	 * then only the installable units that were installed will be uninstalled.
	 * 
	 * @return <code>true</code> if profile should be removed
	 */
	public boolean getRemoveProfile();

	/**
	 * Sets the product licenses
	 * 
	 * @param value License descriptors or <code>null</code>.
	 */
	public void setLicenses(LicenseDescriptor[] value);
	
	/**
	 * Returns the product licenses.
	 * 
	 * @return License descriptor or <code>null</code> if product has no
	 * licenses.
	 */
	public LicenseDescriptor[] getLicenses();
	
	/**
	 * Sets if license information for installable units will be displayed.
	 * 
	 * @param value <code>true</code> to show IU license information
	 */
	public void setLicenseIU(boolean value);
	
	/**
	 * Returns if license information for installable units will be displayed.
	 * 
	 * @return <code>true</code> if IU license information will be displayed
	 */
	public boolean getLicenseIU();

	/**
	 * Sets the product information text.
	 * 
	 * @param value Information text or <code>null</code>.
	 */
	public void setInformationText(String value);
	
	/**
	 * Returns the product information text.
	 * 
	 * @return Information text or <code>null</code>.
	 */
	public String getInformationText();

	/**
	 * Sets the uninstall file and directory locations.
	 * 
	 * @param value Uninstall files
	 */
	public void setUninstallFiles(String[] value);
	
	/**
	 * Returns the files to copy into the uninstall
	 * directory.
	 * 
	 * @return Files or <code>null</code>
	 * @see #getUninstallLocation()
	 */
	public String[] getUninstallFiles();
	
	/**
	 * Sets uninstaller file name.
	 * 
	 * @param Uninstaller file name
	 */
	public void setUninstallerName(String name);
	
	/**
	 * Returns the name of uninstaller.
	 * 
	 * @return Name of uninstaller
	 */
	public String getUninstallerName();
	
	/**
	 * Set the list of roots to install
	 * 
	 * @param value the set of roots to install
	 */
	public void setRequiredRoots(IVersionedId[] value);

	/**
	 * Returns the set of required installabe units to install.
	 * 
	 * @return Required installable units
	 */
	public IVersionedId[] getRequiredRoots();
	
	/**
	 * Sets the list of optional roots to be
	 * installed.
	 * 
	 * @param value Optional roots or <code>null</code>
	 * if all roots are required.
	 */
	public void setOptionalRoots(IVersionedId[] value);
	
	/**
	 * Returns the set of optional installable units that can be selected for 
	 * installation.
	 * 
	 * @return Optional installable units or <code>null</code>.
	 */
	public IVersionedId[] getOptionalRoots();

	/**
	 * Sets the list of optional roots that should be selected for install
	 * by default.
	 * 
	 * @param value Optional roots
	 */
	public void setDefaultOptionalRoots(IVersionedId[] value);
	
	/**
	 * Returns the set of optional roots that should be selected for
	 * installation by default.
	 * 
	 * @return Optional roots or <code>null</code> to select no optional
	 * components.
	 * @see #getOptionalRoots()
	 */
	public IVersionedId[] getDefaultOptionalRoots();

	/**
	 * Sets the short-cut links.
	 * 
	 * @param value Links
	 */
	public void setLinks(LinkDescription[] value);
	
	/**
	 * Returns the short-cut links to create after installation.
	 * 
	 * @return Short-cut links or <code>null</code>
	 */
	public LinkDescription[] getLinks();
	
	/**
	 * Sets the short-cut links location.
	 * 
	 * @param value Short-cuts location
	 */
	public void setLinksLocation(IPath value);
	
	/**
	 * Returns location to create short-cuts in.
	 * 
	 * @return Short-cuts location
	 */
	public IPath getLinksLocation();
	
	/**
	 * Sets the wizard pages to display first.
	 * 
	 * @param wizardPages Wizard pages or <code>null</code> to use the default
	 * order.
	 */
	public void setWizardPagesOrder(String[] wizardPages);
	
	/**
	 * Returns the set of wizard pages that should be displayed first.
	 * 
	 * @return Wizard pages or <code>null</code> for default order.
	 */
	public String[] getWizardPagesOrder();
	
	/**
	 * Sets the regular expression find patterns to apply to
	 * P2 progress messages.
	 * 
	 * @param value Patterns or <code>null</code>
	 */
	public void setProgressFindPatterns(String[] value);

	/**
	 * Returns the regular expression find patterns to apply to
	 * P2 progress messages.
	 * 
	 * @return Find patterns or <code>null</code>
	 * @see #getProgressReplacePatterns()
	 */
	public String[] getProgressFindPatterns();
	
	/**
	 * Sets the regular expression replacement patterns to apply to
	 * P2 progress messages.
	 * 
	 * @param value Patterns
	 */
	public void setProgressReplacePatterns(String[] value);

	/**
	 * Returns the regular expression replacement patterns to apply to
	 * P2 progress messages.
	 * 
	 * @return Replacement patterns or <code>null</code>
	 * @see #getProgressFindPatterns()
	 */
	public String[] getProgressReplacePatterns();
	
	/**
	 * Sets the list of install module IDs for inclusion in this install.
	 * 
	 * @param value The list of module IDs or <code>null</code> for all 
	 * registered modules.
	 */
	public void setModuleIDs(String[] value);

	/**
	 * Returns a list of install module IDs for inclusion in this 
	 * install.
	 * 
	 * @return The list of module IDs or <code>null</code> for all registered
	 * modules.
	 */
	public String[] getModuleIDs();

	/**
	 * Sets required components to be sorted by name.
	 * 
	 * @param value <code>true</code> to sort required components
	 */
	public void setSortRequiredComponents(boolean value);

	/**
	 * Returns if required components should be sorted for display.
	 * 
	 * @return <code>true</code> to sort required components
	 */
	public boolean getSortRequiredComponents();
	
	/**
	 * Sets optional components to be sorted by name.
	 * 
	 * @param value <code>true</code> to sort optional components
	 */
	public void setSortOptionalComponents(boolean value);

	/**
	 * Returns if optional components should be sorted for display.
	 * 
	 * @return <code>true</code> to sort optional components
	 */
	public boolean getSortOptionalComponents();
	
	/**
	 * Sets visibility of components version on components page
	 * 
	 * @param hideComponentsVersion <code>true</code> to hide components version
	 */
	public void setHideComponentsVersion(boolean hideComponentsVersion);

	/**
	 * Returns if components version should be hidden.
	 * 
	 * @return <code>true</code> to hide components version
	 */
	public boolean getHideComponentsVersion();
	
	/**
	 * Sets the install wizard page navigation.
	 * 
	 * @param navigation Page navigation
	 */
	public void setPageNavigation(PageNavigation pageNavigation);
	
	/**
	 * Returns the install wizard page navigation.
	 * 
	 * @return Page navigation
	 */
	public PageNavigation getPageNavigation();
}
