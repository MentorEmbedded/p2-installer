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
import java.util.Iterator;
import java.util.Stack;

import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * This class provides utility methods for a profile.
 */
public class ProfileAdapter {
	/** Profile */
	private IProfile profile;
	
	/**
	 * Constructor
	 * 
	 * @param profile Profile
	 */
	public ProfileAdapter(IProfile profile) {
		this.profile = profile;
	}
	
	/**
	 * @return The profile
	 */
	public IProfile getProfile() {
		return profile;
	}

	/**
	 * Returns installable units from a profile.
	 * 
	 * @param versions Versions for installable units
	 * @return Installable units
	 */
	public IInstallableUnit[] findUnits(IVersionedId[] versions) {
		ArrayList<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
		
		for (IVersionedId version : versions) {
			IQueryResult<IInstallableUnit> query = getProfile().query(QueryUtil.createIUQuery(version), null);
			Iterator<IInstallableUnit> iter = query.iterator();
			while (iter.hasNext()) {
				units.add(iter.next());
			}
		}
		
		return units.toArray(new IInstallableUnit[units.size()]);
	}

	/**
	 * Returns the latest version of an installable unit in a profile.
	 * 
	 * @param id Installable unit identifier
	 * @return Latest version found or <code>null</code>
	 */
	public IInstallableUnit findUnit(String id) {
		IInstallableUnit unit = null;
		if (getProfile() != null) {
			IQueryResult<IInstallableUnit> query = getProfile().query(QueryUtil.createIUQuery(id), null);
			Iterator<IInstallableUnit> iter = query.iterator();
			while (iter.hasNext()) {
				IInstallableUnit foundUnit = iter.next();
				if ((unit == null) || (unit.getVersion().compareTo(unit.getVersion()) > 0)) {
					unit = foundUnit;
				}
			}
		}
		
		return unit;
	}

	/**
	 * Returns all root IU's that require a specified IU in a profile.
	 * 
	 * Note: This method does not work in all cases.
	 * @param unit Unit to get dependents for
	 * @return Dependent units
	 * 
	 * Note: This routine is known to have problems as it doesn't check for 'strict' dependencies.
	 */
	public IInstallableUnit[] findAllDependentIUs(IInstallableUnit unit) {
		// Dependent units
		ArrayList<IInstallableUnit> dependents = new ArrayList<IInstallableUnit>();
		// The units left to calculate dependencies for
		Stack<IInstallableUnit> unitsToCalculate = new Stack<IInstallableUnit>();
		// Start with getting dependencies for specified unit
		unitsToCalculate.push(unit);

		// Get the units in the profile
		IQueryResult<IInstallableUnit> profileUnitsQuery = getProfile().query(QueryUtil.createIUAnyQuery(), null);
		IInstallableUnit[] profileUnits = profileUnitsQuery.toArray(IInstallableUnit.class);

		// While there are units to get dependents for
		while (!unitsToCalculate.empty()) {
			// Get units dependent on the specified unit
			IInstallableUnit[] children = findDependentIUs(unitsToCalculate.pop(), profileUnits);
			for (IInstallableUnit child : children) {
				if (!dependents.contains(child)) {
					// Add the unit as a dependent
					dependents.add(child);
					// Push so it's root dependencies are also calculated
					unitsToCalculate.add(child);
				}
			}
		}
		
		return dependents.toArray(new IInstallableUnit[dependents.size()]);
	}

	/**
	 * Returns root IU's from a set that require the specified unit.
	 *
	 * Note: This method does not work in all cases.
	 * @param unit Unit to get dependencies for
	 * @param candidates The set of units to find dependents
	 * @return Dependent units
	 */
	public IInstallableUnit[] findDependentIUs(IInstallableUnit unit, IInstallableUnit[] candidates) {
		ArrayList<IInstallableUnit> dependentRoots = new ArrayList<IInstallableUnit>();
		final String TRUE = Boolean.TRUE.toString();
		
		for (IInstallableUnit candidate : candidates) {
			// If root installable unit
			String rootProperty = getProfile().getInstallableUnitProperty(candidate, IProfile.PROP_PROFILE_ROOT_IU); 
			if (TRUE.equals(rootProperty)) {
				// Get candidate unit requirements
				Iterator<IRequirement> requirements = candidate.getRequirements().iterator();
				while (requirements.hasNext()) {
					IRequirement requirement = requirements.next();
					// If unit satisfies the candidate requirement, the candidate
					// is dependent on the unit
					if (unit.satisfies(requirement)) {
						if (!dependentRoots.contains(candidate)) {
							dependentRoots.add(candidate);
						}
						break;
					}
				}
			}
		}
		
		return dependentRoots.toArray(new IInstallableUnit[dependentRoots.size()]);
	}
	
}
