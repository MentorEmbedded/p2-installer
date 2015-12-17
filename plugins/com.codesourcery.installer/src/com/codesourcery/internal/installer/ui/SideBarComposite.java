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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.codesourcery.installer.Installer;

/**
 * Composite that shows a list of steps to be performed,
 * steps that have been performed, and the
 * current step.
 */
public class SideBarComposite extends Composite {
	/** Step states */
	public enum StepState {NONE, ERROR, SUCCESS};
	/** Bullet empty image */
	private Image bulletEmpty;
	/** Bullet error image */
	private Image bulletError;
	/** Bullet checked image */
	private Image bulletChecked;
	/** Bullet arrow image */
	private Image bulletArrow;
	/** Bullet solid image */
	private Image bulletSolid;

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
	 * @param bulletEmpty Empty bullet image
	 * @param bulletSolid Solit bullet image
	 * @param bulletChecked Checked bullet image
	 * @param bulletArrow Arrow bullet image
	 * @param bulletError Error bullet image
	 * @param boldCurrent <code>true</code> to bold current step text, <code>false</code> to enlarge current step text.
	 */
	public SideBarComposite(Composite parent, int style, Image bulletEmpty, Image bulletSolid, Image bulletChecked, 
			Image bulletArrow, Image bulletError, boolean boldCurrent) {
		super(parent, style);

		this.bulletEmpty = bulletEmpty;
		this.bulletSolid = bulletSolid;
		this.bulletChecked = bulletChecked;
		this.bulletArrow = bulletArrow;
		this.bulletError = bulletError;
		
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
		if (boldCurrent) {
			fontData[0].setStyle(SWT.BOLD);
		}
		else {
			fontData[0].setHeight(fontData[0].getHeight() + 3);
		}

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
		stepLabel.setImage(bulletEmpty);
		stepLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 1, 1));
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

	public Point computeSize (int wHint, int hHint, boolean changed) {
		try {
			if (wHint == SWT.DEFAULT) {
				// Compute widest image
				int maxImageWidth = 0;
				Image widestImage = null;
				if ((bulletError != null) && (bulletError.getImageData().width > maxImageWidth)) {
					maxImageWidth = bulletError.getImageData().width;
					widestImage = bulletError;
				}
				if ((bulletChecked != null) && (bulletChecked.getImageData().width > maxImageWidth)) {
					maxImageWidth = bulletChecked.getImageData().width;
					widestImage = bulletChecked;
				}
				if ((bulletArrow != null) && (bulletArrow.getImageData().width > maxImageWidth)) {
					maxImageWidth = bulletArrow.getImageData().width;
					widestImage = bulletArrow;
				}
				if ((bulletSolid != null) && (bulletSolid.getImageData().width > maxImageWidth)) {
					maxImageWidth = bulletSolid.getImageData().width;
					widestImage = bulletSolid;
				}
				if ((bulletEmpty != null) && (bulletEmpty.getImageData().width > maxImageWidth)) {
					maxImageWidth = bulletEmpty.getImageData().width;
					widestImage = bulletEmpty;
				}

				// Set attributes to produce widest step
				for (CLabel stepLabel : stepLabels) {
					stepLabel.setFont(currentFont);
					stepLabel.setImage(widestImage);
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}

		// Compute size
		Point preferredSize = super.computeSize(wHint, hHint, changed);
		
		// Restore step attributes
		if (wHint == SWT.DEFAULT) {
			try {
				updateLabels();
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		return preferredSize;
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
				stepLabel.setImage(bulletError);
			}
			// Step is successful
			else if (step.getState() == StepState.SUCCESS) {
				stepLabel.setImage(bulletChecked);
			}
			// Step is current
			else if (step.isCurrent()) {
				stepLabel.setImage(bulletArrow);
				stepLabel.setFont(currentFont);
			}
			// Step is completed
			else if (step.isCompleted()) {
				stepLabel.setImage(bulletSolid);
			}
			// Step is not completed
			else {
				stepLabel.setImage(bulletEmpty);
			}
		}
		stepsArea.layout(true);
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
