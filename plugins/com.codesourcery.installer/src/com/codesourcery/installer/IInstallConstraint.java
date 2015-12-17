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
package com.codesourcery.installer;


/**
 * Information for constraints regarding what can be installed.
 */
public interface IInstallConstraint {
	/** Constraint type */
	public enum Constraint {
		/** At least one item must be included */
		ONE_OF,
		/** One item requires one or more other items */
		REQUIRES,
		/** Only one of a set of items can be included */
		ONLY_ONE
	}

	/**
	 * Returns the source component for the constraint.
	 * 
	 * @return Source component for <code>REQUIRES</code>, <code>null</code>
	 * otherwise. 
	 */
	public IInstallComponent getSource();
	
	/**
	 * Returns the target components for the constraint.
	 * <ul>
	 * <li>ONE_OF - One of the components returned must be included.</li>
	 * <li>REQUIRES - The components that the source component depends on.</li>
	 * <li>ONLY_ONE - Only one component in the targets returned can be included<li>
	 * 
	 * @return Target components
	 */
	public IInstallComponent[] getTargets();

	/**
	 * Returns the constraint type.
	 * 
	 * @return Constraint type
	 */
	public Constraint getConstraint();

	/**
	 * Validates the constraint for a set of included components.
	 * 
	 * @param components Components
	 * @return <code>true</code> if the constraint is met for the set of
	 * components.
	 */
	public boolean validate(IInstallComponent[] components);
}
