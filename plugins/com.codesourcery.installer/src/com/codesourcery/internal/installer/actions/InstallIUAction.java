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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.ActionConstants;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions.AddRepositoryAction;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
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
	/** Remove profile attribute */
	private static final String ATTRIBUTE_REMOVE_PROFILE = "removeProfile";
	
	/** Units to install */
	private IInstallableUnit[] unitsToInstall;
	/** Units to uninstall */
	private IInstallableUnit[] unitsToUninstall;
	/** Profile for operation */
	private String profileName;
	/** Update sites */
	private UpdateSite[] updateSites;
	/** Profile properties */
	private Map<String, String> profileProperties;
	/** <code>true</code> to remove profile on uninstallation */
	private boolean removeProfile;
	
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
	 */
	public InstallIUAction(String profileName, Map<String, String> profileProperties,
			UpdateSite[] updateSites, IInstallableUnit[] unitsToInstall, IInstallableUnit[] unitsToUninstall) {
		super(ID);
		
		this.profileName = profileName;
		this.updateSites = updateSites;
		this.unitsToInstall = unitsToInstall;
		this.unitsToUninstall = unitsToUninstall;
		this.profileProperties = profileProperties;
	}

	/**
	 * Sets the profile to be removed on uninstallation.
	 * 
	 * @param removeProfile <code>true</code> to remove profile
	 */
	public void setRemoveProfile(boolean removeProfile) {
		this.removeProfile = removeProfile;
	}
	
	/**
	 * Returns if the profile will be removed on uninstallation.
	 * 
	 * @return <code>true</code> to remove profile
	 */
	public boolean getRemoveProfile() {
		return removeProfile;
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
	
	@Override
	public int getProgressWeight() {
		final int SCALE = 100;
		int weight = 0;
		
		if (getRootsToInstall() != null) {
			weight += getRootsToInstall().length * DEFAULT_PROGRESS_WEIGHT * SCALE;
		}
		if (getRootsToUninstall() != null) {
			weight += getRootsToInstall().length * DEFAULT_PROGRESS_WEIGHT * SCALE;
		}
		if (weight == 0) {
			weight = DEFAULT_PROGRESS_WEIGHT *  SCALE;
		}
		
		return weight;
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor monitor) throws CoreException {
		try {
			ArrayList<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
			ArrayList<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>();
			
			IProfile profile = RepositoryManager.getDefault().getProfile(getProfileName());

			// Install
			if (mode.isInstall()) {
				// Units to add
				if (getRootsToInstall() != null) {
					toAdd.addAll(Arrays.asList(getRootsToInstall()));
				}

				// Units to remove
				if (getRootsToUninstall() != null) {
					toRemove.addAll(Arrays.asList(getRootsToUninstall()));
				}
				
				if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
					RepositoryManager.getDefault().provision(profile, toAdd, toRemove, true, monitor);
				}

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
							params.put(ActionConstants.PARM_PROFILE, getProfileName());
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
			// Uninstall
			else {
				String propRemoveDirs = product.getProperty(IInstallProduct.PROPERTY_REMOVE_DIRS);
				boolean removeDirs = ((propRemoveDirs == null) || Boolean.parseBoolean(propRemoveDirs));
				
				// Optimization: If there are no more products and the installation directory is 
				// being removed, there is no need to unprovision install units.
				if ((Installer.getDefault().getInstallManager().getInstallManifest().getProducts().length != 0) ||
						!removeDirs){
					// Remove all units in profile
					if (getRemoveProfile()) {
						IQueryResult<IInstallableUnit> query = profile.query(QueryUtil.createIUAnyQuery(), null);
						Iterator<IInstallableUnit> i = query.iterator();
						while (i.hasNext()) {
							toRemove.add(i.next());
						}
					}
					// Or remove only units that were installed
					else {
						IVersionedId[] productUnits = product.getInstallUnits();
						
						// Get installable units to uninstall
						for (IVersionedId unit : productUnits) {
							IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(unit);
							IQueryResult<IInstallableUnit> roots = profile.query(query, new NullProgressMonitor());
							Iterator<IInstallableUnit> iter = roots.iterator();
							while (iter.hasNext()) {
								IInstallableUnit iu = iter.next();
		
								toRemove.add(iu);
							}
						}
					}
	
					RepositoryManager.getDefault().provision(profile, null, toRemove, true, monitor);
				}
			}
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
		node.setAttribute(ATTRIBUTE_REMOVE_PROFILE, Boolean.toString(getRemoveProfile()));
	}

	@Override
	public void load(Element element) throws CoreException {
		profileName = element.getAttribute(ATTRIBUTE_PROFILE);
		String propRemoveProfile = element.getAttribute(ATTRIBUTE_REMOVE_PROFILE);
		if (propRemoveProfile != null) {
			setRemoveProfile(Boolean.parseBoolean(propRemoveProfile));
		}
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
		// during install because we might be replacing units that other
		// units are dependent on.
		return false;
	}
}
