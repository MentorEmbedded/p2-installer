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

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This control shows a spinning progress animation 
 * and optional text.
 * Call <code>setProgress</code> to start and stop the progress 
 * animation.
 */
public class SpinnerProgress extends Canvas {
	/** Text drawing flags */
	private final int TEXT_FLAGS = SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC;
    /** Default number of animation spokes */
    private final int DEFAULT_NUMBER_OF_SPOKES = 8;
    /** Default width */
    private final int DEFAULT_WIDTH = 16;
    /** Default height */
    private final int DEFAULT_HEIGHT = 16;
    /** Animation delay */
    private final int ANIMATION_DELAY = 100;
    /** Margin */
    private final int MARGIN = 0;
    /** Margin between animation and text */
    private final int TEXT_MARGIN = 4;
    /** Colors */
    private Color[] colors;
    /** Computed spoke angles */
    private double[] spokeAngles;
    /** Animate progress */
    private int animateProgress = 0;
    /** Animation timer */
    private Timer animationTimer;
    /** Text */
    private String text;
    /** Number of spokes */
    private int numberOfSpokes = DEFAULT_NUMBER_OF_SPOKES; 

    /**
     * Constructor
     * 
     * @param parent Parent
     * @param style Style flags
     */
	public SpinnerProgress(Composite parent, int style) {
		super(parent, style);
		
		// Add paint listener
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				onPaint(e);
			}
		});
		// Set font
		setFont(parent.getFont());
		// Create spoke colors
		createColors(getDisplay());
		// Compute spoke angles
		computeSpokeAngles();
	}
	
	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style flags
	 * @param numberOfSpokes Number of spokes in animation
	 */
	public SpinnerProgress(Composite parent, int style, int numberOfSpokes) {
		this(parent, style);
		this.numberOfSpokes = numberOfSpokes;
	}

	/**
	 * Starts/stops progress animation.
	 * 
	 * @param enable <code>true</code> to enable
	 */
	public void setProgress(boolean enable) {
		if (enable) {
			animationTimer = new Timer(true);
			animationTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					onTimer();
				}
			}, 0, ANIMATION_DELAY);
		}
		else {
			if (animationTimer != null) {
				animationTimer.cancel();
				animationTimer = null;
				redraw();
			}
		}
	}
	
	@Override
	public void dispose() {
		setProgress(false);
		
		if (colors != null) {
			for (Color color : colors) {
				color.dispose();
			}
		}
		
		super.dispose();
	}

	/**
	 * Sets the text to display.
	 * 
	 * @param text Text
	 */
	public void setText(String text) {
		this.text = text;
		if (!isDisposed()) {
			redraw();
		}
	}
	
	/**
	 * Returns the text.
	 * 
	 * @return Text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * Called on the animation timer.
	 */
	private void onTimer() {
		if (!isDisposed()) {
			getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!isDisposed()) {
						if (isEnabled()) {
							animateProgress = ++animateProgress % numberOfSpokes;
							redraw();
						}
					}
				}
			});
		}
	}

    @Override
	public void setForeground(Color c) {
		super.setForeground(c);
		
		if (colors != null) {
			for (Color color : colors) {
				color.dispose();
			}
		}
		createColors(getDisplay());
	}

    /**
     * Creates the colors for the spoke animation.
     * 
     * @param display Display
     */
	private void createColors(Display display)
    {
        colors = new Color[numberOfSpokes];

        byte bytIncrement = (byte)(Byte.MAX_VALUE / numberOfSpokes);
        Color foreground = getForeground();
        Color background = getBackground();

        byte PERCENTAGE_OF_DARKEN = 0;

        for (int intCursor = 0; intCursor < numberOfSpokes; intCursor++)
        {
            if (intCursor == 0)
                colors[intCursor] = foreground;
            else
            {
                PERCENTAGE_OF_DARKEN += bytIncrement;
                if (PERCENTAGE_OF_DARKEN > Byte.MAX_VALUE)
                    PERCENTAGE_OF_DARKEN = Byte.MAX_VALUE;

                RGB rgb = blendRGB(foreground.getRGB(), background.getRGB(), PERCENTAGE_OF_DARKEN);
                colors[intCursor] = new Color(display, rgb);
            }
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
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		return computeSize(wHint, hHint);
	}

	@Override
	public Point computeSize(int wHint, int hHint)
	{
		Point textSize = getTextSize(getText());
		int minHeight;
		if (hHint == SWT.DEFAULT) {
			minHeight = MARGIN + MARGIN + Math.max(DEFAULT_HEIGHT, textSize.y);
		} else {
			minHeight = hHint; 
		}

		int minWidth = Math.max(wHint, MARGIN + MARGIN + TEXT_MARGIN + DEFAULT_WIDTH + textSize.x);
		
		return new Point(minWidth, minHeight);
	}

	/**
	 * Computes text size.
	 * 
	 * @param text Text to compute
	 * @return Text size
	 */
	private Point getTextSize(String text) {
		Point textSize;
		if (text == null) {
			textSize = new Point(0, 0);
		}
		else {
			GC gc = new GC(getShell());
			gc.setFont(getFont());
			textSize = gc.textExtent(getText(), TEXT_FLAGS);
			gc.dispose();
		}

		return textSize;
	}
	
	/**
	 * Computes the animation spoke angles.
	 */
    private void computeSpokeAngles()
    {
        spokeAngles = new double[numberOfSpokes];
        double dblAngle = (double)360 / numberOfSpokes;

        for (int shtCounter = 0; shtCounter < numberOfSpokes; shtCounter++)
            spokeAngles[shtCounter] = (shtCounter == 0 ? dblAngle : spokeAngles[shtCounter - 1] + dblAngle);
    }
    
    /**
	 * Called to paint the control.
	 * |----------------------------------------------|
	 * |{MARGIN}{Animation}{TEXT_MARGIN}{Text}{MARGIN}|
	 * |----------------------------------------------|
	 * 
	 * @param event Paint event
	 */
	private void onPaint(PaintEvent event) {
		// Get the client area
		Rectangle clientArea = getClientArea();
		// Get the graphics context
		GC gc = event.gc;
		// Set the font
		gc.setFont(getFont());
		// Color
		gc.setForeground(getForeground());
		
		int max = Math.min((clientArea.width - MARGIN) / 2, (clientArea.height - MARGIN) / 2);
		int outerRadius = max;
		int innerRadius = max / 2;
		int spokeThickness = max / 4;

		if (animationTimer != null) {
			Point center = new Point(clientArea.x + max + MARGIN, clientArea.y + max + MARGIN);
			int position = animateProgress;
			for (int counter = 0; counter < numberOfSpokes; counter++) {
				position = position % numberOfSpokes;
				gc.setLineWidth(spokeThickness);
				gc.setForeground(colors[counter]);
				Point startPoint = GetCoordinate(center, innerRadius, spokeAngles[position]);
				Point endPoint = GetCoordinate(center, outerRadius, spokeAngles[position]);
				gc.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
				position++;
			}
		}
		
		// Draw text
		String text = getText();
		if (text != null) {
			gc.setForeground(colors[numberOfSpokes / 2]);
			gc.drawText(text, max + max + TEXT_MARGIN + MARGIN, MARGIN, TEXT_FLAGS);
		}
	}
	
	/**
	 * Returns the coordinates for a point
	 * at a given radius and angle from a center
	 * coordinate. 
	 * 
	 * @param center Center coordinate
	 * @param radius Radius
	 * @param angle Angle
	 * @return Coordinate
	 */
    private Point GetCoordinate(Point center, int radius, double angle)
    {
        double ang = Math.PI * angle / 180;
        return new Point(center.x + (int)(radius * (float)Math.cos(ang)), (int)(center.y + radius * (float)Math.sin(ang)));
    }
}
