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
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.touchpoint.natives.Util;
import org.eclipse.equinox.internal.p2.ui.query.RequiredIUsQuery;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ISizingPhaseSet;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.codesourcery.installer.IInstallComponent;
import com.codesourcery.installer.IInstallConstraint;
import com.codesourcery.installer.IInstallConstraint.Constraint;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IRepositoryLocation;
import com.codesourcery.installer.Installer;

/**
 * This class manages repositories used in the installation.
 * The manager can be accessed using the {@link #getDefault()} method.
 */
@SuppressWarnings("restriction")
public final class RepositoryManager {
	/** Install component property indicating it is available from the cache */
	private static final String PROPERTY_CACHE = "com.codesourcery.installer.cache";
	
	/** Default instance */
	private static RepositoryManager instance = new RepositoryManager();
	/** Install components */
	private ArrayList<IInstallComponent> components = new ArrayList<IInstallComponent>();
	/** Provisioning agent */
	private IProvisioningAgent agent;
	/** Meta-data repository manager */
	private IMetadataRepositoryManager metadataRepoMan;
	/** Artifact repository manager */
	private IArtifactRepositoryManager artifactRepoMan;
	/** Listeners to repository changes */
	private ListenerList repositoryListeners = new ListenerList();
	/** Artifact repositories */
	private ArrayList<IArtifactRepository> artifactRepositories = new ArrayList<IArtifactRepository>();
	/** Meta-data repositories */
	private ArrayList<IMetadataRepository> metadataRepositories = new ArrayList<IMetadataRepository>();
	/** Cache to store computed installation plans */
	protected Map<String, IInstallPlan> planCache;
	/** Installer size thread */
	Thread uninstallerSizeThread;
	/** Size of uninstaller files */
	protected long uninstallerSize = 0;
	/** Install location */
	private IPath installLocation;
	/** Installation profile identifier */
	private String profileId;
	/** Cache location */
	private IPath cacheLocation;
	/** Cache artifact repository */
	private IArtifactRepository cacheArtifactRepository;
	/** Cache meta-data repository */
	private IMetadataRepository cacheMetadataRepository;
	/** <code>true</code> to only install from the cache repository */
	private boolean cacheOnly = false;
	/** <code>true</code> to update the cache with installed IU's */
	private boolean cacheUpdate = false;
	/** Temporary repository to hold product installation IU */
	private ProductRepository productRepository;
	
	/**
	 * Constructor
	 */
	private RepositoryManager() {
		// Synchronize size cache
		planCache = Collections.synchronizedMap(new HashMap<String, IInstallPlan>());
	}
	
	/**
	 * @return The product repository or <code>null</code> if no product repository will be used.
	 */
	private ProductRepository getProductRepository() {
		return productRepository;
	}
	
	/**
	 * Sets the location for the install cache.  The cache will store a mirrored P2 repository containing any 
	 * installed IU's.
	 * 
	 * @param cacheLocation Cache location.
	 * @see #getCacheLocation()
	 */
	public void setCacheLocation(IPath cacheLocation) {
		this.cacheLocation = cacheLocation;
	}
	
	/**
	 * @return Returns the cache location.
	 * @see #setCacheLocation(IPath)
	 */
	public IPath getCacheLocation() {
		return cacheLocation;
	}
	
	/**
	 * Returns if a component is available in the cache.
	 * 
	 * @param component Component
	 * @return <code>true</code> if component is in cache.
	 */
	public boolean isCachedComponent(IInstallComponent component) {
		String cached = (String)component.getProperty(PROPERTY_CACHE);
		return ((cached != null) && Boolean.TRUE.toString().equals(cached));
	}
	
	/**
	 * Sets whether IU's will only be installed from the cache.  If only the cache is used, any IU's not found in the 
	 * cache will not be available for installation.
	 * 
	 * @param cacheOnly <code>true</code> to only install from cache.
	 * @see #setCacheLocation(IPath)
	 */
	public void setCacheOnly(boolean cacheOnly) {
		this.cacheOnly = cacheOnly;
	}
	
	/**
	 * @return <code>true</code> if only the cache will be used for installation.
	 */
	public boolean getCacheOnly() {
		return cacheOnly;
	}

	/**
	 * Sets whether the cache will be updated with installed IU's.
	 * 
	 * @param cacheUpdate <code>true</code> to update cache.
	 */
	public void setUpdateCache(boolean cacheUpdate) {
		this.cacheUpdate = cacheUpdate;
	}
	
	/**
	 * @return <code>true</code> to update cache.
	 */
	public boolean getUpdateCache() {
		return cacheUpdate;
	}
	
	/**
	 * Creates the P2 provisioning agent.
	 * 
	 * @param installLocation Install location or <code>null</code> to use installer agent
	 * @param monitor Progress monitor or <code>null</code>
	 * @return Provisioning agent or <code>null</code>
	 */
	public IProvisioningAgent createAgent(IPath installLocation, IProgressMonitor monitor) throws CoreException {
		try {
			boolean locationChanged = false;
			if ((installLocation != null) && !installLocation.equals(getInstallLocation())) {
				locationChanged = true;
			}
			
			// Agent not created or install location has changed
			if ((agent == null) || locationChanged) {
				this.installLocation = installLocation;

				if (monitor == null)
					monitor = new NullProgressMonitor();
				
				// Clear plan cache
				planCache.clear();
				
				// Start P2 agent
				agent = startAgent(getAgentLocation());
				// Setup certificate handling
				agent.registerService(UIServices.SERVICE_NAME, AuthenticationService.getDefault());
				// Meta-data repository manager
				metadataRepoMan = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
				// Artifact repository manager
				artifactRepoMan = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

				IInstallDescription installDescription = Installer.getDefault().getInstallManager().getInstallDescription();
				
				if (installDescription != null) {
					// Initialize the profile identifier
					profileId = Installer.getDefault().getInstallManager().getInstallDescription().getProfileName();
					// If no profile specified, use the first installed profile
					if (profileId == null) {
						IProfileRegistry profileRegistry = (IProfileRegistry)getAgent().getService(IProfileRegistry.SERVICE_NAME);
						IProfile[] profiles = profileRegistry.getProfiles();
						if (profiles.length > 0) {
							profileId = profiles[0].getProfileId();
						}
					}
	
					// Initialize product repository if required
					if (productRepository != null) {
						productRepository.dispose();
					}
					if (installDescription.getProductRoot()) {
						productRepository = new ProductRepository(agent);
						productRepository.createRepository();
					}
				}
			}
		}
		catch (Exception e) {
			Installer.fail(e.getLocalizedMessage());
		}
		
		return agent;
	}
	
	/**
	 * Loads the cache repository if it is available.  If the cache repository has already been loaded then it will be
	 * refreshed.
	 * 
	 * @return <code>true</code> if cache repository was loaded.
	 * @throws ProvisionException on provisioning failure.
	 * @throws OperationCanceledException if operation was cancelled.
	 */
	private synchronized boolean loadCacheRepository() throws ProvisionException, OperationCanceledException {
		boolean loaded = false;
		
		IPath cacheLocation = getCacheLocation();
		if (cacheLocation != null) {
			// If cache repository is already loaded then refresh it
			if ((cacheMetadataRepository != null) && (cacheArtifactRepository != null)) {
				getMetadataRepositoryManager().refreshRepository(cacheMetadataRepository.getLocation(), null);
				getArtifactRepositoryManager().refreshRepository(cacheArtifactRepository.getLocation(), null);
				loaded = true;
			}
			// Load the cache repository
			else {
				File cacheFile = cacheLocation.toFile();
				if (cacheFile.exists()) {
					// Cache location (co-located)
					URI repositoryLocation = cacheFile.toURI();
					
					fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingStarted);
	
					try {
						getMetadataRepositoryManager().addRepository(repositoryLocation);
						cacheMetadataRepository = getMetadataRepositoryManager().loadRepository(repositoryLocation, null);
						if (cacheMetadataRepository != null) {
							getArtifactRepositoryManager().addRepository(repositoryLocation);
							cacheArtifactRepository = getArtifactRepositoryManager().loadRepository(repositoryLocation, null);
							
							loaded = true;
						}
					}
					catch (Exception e) {
						unloadCacheRepository();
						fireRepositoryError(repositoryLocation, e.getLocalizedMessage());
						throw e;
					}
				}
			}
			
			// Load components
			if (loaded) {
				// Load components available in cache repository
				IInstallComponent[] components = loadComponents(cacheMetadataRepository);
				if (components.length > 0) {
					// Mark the components as coming from the cache
					for (IInstallComponent component : components) {
						component.setProperty(PROPERTY_CACHE, Boolean.TRUE.toString());
					}
					
					// Fire notification
					fireComponentsChanged();
				}

				fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingCompleted);
			}
		}
		
		return loaded;
	}
	
	/**
	 * Unloads the cache repository.
	 */
	private void unloadCacheRepository() {
		if (cacheMetadataRepository != null) {
			getMetadataRepositoryManager().removeRepository(cacheMetadataRepository.getLocation());
			cacheMetadataRepository = null;
		}
		if (cacheArtifactRepository != null) {
			getArtifactRepositoryManager().removeRepository(cacheArtifactRepository.getLocation());
			cacheArtifactRepository = null;
		}
	}
	
	/**
	 * @return <code>true</code> if a cache repository is available.
	 */
	private boolean cacheAvailable() {
		IPath cacheLocation = getCacheLocation();
		return ((cacheLocation != null) && cacheLocation.toFile().exists());
	}
	
	/**
	 * Loads the P2 repositories for installation.
	 * 
	 * @param monitor Progress monitor
	 * @throws CoreException if no repositories could be loaded
	 */
	public void loadInstallRepositories(IProgressMonitor monitor) throws CoreException {
		IInstallDescription installDescription = Installer.getDefault().getInstallManager().getInstallDescription();
		if (installDescription != null) {
			// Get the repository locations
			List<IRepositoryLocation> locations = installDescription.getRepositoryLocations();
			// Get the command line that can specify a specific mirror to use
			String commandLineGroup = Installer.getDefault().getCommandLineOption(IInstallConstants.COMMAND_LINE_MIRROR);
			
			// Clear install components
			components.clear();

			boolean loaded = false;

			// Load cache repository
			if (cacheAvailable()) {
				try {
					loaded = loadCacheRepository();
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
			
			if (!getCacheOnly()) {
				for (IRepositoryLocation location : locations) {
					// If a specific mirror has been specified, skip the rest
					if ((commandLineGroup != null) && !location.getId().equals(commandLineGroup))
						continue;
					
					// Attempt to load the repositories from the location
					if (loadMetadataRepositories(location.getMetadataLocations(), monitor)) {
						if (loadArtifactRepositories(location.getArtifactLocations(), monitor)) {
							loaded = true;
							break;
						}
					}
				}
			}
			
			// No repositories could be loaded from any locations
			if (!loaded) {
				Installer.fail(InstallMessages.Error_FailedToLoadRepositories);
			}
		}
	}
	
	/**
	 * Unloads the install repositories.
	 */
	private void unloadInstallRepositories() {
		try {
			// Remove repositories
			for (IMetadataRepository repository : getMetadataRepositories()) {
				getMetadataRepositoryManager().removeRepository(repository.getLocation());
			}
			for (IArtifactRepository repository : getArtifactRepositories()) {
				getArtifactRepositoryManager().removeRepository(repository.getLocation());
			}
			metadataRepositories.clear();
			artifactRepositories.clear();
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}

	/**
	 * Stops the P2 provisioning agent.
	 */
	public void stopAgent() {
		if (agent != null) {

			unloadInstallRepositories();
			
			agent.stop();
			agent = null;
		}
	}
	
	/**
	 * @return Returns the installation profile identifier
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * Returns the install location.
	 * 
	 * @return Install location
	 */
	public IPath getInstallLocation() {
		return installLocation;
	}
	
	/**
	 * Returns the agent location.
	 * 
	 * @return Agent location or <code>null</code>
	 */
	public IPath getAgentLocation() {
		if (getInstallLocation() == null) {
			return null;
		}
		else {
			return getInstallLocation().append(IInstallConstants.P2_DIRECTORY);
		}
	}
	
	/**
	 * Creates a profile.
	 * 
	 * @param profileId Profile identifier
	 * @return Profile
	 * @throws ProvisionException on failure
	 */
	public IProfile createProfile(String profileId) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry)getAgent().getService(IProfileRegistry.SERVICE_NAME);
		IProfile profile = profileRegistry.getProfile(profileId);
		// Note: On uninstall, the profile will always be available
		if (profile == null) {
			Map<String, String> properties = new HashMap<String, String>();
			// Install location - this is where the p2 directory will be located
			properties.put(IProfile.PROP_INSTALL_FOLDER, getInstallLocation().toString());
			// Environment
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Installer.getDefault().getContext(), EnvironmentInfo.class.getName());
			String env = "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			properties.put(IProfile.PROP_ENVIRONMENTS, env);
			// Profile identifier
			properties.put(IProfile.PROP_NAME, profileId);
			// Cache location - this is where features and plugins will be deployed
			properties.put(IProfile.PROP_CACHE, getInstallLocation().toOSString());
			// Set roaming.  This will put a path relative to the OSGi configuration area in the config.ini
			// so that the installation can be moved.  Without roaming, absolute paths will be written
			// to the config.ini and Software Update will not work correctly for a moved installation.
			properties.put(IProfile.PROP_ROAMING, Boolean.TRUE.toString());
			// Profile properties specified in install description
			if (Installer.getDefault().getInstallManager().getInstallDescription().getProfileProperties() != null)
				properties.putAll(Installer.getDefault().getInstallManager().getInstallDescription().getProfileProperties());
			
			profile = profileRegistry.addProfile(profileId, properties);
		}
		
		return profile;
	}

	public IProfile getExistingInstallProfile() {
		String profileId = getProfileId();
		IProfile profile = getProfile(profileId);
		return profile;
	}
	
	/**
	 * Returns the profile for this installation.  The profile is created if
	 * necessary.
	 * 
	 * @return Profile Profile or <code>null</code> if no install location was specified
	 * @throws ProvisionException on failure to create the profile
	 */
	public IProfile getInstallProfile() throws ProvisionException {
		IProfile profile = null;
		
		if (getInstallLocation() != null) {
			String profileId = getProfileId();
			profile = getProfile(profileId);
			if (profile == null) {
				profile = createProfile(profileId);
			}
		}
		
		return profile;
	}
	
	/**
	 * Returns a profile.
	 * 
	 * @param profileId Profile identifier
	 * @return Profile or <code>null</code> if the profile does not exist.
	 */
	public IProfile getProfile(String profileId) {
		if (profileId == null)
			return null;
		
		IProfileRegistry profileRegistry = (IProfileRegistry)getAgent().getService(IProfileRegistry.SERVICE_NAME);
		return profileRegistry.getProfile(profileId);
	}
	
	/**
	 * Removes a profile.
	 * 
	 * @param profileId Profile identifier
	 * @throws ProvisionException on failure
	 */
	public void deleteProfile(String profileId) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
		profileRegistry.removeProfile(profileId);
	}
	
	/**
	 * Starts the p2 bundles needed to continue with the install.
	 * 
	 * @param agentLocation Agent location or <code>null</code> to use default agent.
	 * @throws ProvisionException on failure to start the agent
	 */
	private IProvisioningAgent startAgent(IPath agentLocation) throws ProvisionException {
		IProvisioningAgentProvider provider = (IProvisioningAgentProvider) getService(Installer.getDefault().getContext(), IProvisioningAgentProvider.SERVICE_NAME);

		return provider.createAgent(agentLocation == null ? null : agentLocation.toFile().toURI());
	}
	
	/**
	 * Copied from ServiceHelper because we need to obtain services
	 * before p2 has been started.
	 */
	public static Object getService(BundleContext context, String name) {
		if (context == null)
			return null;
		ServiceReference<?> reference = context.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}
	
	/**
	 * Shuts down the repository manager.
	 */
	public synchronized void shutdown() {
		// Stop the P2 agent
		stopAgent();
	}

	/**
	 * Returns the provisioning agent.
	 * 
	 * @return Provisioning agent
	 */
	public IProvisioningAgent getAgent() {
		return agent;
	}
	
	/**
	 * @return Returns the meta-data repository manager.
	 */
	private IMetadataRepositoryManager getMetadataRepositoryManager() {
		return metadataRepoMan;
	}
	
	/**
	 * @return Returns the artifact repository manager.
	 */
	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return artifactRepoMan;
	}
	
	/**
	 * Returns the default instance.
	 * 
	 * @return Instance
	 */
	public static RepositoryManager getDefault() {
		return instance;
	}
	
	/**
	 * Loads meta-data repositories.
	 * 
	 * @param repositoryLocations Locations of repositories to load
	 * @param monitor Progress monitor
	 * @param <code>true</code> if all repositories were loaded
	 */
	public boolean loadMetadataRepositories(URI[] repositoryLocations, IProgressMonitor monitor) {
		return internalLoadMetadataRepositories(repositoryLocations, monitor);
	}
	
	/**
	 * Loads artifact repositories.
	 * 
	 * @param repositoryLocations Locations of repositories to load.
	 * @param monitor Progress monitor
	 * @param <code>true</code> if all repositories were loaded
	 */
	public boolean loadArtifactRepositories(URI[] repositoryLocations, IProgressMonitor monitor) {
		return internalLoadArtifactRepositories(repositoryLocations, monitor);
	}

	/**
	 * Returns if an IU is a category.
	 *  
	 * @param unit Install unit
	 * @return <code>true</code> if category
	 */
	private boolean isCategoryIu(IInstallableUnit unit) {
		String category = unit.getProperty(QueryUtil.PROP_TYPE_CATEGORY);
		return ((category != null) && Boolean.TRUE.toString().equals(category));
	}
	
	/**
	 * Adds a new install component for an installable unit.  If the installable
	 * unit is a category, it's member units will also be added.
	 * 
	 * @param unit Installable unit
	 * @param parentGroup Parent component or <code>null</code>
	 * @return Added components or <code>null</code> if the component has already been added
	 * @throws ProvisionException on failure
	 */
	private List<IInstallComponent> addInstallComponent(IInstallableUnit unit, InstallComponent parentGroup) throws ProvisionException {
		IInstallComponent existingComponent = getInstallComponent(unit.getId());
		ArrayList<IInstallComponent> addedComponents = new ArrayList<IInstallComponent>();
		InstallComponent component = null;

		// Category IU
		boolean isCategory = isCategoryIu(unit);
		
		// If there is an existing component for the unit
		if (existingComponent != null) {
			// If category, add to existing component
			if (isCategory)
				component = (InstallComponent)existingComponent;
			// Else do not allow duplicates units
			else 
				return null;
		}
		// Create component
		if (component == null) {
			component = new InstallComponent(unit);
		}
		
		// If category unit, add members
		if (isCategory) {
			IQuery<IInstallableUnit> categoryQuery = QueryUtil.createIUCategoryMemberQuery(unit);
			IQueryResult<IInstallableUnit> query = getMetadataRepositoryManager().query(categoryQuery, null);
			Iterator<IInstallableUnit> iter = query.iterator();
			while (iter.hasNext()) {
				IInstallableUnit categoryMemberUnit = iter.next();
				List<IInstallComponent> members = addInstallComponent(categoryMemberUnit, component);
				if (members != null) {
					addedComponents.addAll(members);
				}
			}
		}
		
		// Add component
		if (existingComponent == null) {
			if (parentGroup != null) {
				parentGroup.addComponent(component);
			}
	
			// Set component parent
			component.setParent(parentGroup);
			// Add component
			components.add(component);
			addedComponents.add(component);
		}
		
		return addedComponents;
	}
	
	/**
	 * Loads install components from a meta-data repository.
	 * 
	 * @param repository Meta-data repository
	 * @return Install components
	 * @throws ProvisionException on failure
	 */
	private IInstallComponent[] loadComponents(IMetadataRepository repository) throws ProvisionException {
		ArrayList<IInstallComponent> loadedComponents = new ArrayList<IInstallComponent>();
		ArrayList<IVersionedId> units = new ArrayList<IVersionedId>();

		// Required roots
		IVersionedId[] requiredRoots = Installer.getDefault().getInstallManager().getInstallDescription().getRequiredRoots();
		if (requiredRoots != null) {
			for (IVersionedId unit : requiredRoots) {
				units.add(unit);
			}
		}
		// Optional roots
		IVersionedId[] optionalRoots = Installer.getDefault().getInstallManager().getInstallDescription().getOptionalRoots();
		if (optionalRoots != null) {
			for (IVersionedId unit : optionalRoots) {
				units.add(unit);
			}
		}
		
		// Create components
		RepositoryAdapter adapter = new RepositoryAdapter(repository);
		for (IVersionedId unit : units) {
			IInstallableUnit iu = adapter.findUnit(unit);
			if (iu != null) {
				List<IInstallComponent> addedComponents = addInstallComponent(iu, null);
				// If component has not already been added
				if (addedComponents != null) {
					loadedComponents.addAll(addedComponents);
				}
			}
		}
		
		IInstallComponent[] components = loadedComponents.toArray(new IInstallComponent[loadedComponents.size()]);
		
		// Setup component attributes
		setupComponents(components);
		// Sort components
		sortComponents();
		
		return components;
	}
	
	/**
	 * Loads repository meta-data information.  Components will be added for
	 * all non-category group roots found in the repositories.
	 * 
	 * @param repositoryLocations Locations of repositories to load
	 * @param monitor Progress monitor or <code>null</code>
	 * @return <code>true</code> if all repositories were loaded
	 */
	public synchronized boolean internalLoadMetadataRepositories(URI[] locations, IProgressMonitor monitor) {
		boolean loaded = true;
		
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingStarted);

		try {
			SubMonitor subprogress = SubMonitor.convert(monitor, InstallMessages.LoadingRepositories, locations.length * 2);
			
			// Load the repositories
			for (URI repositoryLocation : locations) {
				try {
					// Load the meta-data repository
					monitor.setTaskName(NLS.bind(InstallMessages.LoadingMetadataRepository0, repositoryLocation.toString()));
					getMetadataRepositoryManager().addRepository(repositoryLocation);
					IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, subprogress.newChild(1));
					metadataRepositories.add(repository);

					// Load components
					loadComponents(repository);
					
					// Fire components notification
					fireComponentsChanged();
				}
				catch (Exception e) {
					loaded = false;
					Installer.log(e);
					getMetadataRepositoryManager().removeRepository(repositoryLocation);
					getArtifactRepositoryManager().removeRepository(repositoryLocation);
					fireRepositoryError(repositoryLocation, e.getLocalizedMessage());
					
					// Don't attempt to load other repositories
					break;
				}
			}

			if (loaded) {
				setupComponentConstraints();
				
				// Pre-calculate the install size for complete set of
				// of repository components.
				IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
				if (!mode.isUpdate() && !mode.isUpdate())
					initializeUninstallerSize();
	
				fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingCompleted);
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
		
		return loaded;
	}
	
	/**
	 * Loads repository artifact information.
	 * 
	 * @param locations Locations of repositories to load
	 * @param monitor Progress monitor
	 * @return <code>true</code> if all repositories were loaded
	 */
	public synchronized boolean internalLoadArtifactRepositories(URI[] locations, IProgressMonitor monitor) {
		boolean loaded = true;
		
		SubMonitor subprogress = SubMonitor.convert(monitor, InstallMessages.LoadingRepositories, locations.length);
		for (URI location : locations) {
			try {
				// Load the artifact repository
				monitor.setTaskName(NLS.bind(InstallMessages.LoadingArtifactRepository0, location.toString()));
				getArtifactRepositoryManager().addRepository(location);
				IArtifactRepository repository = getArtifactRepositoryManager().loadRepository(location, subprogress.newChild(1));
				artifactRepositories.add(repository);
			}
			catch (Exception e) {
				loaded = false;
				Installer.log(e);
				getArtifactRepositoryManager().removeRepository(location);
			}
		}
		
		return loaded;
	}
	
	/**
	 * Sorts components according to the order they appear in the install
	 * description eclipse.p2.requiredRoots or eclipse.p2.optionalRoots 
	 * properties.  If the component does not appear in either list, it is
	 * sorted according to its name.
	 */
	private void sortComponents() {
		// Build one list containing the required and optional roots
		IVersionedId[] requiredRoots = Installer.getDefault().getInstallManager().getInstallDescription().getRequiredRoots();
		IVersionedId[] optionalRoots = Installer.getDefault().getInstallManager().getInstallDescription().getOptionalRoots();
		final ArrayList<IVersionedId> roots = new ArrayList<IVersionedId>();
		if (requiredRoots != null) {
			roots.addAll(Arrays.asList(requiredRoots));
		}
		if (optionalRoots != null) {
			roots.addAll(Arrays.asList(optionalRoots));
		}
		
		Comparator<IInstallComponent> componentOrderComparator = new Comparator<IInstallComponent>() {
			@Override
			public int compare(IInstallComponent arg0, IInstallComponent arg1) {
				int i0 = -1;
				int i1 = -1;
				
				// Find the index of both arguments in the 
				// required/optional list
				for (int index = 0; index < roots.size(); index ++) {
					IVersionedId root = roots.get(index);
					if (arg0.getInstallUnit().getId().equals(root.getId())) {
						i0 = index;
					}
					if (arg1.getInstallUnit().getId().equals(root.getId())) {
						i1 = index;
					}
						
				}
				
				// If both arguments are not in list then sort by name
				if ((i0 == -1) && (i1 == -1)) {
					return arg0.getName().compareTo(arg1.getName());
				}
				// Push first argument to end if not in list
				else if (i0 == -1) {
					return 1;
				}
				// Push second argument to end if not in list
				else if (i1 == -1) {
					return -1;
				}
				// Else sort by order in required/optional list
				else {
					return Integer.compare(i0, i1);
				}
			}
		};
		Collections.sort(components, componentOrderComparator);
	}

	/**
	 * Returns if a component is default.
	 * 
	 * @param component Install component
	 * @return <code>true</code> if component is default
	 */
	private boolean isDefaultComponent(IInstallComponent component) {
		boolean isDefault = false;
		// Default roots
		IVersionedId[] defaultRoots = Installer.getDefault().getInstallManager().getInstallDescription().getDefaultOptionalRoots();
		if (defaultRoots != null) {
			for (IVersionedId defaultRoot : defaultRoots) {
				// Component is default root
				if (component.getInstallUnit().getId().equals(defaultRoot.getId())) {
					isDefault = true;
					break;
				}
				// Else check if parent of component is default root
				else {
					IInstallComponent parent = component.getParent();
					while (parent != null) {
						if (parent.getInstallUnit().getId().equals(defaultRoot.getId())) {
							return true;
						}
						parent = parent.getParent();
					}
				}
			}
		}
		
		return isDefault;
	}

	/**
	 * Returns if a component is required.
	 * 
	 * @param component Install component
	 * @return <code>true</code> if component is required
	 */
	private boolean isRequiredComponent(IInstallComponent component) {
		boolean isRequired = false;
		IVersionedId[] requiredRoots = Installer.getDefault().getInstallManager().getInstallDescription().getRequiredRoots();
		// Find in required roots specification
		if (requiredRoots != null) {
			for (IVersionedId requiredRoot : requiredRoots) {
				// Component is required root
				if (component.getInstallUnit().getId().equals(requiredRoot.getId())) {
					return true;
				}
				// Else check if parent of component is a required root
				else {
					IInstallComponent parent = component.getParent();
					while (parent != null) {
						if (parent.getInstallUnit().getId().equals(requiredRoot.getId())) {
							return true;
						}
						parent = parent.getParent();
					}
				}
			}
		}
		
		return isRequired;
	}
	
	/**
	 * Sets up attributes for all loaded components.
	 * Required components will be set to install.
	 * If at least one component has been installed (update) then optional
	 * components will be set to install only if they are already installed.
	 * If no components have been installed (new install) then optional 
	 * components will be set to install if they are default.
	 * 
	 * @param components Install components
	 */
	private void setupComponents(IInstallComponent[] components) {
		// Installation mode
		IInstallMode installMode = Installer.getDefault().getInstallManager().getInstallMode();

		// Query for existing component IU's.
		IQueryResult<IInstallableUnit> installedResult = null;
		IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();
		IProfile profile = getExistingInstallProfile();
		if (profile != null) {
			installedResult = profile.query(query, null);
		}
		
		// Step 1: Set optional, default, and installed state of components
		for (IInstallComponent component : components) {
			InstallComponent comp = (InstallComponent)component;

			// Set optional status
			boolean isOptional = !isRequiredComponent(component);
			comp.setOptional(isOptional);
			
			// Set default components
			if (isOptional) {
				boolean isDefault = isDefaultComponent(component);
				comp.setDefault(isDefault);
			}
			// Required component
			else {
				comp.setDefault(true);
			}
			
			// Set existing IU (if available)
			if (installedResult != null) {
				Iterator<IInstallableUnit> iter = installedResult.iterator();
				while (iter.hasNext()) {
					IInstallableUnit existingUnit = iter.next();
					if (existingUnit.getId().equals(comp.getInstallUnit().getId())) {
						comp.setInstalledUnit(existingUnit);
						break;
					}
				}
			}
		}

		// Step 2: Set install state for non-group components.  Note that due to groups, this must be done after the 
		// optional state has been set for all components.  The install state for groups will be computed later based
		// on the install state of its members.
		for (IInstallComponent component : components) {
			InstallComponent comp = (InstallComponent)component;

			// Skip groups
			if (comp.hasMembers())
				continue;

			// Optional component
			if (comp.isOptional()) {
				// If component is not already installed, set it to install if it is default
				if (comp.getInstalledUnit() == null) {
					comp.setInstall(installMode.isUpdate() ? false : comp.isDefault());
				}
				// Else set it to install
				else {
					comp.setInstall(true);
					// If this is a new install or upgrade, change the component to be required.  This will prevent
					// an installation of a second product into a first product from removing a component installed by
					// the first product.
					if (!installMode.isUpdate()) {
						comp.setOptional(false);
					}
				}
			}
			// Required component
			else {
				comp.setInstall(true);
			}
		}

		// Step 3: Set the install state for group components based on the install state of members
		for (IInstallComponent component : components) {
			InstallComponent comp = (InstallComponent)component;

			// Is group
			if (comp.hasMembers()) {
				ArrayList<IInstallComponent> members = new ArrayList<IInstallComponent>();
				getInstallComponentMembers(comp, members);
				
				boolean allInstall = true;
				boolean allRequired = true;
				// Check if all group members are set to install and if they are required
				for (IInstallComponent member : members) {
					if (!member.getInstall()) {
						allInstall = false;
					}
					if (member.isOptional()) {
						allRequired = false;
					}
				}
				// Set group to install
				if (allInstall) {
					comp.setInstall(true);
				}
				// Set group to be required
				if (allRequired) {
					comp.setOptional(false);
				}
			}
		}
	}
	
	/**
	 * Sets up components according to any install constraints specified.
	 */
	private void setupComponentConstraints() {
		try {
			// Loop through the constraints
			IInstallConstraint[] constraints = Installer.getDefault().getInstallManager().getInstallDescription().getInstallConstraints();
			if (constraints != null) {
				for (IInstallConstraint constraint : constraints) {
					// If a constraint that one component out of a set of 'one' components is specified, mark that component 
					// as required (as there are no other components specified that can be selected)
					if (constraint.getConstraint() == Constraint.ONE_OF) {
						IInstallComponent[] targets = constraint.getTargets();
						if ((targets != null) && (targets.length == 1)) {
							if (targets[0] != null) {
								((InstallComponent)targets[0]).setOptional(false);
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	/**
	 * @return Meta-data repositories
	 */
	public IMetadataRepository[] getMetadataRepositories() {
		return metadataRepositories.toArray(new IMetadataRepository[metadataRepositories.size()]);
	}
	
	/**
	 * @return Returns the meta-data repository locations.
	 */
	public URI[] getMetadataRepositoryLocations() {
		ArrayList<URI> locations = new ArrayList<URI>();
		for (IMetadataRepository repository : getMetadataRepositories()) {
			locations.add(repository.getLocation());
		}
		
		return locations.toArray(new URI[locations.size()]);
	}

	/**
	 * @return Artifact repositories
	 */
	public IArtifactRepository[] getArtifactRepositories() {
		return artifactRepositories.toArray(new IArtifactRepository[artifactRepositories.size()]);
	}
	
	/**
	 * @return Returns the artifact repository locations.
	 */
	public URI[] getArtifactRepositoryLocations() {
		ArrayList<URI> locations = new ArrayList<URI>();
		for (IArtifactRepository repository : getArtifactRepositories()) {
			locations.add(repository.getLocation());
		}
		
		return locations.toArray(new URI[locations.size()]);
	}

	/**
	 * Adds a listener to component changes.
	 * 
	 * @param listener Listener to add
	 */
	public void addRepositoryListener(IInstallRepositoryListener listener) {
		repositoryListeners.add(listener);
	}
	
	/**
	 * Removes a listener from component changes.
	 * 
	 * @param listener Listener to remove
	 */
	public void removeRepositoryListener(IInstallRepositoryListener listener) {
		repositoryListeners.remove(listener);
	}
	
	/**
	 * Returns install components.
	 * 
	 * @param groupsOnly <code>true</code> to return group components and 
	 * components not contained in a group.  <code>false</code> to return all 
	 * install components including groups.  This will include component group
	 * members.
	 * @return Install components
	 */
	public IInstallComponent[] getInstallComponents(boolean groupsOnly) {
		if (groupsOnly) {
			ArrayList<IInstallComponent> groups = new ArrayList<IInstallComponent>();
			for (IInstallComponent component : components) {
				if (component.getParent() == null) {
					groups.add(component);
				}
			}
			return groups.toArray(new IInstallComponent[groups.size()]);
		}
		else {
			return components.toArray(new IInstallComponent[components.size()]);
		}
	}

	/**
	 * Gets all members of a group component. This includes all member components of contained group members.
	 * If the component is not a group (contains no member components) then this method does nothing.
	 * 
	 * @param component Component for members
	 * @param componentMembers All member components.
	 */
	private void getInstallComponentMembers(IInstallComponent component, List<IInstallComponent> componentMembers) {
		if (component.hasMembers()) {
			IInstallComponent[] members = component.getMembers();
			for (IInstallComponent member : members) {
				getInstallComponentMembers(member, componentMembers);
			}
			componentMembers.addAll(Arrays.asList(members));
		}
	}
	
	/**
	 * Returns if there are components to add or remove in installation.
	 * 
	 * @return <code>true</code> if there is an install plan
	 */
	public boolean hasInstallUnits() {
		ArrayList<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
		ArrayList<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>();
		getInstallUnits(toAdd, toRemove);
		
		return ((toAdd.size() != 0) || (toRemove.size() != 0));
	}
	
	/**
	 * Returns all installable units including groups.
	 * 
	 * @param toAdd Filled with installable units to add.
	 */
	public void getAllInstallUnits(List<IInstallableUnit> toAdd) {
		toAdd.clear();
		for (IInstallComponent component : getInstallComponents(false)) {
			if (component.isIncluded() && component.getInstall()) {
				toAdd.add(component.getInstallUnit());
			}
		}
	}
	
	/**
	 * Get the installation units.
	 * 
	 * @param toAdd Filled with installable units to add
	 * @param toRemove Filled with installable units to remove
	 */
	public void getInstallUnits(List<IInstallableUnit> toAdd, List<IInstallableUnit> toRemove) {
		toAdd.clear();
		toRemove.clear();

		IProfile installProfile = null;
		try {
			installProfile = getInstallProfile();
		} catch (ProvisionException e) {
			Installer.log(e);
			return;
		}

		// If a temporary repository is setup for the product, a root IU will be created for the product.  This IU will 
		// specify requirements on the installation IU's and be used to provision the product.
		// If no product repository is available, the IU's selected for installation will be provisioned directly as
		// roots.
		boolean createProductRoot = (getProductRepository() != null);
		
		// Installation mode
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		boolean removeProfile = Installer.getDefault().getInstallManager().getInstallDescription().getRemoveProfile();
		
		ArrayList<IInstallableUnit> unitsToAdd = new ArrayList<IInstallableUnit>();
		for (IInstallComponent component : getInstallComponents(false)) {
			// Unit to install
			IInstallableUnit installUnit = component.getInstallUnit();

			// If component is included and not a group
			if (component.isIncluded() && !component.hasMembers()) {
				// Existing installed unit
				IInstallableUnit installedUnit = component.getInstalledUnit();
	
				// Create product IU for IU's
				if (createProductRoot) {
					if (component.getInstall()) {
						unitsToAdd.add(installUnit);
					}
					// Remove any product IU's that were previously provisioned as
					// root IU's.
					if (installedUnit != null) {
						String rootProperty = installProfile.getInstallableUnitProperty(installedUnit, IProfile.PROP_PROFILE_ROOT_IU);
						if (Boolean.TRUE.toString().equals(rootProperty)) {
							toRemove.add(installedUnit);
						}
					}
				}
				// Provision individual IU's
				else {
					// Component marked for install
					if (component.getInstall()) {
						// If newer version to install
						if ((installedUnit == null) || installUnit.getVersion().compareTo(installedUnit.getVersion()) > 0) {
							// Add new unit
							unitsToAdd.add(installUnit);
		
							// If there is an existing unit, remove it if we are not
							// removing all existing units in the profile
							if ((installedUnit != null) && !removeProfile) {
								toRemove.add(installedUnit);
							}
						}
					}
					// Component marked for uninstall, remove older unit if present
					else if (installedUnit != null) {
						toRemove.add(installedUnit);
					}
				}
			}
		}
		
		if (installProfile != null) {
			// A root IU being added could require other IU's that have been previously provisioned as root IU's.
			// If these IU's are a newer version than the existing IU's, provisioning will fail because the required
			// root IU has not been requested for removal (P2 will not automatically replace root IU's).
			// Add any required root IU's for removal
			for (IInstallableUnit unit : unitsToAdd) {
				ArrayList<IInstallableUnit> requiredRoots = new ArrayList<IInstallableUnit>();
				getExistingRequiredRootIUs(unit, requiredRoots);
				if (!requiredRoots.isEmpty()) {
					toRemove.addAll(requiredRoots);
				}
			}
		}

		// Remove all units in the profile
		if (removeProfile) {
			if (mode.isUpgrade()) {
				IProfile profile = getExistingInstallProfile();
				if (profile != null) {
					IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUAnyQuery(), null);
					Iterator<IInstallableUnit> i = query.iterator();
					while (i.hasNext()) {
						toRemove.add(i.next());
					}
				}
			}
		}

		// Create product root IU
		if (createProductRoot) {
			try {
				// Add product IU
				IInstallableUnit[] productRoots = getProductRepository().createProductIu(getInstallProfile(), unitsToAdd);
				toAdd.add(productRoots[0]);
				// Remove existing product IU if one is installed
				if (productRoots[1] != null) {
					toRemove.add(productRoots[1]);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		// Or provision each IU as a root
		else {
			toAdd.addAll(unitsToAdd);
		}
	}
	
	/**
	 * Checks the requirements of a specified IU and returns any that are already installed as a root IU.
	 * 
	 * @param unit IU to check for requirements.
	 * @param requiredRoots Filled with root IU's that are required by the IU.
	 */
	private void getExistingRequiredRootIUs(IInstallableUnit unit, ArrayList<IInstallableUnit> requiredRoots) {
		try {
			// Get requirements for the IU
			IQueryResult<IInstallableUnit> requirements = getMetadataRepositoryManager().query(new RequiredIUsQuery(unit), new NullProgressMonitor());
			Iterator<IInstallableUnit> iter = requirements.iterator();
			ProfileAdapter profileAdapter = new ProfileAdapter(getInstallProfile());
			RepositoryManagerAdapter repositoryAdapter = new RepositoryManagerAdapter(getMetadataRepositoryManager());
			while (iter.hasNext()) {
				IInstallableUnit required = iter.next();
				// Check if the required IU is already present in the profile
				IInstallableUnit existingRequired = profileAdapter.findUnit(required.getId());
				if (existingRequired != null) {
					// See if the required IU is present in the repository
					IInstallableUnit installingUnit = repositoryAdapter.findUnit(new VersionedId(existingRequired.getId(), Version.emptyVersion));
					if ((installingUnit != null) && !requiredRoots.contains(installingUnit)) {
						// Is the required IU a root
						String root = getInstallProfile().getInstallableUnitProperties(existingRequired).get(IProfile.PROP_PROFILE_ROOT_IU);
						boolean newerVersion = (required.getVersion().compareTo(existingRequired.getVersion()) > 0);
						
						// If the required IU is an existing root then add it
						if (Boolean.TRUE.toString().equals(root) && newerVersion) {
							if (!requiredRoots.contains(existingRequired)) {
								requiredRoots.add(existingRequired);
								getExistingRequiredRootIUs(required, requiredRoots);
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	/**
	 * Returns the context for provisioning.  The context will be scoped to the installation repositories.  For an 
	 * update, the context will also include the installed repositories.
	 * 
	 * @return Provisioning context
	 */
	public ProvisioningContext getProvisioningContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		
		ArrayList<URI> metadataRepositories = new ArrayList<URI>();
		ArrayList<URI> artifactRepositories = new ArrayList<URI>();
		
		// Add installer repositories
		metadataRepositories.addAll(Arrays.asList(getMetadataRepositoryLocations()));
		artifactRepositories.addAll(Arrays.asList(getArtifactRepositoryLocations()));

		// Add cache meta-data repository if available
		if (cacheMetadataRepository != null) {
			metadataRepositories.add(cacheMetadataRepository.getLocation());
		}
		// Add cache artifact repository if available
		if (cacheArtifactRepository != null) {
			artifactRepositories.add(cacheArtifactRepository.getLocation());
		}
		
		// If update then include installed repositories in addition to installer repositories.
		// Include only local installed repositories for much improved performance.
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		if (mode.isUpdate()) {
			// Include all repositories or only local
			int flags = Installer.getDefault().getInstallManager().getInstallDescription().getIncludeAllRepositories() ?
					IRepositoryManager.REPOSITORIES_ALL : IRepositoryManager.REPOSITORIES_LOCAL;
			
			// Installed meta-data repositories
			IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager)getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			URI[] installedMetadataLocations = metadataManager.getKnownRepositories(flags);
			metadataRepositories.addAll(Arrays.asList(installedMetadataLocations));
			
			// Installed artifact repositories
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			URI[] installedArtifactLocations = artifactManager.getKnownRepositories(flags);
			artifactRepositories.addAll(Arrays.asList(installedArtifactLocations));
		}
		
		if (!metadataRepositories.isEmpty()) {
			context.setMetadataRepositories(metadataRepositories.toArray(new URI[metadataRepositories.size()]));
		}
		if (!artifactRepositories.isEmpty()) {
			context.setArtifactRepositories(artifactRepositories.toArray(new URI[artifactRepositories.size()]));
		}
		
		return context;
	}
	
	/**
	 * Computes the install plan.
	 * 
	 * @param monitor Progress monitor or <code>null</code>
	 * @return Install plan or <code>null</code> if canceled.
	 */
	public IInstallPlan computeInstallPlan(IProgressMonitor monitor) {
		IInstallPlan installPlan = null;
		
		try {
			if (monitor == null)
				monitor = new NullProgressMonitor();
			
			SubMonitor mon = SubMonitor.convert(monitor, 600);
			
			// Get the install units to add or remove
			ArrayList<IInstallableUnit> unitsToAdd = new ArrayList<IInstallableUnit>();
			ArrayList<IInstallableUnit> unitsToRemove = new ArrayList<IInstallableUnit>();
			getInstallUnits(unitsToAdd, unitsToRemove);
			// Nothing to do
			if (unitsToAdd.isEmpty() && unitsToRemove.isEmpty()) {
				return new InstallPlan(Installer.getDefault().getInstallManager().getInstallLocation(), Status.OK_STATUS, 0, 0);
			}

			// Return cached install plan if available.
			final String hash = getComponentsHash();
			installPlan = planCache.get(hash);
			if (installPlan != null) {
				return installPlan;
			}

			mon.worked(100);
			if (mon.isCanceled())
				return null;
			
			if ((getAgent() == null) || (getInstallLocation() == null))
				return null;
			IProfile profile = getInstallProfile();

			IPlanner planner = (IPlanner)agent.getService(IPlanner.SERVICE_NAME);
			IEngine engine = (IEngine)agent.getService(IEngine.SERVICE_NAME);
			IProfileChangeRequest request = planner.createChangeRequest(profile);
			
			request.addAll(unitsToAdd);
			request.removeAll(unitsToRemove);

			IProvisioningPlan plan = planner.getProvisioningPlan(request, getProvisioningContext(), mon.newChild(300));

			IStatus status = plan.getStatus();
			// Problem computing plan
			if (!status.isOK()) {
				StringBuilder buffer = new StringBuilder();
				buffer.append(status.getMessage());
				buffer.append('\n');
				IStatus[] children = status.getChildren();
				for (IStatus child : children) {
					buffer.append(child.getMessage());
					buffer.append('\n');
				}
				Installer.log(buffer.toString());
			}

			if (mon.isCanceled())
				return null;

			long installPlanSize = 0;
			long installPlanDownloadSize = 0;
			if (plan.getInstallerPlan() != null) {
				ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
				engine.perform(plan.getInstallerPlan(), sizingPhaseSet, mon.newChild(100));
				installPlanSize = sizingPhaseSet.getDiskSize();
				installPlanDownloadSize = sizingPhaseSet.getDownloadSize();
			} else {
				mon.worked(100);
			}

			if (mon.isCanceled())
				return null;

			ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
			engine.perform(plan, sizingPhaseSet, mon.newChild(100));
			
			long installSize = installPlanSize + sizingPhaseSet.getDiskSize() + getUninstallerSize();
			long requiredSize = installSize + installPlanDownloadSize + sizingPhaseSet.getDownloadSize();

			installPlan = new InstallPlan(
					Installer.getDefault().getInstallManager().getInstallLocation(), 
					status, 
					installSize, 
					requiredSize);
			planCache.put(hash, installPlan);
		} catch (Exception e) {
			monitor.setCanceled(true);
			Installer.log(e);
		}
		
		return installPlan;
	}

	/**
	 * Returns an install component.
	 * 
	 * @param id Install component root identifier
	 * @return Install component or <code>null</code>
	 */
	public IInstallComponent getInstallComponent(String id) {
		IInstallComponent foundComponent = null;
		
		for (IInstallComponent component : components) {
			if (component.getInstallUnit().getId().equals(id)) {
				foundComponent = component;
				break;
			}
		}
		
		return foundComponent;
	}
	
	/**
	 * Fires a repository status notification.
	 * 
	 * @param status Status
	 */
	void fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus status) {
		Object[] listeners = repositoryListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((IInstallRepositoryListener)listener).repositoryStatus(status);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Fires components changed notification.
	 */
	void fireComponentsChanged() {
		Object[] listeners = repositoryListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((IInstallRepositoryListener)listener).installComponentsChanged();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Fires a repository loading error.
	 * 
	 * @param location Repository location
	 * @param errorMessage Error information
	 */
	void fireRepositoryError(URI location, String errorMessage) {
		Object[] listeners = repositoryListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((IInstallRepositoryListener)listener).repositoryError(location, errorMessage);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Fires a component changed notification.
	 * 
	 * @param component Install component
	 */
	void fireComponentChanged(IInstallComponent component) {
		Object[] listeners = repositoryListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((IInstallRepositoryListener)listener).installComponentChanged(component);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the size of the uninstaller.  If the uninstaller size computation
	 * is still in progress, this method waits.
	 * 
	 * @return Uninstaller size in bytes
	 */
	private long getUninstallerSize() {
		// Wait for uninstaller thread if running
		if (uninstallerSizeThread != null) {
			try {
				uninstallerSizeThread.join();
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		
		return uninstallerSize;
	}
	
	/**
	 * Computes the size of the uninstaller.
	 */
	protected void initializeUninstallerSize() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		
		// If installing and not an update
		if (mode.isInstall() && !mode.isUpdate()) {
			if (uninstallerSizeThread == null) {
				uninstallerSizeThread = new Thread() {
					@Override
					public void run() {
						String [] uninstallFiles = Installer.getDefault().getInstallManager().getInstallDescription().getUninstallFiles();
						long totalSize = 0;
						if (uninstallFiles != null) {
							for (String uninstallFilePath : uninstallFiles) {
								class SizeCalculationVisitor extends SimpleFileVisitor<Path> {
									private long size = 0;	
									@Override
									public FileVisitResult visitFile(Path file,
											BasicFileAttributes attrs) throws IOException {
										size += file.toFile().length();
										return FileVisitResult.CONTINUE;
									}
									
									public long getSize() {
										return size;
									}
								}
								
								try {
									File srcFile = Installer.getDefault().getInstallFile(uninstallFilePath);
									if (srcFile != null && srcFile.exists()) {
										SizeCalculationVisitor visitor = new SizeCalculationVisitor();
										Files.walkFileTree(srcFile.toPath(), visitor);
										totalSize += visitor.getSize();
									}
								} catch (Exception e) {
									Installer.log(e);
								}
							}
						}
						uninstallerSize = totalSize;
					}
				};
				uninstallerSizeThread.start();
			}
		}
		// Else not uninstaller
		else {
			uninstallerSize = 0;
		}
	}
	
	/**
	 * Generate a string hash from the install components.
	 * @return hash of root list
	 */
	private String getComponentsHash() {
		StringBuilder hash = new StringBuilder();
		for (IInstallComponent component : getInstallComponents(false)) {
			if (component.getInstall()) {
				IInstallableUnit unit = component.getInstallUnit();
				if (unit != null) {
					hash.append(unit.getId());
				}
			}
		}

		return hash.toString();
	}
	
	/**
	 * Provisions Installable Units into a system and/or saves to the cache mirror.
	 * 
	 * @param profile Profile for provision
	 * @param toAdd Units to add or <code>null</code>.
	 * @param toRemove Units to remove or <code>null</code>.
	 * @param clearDownloadCache <code>true</code> to remove the download cache.
	 * @param progressMonitor Progress monitor.
	 * @throws CoreException on provisioning failure.
	 */
	public void provision(IProfile profile, List<IInstallableUnit> toAdd, List<IInstallableUnit> toRemove, 
			boolean clearDownloadCache, IProgressMonitor progressMonitor) throws CoreException {

		final int WORK_SEGMENT = 100;

		int totalWork = 0;
		// Update cache work
		if (getUpdateCache()) {
			totalWork += WORK_SEGMENT;
		}
		// Install work
		if (getInstallLocation() != null) {
			totalWork += WORK_SEGMENT;
		}
		
		ProvisioningProgressMonitor monitor = new ProvisioningProgressMonitor(progressMonitor);
		
		monitor.beginTask("", totalWork);
		
		try {
			// Update cache repository
			if (getUpdateCache()) {
				monitor.setShowRemainingTime(true);
				updateCacheRepository(toAdd, new SubProgressMonitor(monitor, WORK_SEGMENT));
				monitor.setShowRemainingTime(false);
			}

			// Install from the cache repository
			if (getCacheOnly() || getUpdateCache()) {
				unloadInstallRepositories();
			}

			// Performing an installation	File
			if (getInstallLocation() != null) {
				// Reload the cache repository
				try {
					loadCacheRepository();
				}
				catch (Exception e) {
					Installer.log(e);
				}

				// Get the planner
				IPlanner planner = (IPlanner)agent.getService(IPlanner.SERVICE_NAME);
				// Provisioning context
				ProvisioningContext context = getProvisioningContext();
				// Provisioning request
				IProfileChangeRequest request = planner.createChangeRequest(profile);
				
				// Units to add
				if ((toAdd != null) && !toAdd.isEmpty())
					request.addAll(toAdd);
				// Units to remove
				if ((toRemove != null) && !toRemove.isEmpty())
					request.removeAll(toRemove);
				
				// When creating a product root IU, all component IU's will be provisioned as (non-root) requirements
				// of the product IU.  There can be a case where these component IU's were previously provisioned as
				// root IU's.  This can happen for an upgrade if the release uses eclipse.p2.product.root=true, but the
				// previous version used eclipse.p2.product.root=false.
				// To handle this, the root property is set to false for all members of the product root IU.
				if (getProductRepository() != null) {
					try {
						RepositoryManagerAdapter repositoryManager = new RepositoryManagerAdapter(getMetadataRepositoryManager());
						for (IInstallableUnit unitToAdd : toAdd) {
							ArrayList<IInstallableUnit> members = new ArrayList<IInstallableUnit>();
							repositoryManager.findMemberUnits(unitToAdd, members);
							for (IInstallableUnit member : members) {
								request.setInstallableUnitProfileProperty(member, IProfile.PROP_PROFILE_ROOT_IU, 
										Boolean.FALSE.toString());
							}
						}
					}
					catch (Exception e) {
						// Don't fail the operation
						Installer.log(e);
					}
				}
				
				// Logging
				Installer.log("(Provision) Adding:");
				if ((toAdd == null) || (toAdd.size() == 0)) {
					Installer.log("\tNone");
				}
				else {
					for (IInstallableUnit unit : toAdd) {
						Installer.log("\t" + unit.getId() + ":" + unit.getVersion().toString());
					}
				}
				Installer.log("(Provision) Removing:");
				if ((toRemove == null) || (toRemove.size() == 0)) {
					Installer.log("\tNone");
				}
				else {
					for (IInstallableUnit unit : toRemove) {
						Installer.log("\t" + unit.getId() + ":" + unit.getVersion().toString());
					}
				}
		
				// Create provisioning progress monitor
				IProgressMonitor provisioningMonitor = new SubProgressMonitor(monitor, WORK_SEGMENT);
				IInstallDescription desc = Installer.getDefault().getInstallManager().getInstallDescription();
				if (desc != null) {
					monitor.setFilter(desc.getProgressFindPatterns(), desc.getProgressReplacePatterns());
				}
		
				// Provision operation
				IDirector director = (IDirector)agent.getService(IDirector.SERVICE_NAME);
				IStatus status = director.provision(request, context, provisioningMonitor);
				if ((status != null) && (status.getSeverity() == IStatus.ERROR))
					throw new CoreException(status);
	
				if ((toAdd != null) && !toAdd.isEmpty()) {
					// Clear download cache
					if (clearDownloadCache) {
						try {
							IFileArtifactRepository cacheRepo = Util.getDownloadCacheRepo(agent);
							cacheRepo.removeAll(monitor);
						}
						catch (Exception e) {
							// Just log any failure to clear the cache as it is
							// non-critical to the installation.
							Installer.log(e);
						}
					}
				}
			}
		}
		finally {
			monitor.done();
		}
	}

	/**
	 * Returns members of a category IU.  If the IU corresponds to an install component, all members collected from
	 * category meta-data from all repositories will be returned.  If no install component is available, the meta-data
	 * repository manager will be queried for the members.
	 * 
	 * @param categoryIu Category IU
	 * @return Category member IU's
	 */
	private List<IInstallableUnit> getCategoryMembers(IInstallableUnit categoryIu) {
		ArrayList<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
		IInstallComponent categoryComponent = getInstallComponent(categoryIu.getId());
		
		if (categoryComponent != null) {
			IInstallComponent[] members = categoryComponent.getMembers();
			for (IInstallComponent member : members) {
				units.add(member.getInstallUnit());
			}
		}
		else {
			IQuery<IInstallableUnit> categoryQuery = QueryUtil.createIUCategoryMemberQuery(categoryIu);
			IQueryResult<IInstallableUnit> query = getMetadataRepositoryManager().query(categoryQuery, null);
			Iterator<IInstallableUnit> iter = query.iterator();
			while (iter.hasNext()) {
				IInstallableUnit member = iter.next();
				units.add(member);
			}
		}
		
		return units;
	}

	/**
	 * Returns the repository mirror application.
	 * 
	 * @param units Installable units
	 * @param monitor Progress monitor
	 * @return Mirror application
	 * @throws ProvisionException on failure
	 */
	private InstallerMirrorApplication getMirrorApplication(List<IInstallableUnit> units, IProgressMonitor monitor) throws ProvisionException {
		// Create the mirror application
		String progressText = 
				Installer.getDefault().getInstallManager().getInstallDescription().getText(IInstallDescription.TEXT_PROGRESS_MIRRORING,
						InstallMessages.Progress_Saving);
		InstallerMirrorApplication mirrorApp = new InstallerMirrorApplication(agent, monitor, progressText);
		
		// No base-line comparison
		mirrorApp.setCompare(false);

		// Set source meta-data repositories
		for (URI location : getMetadataRepositoryLocations()) {
			RepositoryDescriptor source = new RepositoryDescriptor();
			source.setLocation(location);
			source.setKind(RepositoryDescriptor.KIND_METADATA);
			source.setOptional(false);
			mirrorApp.addSource(source);
		}
		
		// Set source artifact repositories
		for (URI location : getArtifactRepositoryLocations()) {
			RepositoryDescriptor source = new RepositoryDescriptor();
			source.setLocation(location);
			source.setKind(RepositoryDescriptor.KIND_ARTIFACT);
			source.setOptional(false);
			mirrorApp.addSource(source);
		}
		
		File cacheFile = getCacheLocation().toFile();

		// Set mirror destination
		RepositoryDescriptor dest = new RepositoryDescriptor();
		// Note, appending does not seem to work correctly
		dest.setAppend(false);
		dest.setLocation(cacheFile.toURI());
		dest.setName(Installer.getDefault().getInstallManager().getInstallDescription().getProductName());
		mirrorApp.addDestination(dest);
		
		mirrorApp.initializeRepos(null);
		// Set the installable units to mirror into the cache repository
		mirrorApp.setSourceIUs(units);
		// Abort on errors
		mirrorApp.setIgnoreErrors(false);
		
		return mirrorApp;
	}
	
	/**
	 * Returns the size of the cache download.
	 * 
	 * @param monitor Progress monitor
	 * @return Cache size in bytes
	 */
	public InstallPlan computeCacheSize(IProgressMonitor monitor) {
		try {
			ArrayList<IInstallableUnit> unitsToAdd = new ArrayList<IInstallableUnit>();
			ArrayList<IInstallableUnit> unitsToRemove = new ArrayList<IInstallableUnit>();
			getInstallUnits(unitsToAdd, unitsToRemove);
	
			InstallerMirrorApplication mirrorApp = getMirrorApplication(unitsToAdd, monitor);
			
			return new InstallPlan(getCacheLocation(), mirrorApp.getDownloadSize());
		}
		catch (Exception e) {
			Installer.log(e);
			return null;
		}
	}
	
	/**
	 * Updates the local cache repository from the install repositories.
	 * 
	 * @param toAdd Installable units to add to the cache.
	 * @param monitor Installable units to remove from the cache.
	 * @throws CoreException on failure to update the cache repository.
	 */
	private void updateCacheRepository(List<IInstallableUnit> toAdd, IProgressMonitor monitor) throws CoreException {
		try {
			// Non-category IU's
			ArrayList<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
			// Category IU's
			ArrayList<IInstallableUnit> categoryUnits = new ArrayList<IInstallableUnit>();
			
			// Separate the IU's as the category IU's will be mirrored separately
			for (IInstallableUnit unit : toAdd) {
				// Category unit
				if (isCategoryIu(unit)) {
					categoryUnits.add(unit);
				}
				// Non-category unit
				else {
					units.add(unit);
				}
			}
			
			IPath cacheLocation = getCacheLocation();
			// No cache location
			if (cacheLocation == null)
				return;

			// Get the mirror application
			InstallerMirrorApplication mirrorApp = this.getMirrorApplication(units, monitor);
			
			// Create the cache directory
			File cacheFile = getCacheLocation().toFile();
			if (!cacheFile.exists()) {
				Files.createDirectories(cacheFile.toPath());
			}

			// Perform the mirror
			IStatus status = mirrorApp.run(monitor);
			if (status.getSeverity() == IStatus.ERROR) {
				Installer.fail(InstallMessages.Error_UpdateCache);
			}
			
			// The normal P2 mirror operation will not mirror a category IU that is defined in multiple source 
			// repositories with different requirements.  Attempting to slice this type of IU will result in the 
			// meta-data for the IU only containing requirements from one repository.  To avoid this, the category IU's 
			// are re-created in the destination repository.
			if (categoryUnits.size() > 0) {
				ArrayList<IInstallableUnit> categoryUnitsToAdd = new ArrayList<IInstallableUnit>();
				for (IInstallableUnit unit : categoryUnits) {
					// Create IU description for category
					InstallableUnitDescription iuDesc = InstallUtils.createIuDescription(
							unit.getId(), 
							unit.getVersion(), 
							unit.isSingleton(), 
							unit.getProperties());
	
					// Create requirements for category member IU's
					List<IInstallableUnit> members = getCategoryMembers(unit);
					InstallUtils.addInstallableUnitRequirements(iuDesc, members, false);
					
					// Create category IU and add it to repository
					IInstallableUnit categoryIu = MetadataFactory.createInstallableUnit(iuDesc);
					categoryUnitsToAdd.add(categoryIu);
				}
				
				try {
					loadCacheRepository();
					cacheMetadataRepository.addInstallableUnits(categoryUnitsToAdd);
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
		}
		catch (Exception e) {
			Installer.fail(e);
		}
	}
	
	/**
	 * Returns the name for an installable unit.
	 * 
	 * @param id IU identifier.
	 * @return IU name or <code>null</code> if the IU is not found.
	 */
	public String queryIuName(String id) {
		IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id);
		IQueryResult<IInstallableUnit> result = getMetadataRepositoryManager().query(query, null);
		Iterator<IInstallableUnit> iter = result.iterator();
		while (iter.hasNext()) {
			IInstallableUnit unit = iter.next();
			return unit.getProperty(IInstallableUnit.PROP_NAME, null);
		}
		
		return null;
	}
}
