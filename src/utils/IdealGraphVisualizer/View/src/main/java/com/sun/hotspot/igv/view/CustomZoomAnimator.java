/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package com.sun.hotspot.igv.view;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JViewport;
import org.netbeans.api.visual.animator.Animator;
import org.netbeans.api.visual.animator.AnimatorEvent;
import org.netbeans.api.visual.animator.AnimatorListener;
import org.netbeans.api.visual.animator.SceneAnimator;

public class CustomZoomAnimator extends Animator implements AnimatorListener {
    private volatile double sourceZoom;
    private volatile double targetZoom;
    private volatile Point zoomCenter;
    private volatile double oldZoom;
    private volatile Rectangle visibleRect;
    private DiagramViewer diagramViewer;

    public CustomZoomAnimator(DiagramViewer viewer, SceneAnimator sceneAnimator) {
        super(sceneAnimator);
        this.diagramViewer = viewer;
        super.addAnimatorListener(this);
    }

    public synchronized void animateZoomFactor(double zoomFactor, Point zoomCenter) {
        assert zoomCenter != null;
        if (this.isRunning()) {
            if (this.sourceZoom < this.targetZoom && this.targetZoom < zoomFactor) {
                this.targetZoom = zoomFactor;
            } else if (this.sourceZoom > this.targetZoom && this.targetZoom > zoomFactor) {
                this.targetZoom = zoomFactor;
            }
        } else {
            this.targetZoom = zoomFactor;
            this.zoomCenter = zoomCenter;
            this.sourceZoom = this.getScene().getZoomFactor();
            this.oldZoom = this.sourceZoom;
            this.visibleRect = this.getScene().getView().getVisibleRect();
            this.start();
        }
    }

    public synchronized double getTargetZoom() {
        if (this.isRunning()) {
            return this.targetZoom;
        } else {
            return this.getScene().getZoomFactor();
        }
    }

    private synchronized void do_tick(double progress) {
        double newZoom = progress >= 1.0 ? this.targetZoom : this.sourceZoom + progress * (this.targetZoom - this.sourceZoom);
        this.getScene().setZoomFactor(newZoom);
        this.getScene().validate();

        Point location = this.getScene().getLocation();
        visibleRect.x += (int)(newZoom * (double)(location.x + this.zoomCenter.x)) - (int)(this.oldZoom * (double)(location.x + this.zoomCenter.x));
        visibleRect.y += (int)(newZoom * (double)(location.y + this.zoomCenter.y)) - (int)(this.oldZoom * (double)(location.y + this.zoomCenter.y));

        // Ensure to be within area
        visibleRect.x = Math.max(0, visibleRect.x);
        visibleRect.y = Math.max(0, visibleRect.y);

        this.getScene().getView().scrollRectToVisible(visibleRect);
        this.oldZoom = newZoom;
    }

    @Override
    public void animatorStarted(AnimatorEvent animatorEvent) {}

    @Override
    public void animatorPreTick(AnimatorEvent animatorEvent) {}

    @Override
    public void tick(double progress) {
        EventQueue.invokeLater(()->this.do_tick(progress));
    }

    @Override
    public void animatorPostTick(AnimatorEvent animatorEvent) {}

    @Override
    public void animatorFinished(AnimatorEvent animatorEvent) {
        this.diagramViewer.getZoomChangedEvent().fire();
    }

    @Override
    public void animatorReset(AnimatorEvent animatorEvent) {}
}
