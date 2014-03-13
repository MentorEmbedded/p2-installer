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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Control that will animate a sequence of images and display optional text.
 */
public class AnimateControl extends Canvas {
    /** Default animation delay */
    private final int DEFAULT_ANIMATION_DELAY = 150;
    /** Margin between animation and text */
    private final int TEXT_MARGIN = 4;
	/** Text drawing flags */
	private final int TEXT_FLAGS = SWT.DRAW_TRANSPARENT | SWT.DRAW_MNEMONIC;
	/** Images for animation */
	private Image[] images;
	/** Width of image */
	private int imageWidth;
	/** Height of image */
	private int imageHeight;
	/** Current image */
	private int currentImage = 0;
    /** Animation timer */
    private Timer animationTimer;
    /** Animation delay */
    private int animationDelay = DEFAULT_ANIMATION_DELAY;
    /** Text to display */
    private String text = "";
	
	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param image Image to display
	 */
	public AnimateControl(Composite parent, int style, Image[] images) {
		super(parent, style);
		
		// Image attributes
		this.images = images;
		imageWidth = images[0].getImageData().width;
		imageHeight = images[0].getImageData().height;
		
		// Add paint listener
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				onPaint(e);
			}
		});
	}

	/**
	 * Sets the animation delay.
	 * 
	 * @param animationDelay Animation delay
	 */
	public void setAnimationDelay(int animationDelay) {
		this.animationDelay = animationDelay;
	}

	/**
	 * Returns the animation delay.
	 * 
	 * @return Animation delay
	 */
	public int getAnimationDelay() {
		return animationDelay;
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
	 * Returns the image to display.
	 * 
	 * @return Image
	 */
	protected Image[] getImages() {
		return images;
	}

	/**
	 * Starts or stops animation.
	 * 
	 * @param animate <code>true</code> to start animation,
	 * <code>false</code> to stop animation.
	 */
	public void animate(boolean animate) {
		if (animate) {
			if (animationTimer == null) {
				animationTimer = new Timer(true);
				animationTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						onTimer();
					}
				}, 0, getAnimationDelay());
			}
		}
		else {
			if (animationTimer != null) {
				animationTimer.cancel();
				animationTimer = null;
				currentImage = 0;
				redraw();
			}
		}
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
						if (++currentImage == images.length)
							currentImage = 0;
						redraw();
					}
				}
			});
		}
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		return computeSize(wHint, hHint);
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

	@Override
	public Point computeSize(int wHint, int hHint)
	{
		if (wHint < 0) wHint = 0;
		if (hHint < 0) hHint = 0;

		Point textSize = getTextSize(getText());
		int minWidth = Math.max(wHint, imageWidth + TEXT_MARGIN + textSize.x);
		int minHeight = Math.max(hHint, Math.max(imageHeight, textSize.y));
		
		return new Point(minWidth, minHeight);
	}
	
	/**
	 * Called to paint the control.
	 * 
	 * @param event Paint event
	 */
	private void onPaint(PaintEvent event) {
		GC gc = event.gc;

		gc.setFont(getFont());
		
		// Get the client area
		Rectangle clientArea = getClientArea();
		Point textSize = gc.textExtent(getText(), TEXT_FLAGS);
		int offset = clientArea.height / 2 - textSize.y / 2;

		// Draw image
		gc.drawImage(getImages()[currentImage], 0, 0);
		gc.setForeground(getForeground());
		// Draw text
		gc.drawText(getText(), imageWidth + TEXT_MARGIN, offset, TEXT_FLAGS);
	}
}
