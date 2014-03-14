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
package com.codesourcery.internal.installer.actions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.UpdateSite;
import com.codesourcery.installer.actions.AbstractInstallAction;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;
import com.codesourcery.internal.installer.ProvisioningProgressMonitor;
import com.codesourcery.internal.installer.RepositoryManager;

/**
 * Action to install/uninstall p2 installable units.
 */
@SuppressWarnings("restriction") // Accesses internal p2 API's
public class InstallIUAction extends AbstractInstallAction {
	private static final String ID = "com.codesourcery.installer.installIUAction";
	private static final String ELEMENT_ROOTS = "roots";
	private static final String ELEMENT_ROOT = "root";
	private static final String ATTRIBUTE_INSTALL_LOCATION = "installLocation";
	private static final String ATTRIBUTE_PROFILE = "profile";
	private static final String ATTRIBUTE_ID = "id";
	private static final String ATTRIBUTE_VERSION = "version";
	
	/** Roots to install */
	private IVersionedId[] rootsToInstall;
	/** Roots to uninstall */
	private IVersionedId[] rootsToUninstall;
	/** <code>true</code> if first install */
	private boolean isFirstInstall = false;
	/** p2 director */
	private IDirector director;
	/** Metadata repository manager */
	private IMetadataRepositoryManager metadataRepoMan;
	/** Profile registry */
	private IProfileRegistry profileRegistry;
	/** p2 install location */
	private IPath installLocation;
	/** Shared bundle location */
	private IPath bundleLocation;
	/** p2 agent location */
	private IPath agentLocation;
	/** Profile for operation */
	private String profileName;
	/** Update sites */
	private UpdateSite[] updateSites;
	/** Profile properties */
	private Map<String, String> profileProperties;
	/** Progress regular expression find patterns */
	private String[] progressFindPatterns;
	/** Progress regular expression replace patterns */
	private String[] progressReplacePatterns;
	
	/**
	 * Required no argument constructor.
	 */
	public InstallIUAction() {
		super(ID);
	}
	
	/**
	 * Constructor for install operation
	 * 
	 * @param installLocation Install location
	 * @param profileName Profile name
	 * @param profileProperties Profile properties
	 * @param productName Product name
	 * @param updateSites Update sites or <code>null</code>
	 * @param rootsToInstall Roots to install or <code>null</code> to remove
	 * the entire profile (and all roots).
	 * @param rootsToUninstall Roots to uninstall or <code>null</code>
	 * @param progressFindPatterns P2 progress regular expression find patterns
	 * @param progressReplacePatterns P2 progress regular expression replacement patterns
	 */
	public InstallIUAction(IPath installLocation, String profileName, Map<String, String> profileProperties,
			UpdateSite[] updateSites, IVersionedId[] rootsToInstall, IVersionedId[] rootsToUninstall, 
			String[] progressFindPatterns, String[] progressReplacePatterns) {
		super(ID);
		
		this.installLocation = installLocation;
		this.bundleLocation = installLocation;
		this.agentLocation = installLocation.append(IInstallConstants.P2_DIRECTORY);
		this.profileName = profileName;
		this.updateSites = updateSites;
		this.rootsToInstall = rootsToInstall;
		this.rootsToUninstall = rootsToUninstall;
		this.profileProperties = profileProperties;
		this.progressFindPatterns = progressFindPatterns;
		this.progressReplacePatterns = progressReplacePatterns;
	}

	/**
	 * Sets the install location.
	 * 
	 * @param installLocation Install location
	 */
	private void setInstallLocation(IPath installLocation) {
		this.installLocation = installLocation;
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
	 * Returns the bundle location.
	 * 
	 * @return bundle location
	 */
	public IPath getBundleLocation() {
		return bundleLocation;
	}
	
	/**
	 * Returns the p2 agent location.
	 * 
	 * @return Agent location
	 */
	public IPath getAgentLocation() {
		return agentLocation;
	}
	
	/**
	 * Returns the profile name.
	 * 
	 * @return Profile name
	 */
	public String getProfileName() {
		return profileName;
	}
	
	/**
	 * Returns the roots to install.
	 * 
	 * @return Roots or <code>null</code>
	 */
	public IVersionedId[] getRootsToInstall() {
		return rootsToInstall;
	}
	
	/**
	 * Returns roots to uninstall.
	 * 
	 * @return Roots or <code>null</code>
	 */
	public IVersionedId[] getRootsToUninstall() {
		return rootsToUninstall;
	}
	
	/**
	 * Returns the update sites.
	 * 
	 * @return Update sites or <code>null</code>
	 */
	public UpdateSite[] getUpdateSites() {
		return updateSites;
	}
	
	/**
	 * Returns the profile properties.
	 * 
	 * @return Profile properties
	 */
	public Map<String, String> getProfileProperties() {
		return profileProperties;
	}
	
	/**
	 * Returns if this is the first
	 * installation.
	 * 
	 * @return <code>true</code> if first install
	 */
	public boolean isFirstInstall() {
		return isFirstInstall;
	}

	/**
	 * Initializes agent services.
	 * 
	 * @param agent Agent
	 */
	private void initServices(IProvisioningAgent agent) {
		director = (IDirector)agent.getService(IDirector.SERVICE_NAME);
		metadataRepoMan = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		profileRegistry = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
	}
	
	@Override
	public int getProgressWeight() {
		return DEFAULT_PROGRESS_WEIGHT * 10;
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor pm) throws CoreException {
		try {
			SubMonitor monitor = SubMonitor.convert(pm, InstallMessages.Op_Preparing, 100);

			// Initialize services
			initServices(agent);
			
			// Create or get the profile
			IProfile profile = profileRegistry.getProfile(getProfileName());
			isFirstInstall = (profile == null);
			profile = RepositoryManager.getDefault().createProfile(getProfileName());
			// Get the planner
			IPlanner planner = (IPlanner)agent.getService(IPlanner.SERVICE_NAME);

			IStatus status = null;
			
			// Install action
			if (mode.isInstall()) {
				// Get the installable units to install
				ArrayList<IInstallableUnit> toInstall = new ArrayList<IInstallableUnit>();
				for (IVersionedId id : getRootsToInstall()) {
					IInstallableUnit iu = InstallUtils.findUnitAll(metadataRepoMan, id);
					if (iu != null) {
						monitor.setTaskName(NLS.bind(InstallMessages.AddingInstallUnit0, iu.getId() + " " + iu.getVersion().toString()));
						toInstall.add(iu);
					}
				}
	
				// Set up provisioning context for local repositories
				// Without a provisioning context, online repositories will
				// be included in the operation.
				ProvisioningContext context = new ProvisioningContext(agent);
				URI[] repositories = RepositoryManager.getDefault().getRepositoryLocations();
				context.setArtifactRepositories(repositories);
				context.setMetadataRepositories(repositories);

				IProfileChangeRequest request = planner.createChangeRequest(profile);
				// First install
				if (isFirstInstall) {
					monitor.setTaskName(NLS.bind(InstallMessages.Op_Installing0, getProfileName()));
					
					// Install
					request.addAll(toInstall);
					status = director.provision(request, context, new ProvisioningProgressMonitor(monitor.newChild(90), 
							progressFindPatterns, progressReplacePatterns));

					// Add update sites
					UpdateSite[] updateSites = getUpdateSites();
					if (updateSites != null) {
						for (UpdateSite updateSite : updateSites) {
							AddRepositoryAction action = new AddRepositoryAction();
							HashMap<String, Object> params = new HashMap<String, Object>();
							params.put("agent", agent);
							params.put(ActionConstants.PARM_PROFILE, profile);
							params.put(ActionConstants.PARM_REPOSITORY_LOCATION, updateSite.getLocation());
							params.put(ActionConstants.PARM_REPOSITORY_TYPE, Integer.toString(IRepository.TYPE_METADATA));
							if (updateSite.getName() != null)
								params.put(ActionConstants.PARM_REPOSITORY_NICKNAME, updateSite.getName());
							action.execute(params);
							params.put(ActionConstants.PARM_REPOSITORY_TYPE, Integer.toString(IRepository.TYPE_ARTIFACT));
							action.execute(params);
						}
					}
				}
				// Upgrade
				else {
					monitor.setTaskName(NLS.bind(InstallMessages.Op_Updating0, product.getName()));

					// If upgrading an existing product, remove existing roots
					if (mode.isUpgrade()) {
						IVersionedId[] rootsToUninstall = getRootsToUninstall();
						// Remove selected roots
						if (rootsToUninstall != null)
						{
							if (rootsToUninstall != null) {
								for (IVersionedId rootToUninstall : rootsToUninstall) {
									monitor.setTaskName(NLS.bind(InstallMessages.RemovingInstallUnit0, rootToUninstall.getId() + " " + rootToUninstall.getVersion().toString()));
								}
								
								IInstallableUnit[] unitsToRemove = InstallUtils.getUnitsFromProfile(profile, rootsToUninstall);
								request.removeAll(Arrays.asList(unitsToRemove));
							}
						}
						// Remove entire profile
						else {
							IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUAnyQuery(), null);
							Iterator<IInstallableUnit> i = query.iterator();
							ArrayList<IInstallableUnit> removes = new ArrayList<IInstallableUnit>();
							while (i.hasNext()) {
								removes.add(i.next());
							}
							request.removeAll(removes);
						}
					}
					
					// Add new install units
					request.addAll(toInstall);
					
					
					// Provision
					status = director.provision(request, context, new ProvisioningProgressMonitor(monitor.newChild(90), 
							progressFindPatterns, progressReplacePatterns));
				}
			}
			// Uninstall action
			else {
				// This action does not need to run for a root uninstallation
				// because the entire product directory is being removed.
				if (!mode.isRootUninstall()) {
					// Get installable units to uninstall
					ArrayList<IInstallableUnit> toUninstall = new ArrayList<IInstallableUnit>();
					for (IVersionedId id : getRootsToInstall()) {
						IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(id);
						IQueryResult<IInstallableUnit> roots = profile.query(query, new NullProgressMonitor());
						Iterator<IInstallableUnit> iter = roots.iterator();
						while (iter.hasNext()) {
							IInstallableUnit iu = iter.next();
	
							monitor.setTaskName(NLS.bind(InstallMessages.RemovingInstallUnit0, iu.getId() + " " + iu.getVersion().toString()));
							toUninstall.add(iu);
						}
					}
	
					IProfileChangeRequest request = planner.createChangeRequest(profile);
					request.removeAll(toUninstall);
					status = director.provision(request, null, new ProvisioningProgressMonitor(monitor.newChild(100), 
							progressFindPatterns, progressReplacePatterns));
					
					// Remove the profile
					if (status.isOK() && (rootsToUninstall == null)) {
						removeProfile(profile.getProfileId());
					}
				}
			}
			
			if ((status != null) && (status.getSeverity() == IStatus.ERROR))
				throw new CoreException(status);
		}
		catch (Exception e) {
			if (e instanceof CoreException)
				throw e;
			else
				Installer.fail(InstallMessages.Error_FailedToInstall, e);
		}
	}

	@Override
	public void save(Document document, Element node) throws CoreException {
		Element element = document.createElement(ELEMENT_ROOTS);
		node.appendChild(element);
		element.setAttribute(ATTRIBUTE_INSTALL_LOCATION, getInstallLocation().toPortableString());
		element.setAttribute(ATTRIBUTE_PROFILE, getProfileName());
		for (IVersionedId root : rootsToInstall) {
			Element rootElement = document.createElement(ELEMENT_ROOT);
			element.appendChild(rootElement);
			rootElement.setAttribute(ATTRIBUTE_ID, root.getId());
			String version = root.getVersion().getOriginal();
			if (version != null) {
				rootElement.setAttribute(ATTRIBUTE_VERSION, version);
			}
		}
	}

	@Override
	public void load(Element element) throws CoreException {
		ArrayList<IVersionedId> docRoots = new ArrayList<IVersionedId>();
		
		NodeList rootsNodes = element.getElementsByTagName(ELEMENT_ROOTS);
		for (int rootsIndex = 0; rootsIndex < rootsNodes.getLength(); rootsIndex++) {
			Node rootsNode = rootsNodes.item(rootsIndex);
			if (rootsNode.getNodeType() == Node.ELEMENT_NODE) {
				Element rootsElement = (Element)rootsNode;
				profileName = rootsElement.getAttribute(ATTRIBUTE_PROFILE);
				String location = rootsElement.getAttribute(ATTRIBUTE_INSTALL_LOCATION);
				setInstallLocation(Path.fromPortableString(location));
				NodeList rootNodes = rootsElement.getElementsByTagName(ELEMENT_ROOT);
				for (int rootIndex = 0; rootIndex < rootNodes.getLength(); rootIndex ++) {
					Node rootNode = rootNodes.item(rootIndex);
					if (rootNode.getNodeType() == Node.ELEMENT_NODE) {
						Element rootElement = (Element)rootNode;
						String id = rootElement.getAttribute(ATTRIBUTE_ID);
						String version = rootElement.getAttribute(ATTRIBUTE_VERSION);
						VersionedId root;
						if ((version != null) && !version.trim().isEmpty()) {
							root = new VersionedId(id, version);
						}
						else {
							root = new VersionedId(id, Version.emptyVersion);
						}
						docRoots.add(root);
					}
				}
			}
		}
		
		rootsToInstall = docRoots.toArray(new IVersionedId[docRoots.size()]);
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
	 * Removes a profile.
	 * 
	 * @param profileId Profile identifier
	 */
	private void removeProfile(String profileId) {
		profileRegistry.removeProfile(profileId);
	}

	@Override
	public boolean uninstallOnUpgrade() {
		// Removeal of installable units on an upgrade will be handled
		// during install.
		return false;
	}
}
