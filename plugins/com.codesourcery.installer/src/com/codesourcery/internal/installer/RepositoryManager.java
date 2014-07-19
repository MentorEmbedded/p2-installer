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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.ui.IInstallComponent;

/**
 * This class manages repositories used in the installation.
 * The manager can be accessed using the {@link #getDefault()} method.
 */
@SuppressWarnings("restriction")
public final class RepositoryManager {
	/** Repository job family */
	private static final String REPOSITORY_JOB_FAMILY = "RepositoryJobFamily";
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
	/** Number of loads in progress */
	private int loads = 0;
	/** Repositories */
	private ArrayList<URI> repositoryLocations = new ArrayList<URI>();
	/** Cache to store computed installation plans */
	protected Map<String, IInstallPlan> planCache;
	/** Installer size thread */
	Thread uninstallerSizeThread;
	/** Size of uninstaller files */
	protected long uninstallerSize = 0;
	/** Install location */
	private IPath installLocation;

	/**
	 * Constructor
	 */
	private RepositoryManager() {
		// Synchronize size cache
		planCache = Collections.synchronizedMap(new HashMap<String, IInstallPlan>());
	}
	
	/**
	 * Creates the P2 provisioning agent.
	 * 
	 * @param installLocation Install location
	 */
	public void createAgent(IPath installLocation) throws CoreException {
		try {
			// Install location changed
			if (!installLocation.equals(getInstallLocation())) {
				this.installLocation = installLocation;

				// Start P2 agent
				agent = startAgent(getAgentLocation());
				// Setup certificate handling
				agent.registerService(UIServices.SERVICE_NAME, AuthenticationService.getDefault());
				// Meta-data repository manager
				metadataRepoMan = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
				// Artifact repository manager
				artifactRepoMan = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

				IInstallDescription installDescription = Installer.getDefault().getInstallManager().getInstallDescription();
				// Load repositories
				if (installDescription != null) {
					loadRepositories(installDescription.getMetadataRepositories());
				}
			}
		}
		catch (Exception e) {
			Installer.fail(e.getLocalizedMessage());
		}
	}
	
	/**
	 * Stopes the P2 provisioning agent.
	 */
	public void stopAgent() {
		if (agent != null) {
			
			try {
				// Remove repositories
				for (URI repositoryLocation : repositoryLocations) {
					getMetadataRepositoryManager().removeRepository(repositoryLocation);
					getArtifactRepositoryManager().removeRepository(repositoryLocation);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
			
			agent.stop();
			agent = null;
		}
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
	 * @return Agent location
	 */
	public IPath getAgentLocation() {
		return getInstallLocation().append(IInstallConstants.P2_DIRECTORY);
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
	
	/**
	 * Returns the profile for this installation.  The profile is created if
	 * necessary.
	 * 
	 * @return Profile Profile
	 * @throws ProvisionException on failure to create the profile
	 */
	public IProfile getInstallProfile() throws ProvisionException {
		String profileId = Installer.getDefault().getInstallManager().getInstallDescription().getProfileName();
		IProfile profile = getProfile(profileId);
		if (profile == null) {
			profile = createProfile(profileId);
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
		// Cancel and wait for any running repository load jobs
		waitForLoadJobs(true);

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
	 * Returns the repository manager.
	 * 
	 * @return Repository manager
	 */
	private IMetadataRepositoryManager getMetadataRepositoryManager() {
		return metadataRepoMan;
	}
	
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
	 * Loads repository meta-data information.  This method returns immediately
	 * and the repositories are loaded in a job.
	 * A client can call {@link #waitForLoad()} to wait for the repository
	 * information to be loaded.
	 * A client can call {@link #addRepositoryListener(IInstallRepositoryListener)}
	 * to receive notifications about repository loading.
	 *
	 * Install components will be added for all roots specified if they are 
	 * found in the loaded repositories.
	 * The components will be considered required unless specified in the
	 * optional roots.  
	 * 
	 * @param repositoryLocations Locations of repositories to load
	 */
	public void loadRepositories(URI[] repositoryLocations) {
		// Fire loading status
		if (++loads == 1)
			fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingStarted);

		// Start load job
		LoadComponentsJob job = new LoadComponentsJob(repositoryLocations);
		job.schedule();
	}

	/**
	 * Loads repository meta-data information.  Components will be added for
	 * all non-category group roots found in the repositories.
	 * 
	 * @param repositoryLocations Locations of repositories to load
	 * @param optional <code>true</code> if the components are optional,
	 * <code>false</code> if the components are required
	 */
	public void loadRepositories(URI[] repositoryLocations, boolean optional) {
		// Fire loading status
		if (++loads == 1)
			fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingStarted);

		// Start load job
		LoadAllComponentsJob job = new LoadAllComponentsJob(repositoryLocations, optional);
		job.schedule();
	}
	
	/**
	 * Returns repository locations.
	 * If repositories are currently being loaded, this method will not return
	 * until the load has completed.
	 * 
	 * @return Repository locations
	 */
	public URI[] getRepositoryLocations() {
		return repositoryLocations.toArray(new URI[repositoryLocations.size()]);
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
	 * If repositories are currently being loaded, this method will not return
	 * until the load has completed.
	 * 
	 * @return Install components
	 */
	public IInstallComponent[] getInstallComponents() {
		return components.toArray(new IInstallComponent[components.size()]);
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
	 * Get the installation units.
	 * 
	 * @param toAdd Filled with installable units to add
	 * @param toRemove Filled with installable units to remove
	 */
	public void getInstallUnits(List<IInstallableUnit> toAdd, List<IInstallableUnit> toRemove) {
		toAdd.clear();
		toRemove.clear();

		// Installation mode
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		
		for (IInstallComponent component : components) {
			if (component.isIncluded()) {
				IInstallableUnit installUnit = component.getInstallUnit();
				IInstallableUnit installedUnit = component.getInstalledUnit();
	
				// If upgrade, only add new units as all units in profile will be
				// removed
				if (mode.isUpgrade()) {
					if (component.getInstall()) {
						toAdd.add(installUnit);
					}
				}
				// Component marked for install
				else if (component.getInstall()) {
					// If no existing unit, add new unit
					if (installedUnit == null) {
						toAdd.add(installUnit);
					}
					// Else replace existing unit
					else if (!installedUnit.getVersion().equals(installUnit.getVersion())) {
						toAdd.add(installUnit);
						toRemove.add(installedUnit);
					}
				}
				// Component marked for uninstall, remove older unit if present
				else if (installedUnit != null) {
					toRemove.add(installedUnit);
				}
			}
		}
		
		// Remove all units in profile for upgrade
		if (mode.isUpgrade()) {
			String profileId = Installer.getDefault().getInstallManager().getInstallDescription().getProfileName();
			IProfile profile = getProfile(profileId);
			if (profile != null) {
				IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUAnyQuery(), null);
				Iterator<IInstallableUnit> i = query.iterator();
				while (i.hasNext()) {
					toRemove.add(i.next());
				}
			}
		}
	}
	
	/**
	 * Returns the context for provisioning.
	 * 
	 * @return Provisioning context
	 */
	public ProvisioningContext getProvisioningContext() {
		ProvisioningContext context = new ProvisioningContext(getAgent());
		
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		// If initial installation or upgrade, include only installer repositories.
		if (!mode.isUpdate()) {
			context.setArtifactRepositories(getRepositoryLocations());
			context.setMetadataRepositories(getRepositoryLocations());
		}
		// If update then include installed repositories in addition to installer repositories.
		// Include only local installed repositories (much improved performance).
		else {
			URI[] installerLocations = getRepositoryLocations();

			// Include all repositories or only local
			int flags = Installer.getDefault().getInstallManager().getInstallDescription().getIncludeAllRepositories() ?
					IRepositoryManager.REPOSITORIES_ALL : IRepositoryManager.REPOSITORIES_LOCAL;
			
			// Installer and installed meta-data repositories
			IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager)getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
			URI[] installedMetadataLocations = metadataManager.getKnownRepositories(flags);
			URI[] metadataLocations = new URI[installerLocations.length + installedMetadataLocations.length];
			System.arraycopy(installerLocations, 0, metadataLocations, 0, installerLocations.length);
			System.arraycopy(installedMetadataLocations, 0, metadataLocations, installerLocations.length, installedMetadataLocations.length);
			context.setMetadataRepositories(metadataLocations);

			// Installer and installed artifact repositories
			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			URI[] installedArtifactLocations = artifactManager.getKnownRepositories(flags);
			URI[] artifactLocations = new URI[installerLocations.length + installedArtifactLocations.length];
			System.arraycopy(installerLocations, 0, artifactLocations, 0, installerLocations.length);
			System.arraycopy(installedArtifactLocations, 0, artifactLocations, installerLocations.length, installedArtifactLocations.length);
			context.setArtifactRepositories(artifactLocations);
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
				return new InstallPlan(Status.OK_STATUS, 0);
			}

			// Return cached install plan if available.
			IInstallableUnit[] toAdd = unitsToAdd.toArray(new IInstallableUnit[unitsToAdd.size()]);
			IInstallableUnit[] toRemove = unitsToRemove.toArray(new IInstallableUnit[unitsToRemove.size()]);
			final String hash = rootsHash(toAdd, toRemove);
			installPlan = planCache.get(hash);
			if (installPlan != null) {
				return installPlan;
			}

			mon.worked(100);
			if (mon.isCanceled())
				return null;
			
			if (getAgent() == null)
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
			if (plan.getInstallerPlan() != null) {
				ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
				engine.perform(plan.getInstallerPlan(), sizingPhaseSet, mon.newChild(100));
				installPlanSize = sizingPhaseSet.getDiskSize();
			} else {
				mon.worked(100);
			}

			if (mon.isCanceled())
				return null;

			ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
			engine.perform(plan, sizingPhaseSet, mon.newChild(100));
			long installSize = installPlanSize + sizingPhaseSet.getDiskSize();

			// Construct the install plan.  This code will wait for the uninstaller
			// size if it is still being computed.
			installPlan = new InstallPlan(status, installSize + getUninstallerSize());
			planCache.put(hash, installPlan);
		} catch (Exception e) {
			monitor.setCanceled(true);
			Installer.log(e);
		}
		
		return installPlan;
	}
	
	/**
	 * Returns if the repositories are currently being loaded.
	 * 
	 * @return <code>true</code> if repositories are loading.
	 */
	public boolean isLoading() {
		return (loads > 0);
	}

	/**
	 * Waits for repositories to be loaded.  If the repositories are already
	 * loaded, this method returns immediately.
	 */
	public void waitForLoad() {
		waitForLoadJobs(false);
	}
	
	/**
	 * Waits for all repository load jobs to complete or be canceled.
	 * If there are no repository load jobs running, this method returns
	 * immediately.
	 * 
	 * @param cancel
	 */
	private void waitForLoadJobs(boolean cancel) {
		if (isLoading()) {
			Job[] repositoryJobs = Job.getJobManager().find(REPOSITORY_JOB_FAMILY);
			for (Job repositoryJob : repositoryJobs) {
				try {
					if (cancel) {
						repositoryJob.cancel();
					}
					repositoryJob.join();
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}
	}

	/**
	 * Returns an install component.
	 * If repositories are currently being loaded, this method will not return
	 * until the load has completed.
	 * 
	 * @param id Identifier for the install component.
	 * @return Install component or <code>null</code>
	 */
	private IInstallComponent getInstallComponent(String id) {
		IInstallComponent foundComponent = null;
		
		for (IInstallComponent component : getInstallComponents()) {
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
	private void fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus status) {
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
	 * Fires a repository loaded notification.
	 * 
	 * @param location Repository location
	 * @param components Install components
	 */
	private void fireRepositoryLoaded(URI location, IInstallComponent[] components) {
		Object[] listeners = repositoryListeners.getListeners();
		for (Object listener : listeners) {
			try {
				((IInstallRepositoryListener)listener).repositoryLoaded(location, components);
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
	private void fireRepositoryError(URI location, String errorMessage) {
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
	 * Generate a string hash from the specified list of roots.
	 * @param rootsToAdd Roots to add
	 * @param rootsToRemove Roots to remove
	 * @return hash of root list
	 */
	private String rootsHash(IInstallableUnit[] rootsToAdd, IInstallableUnit[] rootsToRemove) {
		List<String> rootIds = new ArrayList<>();
		for (int i = 0; i < rootsToAdd.length; i++) {
			rootIds.add("+" + rootsToAdd[i].getId());
		}
		for (int i = 0; i < rootsToRemove.length; i++) {
			rootIds.add("-" + rootsToRemove[i].getId());
		}
		
		Collections.sort(rootIds);
		String hash = "";
		
		for (String id : rootIds) {
			hash += id;
		}
		
		return hash;
	}
	
	/**
	 * Base job to load repositories.
	 */
	private abstract class RepositoryLoadJob extends Job {
		/** Repository locations */
		private URI[] repositories;
		
		/**
		 * Constructor
		 * 
		 * @param repositories Locations of repositories to load
		 */
		protected RepositoryLoadJob(URI[] repositories) {
			super("Repository Load Job");
			this.repositories = repositories;
		}

		/**
		 * Returns the repository locations.
		 * 
		 * @return Repository locations
		 */
		public URI[] getRepositories() {
			return repositories;
		}
		
		@Override
		public boolean belongsTo(Object family) {
			return (REPOSITORY_JOB_FAMILY == family);
		}

		/**
		 * Called to get any components contained in a loaded repository.
		 * 
		 * @param repository Repository
		 * @return Components Components found in the repository
		 * @throws CoreException on failure
		 */
		protected abstract IInstallComponent[] getComponents(IMetadataRepository repository) throws CoreException;

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
		 * Sets up attributes for all loaded components.
		 * Required components will be set to install.
		 * If at least one component has been installed (update) then optional
		 * components will be set to install only if they are already installed.
		 * If no components have been installed (new install) then optional 
		 * components will be set to install if they are default.
		 */
		private void setupComponents() {
			// Optional roots
			IVersionedId[] requiredRoots = Installer.getDefault().getInstallManager().getInstallDescription().getRequiredRoots();
			// Default roots
			IVersionedId[] defaultRoots = Installer.getDefault().getInstallManager().getInstallDescription().getDefaultOptionalRoots();

			// This flag will be set if no component have been installed
			boolean newInstall = true;
			
			// Profile query if updating an existing installation
			IQueryResult<IInstallableUnit> installedResult = null;
			if (Installer.getDefault().getInstallManager().getInstallMode().isUpdate()) {
				IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();
				String profileId = Installer.getDefault().getInstallManager().getInstallDescription().getProfileName();
				IProfile profile = getProfile(profileId);
				if (profile != null) {
					installedResult = profile.query(query, null);
				}
			}
			
			// Loop through loaded components
			for (IInstallComponent component : getInstallComponents()) {
				InstallComponent comp = (InstallComponent)component;
				
				// Set required components
				boolean isOptional = true;
				if (requiredRoots != null) {
					for (IVersionedId requiredRoot : requiredRoots) {
						if (component.getInstallUnit().getId().equals(requiredRoot.getId())) {
							isOptional = false;
							break;
						}
					}
				}
				comp.setOptional(isOptional);
				
				// Set default components
				if (isOptional) {
					boolean isDefault = false;
					if (defaultRoots != null) {
						for (IVersionedId defaultRoot : defaultRoots) {
							if (component.getInstallUnit().getId().equals(defaultRoot.getId())) {
								isDefault = true;
								break;
							}
						}
					}
					comp.setDefault(isDefault);
				}
				// Required component
				else {
					comp.setDefault(true);
				}
				
				// Set installed state
				if (installedResult != null) {
					Iterator<IInstallableUnit> iter = installedResult.iterator();
					while (iter.hasNext()) {
						IInstallableUnit existingUnit = iter.next();
						if (existingUnit.getId().equals(comp.getInstallUnit().getId())) {
							comp.setInstalledUnit(existingUnit);
							newInstall = false;
							break;
						}
					}
				}
				
				// Initialize install state
				if (installedResult != null) {
					// For optional components, we default its install state
					// to false and only set it true if the IU is found installed.
					// For required components we default its install state to
					// true.  It will not be reinstalled if the same version is
					// already installed.
					comp.setInstall(comp.isOptional() ? false : true);
					Iterator<IInstallableUnit> iter = installedResult.iterator();
					while (iter.hasNext()) {
						IInstallableUnit existingUnit = iter.next();
						if (existingUnit.getId().equals(comp.getInstallUnit().getId())) {
							comp.setInstalledUnit(existingUnit);
							comp.setInstall(true);
							break;
						}
					}
				}
				// Otherwise set install state to default
				else {
					comp.setInstall(comp.isDefault());
				}
			}
			
			// Set install state for all components
			for (IInstallComponent component : components) {
				// Optional component 
				if (component.isOptional()) {
					// If new install, set it to install if default
					// If update, set it to install if already installed
					component.setInstall(newInstall ? component.isDefault() : (component.getInstalledUnit() != null));
				}
				// Required component
				else {
					// Always set it to install
					component.setInstall(true);
				}
			}
			
			// Sort components
			sortComponents();
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				SubMonitor subprogress = SubMonitor.convert(monitor, InstallMessages.LoadingRepositories, getRepositories().length * 2);
				
				// Load the repositories
				for (URI repositoryLocation : getRepositories()) {
					try {
						
						// Load the meta-data repository
						monitor.setTaskName(NLS.bind(InstallMessages.LoadingMetadataRepository0, repositoryLocation.toString()));
						getMetadataRepositoryManager().addRepository(repositoryLocation);
						IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(repositoryLocation, subprogress.newChild(1));
						
						// Load the artifact repository
						monitor.setTaskName(NLS.bind(InstallMessages.LoadingArtifactRepository0, repositoryLocation.toString()));
						getArtifactRepositoryManager().addRepository(repositoryLocation);
						getArtifactRepositoryManager().loadRepository(repositoryLocation, subprogress.newChild(1));

						repositoryLocations.add(repositoryLocation);
						
						// Add components from the repository
						IInstallComponent[] repositoryComponents = getComponents(repository);
						for (IInstallComponent repositoryComponent : repositoryComponents) {
							// If a component for the IU has not already been added
							if (getInstallComponent(repositoryComponent.getInstallUnit().getId()) == null) {
								// Add component
								components.add(repositoryComponent);
							}
						}
						setupComponents();
						// Fire repository loaded notification
						fireRepositoryLoaded(repositoryLocation, repositoryComponents);
					}
					catch (Exception e) {
						Installer.log(e);
						getMetadataRepositoryManager().removeRepository(repositoryLocation);
						getArtifactRepositoryManager().removeRepository(repositoryLocation);
						fireRepositoryError(repositoryLocation, e.getLocalizedMessage());
					}
				}

				// If no more loads are in progress, send notification
				if (--loads <= 0) {
					// Pre-calculate the install size for complete set of
					// of repository components.
					IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
					if (!mode.isUpdate() && !mode.isUpdate())
						initializeUninstallerSize();

					loads = 0;
					fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingCompleted);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
			
			return Status.OK_STATUS;
		}
	}

	/**
	 * Job to load components for a repository.
	 */
	private class LoadComponentsJob extends RepositoryLoadJob {
		/**
		 * Constructor
		 * 
		 * @param repositories Repositories
		 * @param requiredRoots Required roots
		 * @param optionalRoots Optional roots
		 */
		public LoadComponentsJob(URI[] repositories) {
			super(repositories);
		}

		@Override
		protected IInstallComponent[] getComponents(IMetadataRepository repository) throws CoreException {
			// Add components for found install roots
			ArrayList<InstallComponent> comps = new ArrayList<InstallComponent>();

			// Add required roots
			IVersionedId[] requiredRoots = Installer.getDefault().getInstallManager().getInstallDescription().getRequiredRoots();
			if (requiredRoots != null) {
				for (IVersionedId id : requiredRoots) {
					// Find install unit
					IInstallableUnit unit = InstallUtils.findUnit(repository, id);
					if (unit != null) {
						// Add component
						InstallComponent component = new InstallComponent(unit);
						comps.add(component);
					}
				}
			}
			
			// Add optional roots
			IVersionedId[] optionalRoots = Installer.getDefault().getInstallManager().getInstallDescription().getOptionalRoots();
			if (optionalRoots != null) {
				for (IVersionedId id : optionalRoots) {
					// Find install unit
					IInstallableUnit unit = InstallUtils.findUnit(repository, id);
					if (unit != null) {
						// Add component
						InstallComponent component = new InstallComponent(unit);
						comps.add(component);
					}
				}
			}
			
			return comps.toArray(new IInstallComponent[comps.size()]);
		}
	}

	/**
	 * Job to load all components for repositories.
	 */
	private class LoadAllComponentsJob extends RepositoryLoadJob {
		/** <code>true</code> of components are optional */
		private boolean optional = false;
		
		/**
		 * Constructor
		 * 
		 * @param repositories Repositories
		 * @param optional <code>true</code> if components are optional,
		 * <code>false</code> if components are required
		 */
		public LoadAllComponentsJob(URI[] repositories, boolean optional) {
			super(repositories);
			this.optional = optional;
		}

		/**
		 * Returns if components are optional.
		 * 
		 * @return <code>true</code> if components are optional
		 */
		public boolean isOptional() {
			return optional;
		}

		@Override
		protected IInstallComponent[] getComponents(IMetadataRepository repository) throws CoreException {
			ArrayList<InstallComponent> comps = new ArrayList<InstallComponent>();

			// Get group IU's
			IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();
			IQueryResult<IInstallableUnit> queryResult = repository.query(query, null);
			Iterator<IInstallableUnit> iter = queryResult.iterator();
			while (iter.hasNext()) {
				IInstallableUnit unit = (IInstallableUnit)iter.next();
				
				// Skip category IU's
				String category = unit.getProperty("org.eclipse.equinox.p2.type.category");
				if ((category != null) && category.equals("true"))
					continue;
				
				// Add component
				InstallComponent component = new InstallComponent(unit);
				component.setOptional(isOptional());
				comps.add(component);
			}
			
			return comps.toArray(new IInstallComponent[comps.size()]);
		}
	}
}
