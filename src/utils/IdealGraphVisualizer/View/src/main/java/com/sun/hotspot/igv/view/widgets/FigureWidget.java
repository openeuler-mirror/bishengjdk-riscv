/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.view.widgets;

import com.sun.hotspot.igv.data.InputGraph;
import com.sun.hotspot.igv.data.Properties;
import com.sun.hotspot.igv.data.services.GraphViewer;
import com.sun.hotspot.igv.graph.Figure;
import com.sun.hotspot.igv.util.DoubleClickAction;
import com.sun.hotspot.igv.util.DoubleClickHandler;
import com.sun.hotspot.igv.util.PropertiesConverter;
import com.sun.hotspot.igv.util.PropertiesSheet;
import com.sun.hotspot.igv.view.DiagramScene;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.LayoutFactory.SerialAlignment;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Thomas Wuerthinger
 */
public class FigureWidget extends Widget implements Properties.Provider, PopupMenuProvider, DoubleClickHandler {

    public static final boolean VERTICAL_LAYOUT = true;
    private static final double LABEL_ZOOM_FACTOR = 0.3;
    private Figure figure;
    private Widget leftWidget;
    private Widget rightWidget;
    private Widget middleWidget;
    private ArrayList<LabelWidget> labelWidgets;
    private DiagramScene diagramScene;
    private boolean boundary;
    private final Node node;
    private Widget dummyTop;
    private static final Image warningSign = ImageUtilities.loadImage("com/sun/hotspot/igv/view/images/warning.png");

    public void setBoundary(boolean b) {
        boundary = b;
    }

    public boolean isBoundary() {
        return boundary;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        return middleWidget.isHitAt(localLocation);
    }

    public FigureWidget(final Figure f, WidgetAction hoverAction, WidgetAction selectAction, DiagramScene scene, Widget parent) {
        super(scene);

        assert this.getScene() != null;
        assert this.getScene().getView() != null;

        this.figure = f;
        this.setCheckClipping(true);
        this.diagramScene = scene;
        parent.addChild(this);

        Widget outer = new Widget(scene);
        outer.setBackground(f.getColor());
        outer.setLayout(LayoutFactory.createOverlayLayout());

        middleWidget = new Widget(scene);
        SerialAlignment textAlign = scene.getModel().getShowCFG() ?
            LayoutFactory.SerialAlignment.LEFT_TOP :
            LayoutFactory.SerialAlignment.CENTER;
        middleWidget.setLayout(LayoutFactory.createVerticalFlowLayout(textAlign, 0));
        middleWidget.setBackground(f.getColor());
        middleWidget.setOpaque(true);
        middleWidget.getActions().addAction(new DoubleClickAction(this));
        middleWidget.setCheckClipping(true);

        dummyTop = new Widget(scene);
        int extraTopHeight =
            getFigure().getDiagram().isCFG() && getFigure().hasNamedInputSlot() ?
            Figure.TOP_CFG_HEIGHT : 0;
        dummyTop.setMinimumSize(new Dimension(Figure.INSET / 2, 1 + extraTopHeight));
        middleWidget.addChild(dummyTop);

        // This widget includes the node text and possibly a warning sign to the right.
        Widget nodeInfoWidget = new Widget(scene);
        nodeInfoWidget.setLayout(LayoutFactory.createAbsoluteLayout());
        middleWidget.addChild(nodeInfoWidget);

        Widget textWidget = new Widget(scene);
        textWidget.setLayout(LayoutFactory.createVerticalFlowLayout(textAlign, 0));
        nodeInfoWidget.addChild(textWidget);

        String[] strings = figure.getLines();
        labelWidgets = new ArrayList<>(strings.length);

        for (String displayString : strings) {
            LabelWidget lw = new LabelWidget(scene);
            labelWidgets.add(lw);
            textWidget.addChild(lw);
            lw.setLabel(displayString);
            lw.setFont(figure.getDiagram().getFont());
            lw.setForeground(getTextColor());
            lw.setAlignment(LabelWidget.Alignment.CENTER);
            lw.setVerticalAlignment(LabelWidget.VerticalAlignment.CENTER);
            lw.setBorder(BorderFactory.createEmptyBorder());
        }

        if (getFigure().getWarning() != null) {
            ImageWidget warningWidget = new ImageWidget(scene, warningSign);
            Point warningLocation = new Point(getFigure().getWidth() - Figure.WARNING_WIDTH - Figure.INSET / 2, 0);
            warningWidget.setPreferredLocation(warningLocation);
            warningWidget.setToolTipText(getFigure().getWarning());
            nodeInfoWidget.addChild(warningWidget);
        }

        Widget dummyBottom = new Widget(scene);
        int extraBottomHeight =
            getFigure().getDiagram().isCFG() && getFigure().hasNamedOutputSlot() ?
            Figure.BOTTOM_CFG_HEIGHT : 0;
        dummyBottom.setMinimumSize(new Dimension(Figure.INSET / 2, 1  + extraBottomHeight));
        middleWidget.addChild(dummyBottom);

        middleWidget.setPreferredBounds(new Rectangle(0, Figure.getVerticalOffset(), f.getWidth(), f.getHeight()));
        this.addChild(middleWidget);

        // Initialize node for property sheet
        node = new AbstractNode(Children.LEAF) {

            @Override
            protected Sheet createSheet() {
                Sheet s = super.createSheet();
                PropertiesSheet.initializeSheet(f.getProperties(), s);
                return s;
            }
        };
        node.setDisplayName(getName());

        this.setToolTipText(PropertiesConverter.convertToHTML(f.getProperties()));
    }

    public Widget getLeftWidget() {
        return leftWidget;
    }

    public Widget getRightWidget() {
        return rightWidget;
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        super.notifyStateChanged(previousState, state);

        Font font = this.figure.getDiagram().getFont();
        int thickness = 1;
        if (state.isSelected()) {
            font = this.figure.getDiagram().getBoldFont();
            thickness = 2;
        }

        Color borderColor = Color.BLACK;
        Color innerBorderColor = getFigure().getColor();
        if (state.isHighlighted()) {
            innerBorderColor = borderColor = Color.BLUE;
        }

        middleWidget.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, thickness), BorderFactory.createLineBorder(innerBorderColor, 1)));
        for (LabelWidget labelWidget : labelWidgets) {
            labelWidget.setFont(font);
        }
        repaint();
    }

    public String getName() {
        return getProperties().get("name");
    }

    @Override
    public Properties getProperties() {
        return figure.getProperties();
    }

    public Figure getFigure() {
        return figure;
    }

    private Color getTextColor() {
        Color bg = figure.getColor();
        double brightness = bg.getRed() * 0.21 + bg.getGreen() * 0.72 + bg.getBlue() * 0.07;
        if (brightness < 150) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }

    @Override
    protected void paintChildren() {
        Composite oldComposite = null;
        if (boundary) {
            oldComposite = getScene().getGraphics().getComposite();
            float alpha = DiagramScene.ALPHA;
            this.getScene().getGraphics().setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        if (diagramScene.getZoomFactor() < LABEL_ZOOM_FACTOR) {
            for (LabelWidget labelWidget : labelWidgets) {
                labelWidget.setVisible(false);
            }
            super.paintChildren();
            for (LabelWidget labelWidget : labelWidgets) {
                labelWidget.setVisible(true);
            }
        } else {
            Color oldColor = null;
            if (boundary) {
                for (LabelWidget labelWidget : labelWidgets) {
                    oldColor = labelWidget.getForeground();
                    labelWidget.setForeground(Color.BLACK);
                }
            }
            super.paintChildren();
            if (boundary) {
                for (LabelWidget labelWidget : labelWidgets) {
                    labelWidget.setForeground(oldColor);
                }
            }
        }

        if (boundary) {
            getScene().getGraphics().setComposite(oldComposite);
        }
    }

    @Override
    public JPopupMenu getPopupMenu(Widget widget, Point point) {
        JPopupMenu menu = diagramScene.createPopupMenu();
        menu.addSeparator();

        build(menu, getFigure(), this, false, diagramScene);
        menu.addSeparator();
        build(menu, getFigure(), this, true, diagramScene);

        return menu;
    }

    public static void build(JPopupMenu menu, Figure figure, FigureWidget figureWidget, boolean successors, DiagramScene diagramScene) {
        Set<Figure> set = figure.getPredecessorSet();
        if (successors) {
            set = figure.getSuccessorSet();
        }

        boolean first = true;
        for (Figure f : set) {
            if (f == figure) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                menu.addSeparator();
            }

            Action go = diagramScene.createGotoAction(f);
            menu.add(go);

            JMenu preds = new JMenu("Nodes Above");
            preds.addMenuListener(figureWidget.new NeighborMenuListener(preds, f, false));
            menu.add(preds);

            JMenu succs = new JMenu("Nodes Below");
            succs.addMenuListener(figureWidget.new NeighborMenuListener(succs, f, true));
            menu.add(succs);
        }

        if (figure.getPredecessorSet().isEmpty() && figure.getSuccessorSet().isEmpty()) {
            menu.add("(none)");
        }
    }

    /**
     * Builds the submenu for a figure's neighbors on demand.
     */
    public class NeighborMenuListener implements MenuListener {

        private final JMenu menu;
        private final Figure figure;
        private final boolean successors;

        public NeighborMenuListener(JMenu menu, Figure figure, boolean successors) {
            this.menu = menu;
            this.figure = figure;
            this.successors = successors;
        }

        @Override
        public void menuSelected(MenuEvent e) {
            if (menu.getItemCount() > 0) {
                // already built before
                return;
            }

            build(menu.getPopupMenu(), figure, FigureWidget.this, successors, diagramScene);
        }

        @Override
        public void menuDeselected(MenuEvent e) {
            // ignore
        }

        @Override
        public void menuCanceled(MenuEvent e) {
            // ignore
        }
    }

    @Override
    public void handleDoubleClick(Widget w, WidgetAction.WidgetMouseEvent e) {
        if (diagramScene.isAllVisible()) {
            final Set<Integer> hiddenNodes = new HashSet<>(diagramScene.getModel().getGroup().getAllNodes());
            hiddenNodes.remove(this.getFigure().getInputNode().getId());
            this.diagramScene.getModel().setHiddenNodes(hiddenNodes);
        } else if (isBoundary()) {
            final Set<Integer> hiddenNodes = new HashSet<>(diagramScene.getModel().getHiddenNodes());
            hiddenNodes.remove(this.getFigure().getInputNode().getId());
            this.diagramScene.getModel().setHiddenNodes(hiddenNodes);
        } else {
            final Set<Integer> hiddenNodes = new HashSet<>(diagramScene.getModel().getHiddenNodes());
            hiddenNodes.add(this.getFigure().getInputNode().getId());
            this.diagramScene.getModel().setHiddenNodes(hiddenNodes);
        }
    }
}
