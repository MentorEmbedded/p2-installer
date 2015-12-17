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

import java.util.List;
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
	 * Mirror mode 
	 */
	public enum MirrorMode {
		/** Don't mirror install */
		NONE,
		/** Mirror all repositories */
		ALL,
		/** Mirror only if remote repositories are specified */
		REMOTE_ONLY
	}
	
	/**
	 * Wizard navigation
	 */
	public enum WizardNavigation {
		/** No navigation */
		NONE("none"),
		/** Top navigation */
		TOP("top"),
		/** Left navigation */
		LEFT("left"),
		/** Left navigation (minimal scheme) */
		LEFT_MINIMAL("left_minimal");
		
		/** Navigation name */
		private final String name;
		
		/**
		 * Constructs a wizard navigation.
		 * 
		 * @param name Name
		 */
		private WizardNavigation(String name) {
			this.name = name;
		}
		
		/**
		 * Returns the wizard navigation corresponding to a name.
		 * 
		 * @param text Name
		 * @return Value or <code>null</code>
		 */
		public static WizardNavigation fromString(String text) {
			WizardNavigation v = WizardNavigation.NONE;
			for (WizardNavigation value : values()) {
				if (value.name.equals(text)) {
					v = value;
					break;
				}
			}
			
			return v;
		}
	}
	
	/** Identifier of the default repository mirror */
	public static final String DEFAULT_MIRROR = "default";
	
	/** Repositories mirror location property */
	public static final String PROP_REPOS_MIRROR = "eclipse.p2.repos.mirror";//$NON-NLS-1$
	
	/** Existing version of product property */
	public static final String PROP_EXISTING_VERSION = "eclipse.p2.product.existingVersion";//$NON-NLS-1$

	// Text constants
	public static final String TEXT_INSTALL_ADDENDUM = "install.addendum";
	public static final String TEXT_INFORMATION = "information";
	public static final String TEXT_CATEGORY_INSTALL = "categoryInstall";
	public static final String TEXT_EXISTING_INSTALL = "existingInstall";
	public static final String TEXT_WELCOME = "welcome";
	public static final String TEXT_UNINSTALL_ADDENDUM = "uninstall.addendum";
	public static final String TEXT_PROGRESS_MIRRORING = "progress.mirroring";
	public static final String TEXT_INSTALL_FOLDER = "installFolder";
	public static final String TEXT_MIRROR_PAGE_MESSAGE = "mirrorPage.message";
	public static final String TEXT_MIRROR_PAGE_SECTION_INSTALL = "mirrorPage.section.install";
	public static final String TEXT_MIRROR_PAGE_INSTALL = "mirrorPage.install";
	public static final String TEXT_MIRROR_PAGE_SECTION_SAVE = "mirrorPage.section.save";
	public static final String TEXT_MIRROR_PAGE_SAVE = "mirrorPage.save";
	public static final String TEXT_MIRROR_PAGE_LOAD = "mirrorPage.load";
	public static final String TEXT_MIRROR_PAGE_DEFAULT_DIRECTORY = "mirrorPage.defaultDirectory";
	public static final String TEXT_PROGRESS_PAGE_NAME_INSTALLING = "progressPage.name.installing";
	public static final String TEXT_PROGRESS_PAGE_NAME_MIRRORING = "progressPage.name.mirroring";
	public static final String TEXT_PROGRESS_PAGE_MESSAGE_INSTALLING = "progressPage.message.installing";
	public static final String TEXT_PROGRESS_PAGE_MESSAGE_MIRRORING = "progressPage.message.mirroring";
	public static final String TEXT_RESULT_INSTALL = "result.install";
	public static final String TEXT_RESULT_MIRROR = "result.mirror";
	public static final String TEXT_FINISH_INSTALL = "finish.install";
	public static final String TEXT_FINISH_MIRROR = "finish.mirror";
	public static final String TEXT_SHORTCUTS_PAGE_MESSAGE = "shortcutsPage.message";
	public static final String TEXT_PATHS_PAGE_MESSAGE = "pathsPage.message";
	
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
	 * Returns all properties with names that start with a given prefix.
	 * The prefix will be removed from the property names.
	 * 
	 * @param prefix Property name prefix
	 * @return Map of property names (with prefix removed) to property values.  This map can be modified.
	 */
	public Map<String, String> getProperties(String prefix);

	/**
	 * Reads indexed property values.  The format of the property names should
	 * be:
	 *   <prefix>.0=
	 *   <prefix>.1=
	 *   ...
	 * 
	 * @param prefix Property name prefix
	 * @return Property value or <code>null</code> if property is not found
	 */
	public String[] getIndexedProperties(String prefix);
	
	/**
	 * Returns all properties and their values whose name starts with a specified prefix.
	 * Example:
	 *   my.prop.a = 1
	 *   my.prop.b = 2
	 *   my.prop = 3
	 *   
	 *   <code>getPrefixedProperties("my.prop")</code> will return a map with
	 *   <code>my.prop=3,my.prop.a=1, my.prop.b=2</code>
	 *   The map is ordered so that the default property ("my.prop") always appears first if it is available.
	 * 
	 * @param prefx Property name prefix
	 * @return Map of property names to property values
	 */
	public Map<String, String> getPrefixedProperties(String prefx);
	
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
	 * Returns all repository locations.  If the installer repository location properties 
	 * (<code>eclipse.p2.metadata</code> and <code>eclipse.p2.artifacts</code>) are expressed in terms of the 
	 * <code>${eclipse.p2.repos.mirror}</code> property and mirrors are defined then a location will be returned for each.
	 * 
	 * @return Repository locations
	 */
	public List<IRepositoryLocation> getRepositoryLocations();
	
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
	 * Sets if installation require install location to be empty.
	 * <code>true</code> if installation requires empty directory.
	 * 
	 * @param requireEmptyCheck
	 */
	public void setRequireEmptyInstallDirectory(boolean requireEmptyCheck);
	
	/**
	 * Returns if installation requires empty directory.
	 * 
	 * @return <code>true</code> if installation requires empty directory
	 */
	public boolean getRequireEmptyInstallDirectory();
	
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
	 * Sets the product category.  If any products with the same category are
	 * installed, their locations will be offered as choice for the destination
	 * for this installation.
	 * 
	 * @param category Category
	 */
	public void setProductCategory(String category);
	
	/**
	 * Returns the product category.
	 * 
	 * @return Category or <code>null</code>
	 */
	public String getProductCategory();
	
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
	 * Set the name of the product to be used during uninstall.
	 * 
	 * @param value the new uninstall name of the product
	 */
	public void setProductUninstallName(String value);
	
	/**
	 * Returns the name of the product to be used during uninstall.
	 * 
	 * @return the uninstall name of the product
	 */
	public String getProductUninstallName();

	/**
	 * Sets whether a root IU will be created for the product.
	 * 
	 * @param root <code>true</code> to create root IU
	 */
	public void setProductRoot(boolean root);
	
	/**
	 * @return <code>true</code> if root IU will be created for the product.
	 */
	public boolean getProductRoot();
	
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
	 * Sets the installable units that should be expanded in the wizard by
	 * default.  Only category installable units can be expanded.
	 * 
	 * @param expandedRoots Expanded roots
	 */
	public void setWizardExpandedRoots(IVersionedId[] expandedRoots);
	
	/**
	 * Returns the installable units that should be expanded in the wizard by 
	 * default.
	 * 
	 * @return Expanded units
	 */
	public IVersionedId[] getWizardExpandedRoots();

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
	 * Sets wizard pages that are excluded.
	 * 
	 * @param wizardPages Excluded wizard pages or <code>null</code>
	 */
	public void setWizardPagesExcluded(String[] wizardPages);
	
	/**
	 * Returns wizard pages that are excluded.
	 * 
	 * @return Excluded wizard pages or <code>null</code>
	 */
	public String[] getWizardPagesExcluded();
	
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
	 * Sets the install size format specification.  The following can be used 
	 * in the specification:
	 * <ul>
	 * <li>{0} - Size of the installation</li>
	 * <li>{1} - Size required during installation</li>
	 * <li>{2} - Useable free space on installation location</li>
	 * </ul>
	 * 
	 * @param format Size format
	 */
	public void setInstallSizeFormat(String format);
	
	/**
	 * Returns the install size format specification.
	 * 
	 * @return Size format
	 */
	public String getInstallSizeFormat();
	
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
	 * @param showVersion <code>true</code> to display component versions
	 */
	public void setShowComponentVersions(boolean showVersion);

	/**
	 * Returns if components version should be hidden.
	 * 
	 * @return <code>true</code> if component versions will be displayed
	 */
	public boolean getShowComponentVersions();
	
	/**
	 * Sets if optional components should be shown before required components.
	 * 
	 * @param showOptional <code>true</code> to show optional components first.
	 */
	public void setShowOptionalComponentsFirst(boolean showOptional);
	
	/**
	 * Returns if optional components should be shown before required components.
	 * 
	 * @return <code>true</code> to show optional components first.
	 */
	public boolean getShowOptionalComponentsFirst();
	
	/**
	 * Sets the install wizard page navigation.
	 * 
	 * @param navigation Page navigation
	 */
	public void setPageNavigation(WizardNavigation pageNavigation);
	
	/**
	 * Returns the install wizard page navigation.
	 * 
	 * @return Page navigation
	 */
	public WizardNavigation getPageNavigation();
	
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
	
	/**
	 * Sets the uninstall mode.
	 * 
	 * @param mode Uninstall mode
	 */
	public void setUninstallMode(UninstallMode mode);
	
	/**
	 * Returns the uninstall mode.
	 * 
	 * @return Uninstall mode or <code>null</code> if no uninstaller will be
	 * included in the installation
	 */
	public UninstallMode getUninstallMode();

	/**
	 * Sets whether an ordered planner will be used during provisioning.  The
	 * ordered planner will ensure that IU's are provisioned in an order
	 * according to their dependencies.
	 *  
	 * @param ordered <code>true</code> to use ordered planner,
	 * <code>false</code> to use the default planner.
	 */
	public void setOrderPlanner(boolean ordered);
	
	/**
	 * Returns whether an ordered planner will be used during provisioning.  The
	 * ordered planner will ensure that IU's are provisioned in an order
	 * according to their dependencies.
	 * 
	 * @return <code>true<code> if the ordered planner will be used
	 * <code>false</code> if the default planner will be used.
	 */
	public boolean getOrderPlanner();
	
	/**
	 * Sets install constraints.
	 * 
	 * @param constraints Constraints or <code>null</code>
	 */
	public void setInstallConstraints(IInstallConstraint[] constraints);
	
	/**
	 * Returns install constraints.
	 * 
	 * @return Constraints or <code>null</code>
	 */
	public IInstallConstraint[] getInstallConstraints();
	
	/**
	 * Sets if the installer supports product upgrades.
	 * 
	 * @param supportsUpgrade <code>true</code> if supports upgrades
	 */
	public void setSupportsUpgrade(boolean supportsUpgrade);
	
	/**
	 * Returns if the installer supports product upgrades.
	 * 
	 * @return <code>true</code> if supports upgrades
	 */
	public boolean getSupportsUpgrade();
	
	/**
	 * Sets if the installer supports product updates.
	 * 
	 * @param supportsUpdate <code>true</code> if supports updates
	 */
	public void setSupportsUpdate(boolean supportsUpdate);
	
	/**
	 * Returns if the installer supports product updates.
	 * 
	 * @return <code>true</code> if supports updates
	 */
	public boolean getSupportsUpdate();
	
	/**
	 * Sets the actions that should be excluded.
	 * 
	 * @param actions Action identifiers or <code>null</code>
	 */
	public void setExcludedActions(String[] actions);
	
	/**
	 * Returns the actions that should be excluded.
	 * 
	 * @return Identifiers of actions that should be excluded or 
	 * <code>null</code>
	 */
	public String[] getExcludedActions();
	
	/**
	 * Sets install data defaults.
	 * 
	 * @param properties Properties
	 */
	public void setInstallDataDefaults(Map<String, String> properties);
	
	/**
	 * Returns install data defaults.
	 * 
	 * @return Properties
	 */
	public Map<String, String> getInstallDataDefaults();
	
	/**
	 * Sets the minimum version of the product that can be upgraded.
	 * 
	 * @param version Minimum version or <code>null</code>
	 */
	public void setMinimumUpgradeVersion(Version version);
	
	/**
	 * Returns the minimum version of the product that can be upgraded.
	 * 
	 * @return Minimum version or <code>null</code>
	 */
	public Version getMinimumUpgradeVersion();
	
	/**
	 * Sets installer text.  If no installer text is set, default text will be used.
	 * 
	 * @param id Identifier of text
	 * @param text Installer text or <code>null</code>
	 */
	public void setText(String id, String text);
	
	/**
	 * Returns installer text.
	 * 
	 * @param id Identifier of text property
	 * @param message Message to use if property is not available or <code>null</code>
	 * @return Installer text or <code>null</code>
	 */
	public String getText(String id, String message);
	
	/**
	 * Sets the mirror mode.
	 * 
	 * @param mirrorMode Mirror mode
	 */
	public void setMirrorMode(MirrorMode mirrorMode);
	
	/**
	 * @return The mirror mode
	 */
	public MirrorMode getMirrorMode();

	/**
	 * Sets the network time-out value.
	 * 
	 * @param timeout Time-out in milliseconds or <code>-1</code>.
	 */
	public void setNetworkTimeout(int timeout);
	
	/**
	 * @return The network time-out or <code>-1</code>.
	 */
	public int getNetworkTimeout();
	
	/**
	 * Sets the network retries value.
	 * 
	 * @param retries Number of times to retry or <code>-1</code>.
	 */
	public void setNetworkRetry(int retries);

	/**
	 * @return The number of tiems to retry network or <code>-1</code>.
	 */
	public int getNetworkRetry();
}
