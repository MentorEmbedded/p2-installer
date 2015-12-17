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

import java.util.ArrayList;

import org.eclipse.equinox.p2.metadata.IVersionedId;

import com.codesourcery.installer.IInstallComponent;
import com.codesourcery.installer.IInstallConstraint;
import com.codesourcery.installer.Installer;

/**
 * Default implementation of an install constraint.
 */
public class InstallConstraint implements IInstallConstraint {
	/** Constraint roots */
	private IVersionedId[] roots;
	/** Constraint type */
	private Constraint constraint;
	/** Source for constraint or <code>null</code> */
	private IInstallComponent source;
	/** Targets for constraint */
	private IInstallComponent[] targets;

	/**
	 * Constructor
	 * 
	 * @param roots Roots for constraint.  If the constraint requires a source,
	 * it must be the first element.
	 * @param constraint Constraint type
	 */
	public InstallConstraint(IVersionedId[] roots, Constraint constraint) {
		this.roots = roots;
		this.constraint = constraint;
	}

	/**
	 * Returns the roots for the constraint.
	 * 
	 * @return Roots
	 */
	private IVersionedId[] getRoots() {
		return roots;
	}

	@Override
	public IInstallComponent getSource() {
		if (source == null) {
			// Source is only supported for requires constraint
			if (getConstraint() == Constraint.REQUIRES) {
				if ((getRoots() != null) && (getRoots().length != 0)) {
					source = RepositoryManager.getDefault().getInstallComponent(getRoots()[0].getId());			
				}
			}
		}
		
		return source;
	}

	@Override
	public IInstallComponent[] getTargets() {
		if (targets == null) {
			ArrayList<IInstallComponent> components = new ArrayList<IInstallComponent>();
			for (IVersionedId root : getRoots()) {
				if ((getSource() != null) && getSource().getInstallUnit().getId().equals(root.getId()))
					continue;
				IInstallComponent component = RepositoryManager.getDefault().getInstallComponent(root.getId());
				// If component has been loaded
				if (component != null) {
					components.add(component);
				}
			}
			targets = components.toArray(new IInstallComponent[components.size()]);
		}
		
		return targets;
	}

	@Override
	public Constraint getConstraint() {
		return constraint;
	}

	/**
	 * Returns if an install component is a member of an install component
	 * for a given installable unit.
	 * 
	 * @param component Install component to check
	 * @param id Identifier of installable unit
	 * @return <code>true</code> if install component is member
	 */
	private boolean isMemberOf(IInstallComponent component, String id) {
		boolean member = false;
		IInstallComponent parent = component.getParent();
		while (parent != null) {
			if (parent.getInstallUnit().getId().equals(id)) {
				member = true;
				break;
			}
			parent = parent.getParent();
		}
		
		return member;
	}
	
	/**
	 * Finds the constraint roots included in a set of install components.
	 * 
	 * @param components Install components
	 * @param checkMembers <code>true</code> to also check install component members
	 * @return Included state of install roots
	 */
	private boolean[] findComponents(IInstallComponent[] components, boolean checkMembers) {
		IVersionedId[] roots = getRoots();
		boolean[] found = new boolean[roots.length];
		
		for (int index = 0; index < roots.length; index ++) {
			for (IInstallComponent component : components) {
				String id = component.getInstallUnit().getId();
				String rootId = roots[index].getId();
				if (id.equals(rootId)) {
					found[index] = true;
					break;
				}
				if (checkMembers && isMemberOf(component, rootId)) {
					found[index] = true;
					break;
				}
			}
		}
		
		return found;
	}
	
	@Override
	public boolean validate(IInstallComponent[] components) {
		boolean[] found;
		boolean result = true;
		
		switch (getConstraint()) {
		// One component must be included
		case ONE_OF:
			// If not an upgrade
			if (!Installer.getDefault().getInstallManager().getInstallMode().isUpgrade()) {
				found = findComponents(components, false);
				boolean oneSelected = false;
				for (boolean one : found) {
					if (one) {
						oneSelected = true;
						break;
					}
				}
			
				// One of the required components must be included
				if (!oneSelected) {
					result = false;
				}
			}
			break;
		// One component requires one or more other components
		case REQUIRES:
			// Check if the source is included or any of its children
			found = findComponents(components, true);
			if (found[0]) {
				// Check if all required targets are included
				found = findComponents(components, false);
				for (int index = 1; index < found.length; index ++) {
					if (!found[index]) {
						result = false;
					}
				}
			}
			break;
		// Only one component can be included
		case ONLY_ONE:
			found = findComponents(components, true);
			int count = 0;
			for (boolean one : found) {
				if (one) {
					count ++;
				}
			}
			if (count > 1) {
				result = false;
			}
			break;
		};
		
		return result;
	}
}
