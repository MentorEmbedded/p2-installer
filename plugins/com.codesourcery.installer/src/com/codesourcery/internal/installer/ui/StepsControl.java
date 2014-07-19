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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A control that displays a sequence of steps.
 * ---------------------------------
 * | Step 1 > Step 2 > Step 3 > ... >
 * ---------------------------------
 * 
 * Use {@link #addStep(String)} to add steps to the control.
 * Use {@link #setCurrentStep(String)} to set the current step.
 */
public class StepsControl extends Canvas {
	/** Step colors */
	private enum StepColor {
		/** Foreground color of step before current step */
		BEFORE_CURRENT_FOREGROUND,
		/** Background color of step before current step */
		BEFORE_CURRENT_BACKGROUND,
		/** Foreground color of current step */
		CURRENT_FOREGROUND,
		/** Background color of current step */
		CURRENT_BACKGROUND,
		/** Foreground color of step after current step */
		AFTER_CURRENT_FOREGROUND,
		/** Background color of step after current step */
		AFTER_CURRENT_BACKGROUND
	};
	
	/** Text drawing flags */
	private final static int TEXT_FLAGS = SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC;
	/** Default leader width default */
	private final static int LEADER_WIDTH_DEFAULT = 7;
	/** Horizontal margin default */
	private final static int HORIZONTAL_MARGIN_DEFAULT = 2;
	/** Vertical margin default */
	private final static int VERTICAL_MARGIN_DEFAULT = 2;
	/** Text horizontal margin default */
	private final static int TEXT_HORIZONTAL_MARGIN_DEFAULT = 4;
	/** Text vertical margin default */
	private final static int TEXT_VERTICAL_MARGIN_DEFAULT = 4;
	/** Step spacing default */
	private final static int STEP_SPACING_DEFAULT = 2;
	/** Shadow offset */
	private final static int SHADOW_OFFSET = 2;

	/** <code>true</code> to double-buffer drawing */
	private boolean doubleBuffer = true;
	/** Current step */
	private String currentStep;
	/** Scroll offset */
	private int scrollOffset = 0;
	/** Leader width */
	private int leaderWidth = LEADER_WIDTH_DEFAULT;
	/** Horizontal margin */
	private int horizontalMargin = HORIZONTAL_MARGIN_DEFAULT;
	/** Vertical margin */
	private int verticalMargin = VERTICAL_MARGIN_DEFAULT;
	/** Text horizontal margin */
	private int textHorizontalMargin = TEXT_HORIZONTAL_MARGIN_DEFAULT;
	/** Text vertical margin */
	private int textVerticalMargin = TEXT_VERTICAL_MARGIN_DEFAULT;
	/** Step spacing */
	private int stepSpacing = STEP_SPACING_DEFAULT;
	/** <code>true</code> to show shadow */
	private boolean showShadow = true;
	/** Control colors */
	private Color[] colors = new Color[StepColor.values().length];
	/** Steps */
	private ArrayList<String> steps = new ArrayList<String>();

	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags
	 */
	public StepsControl(Composite parent, int style) {
		super(parent, style & SWT.NO_BACKGROUND);

		// Add paint listener
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				onPaint(e);
			}
		});
		// Add size listener
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				// Recompute scroll offset
				scrollOffset = -1;
				redraw();
			}
		});
		
		// Create control colors
		createColors(getDisplay());
	}

	/**
	 * Adds a step.  If the step has already been added, this method does
	 * nothing.
	 * 
	 * @param stepName Step name
	 */
	public void addStep(String stepName) {
		if (!steps.contains(stepName)) {
			steps.add(stepName);
			redraw();
		}
	}

	/**
	 * Sets the current step.
	 * This method will ensure the current step is visible.
	 * 
	 * @param currentStep Step to set current
	 */
	public void setCurrentStep(String currentStep) {
		this.currentStep = currentStep;
		/** Recompute scroll */
		scrollOffset = -1;
		redraw();
	}
	
	/**
	 * Returns the current step.
	 * 
	 * @return Current step or <code>null</code>
	 */
	public String getCurrentStep() {
		return currentStep;
	}

	/**
	 * Sets the control to use a buffer when painting to avoid flicker.
	 * 
	 * @param doubleBuffer <code>true</code> to enable double-buffering
	 */
	public void setDoubleBuffered(boolean doubleBuffer) {
		this.doubleBuffer = doubleBuffer;
	}
	
	/**
	 * Returns if the control will use a buffer to paint.
	 * 
	 * @return <code>true</code> if double-buffering is enabled
	 */
	public boolean isDoubleBuffered() {
		return doubleBuffer;
	}

	/**
	 * Sets the width of the step leader.
	 * 
	 * @param leaderWidth Leader width
	 */
	public void setLeaderWidth(int leaderWidth) {
		this.leaderWidth = leaderWidth;
	}

	/**
	 * Returns the leader width.
	 * 
	 * @return Leader width
	 */
	public int getLeaderWidth() {
		return leaderWidth;
	}
	
	/**
	 * Sets the horizontal margin.
	 * 
	 * @param horizontalMargin Horizontal margin
	 */
	public void setHorizontalMargin(int horizontalMargin) {
		this.horizontalMargin = horizontalMargin;
	}
	
	/**
	 * Returns the horizontal margin.
	 * 
	 * @return Horizontal margin
	 */
	public int getHorizontalMargin() {
		return horizontalMargin;
	}
	
	/**
	 * Sets the vertical margin.
	 * 
	 * @param verticalMargin Vertical margin
	 */
	public void setVerticalMargin(int verticalMargin) {
		this.verticalMargin = verticalMargin;
	}
	
	/**
	 * Returns the vertical margin.
	 * 
	 * @return Vertical margin
	 */
	public int getVerticalMargin() {
		return verticalMargin;
	}
	
	/**
	 * Sets the text horizontal margin.
	 * 
	 * @param textHorizontalMargin Text horizontal margin
	 */
	public void setTextHorizontalMargin(int textHorizontalMargin) {
		this.textHorizontalMargin = textHorizontalMargin;
	}
	
	/**
	 * Returns the text horizontal margin.
	 * 
	 * @return Text horizontal margin
	 */
	public int getTextHorizontalMargin() {
		return textHorizontalMargin;
	}
	
	/**
	 * Sets the text vertical margin.
	 * 
	 * @param textVerticalMargin Text vertical margin
	 */
	public void setTextVerticalMargin(int textVerticalMargin) {
		this.textVerticalMargin = textVerticalMargin;
	}
	
	/**
	 * Returns the text vertical margin.
	 * 
	 * @return Text vertical margin
	 */
	public int getTextVerticalMargin() {
		return textVerticalMargin;
	}
	
	/**
	 * Sets the step spacing.
	 * 
	 * @param stepSpacing Step spacing
	 */
	public void setStepSpacing(int stepSpacing) {
		this.stepSpacing = stepSpacing;
	}
	
	/**
	 * Returns the step spacing.
	 * 
	 * @return Step spacing
	 */
	public int getStepSpacing() {
		return stepSpacing;
	}
	
	/**
	 * Enables/disables drawing a shadow.
	 * 
	 * @param showShadow <code>true</code> to enable show
	 */
	public void setShadowEnabled(boolean showShadow) {
		this.showShadow = showShadow;
	}
	
	/**
	 * Returns if a shadown will be drawn.
	 * 
	 * @return <code>true</code> if shadow will be drawn
	 */
	public boolean isShadowEnabled() {
		return showShadow;
	}
	
	/**
	 * Creates control colors.
	 * 
	 * @param device Device
	 */
	protected void createColors(Device device) {
		// Colors of current step
		colors[StepColor.CURRENT_BACKGROUND.ordinal()] = 
				new Color(device, getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB());
		colors[StepColor.CURRENT_FOREGROUND.ordinal()] = 
				new Color(device, getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB());
		// Colors of steps before current step
		colors[StepColor.BEFORE_CURRENT_BACKGROUND.ordinal()] = 
				new Color(device, blendRGB(
						getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(), 
						getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
						60));
		colors[StepColor.BEFORE_CURRENT_FOREGROUND.ordinal()] = 
				new Color(device, getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB());
		// Colors of steps after current step
		colors[StepColor.AFTER_CURRENT_BACKGROUND.ordinal()] = 
				new Color(device, blendRGB(
						getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB(), 
						getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
						5));
		colors[StepColor.AFTER_CURRENT_FOREGROUND.ordinal()] = 
				new Color(device, blendRGB(
						getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB(), 
						getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
						30));
	}

	/**
	 * Returns a color.
	 * 
	 * @param color Color to return
	 * @return Color
	 */
	protected Color getColor(StepColor color) {
		return colors[color.ordinal()];
	}
	
	/**
	 * Computes text size.
	 * 
	 * @param text Text to compute
	 * @return Text size
	 */
	protected Point getTextSize(GC gc, String text) {
		Point textSize;
		if (text == null) {
			textSize = new Point(0, 0);
		}
		else {
			gc.setFont(getFont());
			textSize = gc.textExtent(text, TEXT_FLAGS);
		}

		return textSize;
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		return computeSize(wHint, hHint);
	}

	@Override
	public Point computeSize(int wHint, int hHint)
	{
		if (wHint < 0) wHint = 0;
		if (hHint < 0) hHint = 0;

		GC gc = new GC(getShell());
		gc.setFont(getFont());

		int width = getHorizontalMargin() + getHorizontalMargin() + (isShadowEnabled() ? SHADOW_OFFSET : 0);
		int height = getVerticalMargin() + getTextVerticalMargin() + gc.getFontMetrics().getHeight()
				+ getTextVerticalMargin() + getVerticalMargin() + (isShadowEnabled() ? SHADOW_OFFSET : 0);
		
		for (String step : steps) {
			Point textSize = getTextSize(gc, step);
			/* |                      |         |                      |           \  */ 
			/* |text horizontal margin|text size|text horizontal margin|leader width> */
			/* |                      |         |                      |           /  */ 
			width += getTextHorizontalMargin() + textSize.x + getTextHorizontalMargin() + getLeaderWidth();
		}
		
		gc.dispose();
		
		int minWidth = Math.max(wHint, width);
		int minHeight = Math.max(hHint, height);
		
		return new Point(minWidth, minHeight);
	}
	
	/**
	 * Returns the extent of a step.
	 * 
	 * @param gc Graphics context 
	 * @param step Step to return extent for
	 * @return Text extent.  The 'x' coordinate will contain the step offset.
	 * The 'y' coordinate will contain the step width.
	 */
	private Point getStepExtent(GC gc, String step) {
		Point extent = null;

		if (step != null) {
			int offset = getHorizontalMargin();
			for (String s : steps) {
				int stepWidth = getTextHorizontalMargin() + getTextSize(gc, s).x + getTextHorizontalMargin() + getLeaderWidth();
				if (s.equals(step)) {
					extent = new Point(offset, stepWidth);
					break;
				}
				else {
					offset += stepWidth; 
				}
			}
		}		
		
		return extent;
	}

	/**
	 * Returns the next step.
	 * 
	 * @param currentStep Step
	 * @return Step after <code>currentStep</code> or <code>null</code>
	 */
	private String getNextStep(String currentStep) {
		String nextStep = null;
		
		for (int index = 0; index < steps.size(); index ++) {
			if (steps.get(index).equals(currentStep)) {
				if (index + 1 < steps.size()) {
					nextStep = steps.get(index + 1);
					break;
				}
			}
		}
		
		return nextStep;
	}
	
	/**
	 * Called to paint the control.
	 * 
	 * @param event Paint event
	 */
	private void onPaint(PaintEvent event) {
		// Get the client area
		Rectangle clientArea = getClientArea();
		
		Image bufferImage = null;
		GC gc;

		// If double-buffering is enabled, create image for graphics context
		if (isDoubleBuffered()) {
			bufferImage = new Image(getDisplay(), clientArea.width, clientArea.height);
			gc = new GC(bufferImage);
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			gc.fillRectangle(clientArea);
		}
		// Else paint graphics context directly
		else {
			gc = event.gc;
		}

		// Set the font
		gc.setFont(getFont());
		// Get height of text
		int textHeight = gc.getFontMetrics().getHeight();
		// Use advanced graphics if available
		gc.setAdvanced(true);
		gc.setAntialias(SWT.ON);

		// If advanced graphics available, draw panel shadow
		if (gc.getAdvanced() && isShadowEnabled()) {
			gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			gc.setAlpha(40);
			for (int offset = SHADOW_OFFSET; offset > 0; offset--) {
				gc.fillRoundRectangle(
						SHADOW_OFFSET + offset, 
						SHADOW_OFFSET + offset, 
						clientArea.width - SHADOW_OFFSET - offset - offset, 
						clientArea.height - SHADOW_OFFSET - offset - offset, 
						3, 3);
			}
			gc.setAlpha(0xFF);
			
			// Adjust client area to exclude shadow
			clientArea.width -= SHADOW_OFFSET;
			clientArea.height -= SHADOW_OFFSET;
		}

		// Does scroll offset require computing?
		if (scrollOffset == -1) {
			// Compute extent of current step
			Point stepExtent = getStepExtent(gc, getCurrentStep());
			// No scroll offset
			if (stepExtent == null) {
				scrollOffset = 0;
			}
			else {
				// Current step extent off client area
				if (stepExtent.x + stepExtent.y > clientArea.width) {
					// If there is a next step, scroll so it is half visible
					String nextStep = getNextStep(getCurrentStep());
					if (nextStep != null) {
						Point nextExtent = getStepExtent(gc, nextStep);
						scrollOffset = clientArea.width - nextExtent.x - (nextExtent.y / 2);
					}
					// Otherwise scroll so entire current step is visible
					else {
						scrollOffset = clientArea.width - stepExtent.x - stepExtent.y;
					}
				}
			}
		}

		// Draw panel
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		gc.fillRoundRectangle(0, 0, clientArea.width, clientArea.height, 3, 3);
		// Adjust clipping
		gc.setClipping(clientArea.x + getHorizontalMargin(), clientArea.y + getVerticalMargin(), clientArea.width - getHorizontalMargin() - getHorizontalMargin(), clientArea.height - getVerticalMargin() - getVerticalMargin());
		
		// Compute widths of steps text
		int[] textWidths = new int[steps.size()];
		for (int index = 0; index < steps.size(); index ++) {
			String step = steps.get(index);
			Point textSize =  gc.textExtent(step, TEXT_FLAGS);
			textWidths[index] = textSize.x;
		}

		boolean afterCurrentStep = false;
		// Drawing off starts past horizontal margin, adjusted for scrolling
		int offset = getHorizontalMargin() + scrollOffset;
		// Draw steps
		for (int index = 0; index < steps.size(); index++) {
			String step = steps.get(index);

			Color stepForeground;
			Color stepBackground;
			
			// Current step color
			if (step.equals(getCurrentStep())) {
				stepForeground = getColor(StepColor.CURRENT_FOREGROUND);
				stepBackground = getColor(StepColor.CURRENT_BACKGROUND);
				afterCurrentStep = true;
			}
			// After current step color
			else if (afterCurrentStep) {
				stepForeground = getColor(StepColor.AFTER_CURRENT_FOREGROUND);
				stepBackground = getColor(StepColor.AFTER_CURRENT_BACKGROUND);
			}
			// Before current step color
			else {
				stepForeground = getColor(StepColor.BEFORE_CURRENT_FOREGROUND);
				stepBackground = getColor(StepColor.BEFORE_CURRENT_BACKGROUND);
			}
			
			gc.setBackground(stepBackground);
			gc.setForeground(stepBackground);
			
			int stepWidth = getTextHorizontalMargin() + textWidths[index] + getTextHorizontalMargin();
			int stepHeight = getTextVerticalMargin() + textHeight + getTextVerticalMargin();

			int[] stepPoints;
			// First step in sequence
			if (index == 0) {
				stepPoints = new int[] { 
					offset, getVerticalMargin(),
					offset + stepWidth, getVerticalMargin(),
					offset + stepWidth + getLeaderWidth(), getVerticalMargin() + getTextVerticalMargin() + textHeight / 2,
					offset + stepWidth, getVerticalMargin() + stepHeight,
					offset, getVerticalMargin() + stepHeight
				};
			}
			// Last step in sequence
			else if (index == steps.size() - 1) {
				stepPoints = new int[] {
						offset - getLeaderWidth() + getStepSpacing(), getVerticalMargin(),
						clientArea.width - getHorizontalMargin(), getVerticalMargin(),
						clientArea.width - getHorizontalMargin(), getVerticalMargin() + stepHeight,
						offset - getLeaderWidth() + getStepSpacing(), getVerticalMargin() + stepHeight,
						offset + getStepSpacing(), getVerticalMargin() + getTextVerticalMargin() + textHeight / 2
					};
			}
			// Step in sequence
			else {
				stepPoints = new int[] {
					offset - getLeaderWidth() + getStepSpacing(), getVerticalMargin(),
					offset + stepWidth, getVerticalMargin(),
					offset + stepWidth + getLeaderWidth(), getVerticalMargin() + getTextVerticalMargin() + textHeight / 2,
					offset + stepWidth, getVerticalMargin() + stepHeight,
					offset - getLeaderWidth() + getStepSpacing(), getVerticalMargin() + stepHeight,
					offset + getStepSpacing(), getVerticalMargin() + getTextVerticalMargin() + textHeight / 2
				};
			}

			gc.fillPolygon(stepPoints);
			
			gc.setForeground(stepForeground);
			gc.drawText(step, offset + getTextHorizontalMargin(), clientArea.height / 2 - textHeight / 2, TEXT_FLAGS);
			
			offset += stepWidth + getLeaderWidth();
		}

		if (bufferImage != null) {
			event.gc.drawImage(bufferImage, 0, 0);
			bufferImage.dispose();
		}
	}

	/**
	 * Blends two RGB values using the provided ratio. 
	 * 
	 * @param c1 First RGB value
	 * @param c2 Second RGB value
	 * @param ratio Percentage of the first RGB to blend with 
	 * second RGB (0-100)
	 * 
	 * @return The RGB value of the blended color
	 */
	public static RGB blendRGB(RGB c1, RGB c2, int ratio) {
		ratio = Math.max(0, Math.min(255, ratio));

		int r = Math.max(0, Math.min(255, (ratio * c1.red + (100 - ratio) * c2.red) / 100));
		int g = Math.max(0, Math.min(255, (ratio * c1.green + (100 - ratio) * c2.green) / 100));
		int b = Math.max(0, Math.min(255, (ratio * c1.blue + (100 - ratio) * c2.blue) / 100));
		
		return new RGB(r, g, b);
	}
	
	@Override
	public void dispose() {
		// Dispose of colors
		for (Color color : colors) {
			color.dispose();
			colors = null;
		}
		super.dispose();
	}
}
