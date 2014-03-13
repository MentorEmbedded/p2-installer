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
package com.codesourcery.internal.installer.ui;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallerImages;

/**
 * Composite that shows a list of steps to be performed,
 * steps that have been performed, and the
 * current step.
 */
public class SideBarComposite extends Composite {
	/** Step states */
	public enum StepState {NONE, ERROR, SUCCESS};

	/**
	 * Step labels
	 */
	ArrayList<CLabel> stepLabels = new ArrayList<CLabel>();
	/**
	 * Steps area
	 */
	Composite stepsArea;
	/**
	 * Font for current step
	 */
	Font currentFont;
	/**
	 * Current step
	 */
	String currentStep;

	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags
	 */
	public SideBarComposite(Composite parent, int style) {
		super(parent, style);
		GridLayout layout = new GridLayout(1, false);
		setLayout(layout);

		// Steps area
		stepsArea = new Composite(this, SWT.NONE);
		GridLayout stepsLayout = new GridLayout(1, true);
		stepsLayout.marginHeight = 0;
		stepsLayout.marginWidth = 0;
		stepsArea.setLayout(stepsLayout);
		stepsArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		stepsArea.setBackground(getBackground());

		setFont(parent.getFont());

		// Create bold font
		FontData[] fontData = getFont().getFontData();
		fontData[0].setStyle(SWT.BOLD);
		currentFont = new Font(getFont().getDevice(), fontData);
	}

	/**
	 * Adds a step
	 * 
	 * @param stepName Step name
	 */
	public void addStep(String stepName) {
		CLabel stepLabel = new CLabel(stepsArea, SWT.NONE);
		stepLabel.setText(stepName);
		stepLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_EMPTY));
		stepLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, true, 1, 1));
		stepLabel.setData(new Step());
		stepLabels.add(stepLabel);
	}

	/**
	 * Sets the current step
	 * 
	 * @param stepName Name of step to set current
	 */
	public void setCurrentStep(String stepName) {
		boolean mark = true;
		for (CLabel stepLabel : stepLabels) {
			Step step = (Step)stepLabel.getData();
			step.setCompleted(mark);
			if (stepLabel.getText().equals(stepName)) {
				step.setCurrent(true);
				mark = false;
			}
		}
		
		updateLabels();
	}
	
	/**
	 * Updates all step labels.
	 */
	private void updateLabels() {
		Color activeColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
		Color inactiveColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

		for (CLabel stepLabel : stepLabels) {
			Step step = (Step)stepLabel.getData();
			
			stepLabel.setFont(getFont());
			stepLabel.setForeground(step.isCompleted ? activeColor : inactiveColor);
			// Step has error
			if (step.getState() == StepState.ERROR) {
				stepLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_ERROR));
			}
			// Step is successful
			else if (step.getState() == StepState.SUCCESS) {
				stepLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_CHECKED));
			}
			// Step is current
			else if (step.isCurrent()) {
				stepLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.ARROW_RIGHT));
				stepLabel.setFont(currentFont);
			}
			// Step is completed
			else if (step.isCompleted()) {
				stepLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_SOLID));
			}
			// Step is not completed
			else {
				stepLabel.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.BULLET_EMPTY));
			}
		}
	}
	
	/**
	 * Sets the state of a step.
	 * 
	 * @param stepName Step name
	 * @param state State
	 */
	public void setState(String stepName, StepState state) {
		for (CLabel stepLabel : stepLabels) {
			if (stepLabel.getText().equals(stepName)) {
				Step step = (Step)stepLabel.getData();
				step.setState(state);
				break;
			}
		}
		
		updateLabels();
	}

	/**
	 * Returns the state of a step.
	 * 
	 * @param stepName Step name
	 * @return State
	 */
	public StepState getState(String stepName) {
		StepState state = StepState.NONE;
		
		for (CLabel stepLabel : stepLabels) {
			if (stepLabel.getText().equals(stepName)) {
				Step step = (Step)stepLabel.getData();
				state = step.getState();
				break;
			}
		}
		
		return state;
	}

	@Override
	public void dispose() {
		if (currentFont != null) {
			currentFont.dispose();
			currentFont = null;
		}

		super.dispose();
	}
	
	/**
	 * Step in bar
	 */
	private class Step {
		/** <code>true</code> if current step */
		private boolean isCurrent = false;
		/** <code>true</code> if state is completed */
		private boolean isCompleted = false;
		/** Step state */
		private StepState state = StepState.NONE;
		
		/**
		 * Sets the step current.
		 * 
		 * @param current <code>true</code> if current
		 */
		public void setCurrent(boolean current) {
			this.isCurrent = current;
		}
		
		/**
		 * Returns if the step is current.
		 * 
		 * @return <code>true</code> if current
		 */
		public boolean isCurrent() {
			return isCurrent;
		}
		
		/**
		 * Sets the step completed.
		 * 
		 * @param completed <code>true</code> if completed.
		 */
		public void setCompleted(boolean completed) {
			this.isCompleted = completed;
		}
		
		/**
		 * Returns if the step is completed.
		 * 
		 * @return <code>true</code> if completed.
		 */
		public boolean isCompleted() {
			return isCompleted;
		}
		
		/**
		 * Sets the step state.
		 * 
		 * @param state Step state
		 */
		public void setState(StepState state) {
			this.state = state;
		}
		
		/**
		 * Returns the step state.
		 * 
		 * @return Step state
		 */
		public StepState getState() {
			return state;
		}
	}
}
