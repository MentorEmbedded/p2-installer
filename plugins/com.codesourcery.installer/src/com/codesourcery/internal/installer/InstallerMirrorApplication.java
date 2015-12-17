/*******************************************************************************
 *  Copyright (c) 2015 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.internal.installer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.PermissiveSlicer;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.internal.repository.mirroring.Mirroring;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * Mirror application that adds the following capabilities to the base P2 mirror application:
 * <ul>
 * <li>Support for using a different provisioning agent</li>
 * <li>Support for progress reporting</li>
 * <li>
 */
@SuppressWarnings("restriction") // Uses P2 internal classes
class InstallerMirrorApplication extends MirrorApplication {
	/** Progress monitor */
	private IProgressMonitor progressMonitor;
	/** Progress status text */
	private String progressText;
	
	/**
	 * Constructs an installer mirror application.
	 * 
	 * @param agent Provisioning agent or <code>null</code> to create a new agent.
	 * @param progressMonitor Progress monitor or <code>null</code> for no progress and cancellation.
	 * @param progressText Progress status text or <code>null</code>.
	 */
	public InstallerMirrorApplication(IProvisioningAgent agent, IProgressMonitor progressMonitor, String progressText) {
		this.agent = agent;
		this.progressMonitor = progressMonitor;
		if (this.progressMonitor == null) {
			this.progressMonitor = new NullProgressMonitor();
		}
		this.progressText = progressText;
	}

	/**
	 * @return Returns the provisioning agent.
	 */
	public IProvisioningAgent getAgent() {
		return agent;
	}
	
	/**
	 * @return Returns the progress monitor.
	 */
	public IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}
	
	/**
	 * @return Returns the progress status text.
	 */
	public String getProgressText() {
		return progressText;
	}

	/**
	 * @return returns the download size of the mirror.
	 */
	public long getDownloadSize() {
		PermissiveSlicer slicer = new PermissiveSlicer(getCompositeMetadataRepository(), new Hashtable<String, String>(), true, true, true, false, false);
		IQueryable<IInstallableUnit> slice = slicer.slice(sourceIUs.toArray(new IInstallableUnit[sourceIUs.size()]), new NullProgressMonitor());
		InstallerMirroring mirror = (InstallerMirroring)getMirroring(slice, new NullProgressMonitor());

		return mirror.getDownloadSize();
	}
	
	/**
	 * This method is overridden to use a special InstallerMirroring class instead of the normal P2 Mirroring class
	 * so that progress reporting can be provided and so the operation can be cancelled.
	 * 
	 * @see org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication#getMirroring
	 */
	@Override
	protected Mirroring getMirroring(IQueryable<IInstallableUnit> slice, IProgressMonitor monitor) {
		// Obtain ArtifactKeys from IUs
		IQueryResult<IInstallableUnit> ius = slice.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		boolean iusSpecified = !ius.isEmpty(); // call before ius.iterator() to avoid bug 420318
		ArrayList<IArtifactKey> keys = new ArrayList<IArtifactKey>();
		for (Iterator<IInstallableUnit> iterator = ius.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			keys.addAll(iu.getArtifacts());
		}

		InstallerMirroring mirror = new InstallerMirroring(getCompositeArtifactRepository(), destinationArtifactRepository, true/*raw*/,
				getProgressMonitor(), getProgressText());
		mirror.setCompare(false);
		mirror.setComparatorId(null);
		mirror.setBaseline(null);
		mirror.setValidate(true);
		mirror.setCompareExclusions(null);
		mirror.setTransport((Transport) agent.getService(Transport.SERVICE_NAME));
		mirror.setIncludePacked(true);

		// If IUs have been specified then only they should be mirrored, otherwise mirror everything.
		if (iusSpecified)
			mirror.setArtifactKeys(keys.toArray(new IArtifactKey[keys.size()]));

		return mirror;
	}
}
