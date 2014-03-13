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
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.codesourcery.installer.IInstallDescription;
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
	/** ID to reference the temporary profile used for size calculation*/
	private static final String SIZING_PROFILE_ID = "sizing_profile";
	/** Default instance */
	private static RepositoryManager instance = new RepositoryManager();
	/** Roots to install */
	private IVersionedId[] roots;
	/** Optional roots */
	private IVersionedId[] optionalRoots;
	/** Install components */
	private ArrayList<IInstallComponent> components = new ArrayList<IInstallComponent>();
	/** Provisioning agent */
	private IProvisioningAgent agent;
	/** Meta-data repository manager */
	private IMetadataRepositoryManager metadataRepoMan;
	/** Artifact repository manager */
	private IArtifactRepositoryManager artifactRepoMan;
	/** Listeners to component changes */
	private ListenerList componentsChangedListeners = new ListenerList();
	/** Number of loads in progress */
	private int loads = 0;
	/** Repositories */
	private ArrayList<URI> repositoryLocations = new ArrayList<URI>();
	/** Current thread for size calculation */
	protected SizeCalculationThread calculationThread;
	/** Cache to store computed sizes */
	protected Map<String, Long> sizesCache;
	/** Size of uninstall files */
	protected long uninstallBytes = -1;
	/** Install location */
	private IPath installLocation;

	/**
	 * Constructor
	 */
	private RepositoryManager() {
		// Synchronize size cache
		sizesCache = Collections.synchronizedMap(new HashMap<String, Long>());
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

				IInstallDescription installDescription = Installer.getDefault().getInstallDescription();
				// Load repositories
				if (installDescription != null) {
					loadRepositories(
							installDescription.getMetadataRepositories(), 
							installDescription.getRequiredRoots(),
							installDescription.getOptionalRoots()
							);
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
			agent.stop();
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
		IProfileRegistry profileRegistry = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile profile = profileRegistry.getProfile(profileId);
		// Note: On uninstall, the profile will always be available
		if (profile == null) {
			Map<String, String> properties = new HashMap<String, String>();
			properties.put(IProfile.PROP_INSTALL_FOLDER, getInstallLocation().toString());
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Installer.getDefault().getContext(), EnvironmentInfo.class.getName());
			String env = "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			properties.put(IProfile.PROP_ENVIRONMENTS, env);
			properties.put(IProfile.PROP_NAME, profileId);
			if (Installer.getDefault().getInstallDescription().getProfileProperties() != null)
				properties.putAll(Installer.getDefault().getInstallDescription().getProfileProperties());
			properties.put(IProfile.PROP_CACHE, getAgentLocation().toOSString());
			profile = profileRegistry.addProfile(profileId, properties);
		}
		
		return profile;
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

		// Cancel and wait for sizing thread to complete
		if (calculationThread != null) {
			calculationThread.cancel();
			try {
				calculationThread.join();
			} catch (InterruptedException e) {
				// Ignore
			}
		}
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
	 * @param roots Install roots
	 * @param optionalRoots Roots that are optional
	 */
	public void loadRepositories(URI[] repositoryLocations, IVersionedId[] roots, IVersionedId[] optionalRoots) {
		// Fire loading status
		if (++loads == 1)
			fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus.loadingStarted);

		// Start load job
		LoadComponentsJob job = new LoadComponentsJob(repositoryLocations, roots, optionalRoots);
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
	 * Returns all roots to install.
	 * 
	 * @return Install roots
	 */
	public IVersionedId[] getRoots() {
		return roots;
	}
	
	/**
	 * Returns optional roots.
	 * 
	 * @return Optional roots
	 */
	public IVersionedId[] getOptionalRoots() {
		return optionalRoots;
	}
	
	/**
	 * Adds a listener to component changes.
	 * 
	 * @param listener Listener to add
	 */
	public void addRepositoryListener(IInstallRepositoryListener listener) {
		componentsChangedListeners.add(listener);
	}
	
	/**
	 * Removes a listener from component changes.
	 * 
	 * @param listener Listener to remove
	 */
	public void removeRepositoryListener(IInstallRepositoryListener listener) {
		componentsChangedListeners.remove(listener);
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
	 * Returns whether a component should be installed by default.
	 * 
	 * @param component Component
	 * @return <code>true</code> if component should be installed by default.
	 */
	public boolean isDefaultComponent(IInstallComponent component) {
		boolean isDefault = false;

		// Optional component that is in the list of default optional
		// roots to be installed will be selected for install
		if (component.isOptional()) {
			IInstallDescription description = Installer.getDefault().getInstallDescription();
			if (description != null) {
				IVersionedId[] optionalRootsDefault = description.getDefaultOptionalRoots();
				if (optionalRootsDefault != null) {
					for (IVersionedId optionalRoot : optionalRootsDefault) {
						if (optionalRoot.getId().equals(component.getInstallUnit().getId())) {
							isDefault = true;
							break;
						}
					}
				}
			}
		}
		// Required component is always installed
		else {
			isDefault = true;
		}
		
		return isDefault;
	}

	/**
	 * Fires a repository status notification.
	 * 
	 * @param status Status
	 */
	private void fireRepositoryStatus(IInstallRepositoryListener.RepositoryStatus status) {
		Object[] listeners = componentsChangedListeners.getListeners();
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
		Object[] listeners = componentsChangedListeners.getListeners();
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
		Object[] listeners = componentsChangedListeners.getListeners();
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
	 * Calculate size for all components.
	 * @param monitor
	 */
	public void startSizeCalculation(ISizeCalculationMonitor monitor) {
		startSizeCalculation(getInstallComponents(), monitor);
	}
	
	/**
	 * Calculate install size for the specified list of roots. Result is passed via the 
	 * ISizeCalculationMonitor.done(long size) call. 
	 * @param roots
	 * @param monitor
	 */
	// Synchronize for thread-safe access to calculationThread.
	synchronized public void startSizeCalculation(IInstallComponent[] roots, ISizeCalculationMonitor monitor) {
		
		// Return cached value if present.
		final String hash = rootsHash(roots);
		Long size = sizesCache.get(hash);
		if (size != null) {
			monitor.done(size + uninstallBytes);
			return;
		}
		
		if (calculationThread != null) {
			calculationThread.cancel();
			calculationThread = null;
		}
		
		calculationThread = new SizeCalculationThread(roots, new SizeCalculationMonitorWrapper(monitor) {
			@Override
			public void done(long installSize) {
				// Cache size value for future calls.
				sizesCache.put(hash, installSize);
				// Uninstall size calculation may not be finished so don't cache result. 
				super.done(installSize + uninstallBytes);
			}
		});
		calculationThread.start();
	}
	
	/** Begin calculating size for uninstall area. Should be 
	 * called as early as possible and only once. */
	private void initializeSizeCalculation() {
		startUninstallSizeThread();
	}
	
	/* Spawn a thread to calculate the number of bytes required
	 * in the uninstall area. */
	protected void startUninstallSizeThread() {
		new Thread() {
			@Override
			public void run() {
				String [] uninstallFiles = Installer.getDefault().getInstallDescription().getUninstallFiles();
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
				uninstallBytes = totalSize;
			}
		}.start();
	}
	
	/**
	 * Generate a string hash from the specified list of roots.
	 * @param roots
	 * @return hash of root list
	 */
	private String rootsHash(IInstallComponent[] roots) {
		List<String> rootIds = new ArrayList<>();
		for (int i = 0; i < roots.length; i++) {
			rootIds.add(roots[i].getInstallUnit().getId());
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
		 * Sorts any added components from repositories according to the order
		 * specified in the install description.
		 */
		private void sortComponents() {
			Comparator<IInstallComponent> componentOrderComparator = new Comparator<IInstallComponent>() {
				@Override
				public int compare(IInstallComponent arg0, IInstallComponent arg1) {
					IVersionedId[] roots = Installer.getDefault().getInstallDescription().getRequiredRoots();
					for (IVersionedId root : roots) {
						if (root.getId().equals(arg0.getInstallUnit().getId()))
							return -1;
						if (root.getId().equals(arg1.getInstallUnit().getId()))
							return 1;
					}
					return 0;
				}
				
			};
			Collections.sort(components, componentOrderComparator);
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
								// If the component is not already marked for installation,
								// check if it is a default component that should be installed
								if (!repositoryComponent.getInstall()) {
									repositoryComponent.setInstall(isDefaultComponent(repositoryComponent));
								}
								// Add component
								components.add(repositoryComponent);
								sortComponents();
							}
						}
						// Fire repository loaded notification
						fireRepositoryLoaded(repositoryLocation, repositoryComponents);
					}
					catch (Exception e) {
						getMetadataRepositoryManager().removeRepository(repositoryLocation);
						getArtifactRepositoryManager().removeRepository(repositoryLocation);
						fireRepositoryError(repositoryLocation, e.getLocalizedMessage());
					}
				}

				// If no more loads are in progress, send notification
				if (--loads <= 0) {
					// Pre-calculate the install size for complete set of
					// of repository components.
					initializeSizeCalculation();

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
		/** Required roots */
		private IVersionedId[] requiredRoots;
		/** Optional roots */
		private IVersionedId[] optionalRoots;
		
		/**
		 * Constructor
		 * 
		 * @param repositories Repositories
		 * @param requiredRoots Required roots
		 * @param optionalRoots Optional roots
		 */
		public LoadComponentsJob(URI[] repositories, IVersionedId[] requiredRoots, 
				IVersionedId[] optionalRoots) {
			super(repositories);
			this.requiredRoots = requiredRoots;
			this.optionalRoots = optionalRoots;
		}

		/**
		 * Returns the required roots.
		 * 
		 * @return Required roots
		 */
		public IVersionedId[] getRequiredRoots() {
			return requiredRoots;
		}
		
		/**
		 * Returns the optional roots.
		 * 
		 * @return Optional roots
		 */
		public IVersionedId[] getOptionalRoots() {
			return optionalRoots;
		}

		@Override
		protected IInstallComponent[] getComponents(IMetadataRepository repository) throws CoreException {
			// Add components for found install roots
			ArrayList<InstallComponent> comps = new ArrayList<InstallComponent>();

			// Add required roots
			if (getRequiredRoots() != null) {
				for (IVersionedId id : getRequiredRoots()) {
					// Find install unit
					IInstallableUnit unit = InstallUtils.findUnit(repository, id);
					if (unit != null) {
						// Add component
						InstallComponent component = new InstallComponent(unit);
						component.setOptional(false);
						comps.add(component);
					}
				}
			}
			
			// Add optional roots
			if (getOptionalRoots() != null) {
				for (IVersionedId id : getOptionalRoots()) {
					// Find install unit
					IInstallableUnit unit = InstallUtils.findUnit(repository, id);
					if (unit != null) {
						// Add component
						InstallComponent component = new InstallComponent(unit);
						component.setOptional(true);
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
	
	class SizeCalculationThread extends Thread {
		private IInstallComponent[] roots;
		private ISizeCalculationMonitor monitor;
		
		public SizeCalculationThread(IInstallComponent[] roots, ISizeCalculationMonitor monitor) {
			this.roots = roots;
			this.monitor = monitor;
		}
		
		public void cancel() {
			monitor.setCanceled(true);
		}
		
		public boolean isCancelled() {
			return monitor.isCanceled();
		}
		
		@Override
		public void run() {
			try {
				
				SubMonitor mon = SubMonitor.convert(monitor, 600);
				
				long installSize;
				
				IProvisioningPlan plan = null;
				IEngine engine = null;
				
				// We need to synchronize on the repositories load check, plus all 
				// operations which modify the profile, so let's put everything in
				// one block.
				IProfile profile=null;
				synchronized (instance) {
					mon.worked(100);
					if (isCancelled())
						return;
					
					IProfileRegistry profileRegistry = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
					profile = profileRegistry.getProfile(SIZING_PROFILE_ID);
					if (profile == null) {
						profile = createProfile(SIZING_PROFILE_ID);
					}

					ProvisioningContext context = new ProvisioningContext(agent);
					context.setArtifactRepositories(getRepositoryLocations());
					context.setMetadataRepositories(getRepositoryLocations());
					IPlanner planner = (IPlanner)agent.getService(IPlanner.SERVICE_NAME);
					engine = (IEngine)agent.getService(IEngine.SERVICE_NAME);
					IProfileChangeRequest request = planner.createChangeRequest(profile);
					List<IInstallableUnit> toInstall = new ArrayList<IInstallableUnit>();
					for (IInstallComponent root : roots) {
						toInstall.add(root.getInstallUnit());
					}
					request.addAll(toInstall);

					plan = planner.getProvisioningPlan(request, context, mon.newChild(300));
				}

				// Problem computing plan
				IStatus status = plan.getStatus();
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

				if (isCancelled())
					return;

				long installPlanSize = 0;
				if (plan.getInstallerPlan() != null) {
					ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
					engine.perform(plan.getInstallerPlan(), sizingPhaseSet, mon.newChild(100));
					installPlanSize = sizingPhaseSet.getDiskSize();
				} else {
					mon.worked(100);
				}

				if (isCancelled())
					return;

				ISizingPhaseSet sizingPhaseSet = PhaseSetFactory.createSizingPhaseSet();
				engine.perform(plan, sizingPhaseSet, mon.newChild(100));
				installSize = installPlanSize + sizingPhaseSet.getDiskSize();
				//System.err.println("size = " + installSize );

				monitor.done(installSize);
			} catch (Exception e) {
				monitor.setCanceled(true);
				Installer.log(e);
			}
			finally {
				try {
					deleteProfile(SIZING_PROFILE_ID);
				} catch (ProvisionException e) {
					Installer.log(e);
				}
			}
		}
	}
}
