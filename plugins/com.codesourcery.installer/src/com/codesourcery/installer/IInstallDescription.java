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
import org.eclipse.equinox.p2.metadata.Version;

/**
 * Information about a product installation.
 * This information is read from the installer properties file.
 * Methods are provided for reading defined data.
 * The {@link #getProperty(String)} method can be used to read custom
 * property values.
 */
public interface IInstallDescription {
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
	 * Sets the image to display in the title area.
	 * 
	 * @param imagePath Path to image file or <code>null</code>
	 */
	public void setTitleImage(IPath imagePath);
	
	/**
	 * Returns the image to display in the title area.
	 * 
	 * @return Path to image file or <code>null</code>
	 */
	public IPath getTitleImage();
	
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
	 * Sets whether the installation will take place for all users or not.
	 * 
	 * @param allUsers Whether installation will take place for all users or not.
	 */
	public void setAllUsers(boolean allUsers);

	/**
	 * Returns whether the installation will take place for all users or not.
	 * 
	 * @return Whether installation will take place for all users or not.
	 */
	public boolean getAllUsers();
	
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
	 * Sets the product version string.
	 * 
	 * @param value Version string
	 */
	public void setProductVersionString(String value);
	
	/**
	 * Returns the product version string.
	 * 
	 * @return Version string or <code>null</code>.
	 */
	public String getProductVersionString();
	
	/**
	 * Returns the product version.
	 * 
	 * @return Version
	 */
	public Version getProductVersion();
	
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
	 * Sets the regular expression find and replace regular expressions to apply to
	 * P2 progress messages.
	 * 
	 * @param find Find expressions
	 * @param replace Replace expressions
	 */
	public void setProgressPatterns(String[] find, String[] replace);

	/**
	 * Returns the regular expression find patterns to apply to
	 * P2 progress messages.
	 * 
	 * @return Find patterns or <code>null</code>
	 * @see #getProgressReplacePatterns()
	 */
	public String[] getProgressFindPatterns();
	
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
	 * <ul>
	 * <li>SWT.NONE</li>
	 * <li>SWT.TOP</li>
	 * <li>SWT.LEFT</li>
	 * </ul>
	 * 
	 * @param navigation Page navigation
	 */
	public void setPageNavigation(int pageNavigation);
	
	/**
	 * Returns the install wizard page navigation.
	 * 
	 * @return Page navigation
	 */
	public int getPageNavigation();
	
	/**
	 * Sets the install wizard page titles.
	 * 
	 * @param pageTitles Page titles
	 */
	public void setPageTitles(InstallPageTitle[] pageTitles);
	
	/**
	 * Returns the install wizard page titles.
	 * 
	 * @return Page title or <code>null</code>
	 */
	public InstallPageTitle[] getPageTitles();
	
	/**
	 * Sets the installation to be a patch for existing products.
	 * 
	 * @param <code>true</code> if patch
	 */
	public void setPatch(boolean patch);
	
	/**
	 * Returns if the installation is a patch for existing products.
	 * 
	 * @return <code>true</code> if patch
	 */
	public boolean getPatch();
	
	/**
	 * Sets a range of products required for this installation.
	 * 
	 * @param range Product range or <code>null</code
	 */
	public void setRequires(IProductRange[] range);
	
	/**
	 * Returns a range of products required for this installation.
	 * 
	 * @return Product range or <code>null</code>
	 */
	public IProductRange[] getRequires();
	
	/**
	 * Sets the text to display on the Welcome page of the installation wizard.
	 * 
	 * @param welcomeText Welcome text or <code>null</code> for default text
	 */
	public void setWelcomeText(String welcomeText);

	/**
	 * Returns the text to display on the Welcome page.
	 * 
	 * @return Welcome text or <code>null</code> for default text
	 */
	public String getWelcomeText();
	
	/**
	 * Sets the find/replace regular expressions to apply to missing requirement
	 * messages.
	 * 
	 * @param find Find expression
	 * @param replace Replace expressions
	 */
	public void setMissingRequirementExpressions(String[] find, String[] replace);
	
	/**
	 * Returns the find/replace regular expressions to apply to missing 
	 * requirement messages.
	 * 
	 * @return Array of find/replace expressions or <code>null</code>.  
	 * [][0] is the find expression.
	 * [][1] is the replace expression.
	 */
	public String[][] getMissingRequirementExpressions();
	
	/**
	 * Sets the installer to include all repositories during installation.
	 * 
	 * @param include <code>true</code> to include all repositories.
	 */
	public void setIncludeAllRepositories(boolean include);
	
	/**
	 * Returns if the installer should include all repositories during
	 * installation.
	 * 
	 * @return <code>true</code> to include all repositories, otherwise include
	 * only local repositories.
	 */
	public boolean getIncludeAllRepositories();
	
	/**
	 * Sets the installer to use an install registry for installed products.
	 * 
	 * @param useRegistry <code>true</code> to use install registry
	 */
	public void setUseInstallRegistry(boolean useRegistry);
	
	/**
	 * Returns if the installer should use an install registry.
	 * 
	 * @return <code>true</code> to use install registry
	 */
	public boolean getUseInstallRegistry();

	/**
	 * Sets that installer data directory.
	 * 
	 * @param dataLocation Path to installer data directory
	 */
	public void setDataLocation(IPath dataLocation);
	
	/**
	 * Returns the installer data directory.
	 * 
	 * @return Path to installer data directory or <code>null</code> to use
	 * data directory passed on command line or default data directory.
	 */
	public IPath getDataLocation();
}
