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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.UpdateSite;
import com.codesourcery.installer.actions.AbstractInstallAction;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.ProvisioningProgressMonitor;
import com.codesourcery.internal.installer.RepositoryManager;

/**
 * Action to install/uninstall p2 installable units.
 */
@SuppressWarnings("restriction") // Accesses internal p2 API's
public class InstallIUAction extends AbstractInstallAction {
	/** Action identifier */
	private static final String ID = "com.codesourcery.installer.installIUAction";
	/** Profile attribute */
	private static final String ATTRIBUTE_PROFILE = "profile";
	
	/** Units to install */
	private IInstallableUnit[] unitsToInstall;
	/** Units to uninstall */
	private IInstallableUnit[] unitsToUninstall;
	/** p2 director */
	private IDirector director;
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
	 * @param profileName Profile name
	 * @param profileProperties Profile properties
	 * @param productName Product name
	 * @param updateSites Update sites or <code>null</code>
	 * @param unitsToInstall Roots to install or <code>null</code>
	 * @param unitsToUninstall Roots to uninstall or <code>null</code>
	 * @param progressFindPatterns P2 progress regular expression find patterns
	 * @param progressReplacePatterns P2 progress regular expression replacement patterns
	 */
	public InstallIUAction(String profileName, Map<String, String> profileProperties,
			UpdateSite[] updateSites, IInstallableUnit[] unitsToInstall, IInstallableUnit[] unitsToUninstall, 
			String[] progressFindPatterns, String[] progressReplacePatterns) {
		super(ID);
		
		this.profileName = profileName;
		this.updateSites = updateSites;
		this.unitsToInstall = unitsToInstall;
		this.unitsToUninstall = unitsToUninstall;
		this.profileProperties = profileProperties;
		this.progressFindPatterns = progressFindPatterns;
		this.progressReplacePatterns = progressReplacePatterns;
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
	public IInstallableUnit[] getRootsToInstall() {
		return unitsToInstall;
	}
	
	/**
	 * Returns roots to uninstall.
	 * 
	 * @return Roots or <code>null</code>
	 */
	public IInstallableUnit[] getRootsToUninstall() {
		return unitsToUninstall;
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
	 * Initializes agent services.
	 * 
	 * @param agent Agent
	 */
	private void initServices(IProvisioningAgent agent) {
		director = (IDirector)agent.getService(IDirector.SERVICE_NAME);
	}
	
	@Override
	public int getProgressWeight() {
		return DEFAULT_PROGRESS_WEIGHT * 10;
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor monitor) throws CoreException {
		try {
			// Initialize services
			initServices(agent);
			
			// Get the planner
			IPlanner planner = (IPlanner)agent.getService(IPlanner.SERVICE_NAME);

			IStatus status = null;
			
			// Set task name
			/*
			String taskName;
			if (mode.isInstall()) {
				if (mode.isUpdate() | mode.isUpgrade())
					taskName = NLS.bind(InstallMessages.Op_Updating0, product.getName());
				else
					taskName = NLS.bind(InstallMessages.Op_Installing0, product.getName());
			}
			else {
				taskName = NLS.bind(InstallMessages.Op_Uninstalling0, product.getName());
			}
			monitor.setTaskName(taskName);
			*/
			
			// Units to add
			ArrayList<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
			if (getRootsToInstall() != null) {
				toAdd.addAll(Arrays.asList(getRootsToInstall()));
			}

			// Units to remove
			ArrayList<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>();
			if (getRootsToUninstall() != null) {
				toRemove.addAll(Arrays.asList(getRootsToUninstall()));
			}
			
			// Install
			if (mode.isInstall()) {
				// Create or get the profile
				IProfile profile = RepositoryManager.getDefault().getInstallProfile();
				// Provisioning context
				ProvisioningContext context = RepositoryManager.getDefault().getProvisioningContext();
				// Provision the request
				IProfileChangeRequest request = planner.createChangeRequest(profile);
				if (!toAdd.isEmpty())
					request.addAll(toAdd);
				if (!toRemove.isEmpty())
					request.removeAll(toRemove);
				ProvisioningProgressMonitor provisioningMonitor = new ProvisioningProgressMonitor(monitor, 
						progressFindPatterns, progressReplacePatterns);
				status = director.provision(request, context, provisioningMonitor);
	
				if (status.isOK()) {
					// Add units to product
					for (IInstallableUnit unit : toAdd) {
						product.addInstallUnit(unit);
					}
					// Remove units from product
					for (IInstallableUnit unit : toRemove) {
						product.removeInstallUnit(unit);
					}
					
					// Add any update sites
					if (!mode.isUpdate() && !mode.isUpgrade()) {
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
				}
			}
			// Uninstall
			else {
				// Get profile
				IProfile profile = RepositoryManager.getDefault().getProfile(getProfileName());
				
				// This action does not need to run for a root uninstallation
				// because the entire product directory is being removed.
				if (!mode.isRootUninstall()) {
					IVersionedId[] productUnits = product.getInstallUnits();
					
					// Get installable units to uninstall
					for (IVersionedId unit : productUnits) {
						IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(unit);
						IQueryResult<IInstallableUnit> roots = profile.query(query, new NullProgressMonitor());
						Iterator<IInstallableUnit> iter = roots.iterator();
						while (iter.hasNext()) {
							IInstallableUnit iu = iter.next();
	
							//monitor.setTaskName(NLS.bind(InstallMessages.RemovingInstallUnit0, iu.getId() + " " + iu.getVersion().toString()));
							toRemove.add(iu);
						}
					}
	
					IProfileChangeRequest request = planner.createChangeRequest(profile);
					request.removeAll(toRemove);
					status = director.provision(request, null, new ProvisioningProgressMonitor(monitor, 
							progressFindPatterns, progressReplacePatterns));
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
		finally {
			monitor.done();
		}
	}

	@Override
	public void save(Document document, Element node) throws CoreException {
		node.setAttribute(ATTRIBUTE_PROFILE, getProfileName());
	}

	@Override
	public void load(Element element) throws CoreException {
		profileName = element.getAttribute(ATTRIBUTE_PROFILE);
		
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

	@Override
	public boolean uninstallOnUpgrade() {
		// Removeal of installable units on an upgrade will be handled
		// during install.
		return false;
	}
}
