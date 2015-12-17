/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Mentor Graphics - Modified from original P2 stand-alone installer
 *******************************************************************************/
package com.codesourcery.internal.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.CollectionUtils;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.metadata.VersionedId;

import com.codesourcery.installer.IInstallConstraint;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.IProductRange;
import com.codesourcery.installer.IRepositoryLocation;
import com.codesourcery.installer.InstallPageTitle;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.LaunchItem.LaunchItemPresentation;
import com.codesourcery.installer.LaunchItem.LaunchItemType;
import com.codesourcery.installer.LicenseDescriptor;
import com.codesourcery.installer.LinkDescription;
import com.codesourcery.installer.UninstallMode;
import com.codesourcery.installer.UpdateSite;

@SuppressWarnings("restriction") // Accesses internal P2 API's
public class InstallDescription implements IInstallDescription {
	/** P2 profile properties prefix */
	public static final String PROP_P2_PROFILE_PREFIX = "p2."; //$NON-NLS-1$
	/** Install data defaults propert */
	public static final String PROP_DATA_PREFIX = "eclipse.p2.default.";//$NON-NLS-1$
	/** Artifact repositories property */
	public static final String PROP_ARTIFACT_REPOSITORY = "eclipse.p2.repos.artifacts";//$NON-NLS-1$
	/** Install location property */
	public static final String PROP_INSTALL_LOCATION = "eclipse.p2.location.install";//$NON-NLS-1$
	/** Meta-data repositories property */
	public static final String PROP_METADATA_REPOSITORY = "eclipse.p2.repos.metadata";//$NON-NLS-1$
	/** Profile name property */
	public static final String PROP_PROFILE_NAME = "eclipse.p2.profile.name";//$NON-NLS-1$
	/** <code>true</code> to remove profile on uninstall property */
	public static final String PROP_REMOVE_PROFILE = "eclipse.p2.profile.remove";//$NON-NLS-1$
	/** Required roots property */
	public static final String PROP_REQUIRED_ROOTS = "eclipse.p2.roots.required";//$NON-NLS-1$
	/** Optional roots property */
	public static final String PROP_OPTIONAL_ROOTS = "eclipse.p2.roots.optional";//$NON-NLS-1$
	/** Relationships property */
	public static final String PROP_ROOTS_CONSTRAINTS = "eclipse.p2.roots.constraints";//$NON-NLS-1$
	/** License property */
	public static final String PROP_LICENSE = "eclipse.p2.license";//$NON-NLS-1$
	/** License installable units property */
	public static final String PROP_LICENSE_IU = "eclipse.p2.license.iu";//$NON-NLS-1$
	/** Install information property */
	public static final String PROP_INFORMATION = "eclipse.p2.wizard.text.information";//$NON-NLS-1$
	/** Optional roots default property */
	public static final String PROP_OPTIONAL_ROOTS_DEFAULT = "eclipse.p2.roots.optional.default";//$NON-NLS-1$
	/** Product identifier property */
	public static final String PROP_PRODUCT_ID = "eclipse.p2.product.id";//$NON-NLS-1$
	/** Product name property */
	public static final String PROP_PRODUCT_NAME = "eclipse.p2.product.name";//$NON-NLS-1$
	/** Product category property */
	public static final String PROP_PRODUCT_CATEGORY = "eclipse.p2.product.category";//$NON-NLS-1$
	/** Vendor name property */
	public static final String PROP_PRODUCT_VENDOR = "eclipse.p2.product.vendor";//$NON-NLS-1$
	/** Product version property */
	public static final String PROP_PRODUCT_VERSION = "eclipse.p2.product.version";//$NON-NLS-1$
	/** Product help link property */
	public static final String PROP_PRODUCT_HELP = "eclipse.p2.product.help";//$NON-NLS-1$
	/** Product root property */
	public static final String PROP_PRODUCT_ROOT = "eclipse.p2.product.root";//$NON-NLS-1$
	/** Product uninstall name */
	public static final String PROP_PRODUCT_UNINSTALL_NAME = "eclipse.p2.product.uninstall.name";//$NON-NLS-1$
	/** Root install location property */
	public static final String PROP_ROOT_LOCATION_PREFIX = "eclipse.p2.location.root";//$NON-NLS-1$
	/** Wizard text prefix property */
	public static final String PROP_WIZARD_TEXT_PREFIX = "eclipse.p2.wizard.text";//$NON-NLS-1$
	/** Uninstall files property */
	public static final String PROP_UNINSTALL_FILES = "eclipse.p2.uninstall.files";//$NON-NLS-1$
	/** Links property */
	public static final String PROP_LINKS = "eclipse.p2.links";//$NON-NLS-1$
	/** Links location property */
	public static final String PROP_LINKS_LOCATION = "eclipse.p2.location.links";//$NON-NLS-1$
	/** Links default property */
	public static final String PROP_LINKS_DEFAULT = "eclipse.p2.links.default";//$NON-NLS-1$
	/** Environment paths property */
	public static final String PROP_ENV_PATHS = "eclipse.p2.location.paths";//$NON-NLS-1$
	/** Launch items property */
	public static final String PROP_LAUNCH = "eclipse.p2.wizard.launch";//$NON-NLS-1$
	/** Launch presentation property */
	public static final String PROP_LAUNCH_PRESENTATION = "eclipse.p2.wizard.launch.presentation";//$NON-NLS-1$
	/** Installer window title property */
	public static final String PROP_WINDOW_TITLE = "eclipse.p2.window.title";//$NON-NLS-1$
	/** Installer title property */
	public static final String PROP_TITLE = "eclipse.p2.wizard.title";//$NON-NLS-1$
	/** Installer title image property */
	public static final String PROP_TITLE_IMAGE = "eclipse.p2.wizard.image";//$NON-NLS-1$
	/** Installer wizard pages property */
	public static final String PROP_WIZARD_PAGES_ORDER = "eclipse.p2.wizard.pages.order";//$NON-NLS-1$
	/** Installer wizard pages excluded property */
	public static final String PROP_WIZARD_PAGES_EXCLUDE = "eclipse.p2.wizard.pages.exclude";//$NON-NLS-1$
	/** Progress regular expression prefix */
	public static final String PROP_PROGRESS_PREFIX = "eclipse.p2.progress";//$NON-NLS-1$
	/** Installer modules property */
	public static final String PROP_MODULES = "eclipse.p2.modules";//$NON-NLS-1$
	/** Update sites property */
	private static final String PROP_UPDATE = "eclipse.p2.update";//$NON-NLS-1$
	/** Show Components version property */
	public static final String PROP_SHOW_COMPONENT_VERSIONS = "eclipse.p2.wizard.components.showVersion";//$NON-NLS-1$
	/** Show optional components first property */
	public static final String PROP_SHOW_OPTIONAL_FIRST = "eclipse.p2.wizard.components.showOptionalFirst";//$NON-NLS-1$
	/** Default expanded roots property */
	public static final String PROP_EXPAND_ROOTS = "eclipse.p2.wizard.components.expand";//$NON-NLS-1$
	/** Install wizard page navigation property */
	public static final String PROP_WIZARD_NAVIGATION = "org.eclipse.p2.wizard.navigation";//$NON-NLS-1$
	/** Install wizard page titles property */
	public static final String PROP_WIZARD_PAGE_TITLES = "eclipse.p2.wizard.pages.titles";//$NON-NLS-1$
	/** Excluded actions property */
	public static final String PROP_EXCLUDED_ACTIONS = "eclipse.p2.actions.exclude";//$NON-NLS-1$
	/** Patch property */
	public static final String PROP_PATCH = "eclipse.p2.patch";//$NON-NLS-1$
	/** Requires property */
	public static final String PROP_REQUIRES = "eclipse.p2.requires";//$NON-NLS-1$
	/** Missing requirement expression property prefix */
	public static final String PROP_MISSING_REQUIREMENT_PREFIX = "eclipse.p2.missingRequirement";//$NON-NLS-1$
	/** Expression property find suffix */
	public static final String PROP_FIND_SUFFIX = ".find";//$NON-NLS-1$
	/** Expression property replace suffix */
	public static final String PROP_REPLACE_SUFFIX = ".replace";//$NON-NLS-1$
	/** Include all repositories property */
	public static final String PROP_INCLUDE_ALL_REPOSITORIES = "eclipse.p2.includeAllRepositories";//$NON-NLS-1$
	/** Use install registry property */
	public static final String PROP_USE_INSTALL_REGISTRY = "eclipse.p2.useInstallRegistry";//$NON-NLS-1$
	/** Installer  data location property */
	public static final String PROP_DATA_LOCATION = "eclipse.p2.location.data";//$NON-NLS-1$
	/** Order planner property */
	public static final String PROP_ORDER_PLANNER = "eclipse.p2.orderPlanner";//$NON-NLS-1$
	/** Install mode */
	public static final String PROP_INSTALL = "eclipse.p2.install";//$NON-NLS-1$
	/** Install size format */
	public static final String PROP_INSTALL_SIZE_FORMAT = "eclipse.p2.install.size.format";//$NON-NLS-1$
	/** Uninstall mode */
	public static final String PROP_UNINSTALL = "eclipse.p2.uninstall";//$NON-NLS-1$
	/** Show product in uninstaller property */
	public static final String UNINSTALL_SHOW_UNINSTALL = "SHOW_UNINSTALL";//$NON-NLS-1$
	/** Create add/remove entry property */
	public static final String UNINSTALL_ADDREMOVE = "CREATE_ADD_REMOVE";//$NON-NLS-1$
	/** Remove installation directories on uninstall property */
	public static final String UNINSTALL_REMOVE_DIRS = "REMOVE_DIRS";//$NON-NLS-1$
	/** Minimum version that can be upgraded property  **/
	public static final String PROP_MINIMUM_UPGRADE_VERSION = "eclipse.p2.install.upgrade.minVersion";//$NON-NLS-1$
	/** Network time-out property  **/
	public static final String PROP_NETWORK_TIMEOUT = "eclipse.p2.network.timeout";//$NON-NLS-1$
	/** Network retry property  **/
	public static final String PROP_NETWORK_RETRY = "eclipse.p2.network.retry";//$NON-NLS-1$
	
	/** Base location for installer */
	private URI base;
	/** Installer properties */
	private Map<String, String> properties = new HashMap<String, String>();
	/** P2 profile properties */
	private Map<String, String> profileProperties = new HashMap<String, String>();
	/** Root install location */
	private IPath rootLocation;
	/** P2 install location */
	private IPath p2Location;
	/** Items to launch after installation */
	private LaunchItem[] launchItems;
	/** Update sites to add */
	private UpdateSite[] updateSites;
	/** Product identifier */
	private String productId;
	/** Product name */
	private String productName;
	/** Product category */
	private String productCategory;
	/** Product vendor */
	private String productVendor;
	/** Product original version text */
	private String productVersionText;
	/** Product version */
	private Version productVersion;
	/** Product help URL */
	private String productHelp;
	/** Product name used during uninstall*/
	private String uninstallproductName;
	/** Required root installable units */
	private IVersionedId[] requiredRoots;
	/** Optional root installable units */
	private IVersionedId[] optionalRoots;
	/** Default optional root installable units */
	private IVersionedId[] optionalRootsDefault;
	/** Roots that should be expanded in the wizard by default */
	private IVersionedId[] expandedRoots;
	/** Product licenses */
	private LicenseDescriptor[] licenses;
	/** P2 profile identifier */
	private String profileName;
	/** <code>true</code> to remove entire profile on uninstall */
	private boolean removeProfile = false;
	/** Files to copy for uninstaller */
	private String[] uninstallFiles;
	/** Uninstaller name */
	private String uninstallerName;
	/** Short-cuts to create */
	private LinkDescription[] links;
	/** Short-cuts location */
	private IPath linksLocation;
	/** Environment pats to add */
	private String[] environmentPaths;
	/** Installer window title */
	private String windowTitle;
	/** Installer title */
	private String title;
	/** Path to title image file */
	private IPath titleImage;
	/** Install wizard pages */
	private String[] wizardPages;
	/** Install wizard pages order */
	private String[] wizardPagesOrder;
	/** Progress regular expression find patterns */
	private String[] progressFindPatterns;
	/** Progress regular expression replace patterns */
	private String[] progressReplacePatterns;
	/** Active module identifiers */
	private String[] moduleIDs;
	/** <code>true</code> to show components version */
	private boolean showComponentVersions = false;
	/** <code>true</code> to show optional components first */
	private boolean showOptionalComponentsFirst = false;
	/** Install wizard page navigation */
	private WizardNavigation pageNavigation;
	/** Wizard page titles */
	private InstallPageTitle[] pageTitles;
	/** Missing requirement find/replace regular expressions */
	private String[][] missingRequirementExpressions;
	/** <code>true</code> to include remote repositories during installation */
	private boolean includeRemoteRepositories = false;
	/** <code>true</code> if install is a patch */
	private boolean patch;
	/** Product ranges */
	private IProductRange[] productRanges;
	/** <code>true</code> to use install registry */
	private boolean useInstallRegistry = true;
	/** Installer data location */
	private IPath dataLocation;
	/** Uninstall mode or <code>null</code> if no uninstaller */
	private UninstallMode uninstallMode;
	/** <code>true</code> to use ordered planner */
	private boolean orderPlanner=true;
	/** Install relationships */
	private IInstallConstraint[] relationships;
	/** <code>true</code> if product upgrades are supported */
	private boolean supportsUpgrade = true;
	/** <code>true</code> if product upgrades are supported */
	private boolean supportsUpdate = true;
	/** Excluded actions */
	private String[] excludedActions;
	/** Install data defaults */
	private Map<String, String> installDataDefaults;
	/** Check installer directory **/
	private boolean checkEmtptyInstallDirectory=false;
	/** Minimum version of product that can be upgraded */
	private Version minimumUpgradeVersion;
	/** Size format specification */
	private String sizeFormat;
	/** Installer text */
	private HashMap<String, String> installerText = new HashMap<String, String>();
	/** Mirror mode */
	private MirrorMode mirrorMode = MirrorMode.NONE;
	/** Network time-out */
	private int networkTimeout = -1;
	/** Network retry */
	private int networkRetry = -1;
	/** <code>true</code> to create product root IU */
	private boolean productRoot = true;

	/**
	 * Loads an install description.
	 * 
	 * @param site Site for description
	 * @param props Install property values to set or <code>null</code>
	 * description file or <code>null</code> to use default
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void load(URI site, Map<String, String> props, IProgressMonitor monitor) 
			throws CoreException {
		try {
			InputStream in = null;
			try {
				in = new RepositoryTransport().stream(site, monitor);
				properties = CollectionUtils.loadProperties(in);
			} finally {
				if (in != null) {
					try {
						in.close();
					}
					catch (IOException e) {
						// Ignore
					}
				}
			}

			// Replace properties
			if (props != null) {
				Iterator<Entry<String, String>> iter = props.entrySet().iterator();
				while (iter.hasNext()) {
					Entry<String, String> prop = iter.next();
					String name = prop.getKey();
					String value = prop.getValue();
					properties.put(name, value);
				}
			}
			
			// Resolve variables
			resolveVariables(properties);
			
			base = getBase(site);
			// Load properties
			initialize();
			// Load profile properties
			initializeProfileProperties();
			// Load install data defaults
			initializeDataDefaults();
		}
		catch (Exception e) {
			Installer.fail("Failed to load install description.", e);
		}
	}

	/**
	 * Reads a property value and performs any special processing specified in a prefix.  The property name can specify
	 * the following prefixes:
	 * <ul>
	 * <li>filename: - Converts the property value to a file system safe format.</li>
	 * <li>nospace:  - Replaces any spaces in the property value with underscores.</li>
	 * <li>nonum:    - Removes any numbers or periods from the property value.</li>
	 * <li>upper:    - Converts the property value to uppercase.</li>
	 * <li>lower:    - Converts the property value to lowercase.</li>
	 * </ul>
	 * 
	 * If the property name contains no prefix, the property value will be returned as is.
	 * 
	 * @param variable Property name optionally containing a prefix
	 * @return Property value
	 */
	private String resolvePrefixedProperty(String variable) {
		String prefix = null;
		
		// Check if special prefix is present
		int prefixIndex = variable.indexOf(':');
		if (prefixIndex != -1) {
			prefix = variable.substring(0, prefixIndex);
			variable = variable.substring(prefixIndex + 1);
		}
		
		// Read the property value
		String propertyValue = readProperty(variable);
		// If property is not available, try system
		// environment variable
		if (propertyValue == null) {
			propertyValue = System.getenv(variable);
		}

		// Handle any prefix processing
		if ((prefix != null) && (propertyValue != null)) {
			switch (prefix) {
			// Replace characters that are no a-z, A-Z, 0-9, or a space
			// with dashes.
			case "filename":
				propertyValue = propertyValue.replaceAll("[^a-zA-Z0-9 ]", "-");
				break;
			// Replace any spaces with underscores
			case "nospace":
				propertyValue = propertyValue.replaceAll("[ ]", "_");
				break;
			// Remove any number characters
			case "nonum":
				propertyValue = propertyValue.replaceAll("[^a-zA-Z ]", "");
				break;
			// Convert all characters to upper case
			case "upper":
				propertyValue = propertyValue.toUpperCase();
				break;
			// Convert all characters to lower case
			case "lower":
				propertyValue = propertyValue.toLowerCase();
				break;
			}
		}
		
		return propertyValue;
	}
	
	/**
	 * Resolves property variables, replacing any ${property} with the value
	 * of the 'property'.
	 * 
	 * @param properties Properties to replace
	 */
	private void resolveVariables(Map<String, String> properties) {
		try {
			Iterator<Entry<String, String>> iter = properties.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, String> entry = iter.next();
				String value = entry.getValue();
				int start = 0;
				int index, index2;
				// Start of variable
				while ((index = value.indexOf('$', start)) != -1) {
					if (index + 1 < value.length()) {
						if (value.charAt(index + 1) == '{') {
							index2 = value.indexOf('}', index);
							if (index2 != -1) {
								// Property to replace
								String property = value.substring(index + 2, index2);
								
								// Defer resolving late binding properties
								if (property.equals(PROP_REPOS_MIRROR) || property.equals(PROP_EXISTING_VERSION)) {
									start ++;
								}
								// Resolve property references
								else {
									// Property value
									String sub = resolvePrefixedProperty(property);
									// Replace variable
									value = value.substring(0, index) + 
											(sub != null ? sub : "") + 
											value.substring(index2 + 1);
									entry.setValue(value);
								}
							}
							else {
								Installer.log("Error in resolving macros: No matching \"}\" for \"${\".");
								break;
							}
						}
						else if (index + 1 < value.length()){
							start = index + 1;
						}
					}
					else {
						break;
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}

	/**
	 * Returns the base URI.
	 * 
	 * @return Base URI
	 */
	protected URI getBase() {
		return base;
	}
	
	/**
	 * Initializes the install description.
	 */
	protected void initialize() {
		String property;
		
		// Update sites
		property = readProperty(PROP_UPDATE);
		if (property != null) {
			ArrayList<UpdateSite> updateSites = new ArrayList<UpdateSite>();
			String[] sites = getArrayFromString(property, ",");
			for (String site : sites) {
				String[] parts = getArrayFromString(site, ";");
				UpdateSite updateSite = new UpdateSite(parts[0], parts.length == 2 ? parts[1] : null);
				updateSites.add(updateSite);
			}
			setUpdateSites(updateSites.toArray(new UpdateSite[updateSites.size()]));
		}
		
		// Presentation property (used by launch property below)
		String presentationProperty = readProperty(PROP_LAUNCH_PRESENTATION);
		HashMap<String, LaunchItemPresentation>presentationMap = null;
		if (presentationProperty != null) {
			presentationMap = new HashMap<String, LaunchItemPresentation>();
			String [] presentationPairs = getArrayFromString(presentationProperty, ",");
			for (String presentationPair: presentationPairs) {
				String [] presentationPairParts = getArrayFromString(presentationPair, ":");
				if (presentationPairParts.length == 2) {
					presentationMap.put(presentationPairParts[0], LaunchItemPresentation.fromString(presentationPairParts[1]));
				}
			}			
		}
		
		// Items to launch
		property = readProperty(PROP_LAUNCH);
		if (property != null) {
			ArrayList<LaunchItem> launchItems = new ArrayList<LaunchItem>();
			boolean restartOrLogoutPresent = false;
			
			String[] programs = getArrayFromString(property, ",");
			for (String program : programs) {
				String[] parts = getArrayFromString(program, ";");

				// Launch type
				LaunchItemType type = null;

				// Executable
				if (parts[2].equals(LaunchItemType.EXECUTABLE.getName()))
					type = LaunchItemType.EXECUTABLE;
				// File
				else if (parts[2].equals(LaunchItemType.FILE.getName()))
					type = LaunchItemType.FILE;
				// URL
				else if (parts[2].equals(LaunchItemType.HTML.getName()))
					type = LaunchItemType.HTML;
				//RESTART and LOGOUT
				else if (parts[2].equals(LaunchItemType.RESTART.getName()) ||
						parts[2].equals(LaunchItemType.LOGOUT.getName()))
				{
					/* If multiple restart and logout items are specified, skip
					 * them. 
					 */
					if (restartOrLogoutPresent)
						continue;

					type = parts[2].equals(LaunchItemType.RESTART.getName()) ? LaunchItemType.RESTART: LaunchItemType.LOGOUT;
					restartOrLogoutPresent = true;
				}
				
				parts[1] = parts[1].replace("{exe}", Installer.isWindows() ? ".exe" : "");
				
				// Presentation
				LaunchItemPresentation presentation = null;
				if (presentationMap != null) {
					LaunchItemPresentation overrideType = presentationMap.get(type.getName());
					if (overrideType != null) {
						presentation = overrideType;
					}					
				}
				if (presentation == null) {
					if (parts.length > 3) {
						/* Older installer.properties use false and true flags */
						if (parts[3].equals(LaunchItemPresentation.UNCHECKED.getName()) |
							parts[3].equals(Boolean.FALSE.toString())) {
							presentation = LaunchItemPresentation.UNCHECKED;
						} 
						else if (parts[3].equals(LaunchItemPresentation.LINK.getName())) {
							presentation = LaunchItemPresentation.LINK;
						}
					}
				}
				if (presentation == null) {
					presentation = LaunchItemPresentation.CHECKED;
				}
								
				LaunchItem item = new LaunchItem(type, parts[0], parts[1], presentation);
				launchItems.add(item);
			}
			setLaunchItems(launchItems.toArray(new LaunchItem[launchItems.size()]));
		}

		// Default install location
		property = readProperty(PROP_INSTALL_LOCATION);
		if (property != null) {
			setInstallLocation(InstallUtils.resolvePath(property));
		}

		// Profile name
		property = readProperty(PROP_PROFILE_NAME);
		if (property != null)
			setProfileName(property);
		
		// Remove profile
		property = readProperty(PROP_REMOVE_PROFILE);
		if (property != null)
			setRemoveProfile(property.trim().toLowerCase().equals("true"));

		// Product identifier
		property = readProperty(PROP_PRODUCT_ID);
		if (property != null)
			setProductId(property);

		// Product name
		property = readProperty(PROP_PRODUCT_NAME);
		if (property != null)
			setProductName(property);
		
		// Product category
		property = readProperty(PROP_PRODUCT_CATEGORY);
		if (property != null)
			setProductCategory(property);
		
		// Product vendor
		property = readProperty(PROP_PRODUCT_VENDOR);
		if (property != null)
			setProductVendor(property);
		
		// Product version
		property = readProperty(PROP_PRODUCT_VERSION);
		if (property != null)
			setProductVersionString(property);
		
		// Product help
		property = readProperty(PROP_PRODUCT_HELP);
		if (property != null)
			setProductHelp(property);
		
		// Product root IU property
		property = readProperty(PROP_PRODUCT_ROOT);
		if (property != null) {
			setProductRoot(Boolean.parseBoolean(property));
		}
		
		// Product Uninstall Name
		property = readProperty(PROP_PRODUCT_UNINSTALL_NAME);
		if (property != null)
			setProductUninstallName(property);

		// Required roots
		setRequiredRoots(readRootsProperty(PROP_REQUIRED_ROOTS));
		
		// Optional roots
		setOptionalRoots(readRootsProperty(PROP_OPTIONAL_ROOTS));

		// Default optional roots
		setDefaultOptionalRoots(readRootsProperty(PROP_OPTIONAL_ROOTS_DEFAULT));

		// Expanded roots
		setWizardExpandedRoots(readRootsProperty(PROP_EXPAND_ROOTS));

		// Root constraints
		property = readProperty(PROP_ROOTS_CONSTRAINTS);
		if (property != null) {
			ArrayList<IInstallConstraint> relations = new ArrayList<IInstallConstraint>();
			// Get relation specs
			String[] relationSpecs = InstallUtils.getArrayFromString(property, ";");
			for (String relationSpec : relationSpecs) {
				int start = relationSpec.indexOf('(');
				if (start != -1) {
					String typeSpec = relationSpec.substring(0, start);
					IInstallConstraint.Constraint type = IInstallConstraint.Constraint.valueOf(typeSpec);
					if (type != null) {
						int end = relationSpec.indexOf(')');
						if (end != -1) {
							String rootsSpec = relationSpec.substring(start + 1, end);
							ArrayList<IVersionedId> roots = new ArrayList<IVersionedId>();
							String[] parts = InstallUtils.getArrayFromString(rootsSpec, ",");
							for (String part : parts) {
								VersionedId id = new VersionedId(part, Version.emptyVersion);
								roots.add(id);
							}
							// Add relation
							IInstallConstraint relation = new InstallConstraint(roots.toArray(new IVersionedId[roots.size()]), type);
							relations.add(relation);
						}
					}
				}
			}
			// Set relations
			setInstallConstraints(relations.toArray(new IInstallConstraint[relations.size()]));
		}
		
		// Licenses
		ArrayList<LicenseDescriptor> licenses = new ArrayList<LicenseDescriptor>();
		// Licenses from files
		readLicenses(licenses, PROP_LICENSE, true);
		// Install units for licenses
		readLicenses(licenses, PROP_LICENSE_IU, false);
		if (licenses.size() > 0) {
			setLicenses(licenses.toArray(new LicenseDescriptor[licenses.size()]));
		}
		
		// Installer text
		Map<String, String> wizardText = getPrefixedProperties(PROP_WIZARD_TEXT_PREFIX);
		for (Entry<String, String> entry : wizardText.entrySet()) {
			String prop = entry.getKey();
			String value = entry.getValue();
			
			// Information
			if (PROP_INFORMATION.equals(prop)) {
				URI[] informationLocations = getURIs(value, getBase());
				if (informationLocations.length > 0) {
					try {
						setText(stripIdentifierPrefix(PROP_INFORMATION, PROP_WIZARD_TEXT_PREFIX), 
								readFile(informationLocations[0]));
					} catch (IOException e) {
						LogHelper.log(new Status(IStatus.ERROR, Installer.ID, 
								"Failed to read information file: " + informationLocations[0], e)); //$NON-NLS-1$
					}
				}
			}
			else {
				this.setText(stripIdentifierPrefix(prop, PROP_WIZARD_TEXT_PREFIX), value);
			}
		}

		// Install mode
		property = readProperty(PROP_INSTALL);
		if (property != null) {
			String[] flags = getArrayFromString(property, "|");
			for (String flag : flags) {
				if (flag.equals("NO_UPGRADE")) {
					setSupportsUpgrade(false);
				}
				else if (flag.equals("NO_UPDATE")) {
					setSupportsUpdate(false);
				}
				else if (flag.equals("EMPTY_DIRECTORY")) {
					setRequireEmptyInstallDirectory(true);
				}
				else if (flag.equals("MIRROR")) {
					setMirrorMode(MirrorMode.ALL);
				}
				else if (flag.equals("MIRROR_REMOTE")) {
					setMirrorMode(MirrorMode.REMOTE_ONLY);
				}
			}
		}
		
		// Uninstall mode
		property = readProperty(PROP_UNINSTALL);
		if (property != null) {
			boolean showUninstall = false;
			boolean createAddRemove = false;
			boolean removeDirectories = false;
			String[] flags = getArrayFromString(property, "|");
			for (String flag : flags) {
				if (flag.equals(UNINSTALL_ADDREMOVE))
					createAddRemove = true;
				else if (flag.equals(UNINSTALL_REMOVE_DIRS))
					removeDirectories = true;
				else if (flag.equals(UNINSTALL_SHOW_UNINSTALL))
					showUninstall = true;
			}
			UninstallMode mode = new UninstallMode(showUninstall, createAddRemove, removeDirectories);
			setUninstallMode(mode);
		}

		// Uninstall files
		property = readProperty(PROP_UNINSTALL_FILES);
		// Include uninstaller
		if (property != null) {
			String[] uninstallFiles = getArrayFromString(property, ","); //$NON-NLS-1$
			String uninstallFile;
			String uninstallerFileName = IInstallConstants.INSTALLER_NAME;
			// Replace executable extensions
			for (int index = 0; index < uninstallFiles.length; index++) {
				uninstallFile = uninstallFiles[index];
				if (uninstallFile.contains(":") && uninstallFile.startsWith("setup{exe}")) {
					uninstallerFileName = uninstallFile.substring(uninstallFile.indexOf(":") + 1);
					uninstallerFileName = uninstallerFileName.substring(0,uninstallerFileName.indexOf("{exe}"));
				}
				uninstallFiles[index] = uninstallFiles[index].replace("{exe}", 
					Installer.isWindows() ? "." + IInstallConstants.EXTENSION_EXE : "");
			}
			setUninstallFiles(uninstallFiles);
			setUninstallerName(uninstallerFileName);
			
			// If the description did not set an uninstall mode, but is including
			// uninstaller files, set a default uninstall mode.
			if (getUninstallMode() == null) {
				setUninstallMode(new UninstallMode(false, true, true));
			}
		}

		// Root location
		property = readProperty(PROP_ROOT_LOCATION_PREFIX);
		if (property != null) {
			setRootLocation(InstallUtils.resolvePath(property));
		}
		
		// Data location
		property = readProperty(PROP_DATA_LOCATION);
		if (property != null) {
			setDataLocation(InstallUtils.resolvePath(property));
		}
		
		// Network timeout
		property = readProperty(PROP_NETWORK_TIMEOUT);
		if (property != null) {
			try {
				setNetworkTimeout(Integer.parseInt(property));
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		// Network retry
		property = readProperty(PROP_NETWORK_RETRY);
		if (property != null) {
			try {
				setNetworkRetry(Integer.parseInt(property));
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}

		// Short-cuts links location
		property = readProperty(PROP_LINKS_LOCATION);
		if (property != null) {
			setLinksLocation(new Path(property));
		}
		
		// Default short-cut link folders
		ArrayList<ShortcutFolder> defaultShortcutFolders = new ArrayList<ShortcutFolder>();
		property = readProperty(PROP_LINKS_DEFAULT);
		if (property != null) {
			String[] folders = getArrayFromString(property, ",");
			for (String folder : folders) {
				if (folder.equals("programs"))
					defaultShortcutFolders.add(ShortcutFolder.PROGRAMS);
				else if (folder.equals("desktop"))
					defaultShortcutFolders.add(ShortcutFolder.DESKTOP);
				else if (folder.equals("launcher"))
					defaultShortcutFolders.add(ShortcutFolder.UNITY_DASH);
			}
		}

		// Short-cut links
		property = readProperty(PROP_LINKS);
		if (property != null) {
			ArrayList<LinkDescription> linkDescriptions = new ArrayList<LinkDescription>();
			
			String[] links = getArrayFromString(property, ",");
			for (String link : links) {
				String[] parts = getArrayFromString(link, ";");
				// Replace executable extension (if specified)
				parts[3] = parts[3].replace("{exe}", Installer.isWindows() ? ".exe" : "");
				// Replace icon extension (if specified)
				parts[4] = parts[4].replace("{exe}", Installer.isWindows() ? ".exe" : "");
				parts[4] = parts[4].replace("{icon}", Installer.isWindows() ? ".ico" : ".png");
				String folderSpec = parts[0].trim();
				ShortcutFolder folder = null;
				if (folderSpec.equals("programs"))
					folder = ShortcutFolder.PROGRAMS;
				else if (folderSpec.equals("desktop"))
					folder = ShortcutFolder.DESKTOP;
				else if (folderSpec.equals("launcher"))
					folder = ShortcutFolder.UNITY_DASH;

				// Default to selected if optional defaults not specified
				boolean selected = defaultShortcutFolders.size() == 0;
				// Else, check if folder is selected by default
				for (ShortcutFolder defaultFolder : defaultShortcutFolders) {
					if (folder == defaultFolder) {
						selected = true;
						break;
					}
				}
				
				String[] args;
				if (parts.length > 5) {
					args = getArrayFromString(parts[5], " ");
				}
				else {
					args = new String[0];
				}
				
				// Add link description
				LinkDescription linkDescription = new LinkDescription(
						folder,				// Folder
						parts[1].trim(),	// Link path
						parts[2].trim(),	// Link name
						parts[3].trim(),	// Link target
						parts[4].trim(),	// Icon path
						args,				// Launcher arguments
						selected			// Default selection
						);
				linkDescriptions.add(linkDescription);
			}
			setLinks(linkDescriptions.toArray(new LinkDescription[linkDescriptions.size()]));
		}
		
		// Environment path variables
		property = readProperty(PROP_ENV_PATHS);
		if (property != null) {
			String[] paths = getArrayFromString(property, ",");
			setEnvironmnetPaths(paths);
		}
		
		// Title
		property = readProperty(PROP_TITLE);
		if (property != null) {
			setTitle(property);
		}
		
		// Title image
		try {
			property = readProperty(PROP_TITLE_IMAGE);
			if (property != null) {
				URI titleImageLocation = getURI(property, getBase());
				File imageFile = URIUtil.toFile(titleImageLocation);
				if ((imageFile != null) && imageFile.exists()) {
					setTitleImage(new Path(imageFile.getAbsolutePath()));
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
		
		// Window title
		property = readProperty(PROP_WINDOW_TITLE);
		if (property != null) {
			setWindowTitle(property);
		}
		
		// Excluded actions
		property = readProperty(PROP_EXCLUDED_ACTIONS);
		if (property != null) {
			String[] actions = getArrayFromString(property, ",");
			setExcludedActions(actions);
		}
		
		// Wizard pages excluded
		property = readProperty(PROP_WIZARD_PAGES_EXCLUDE);
		if (property != null) {
			String[] pages = getArrayFromString(property, ",");
			setWizardPagesExcluded(pages);
		}
		
		// Wizard pages order
		property = readProperty(PROP_WIZARD_PAGES_ORDER);
		if (property != null) {
			String[] pages = getArrayFromString(property, ",");
			setWizardPagesOrder(pages);
		}
		
		// Wizard page titles
		property = readProperty(PROP_WIZARD_PAGE_TITLES);
		if (property != null) {
			ArrayList<InstallPageTitle> titles = new ArrayList<InstallPageTitle>();
			
			String[] pages = getArrayFromString(property, ",");
			for (String page : pages) {
				int index = page.indexOf(':');
				if (index != -1) {
					String pageName = page.substring(0, index);
					String pageTitle = page.substring(index + 1);
					titles.add(new InstallPageTitle(pageName, pageTitle));
				}
				
			}
			setPageTitles(titles.toArray(new InstallPageTitle[titles.size()]));
		}

		// P2 progress find/replace
		String[] find = getIndexedProperties(PROP_PROGRESS_PREFIX + PROP_FIND_SUFFIX);
		if (find != null) {
			String[] replace = getIndexedProperties(PROP_PROGRESS_PREFIX + PROP_REPLACE_SUFFIX);
			if ((replace != null) && (replace.length == find.length)) {
				setProgressPatterns(find, replace);
			}
		}
		
		// P2 missing requirement message find/replace expressions
		find = getIndexedProperties(PROP_MISSING_REQUIREMENT_PREFIX + PROP_FIND_SUFFIX);
		if (find != null) {
			String[] replace = getIndexedProperties(PROP_MISSING_REQUIREMENT_PREFIX + PROP_REPLACE_SUFFIX);
			if ((replace != null) && (replace.length == find.length)) {
				setMissingRequirementExpressions(find, replace);
			}
		}
		
		// Installer modules
		property = readProperty(PROP_MODULES);
		if (property != null) {
			String[] moduleList = getArrayFromString(property, ","); //$NON-NLS-1$
			setModuleIDs(moduleList);
		}
		
		// Show/Hide components version on components page
		property = readProperty(PROP_SHOW_COMPONENT_VERSIONS);
		if (property != null) {
			setShowComponentVersions(property.trim().toLowerCase().equals("true"));
		}
		
		// Show optional components first on components page
		property = readProperty(PROP_SHOW_OPTIONAL_FIRST);
		if (property != null) {
			setShowOptionalComponentsFirst(property.trim().toLowerCase().equals("true"));
		}

		// Wizard page navigation
		property = readProperty(PROP_WIZARD_NAVIGATION);
		if (property != null) {
			WizardNavigation navigation = WizardNavigation.fromString(property);
			setPageNavigation(navigation);
		}
		
		// Patch
		property = readProperty(PROP_PATCH);
		if (property != null) {
			setPatch(property.trim().equals(Boolean.TRUE.toString()));
		}
		
		// Requires
		property = readProperty(PROP_REQUIRES);
		if (property != null) {
			try {
				ArrayList<IProductRange> productRanges = new ArrayList<IProductRange>();
				String[] products = InstallUtils.getArrayFromString(property, ";");
				for (String product : products) {
					String[] parts = InstallUtils.getArrayFromString(product, ":");
					if (parts.length > 0) {
						String productId = parts[0];
						VersionRange productRange = null;
						if (parts.length > 1) {
							productRange = InstallUtils.createVersionRange(parts[1]);
						}
						productRanges.add(new ProductRange(productId, productRange));
					}
				}
				setRequires(productRanges.toArray(new IProductRange[productRanges.size()]));
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}

		// Include remote repositories
		property = readProperty(PROP_INCLUDE_ALL_REPOSITORIES);
		if (property != null)
			setIncludeAllRepositories(property.trim().toLowerCase().equals(Boolean.TRUE.toString()));

		// Use install registry
		property = readProperty(PROP_USE_INSTALL_REGISTRY);
		if (property != null)
			setUseInstallRegistry(property.trim().toLowerCase().equals(Boolean.TRUE.toString()));
		
		// Order planner
		setOrderPlanner(true);
		property = readProperty(PROP_ORDER_PLANNER);
		if (property != null)
			setOrderPlanner(property.trim().toLowerCase().equals(Boolean.TRUE.toString()));
		
		// Minimum version that can be upgraded
		property = readProperty(PROP_MINIMUM_UPGRADE_VERSION);
		if (property != null) {
			Version minVersion = InstallUtils.createVersion(property);
			setMinimumUpgradeVersion(minVersion);
		}
		
		// Size format
		property = readProperty(PROP_INSTALL_SIZE_FORMAT);
		// Default formatting
		if (property == null) {
			setInstallSizeFormat(InstallMessages.InstallSizeFormat);
		}
		// Format specified
		else if (!property.trim().isEmpty()) {
			setInstallSizeFormat(property);
		}
	}
	
	/**
	 * Reads license descriptions from an installer property.
	 * 
	 * @param licenses Collection for license descriptions
	 * @param propertyName Installer property name
	 * @param files <code>true</code> if the property is for file based licenses, <code>false</code> if property is
	 * for install unit based licenses.
	 */
	private void readLicenses(List<LicenseDescriptor> licenses, String propertyName, boolean files) {
		String property = readProperty(propertyName);
		if (property != null) {
			String[] licenseInfos = getArrayFromString(property, ",");  //$NON-NLS-1$
			for (String licenseInfo : licenseInfos) {
				String licenseName = null;
				// License name specified
				int index = licenseInfo.lastIndexOf(':');
				if (index != -1) {
					licenseName = licenseInfo.substring(index + 1);
					licenseInfo = licenseInfo.substring(0, index);
				}
				
				// License from file
				if (files) {
					URI[] licenseLocations = getURIs(licenseInfo, getBase());
					if (licenseLocations.length > 0) {
						try {
							String contents = readFile(licenseLocations[0]);
							if (contents != null) {
								LicenseDescriptor license = new LicenseDescriptor(readFile(licenseLocations[0]), licenseName);
								licenses.add(license);
							}
							else {
								Installer.log("Failed to read license file: " + licenseLocations[0]);
							}
						} catch (IOException e) {
							LogHelper.log(new Status(IStatus.ERROR, Installer.ID, "Failed to read license file: " + licenseLocations[0], e)); //$NON-NLS-1$
						}
					}
				}
				// License for installable unit
				else {
					VersionedId id = new VersionedId(licenseInfo, Version.emptyVersion);
					LicenseDescriptor license = new LicenseDescriptor(id, licenseName);
					licenses.add(license);
				}
			}
		}
	}
	
	/**
	 * Reads versioned roots specified in a property.
	 * 
	 * @param property Property
	 * @return Roots or <code>null</code>
	 */
	private IVersionedId[] readRootsProperty(String property) {
		IVersionedId[] readRoots = null;
		String rootSpec = readProperty(property);
		if (rootSpec != null) {
			String[] rootList = getArrayFromString(rootSpec, ","); //$NON-NLS-1$
			ArrayList<IVersionedId> roots = new ArrayList<IVersionedId>(rootList.length);
			for (int i = 0; i < rootList.length; i++) {
				try {
					roots.add(VersionedId.parse(rootList[i]));
				} catch (IllegalArgumentException e) {
					LogHelper.log(new Status(IStatus.ERROR, Installer.ID, InstallMessages.Error_InvalidInstallDescriptionVersion + rootList[i], e)); //$NON-NLS-1$
				}
			}
			if (!roots.isEmpty()) {
				readRoots = roots.toArray(new IVersionedId[roots.size()]);
			}
		}
		
		return readRoots;
	}
	
	@Override
	public String[] getIndexedProperties(String prefix) {
		String[] values = null;
		
		if (readProperty(prefix + ".0") != null) {
			int index = 0;
			ArrayList<String> patterns = new ArrayList<String>();
			for (;;) {
				String indexPostfix = Integer.toString(index);
				String value = readProperty(prefix + "." + indexPostfix);
				if (value == null)
					break;
				patterns.add(value);
				index ++;
			}
			values = patterns.toArray(new String[patterns.size()]);
		}
		return values;
	}
	
	@Override
	public Map<String, String> getPrefixedProperties(String prefix) {
		LinkedHashMap<String, String> prefixedProperties = new LinkedHashMap<String, String>();
		
		// Put the default property first
		String defaultPropertyValue = readProperty(prefix);
		if (defaultPropertyValue != null) {
			prefixedProperties.put(prefix, defaultPropertyValue);
		}
		// Put the remaining properties
		for (Entry<String, String> property : properties.entrySet()) {
			String propertyName = property.getKey();
			// If property is prefixed but not the default value
			if (propertyName.startsWith(prefix) && !propertyName.equals(prefix)) {
				String propertyValue = readProperty(propertyName);
				prefixedProperties.put(propertyName, propertyValue);
			}
		}
		
		return prefixedProperties;
	}
	
	/**
	 * Reads a property set in the install description.  This method will first
	 * attempt to read the value for an operating specific and/or architecture 
	 * specific property.  If those are not found, it will attempt to read the
	 * default property value.  The properties will be read in the following
	 * order:
	 * 
	 * <ul>
	 * <li>prefix.os.arch</li>
	 * <li>prefix.os</li>
	 * <li>prefix<li>
	 * </ul>
	 * 
	 * @param prefix Prefix for the property name
	 * @return Property value or <code>null</code>
	 */
	private String readProperty(String prefix) {
		final String[] PROPS_OLD = new String[] {
				"eclipse.p2.metadata", "eclipse.p2.artifacts", "eclipse.p2.welcomeText",
				"eclipse.p2.installFolderText", "eclipse.p2.windowTitle", "eclipse.p2.title",
				"eclipse.p2.information", "eclipse.p2.rootLocation", "eclipse.p2.installLocation",
				"eclipse.p2.productId", "eclipse.p2.productName", "eclipse.p2.productCategory",
				"eclipse.p2.productVendor", "eclipse.p2.productVersion", "eclipse.p2.productHelp",
				"eclipse.p2.profileName", "eclipse.p2.removeProfile", "eclipse.p2.launch",
				"eclipse.p2.linksLocation", "eclipse.p2.linksDefault", "eclipse.p2.requiredRoots",
				"eclipse.p2.optionalRoots", "eclipse.p2.optionalRootsDefault", "eclipse.p2.licenseIU",
				"org.eclipse.p2.wizardNavigation", "eclipse.p2.wizardPages", "eclipse.p2.wizardPageTitles",
				"eclipse.p2.hideComponentsVersion", "eclipse.p2.titleImage", "eclipse.p2.dataLocation",
				"eclipse.p2.env.paths"
		};
		final String[] PROPS_NEW = new String[] {
				PROP_METADATA_REPOSITORY, PROP_ARTIFACT_REPOSITORY, PROP_WIZARD_TEXT_PREFIX + ".welcome",
				PROP_WIZARD_TEXT_PREFIX + ".installFolder", PROP_WINDOW_TITLE, PROP_TITLE,
				PROP_INFORMATION, PROP_ROOT_LOCATION_PREFIX, PROP_INSTALL_LOCATION,
				PROP_PRODUCT_ID, PROP_PRODUCT_NAME, PROP_PRODUCT_CATEGORY,
				PROP_PRODUCT_VENDOR, PROP_PRODUCT_VERSION, PROP_PRODUCT_HELP,
				PROP_PROFILE_NAME, PROP_REMOVE_PROFILE, PROP_LAUNCH,
				PROP_LINKS_LOCATION, PROP_LINKS_DEFAULT, PROP_REQUIRED_ROOTS,
				PROP_OPTIONAL_ROOTS, PROP_OPTIONAL_ROOTS_DEFAULT, PROP_LICENSE_IU,
				PROP_WIZARD_NAVIGATION, PROP_WIZARD_PAGES_ORDER, PROP_WIZARD_PAGE_TITLES,
				PROP_SHOW_COMPONENT_VERSIONS, PROP_TITLE_IMAGE, PROP_DATA_LOCATION,
				PROP_ENV_PATHS
		};
		String value = null;
		
		if (prefix != null) {
			String os = Platform.getOS();
			if (!os.equals(Platform.OS_UNKNOWN)) {
				String osProperty = prefix + "." + os;
				String archProperty = osProperty + "." + Platform.getOSArch();
				// Attempt to get value for property that includes operating system 
				// and architecture in the name
				value = properties.get(archProperty);
				// If no property is defined, attempt to get value for property with 
				// only operating system in the name
				if (value == null) {
					value = properties.get(osProperty);
				}
			}
			// If OS or OS & ARCH property was not found, get the default property
			// value
			if (value == null) {
				value = properties.get(prefix);
			}
			// Attempt to read older changed property names
			if (value == null) {
				for (int index = 0; index < PROPS_NEW.length; index ++) {
					if (PROPS_NEW[index].equals(prefix)) {
						value = properties.get(PROPS_OLD[index]);
						break;
					}
				}
			}
		}
		
		return value;
	}

	/**
	 * Add all of the given properties to profile properties of the given description 
	 * after removing the keys known to be for the installer.  This allows install descriptions 
	 * to also set random profile properties.
	 */
	private void initializeProfileProperties() {
		// Load profile properties
		Map<String, String> profileProperties = new HashMap<String, String>();
		Set<Entry<String, String>> entries = properties.entrySet();
		int prefixLength = PROP_P2_PROFILE_PREFIX.length();
		for (Entry<String, String> entry : entries) {
			String key = entry.getKey();
			if (key.startsWith(PROP_P2_PROFILE_PREFIX)) {
				String property = key.substring(prefixLength);
				profileProperties.put(property, entry.getValue());
			}
		}
		setProfileProperties(profileProperties);
	}
	
	/**
	 * Sets all install data defaults specified in install description.
	 */
	private void initializeDataDefaults() {
		Map<String, String> dataDefaults = new HashMap<String, String>();
		Set<Entry<String, String>> entries = properties.entrySet();
		int prefixLength = PROP_DATA_PREFIX.length();
		for (Entry<String, String> entry : entries) {
			String key = entry.getKey();
			if (key.startsWith(PROP_DATA_PREFIX)) {
				String property = key.substring(prefixLength);
				dataDefaults.put(property, entry.getValue());
			}
		}
		setInstallDataDefaults(dataDefaults);
	}
	
	/**
	 * Returns the base URI for a given
	 * URI.
	 * 
	 * @param uri URI
	 * @return Base URI or <code>null</code>.
	 */
	protected static URI getBase(URI uri) {
		if (uri == null)
			return null;

		String uriString = uri.toString();
		int slashIndex = uriString.lastIndexOf('/');
		if (slashIndex == -1 || slashIndex == (uriString.length() - 1))
			return uri;

		return URI.create(uriString.substring(0, slashIndex + 1));
	}

	/**
	 * Reads the contents of a file.
	 * 
	 * @param location File location
	 * @return File contents
	 * @throws IOException on failure to read the file.
	 */
	protected String readFile(URI location) throws IOException {
		String contents = null;
		File file = new File(location);
		if (file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				StringBuilder buffer = new StringBuilder();
				String line;
				String separator = System.getProperty("line.separator"); //$NON-NLS-1$
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
					buffer.append(separator);
				}
				contents = buffer.toString();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}

		return contents;
	}

	/**
	 * Returns a URI for a specification.
	 * 
	 * @param spec Specification
	 * @param base Base location
	 * @return URI or <code>null</code>
	 */
	protected URI getURI(String spec, URI base) {
		// Bad specification
		if (spec.startsWith("@"))
			return null;
		
		URI uri = null;
		try {
			uri = URIUtil.fromString(spec);
			String uriScheme = uri.getScheme();
			// Handle jar scheme special to support relative paths
			if ((uriScheme != null) && uriScheme.equals("jar")) { //$NON-NLS-1$
				String path = uri.getSchemeSpecificPart().substring("file:".length()); //$NON-NLS-1$
				URI relPath = URIUtil.append(base, path);
				uri = new URI("jar:" + relPath.toString());  //$NON-NLS-1$
			}
			else {
				uri = URIUtil.makeAbsolute(uri, base);
			}
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.ERROR, Installer.ID, "Invalid URL in install description: " + spec, e)); //$NON-NLS-1$
		}
		
		return uri;
	}
	
	/**
	 * Returns an array of URIs from the given comma-separated list
	 * of URLs. Returns <code>null</code> if the given spec does not contain any URLs.
	 * @param specs Repository specifications
	 * @param base Base location 
	 * @return An array of URIs in the given spec, or <code>null</code>
	 */
	protected URI[] getURIs(String specs, URI base) {
		if (specs.trim().isEmpty())
			return new URI[0];
		
		String[] urlSpecs = getArrayFromString(specs, ","); //$NON-NLS-1$
		ArrayList<URI> result = new ArrayList<URI>(urlSpecs.length);
		for (int i = 0; i < urlSpecs.length; i++) {
			String spec = urlSpecs[i].trim();
			if (!spec.isEmpty()) {
				URI uri = getURI(spec, base);
				if (uri != null) {
					result.add(uri);
				}
			}
		}
		if (result.isEmpty())
			return null;
		return result.toArray(new URI[result.size()]);
	}
	
	/**
	 * Returns an array of strings from a single string of tokens delimited
	 * by a separator.
	 * 
	 * @param list List of tokens
	 * @param separator Separator
	 * @return Array of separated strings
	 */
	protected String[] getArrayFromString(String list, String separator) {
		ArrayList<String> parts = new ArrayList<String>();
		
		int start = 0;
		int end = list.indexOf(separator);
		if (end == -1) {
			if (list.trim().isEmpty()) {
				return new String[] {};
			}
			return new String[] { list };
		}
		else {
			String part;
			while (end != -1) {
				part = list.substring(start, end).trim();
				parts.add(part);
				start = end + 1;
				end = list.indexOf(separator, start);
			}
			if (start < list.length()) {
				part = list.substring(start).trim();
				parts.add(part);
			}
			
		}
		
		return parts.toArray(new String[parts.size()]);
	}

	@Override
	public String getProperty(String propertyName) {
		return readProperty(propertyName);
	}

	@Override
	public void setProperty(String name, String value) {
		properties.put(name, value);
	}
	
	@Override
	public Map<String, String> getProperties(String prefix) {
		int prefixLength = prefix.length();
		HashMap<String, String> properties = new HashMap<String, String>();
		Iterator<Entry<String, String>> iter = getProperties().entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			String propertyName = entry.getKey(); 
			if (propertyName.startsWith(prefix)) {
				propertyName = propertyName.substring(prefixLength);
				if (!propertyName.isEmpty()) {
					properties.put(propertyName, entry.getValue());
				}
			}
		}
		
		return properties;
	}

	/**
	 * Returns all installer properties.
	 * 
	 * @return Properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}
	
	@Override
	public void setWindowTitle(String value) {
		windowTitle = value;
	}
	
	@Override
	public String getWindowTitle() {
		return windowTitle;
	}

	@Override
	public void setTitle(String value) {
		title = value;
	}
	
	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitleImage(IPath imagePath) {
		this.titleImage = imagePath;
	}

	@Override
	public IPath getTitleImage() {
		return titleImage;
	}

	@Override
	public void setProfileProperties(Map<String, String> properties) {
		profileProperties.putAll(properties);
	}
	
	@Override
	public Map<String, String> getProfileProperties() {
		return profileProperties;
	}

	@Override
	public void setEnvironmnetPaths(String[] paths) {
		this.environmentPaths = paths;
	}
	
	@Override
	public String[] getEnvironmentPaths() {
		return environmentPaths;
	}
	
	@Override
	public void setInstallLocation(IPath location) {
		this.p2Location = location;
	}
	
	@Override
	public IPath getInstallLocation() {
		return getRootLocation().append(p2Location);
	}

	@Override
	public void setRequireEmptyInstallDirectory(boolean requireEmptyCheck) {
		this.checkEmtptyInstallDirectory = requireEmptyCheck;
	}

	@Override
	public boolean getRequireEmptyInstallDirectory() {
		return checkEmtptyInstallDirectory;
	}

	@Override
	public void setLaunchItems(LaunchItem[] value) {
		this.launchItems = value;
	}

	@Override
	public LaunchItem[] getLaunchItems() {
		return launchItems;
	}

	@Override
	public void setUpdateSites(UpdateSite[] updateSites) {
		this.updateSites = updateSites;
	}

	@Override
	public UpdateSite[] getUpdateSites() {
		return updateSites;
	}

	@Override
	public void setProductId(String value) {
		productId = value;
	}
	
	@Override
	public String getProductId() {
		return productId;
	}

	@Override
	public void setProductName(String value) {
		productName = value;
	}
	
	@Override
	public String getProductName() {
		return productName;
	}

	@Override
	public void setProductCategory(String category) {
		this.productCategory = category;
	}

	@Override
	public String getProductCategory() {
		return productCategory;
	}

	@Override
	public void setProductVendor(String value) {
		productVendor = value;
	}
	
	@Override
	public String getProductVendor() {
		return productVendor;
	}

	@Override
	public void setProductVersionString(String value) {
		productVersionText = value;
		productVersion = InstallUtils.createVersion(value);
	}
	
	@Override
	public String getProductVersionString() {
		return productVersionText;
	}

	@Override
	public Version getProductVersion() {
		return productVersion;
	}

	@Override
	public void setProductHelp(String value) {
		productHelp = value;
	}
	
	@Override
	public String getProductHelp() {
		return productHelp;
	}

	@Override
	public void setProductUninstallName(String value) {
		uninstallproductName = value;
	}

	@Override
	public String getProductUninstallName() {
		return uninstallproductName;
	}

	@Override
	public void setProfileName(String value) {
		profileName = value;
	}

	@Override
	public String getProfileName() {
		return profileName;
	}

	@Override
	public void setRemoveProfile(boolean removeProfile) {
		this.removeProfile = removeProfile;
	}
	
	@Override
	public boolean getRemoveProfile() {
		return removeProfile;
	}

	@Override
	public void setLicenses(LicenseDescriptor[] value) {
		licenses = value;
	}
	
	@Override
	public LicenseDescriptor[] getLicenses() {
		return licenses;
	}

	@Override
	public void setUninstallFiles(String[] value) {
		uninstallFiles = value;
	}
	
	@Override
	public String[] getUninstallFiles() {
		return uninstallFiles;
	}

	@Override
	public void setModuleIDs(String[] value) {
		moduleIDs = value;
	}

	@Override
	public String[] getModuleIDs() {
		return moduleIDs;
	}

	@Override
	public void setRootLocation(IPath location) {
		this.rootLocation = location;
	}

	@Override
	public IPath getRootLocation() {
		return rootLocation;
	}
	
	@Override
	public void setRequiredRoots(IVersionedId[] value) {
		requiredRoots = value;
	}

	@Override
	public IVersionedId[] getRequiredRoots() {
		return requiredRoots;
	}

	@Override
	public void setOptionalRoots(IVersionedId[] value) {
		optionalRoots = value;
	}
	
	@Override
	public IVersionedId[] getOptionalRoots() {
		return optionalRoots;
	}

	@Override
	public void setDefaultOptionalRoots(IVersionedId[] value) {
		optionalRootsDefault = value;
	}
	
	@Override
	public IVersionedId[] getDefaultOptionalRoots() {
		return optionalRootsDefault;
	}

	@Override
	public void setLinks(LinkDescription[] value) {
		links = value;
	}

	@Override
	public LinkDescription[] getLinks() {
		return links;
	}

	@Override
	public void setLinksLocation(IPath value) {
		linksLocation = value;
	}
	
	@Override
	public IPath getLinksLocation() {
		return linksLocation;
	}

	@Override
	public void setUninstallerName(String name) {
		uninstallerName = name;
	}
	
	@Override
	public String getUninstallerName() {
		return uninstallerName;
	}
	
	@Override
	public void setWizardPagesExcluded(String[] wizardPages) {
		this.wizardPages = wizardPages;
	}

	@Override
	public String[] getWizardPagesExcluded() {
		return wizardPages;
	}

	@Override
	public void setWizardPagesOrder(String[] wizardPages) {
		this.wizardPagesOrder = wizardPages;
	}
	
	@Override
	public String[] getWizardPagesOrder() {
		return wizardPagesOrder;
	}
	
	@Override
	public void setProgressPatterns(String[] find, String[] replace) {
		this.progressFindPatterns = find;
		this.progressReplacePatterns = replace;
	}
	
	@Override
	public String[] getProgressFindPatterns() {
		return progressFindPatterns;
	}

	@Override
	public String[] getProgressReplacePatterns() {
		return progressReplacePatterns;
	}
	
	@Override
	public void setShowComponentVersions(boolean showComponentsVersion) {
		this.showComponentVersions = showComponentsVersion;
	}

	@Override
	public boolean getShowComponentVersions() {
		return this.showComponentVersions;
	}

	@Override
	public void setPageNavigation(WizardNavigation pageNavigation) {
		this.pageNavigation = pageNavigation;
	}

	@Override
	public WizardNavigation getPageNavigation() {
		return pageNavigation;
	}

	@Override
	public void setPageTitles(InstallPageTitle[] pageTitles) {
		this.pageTitles = pageTitles;
	}

	@Override
	public InstallPageTitle[] getPageTitles() {
		return pageTitles;
	}

	@Override
	public void setMissingRequirementExpressions(String[] find, String[] replace) {
		missingRequirementExpressions = new String[find.length][2];
		for (int index = 0; index < find.length; index ++) {
			missingRequirementExpressions[index][0] = find[index];
			missingRequirementExpressions[index][1] = replace[index];
		}
	}

	@Override
	public String[][] getMissingRequirementExpressions() {
		return missingRequirementExpressions;
	}

	@Override
	public void setIncludeAllRepositories(boolean include) {
		this.includeRemoteRepositories = include;
	}

	@Override
	public boolean getIncludeAllRepositories() {
		return includeRemoteRepositories;
	}

	@Override
	public void setPatch(boolean patch) {
		this.patch = patch;
	}

	@Override
	public boolean getPatch() {
		return patch;
	}

	@Override
	public void setRequires(IProductRange[] range) {
		this.productRanges = range;
	}

	@Override
	public IProductRange[] getRequires() {
		return productRanges;
	}

	@Override
	public void setUseInstallRegistry(boolean useRegistry) {
		this.useInstallRegistry = useRegistry;
	}

	@Override
	public boolean getUseInstallRegistry() {
		return useInstallRegistry;
	}

	@Override
	public void setDataLocation(IPath dataLocation) {
		this.dataLocation = dataLocation;
	}

	@Override
	public IPath getDataLocation() {
		return dataLocation;
	}

	@Override
	public void setUninstallMode(UninstallMode mode) {
		this.uninstallMode = mode;
	}

	@Override
	public UninstallMode getUninstallMode() {
		return uninstallMode;
	}

	@Override
	public void setOrderPlanner(boolean ordered) {
		this.orderPlanner = ordered;
	}

	@Override
	public boolean getOrderPlanner() {
		return orderPlanner;
	}

	@Override
	public void setWizardExpandedRoots(IVersionedId[] expandedRoots) {
		this.expandedRoots = expandedRoots;
	}
	
	@Override
	public IVersionedId[] getWizardExpandedRoots() {
		return expandedRoots;
	}

	@Override
	public void setInstallConstraints(IInstallConstraint[] relationships) {
		this.relationships = relationships;
	}

	@Override
	public IInstallConstraint[] getInstallConstraints() {
		return relationships;
	}

	@Override
	public void setSupportsUpgrade(boolean supportsUpgrade) {
		this.supportsUpgrade = supportsUpgrade;
	}

	@Override
	public boolean getSupportsUpgrade() {
		return supportsUpgrade;
	}

	@Override
	public void setSupportsUpdate(boolean supportsUpdate) {
		this.supportsUpdate = supportsUpdate;
	}

	@Override
	public boolean getSupportsUpdate() {
		return supportsUpdate;
	}

	@Override
	public void setExcludedActions(String[] actions) {
		this.excludedActions = actions;
	}

	@Override
	public String[] getExcludedActions() {
		return excludedActions;
	}

	@Override
	public void setInstallDataDefaults(Map<String, String> installData) {
		this.installDataDefaults = installData;
	}

	@Override
	public Map<String, String> getInstallDataDefaults() {
		return installDataDefaults;
	}

	@Override
	public void setMinimumUpgradeVersion(Version version) {
		this.minimumUpgradeVersion = version;
	}

	@Override
	public Version getMinimumUpgradeVersion() {
		return minimumUpgradeVersion;
	}

	@Override
	public void setInstallSizeFormat(String format) {
		this.sizeFormat = format;
	}

	@Override
	public String getInstallSizeFormat() {
		return sizeFormat;
	}

	/**
	 * Resolves the value of a repository location property to a set of URI locations.  The variables are replaced and 
	 * the ${eclipse.p2.repos.mirror} variable is replaced with the value of the specified mirror property.
	 * 
	 * @param propertyName Name of property to replace value
	 * @param mirrorProperty Mirror property to use for the ${eclipse.p2.repos.mirror} value
	 * @return Resolved URI
	 */
	private URI[] resolveRepositoryLocation(String propertyName, String mirrorProperty) {
		URI[] locations = null;
		// Get the repository location property value
		String value = readProperty(propertyName);
		if (value != null) {
			// Get the mirror property value
			String reposPropertyValue = readProperty(mirrorProperty);
			// If it is not specified, just use the default mirror (if available)
			if (reposPropertyValue == null) {
				reposPropertyValue = readProperty(PROP_REPOS_MIRROR);
			}
			// Replace the ${eclipse.p2.repos.mirror} value
			if (reposPropertyValue != null) {
				value = value.replace("${" + PROP_REPOS_MIRROR + "}", reposPropertyValue);
			}
			
			// Resolve the repository locations
			locations = getURIs(value, getBase());
		}

		return locations;
	}

	@Override
	public List<IRepositoryLocation> getRepositoryLocations() {
		ArrayList<IRepositoryLocation> locations = new ArrayList<IRepositoryLocation>();
		
		// Mirror(s) defined
		if (properties.containsKey(PROP_REPOS_MIRROR)) {
			// Get the mirror properties
			Map<String, String> mirrorProperties = getPrefixedProperties(PROP_REPOS_MIRROR);

			// Add the mirror locations
			for (Entry<String, String> property : mirrorProperties.entrySet()) {
				String propertyName = property.getKey();
				// Strip off the mirror property prefix to get mirror identifier
				String groupId = stripIdentifierPrefix(propertyName, PROP_REPOS_MIRROR);
				// If no identifier, it is the default mirror
				if (groupId.isEmpty()) {
					groupId = DEFAULT_MIRROR;
				}
				
				URI[] metadata = resolveRepositoryLocation(PROP_METADATA_REPOSITORY, propertyName);
				URI[] artifact = resolveRepositoryLocation(PROP_ARTIFACT_REPOSITORY, propertyName);
				locations.add(new RepositoryLocation(groupId, metadata, artifact));
			}
		}
		// No mirrors defined
		else {
			URI[] metadata = resolveRepositoryLocation(PROP_METADATA_REPOSITORY, null);
			URI[] artifact = resolveRepositoryLocation(PROP_ARTIFACT_REPOSITORY, null);
			locations.add(new RepositoryLocation(DEFAULT_MIRROR, metadata, artifact));
		}
		
		return Collections.unmodifiableList(locations);
	}
	
	/**
	 * Strips an identifier prefix from a property name, including the '.' is present.
	 * 
	 * @param propertyName Property name
	 * @param prefix Property identifier prefix
	 * @return Property name after prefix
	 */
	private String stripIdentifierPrefix(String propertyName, String prefix) {
		if ((propertyName == null) || (prefix == null))
			return null;
		
		String stripped = propertyName.substring(prefix.length());
		if (!stripped.isEmpty() && stripped.startsWith(".")) {
			stripped = stripped.substring(1);
		}
		
		return stripped;
	}
	
	/**
	 * Repository location
	 */
	private class RepositoryLocation implements IRepositoryLocation {
		/** Mirror identifier */
		private String id;
		/** Mirror meta-data locations */
		private URI[] metadataLocations;
		/** Mirror artifact locations */
		private URI[] artifactLocations;
		
		/**
		 * Constructor
		 * 
		 * @param id Identifier
		 * @param metadataLocations Meta-data locations
		 * @param artifactLocations Artifact locations
		 */
		public RepositoryLocation(String id, URI[] metadataLocations, URI[] artifactLocations) {
			this.id = id;
			this.metadataLocations = metadataLocations;
			this.artifactLocations = artifactLocations;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public URI[] getMetadataLocations() {
			return metadataLocations;
		}

		@Override
		public URI[] getArtifactLocations() {
			return artifactLocations;
		}

		@Override
		public String toString() {
			return getId();
		}
	}

	@Override
	public void setText(String type, String text) {
		installerText.put(type, text);
	}

	@Override
	public String getText(String type, String message) {
		String text = installerText.get(type);
		if (text == null) {
			text = message;
		}
		return text;
	}

	@Override
	public void setShowOptionalComponentsFirst(boolean showOptional) {
		this.showOptionalComponentsFirst = showOptional;
	}

	@Override
	public boolean getShowOptionalComponentsFirst() {
		return showOptionalComponentsFirst;
	}

	@Override
	public void setMirrorMode(MirrorMode mirrorMode) {
		this.mirrorMode = mirrorMode;
	}

	@Override
	public MirrorMode getMirrorMode() {
		return mirrorMode;
	}

	@Override
	public void setNetworkTimeout(int timeout) {
		this.networkTimeout = timeout;
	}

	@Override
	public int getNetworkTimeout() {
		return networkTimeout;
	}

	@Override
	public void setNetworkRetry(int retries) {
		this.networkRetry = retries;
	}

	@Override
	public int getNetworkRetry() {
		return networkRetry;
	}

	@Override
	public void setProductRoot(boolean root) {
		this.productRoot = root;
	}

	@Override
	public boolean getProductRoot() {
		return productRoot;
	}
}
