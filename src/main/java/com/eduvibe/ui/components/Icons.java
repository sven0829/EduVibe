package com.eduvibe.ui.components;

import com.eduvibe.ui.Theme;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/**
 * NEW FILE - com.eduvibe.ui.components.Icons
 *
 * Vector icons drawn with Java2D on a 24x24 grid.
 * No image files or icon fonts are needed.
 */
public final class Icons {

    private Icons() {
    }

    public enum Type {
        HOME, HEART, HEART_FILLED, BOOK, SPARKLE, PLAY, PLUS, COIN, BELL, BOOKMARK,
        SEARCH, USER, GEAR, SHIELD, LOGOUT, CAP, LOCK, MAIL, AT, EYE, EYE_OFF,
        CLOCK, CHECK, CLOSE, INFO, ALERT, ARROW_LEFT, USERS, COMMENT, SHARE, FLAG,
        SMILE, TROPHY, FLAME, STAR, KEY, SEND, IMAGE, EDIT, TRASH, GIFT, DOWNLOAD
    }

    public static Icon icon(Type type, int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                paint(g2, type, x, y, size, color);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    public static void paint(Graphics2D g, Type type, float x, float y, float size, Color color) {

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.antialias(g2);

        g2.translate(x, y);
        g2.scale(size / 24.0, size / 24.0);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {

            case HOME -> {
                Path2D p = new Path2D.Float();
                p.moveTo(3, 11);
                p.lineTo(12, 3.5);
                p.lineTo(21, 11);
                g2.draw(p);
                Path2D b = new Path2D.Float();
                b.moveTo(5.5, 9.5);
                b.lineTo(5.5, 20);
                b.lineTo(10, 20);
                b.lineTo(10, 14.5);
                b.lineTo(14, 14.5);
                b.lineTo(14, 20);
                b.lineTo(18.5, 20);
                b.lineTo(18.5, 9.5);
                g2.draw(b);
            }

            case HEART, HEART_FILLED -> {
                Shape heart = heart();
                if (type == Type.HEART_FILLED) {
                    g2.fill(heart);
                }
                g2.draw(heart);
            }

            case BOOK -> {
                Path2D p = new Path2D.Float();
                p.moveTo(12, 6);
                p.curveTo(10, 4.5, 6, 4, 3, 5);
                p.lineTo(3, 19);
                p.curveTo(6, 18, 10, 18.5, 12, 20);
                p.curveTo(14, 18.5, 18, 18, 21, 19);
                p.lineTo(21, 5);
                p.curveTo(18, 4, 14, 4.5, 12, 6);
                p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Double(12, 6, 12, 20));
            }

            case SPARKLE -> {
                Path2D p = new Path2D.Float();
                p.moveTo(12, 3);
                p.lineTo(14.2, 9.8);
                p.lineTo(21, 12);
                p.lineTo(14.2, 14.2);
                p.lineTo(12, 21);
                p.lineTo(9.8, 14.2);
                p.lineTo(3, 12);
                p.lineTo(9.8, 9.8);
                p.closePath();
                g2.draw(p);
            }

            case PLAY -> {
                g2.draw(new RoundRectangle2D.Double(3, 5, 18, 14, 5, 5));
                Path2D t = new Path2D.Float();
                t.moveTo(10.2, 9);
                t.lineTo(15.5, 12);
                t.lineTo(10.2, 15);
                t.closePath();
                g2.fill(t);
            }

            case PLUS -> {
                g2.draw(new RoundRectangle2D.Double(3, 3, 18, 18, 6, 6));
                g2.draw(new Line2D.Double(12, 8, 12, 16));
                g2.draw(new Line2D.Double(8, 12, 16, 12));
            }

            case COIN -> {
                g2.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g2.draw(new Ellipse2D.Double(7.5, 7.5, 9, 9));
            }

            case BELL -> {
                Path2D p = new Path2D.Float();
                p.moveTo(6, 17);
                p.lineTo(6, 11);
                p.curveTo(6, 7.5, 8.7, 4.8, 12, 4.8);
                p.curveTo(15.3, 4.8, 18, 7.5, 18, 11);
                p.lineTo(18, 17);
                p.lineTo(20, 18.5);
                p.lineTo(4, 18.5);
                p.closePath();
                g2.draw(p);
                Path2D clapper = new Path2D.Float();
                clapper.moveTo(10, 21);
                clapper.quadTo(12, 22.5, 14, 21);
                g2.draw(clapper);
            }

            case BOOKMARK -> {
                Path2D p = new Path2D.Float();
                p.moveTo(6.5, 3.5);
                p.lineTo(17.5, 3.5);
                p.lineTo(17.5, 20.5);
                p.lineTo(12, 16);
                p.lineTo(6.5, 20.5);
                p.closePath();
                g2.draw(p);
            }

            case SEARCH -> {
                g2.draw(new Ellipse2D.Double(4, 4, 12, 12));
                g2.draw(new Line2D.Double(14.5, 14.5, 20.5, 20.5));
            }

            case USER -> {
                g2.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
                Path2D p = new Path2D.Float();
                p.moveTo(4.5, 20.5);
                p.curveTo(4.5, 16.5, 8, 14, 12, 14);
                p.curveTo(16, 14, 19.5, 16.5, 19.5, 20.5);
                g2.draw(p);
            }

            case USERS -> {
                g2.draw(new Ellipse2D.Double(7, 4, 7, 7));
                Path2D p = new Path2D.Float();
                p.moveTo(2.5, 20);
                p.curveTo(2.5, 16.5, 5.5, 14.5, 10.5, 14.5);
                p.curveTo(15.5, 14.5, 18.5, 16.5, 18.5, 20);
                g2.draw(p);
                Path2D q = new Path2D.Float();
                q.moveTo(16, 4.5);
                q.curveTo(18.5, 5, 19.5, 7.5, 18.5, 9.5);
                g2.draw(q);
                Path2D r = new Path2D.Float();
                r.moveTo(18.5, 14.8);
                r.curveTo(20.5, 15.5, 21.5, 17.5, 21.5, 20);
                g2.draw(r);
            }

            case GEAR -> {
                g2.draw(new Ellipse2D.Double(9, 9, 6, 6));
                g2.draw(new Ellipse2D.Double(5, 5, 14, 14));
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    float x1 = (float) (12 + Math.cos(a) * 7);
                    float y1 = (float) (12 + Math.sin(a) * 7);
                    float x2 = (float) (12 + Math.cos(a) * 9.8);
                    float y2 = (float) (12 + Math.sin(a) * 9.8);
                    g2.draw(new Line2D.Double(x1, y1, x2, y2));
                }
            }

            case SHIELD -> {
                Path2D p = new Path2D.Float();
                p.moveTo(12, 3);
                p.lineTo(20, 6);
                p.lineTo(20, 12);
                p.curveTo(20, 17, 16.5, 20, 12, 21.5);
                p.curveTo(7.5, 20, 4, 17, 4, 12);
                p.lineTo(4, 6);
                p.closePath();
                g2.draw(p);
                Path2D tick = new Path2D.Float();
                tick.moveTo(8.5, 12);
                tick.lineTo(11, 14.5);
                tick.lineTo(15.5, 9.5);
                g2.draw(tick);
            }

            case LOGOUT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(9.5, 4);
                p.lineTo(5, 4);
                p.lineTo(5, 20);
                p.lineTo(9.5, 20);
                g2.draw(p);
                g2.draw(new Line2D.Double(10, 12, 21, 12));
                Path2D a = new Path2D.Float();
                a.moveTo(17, 8);
                a.lineTo(21, 12);
                a.lineTo(17, 16);
                g2.draw(a);
            }

            case CAP -> {
                Path2D top = new Path2D.Float();
                top.moveTo(12, 3);
                top.lineTo(22.5, 8.5);
                top.lineTo(12, 14);
                top.lineTo(1.5, 8.5);
                top.closePath();
                g2.fill(top);
                Path2D base = new Path2D.Float();
                base.moveTo(6, 12);
                base.lineTo(6, 16.5);
                base.curveTo(6, 18.5, 9, 20, 12, 20);
                base.curveTo(15, 20, 18, 18.5, 18, 16.5);
                base.lineTo(18, 12);
                g2.draw(base);
                g2.draw(new Line2D.Double(22.5, 9, 22.5, 15));
            }

            case LOCK -> {
                g2.draw(new RoundRectangle2D.Double(5, 11, 14, 10, 4, 4));
                Path2D p = new Path2D.Float();
                p.moveTo(8, 11);
                p.lineTo(8, 8);
                p.curveTo(8, 2.5, 16, 2.5, 16, 8);
                p.lineTo(16, 11);
                g2.draw(p);
            }

            case MAIL -> {
                g2.draw(new RoundRectangle2D.Double(3, 5, 18, 14, 4, 4));
                Path2D p = new Path2D.Float();
                p.moveTo(3.8, 7.5);
                p.lineTo(12, 13.2);
                p.lineTo(20.2, 7.5);
                g2.draw(p);
            }

            case AT -> {
                g2.draw(new Ellipse2D.Double(8.2, 8.2, 7.6, 7.6));
                Path2D p = new Path2D.Float();
                p.moveTo(15.8, 9.5);
                p.lineTo(15.8, 13.2);
                p.curveTo(15.8, 16, 20.5, 16, 20.5, 12);
                p.curveTo(20.5, 7, 16.8, 3.5, 12, 3.5);
                p.curveTo(7, 3.5, 3.5, 7.2, 3.5, 12);
                p.curveTo(3.5, 17, 7.2, 20.5, 12, 20.5);
                p.curveTo(14, 20.5, 15.6, 20, 17, 19);
                g2.draw(p);
            }

            case EYE, EYE_OFF -> {
                Path2D p = new Path2D.Float();
                p.moveTo(2, 12);
                p.curveTo(5, 6.5, 8.5, 5, 12, 5);
                p.curveTo(15.5, 5, 19, 6.5, 22, 12);
                p.curveTo(19, 17.5, 15.5, 19, 12, 19);
                p.curveTo(8.5, 19, 5, 17.5, 2, 12);
                p.closePath();
                g2.draw(p);
                g2.draw(new Ellipse2D.Double(9, 9, 6, 6));
                if (type == Type.EYE_OFF) {
                    g2.draw(new Line2D.Double(4, 4, 20, 20));
                }
            }

            case CLOCK -> {
                g2.draw(new Ellipse2D.Double(3, 3, 18, 18));
                Path2D p = new Path2D.Float();
                p.moveTo(12, 7);
                p.lineTo(12, 12);
                p.lineTo(15.5, 14);
                g2.draw(p);
            }

            case CHECK -> {
                Path2D p = new Path2D.Float();
                p.moveTo(5, 12.5);
                p.lineTo(10, 17.5);
                p.lineTo(19, 7);
                g2.draw(p);
            }

            case CLOSE -> {
                g2.draw(new Line2D.Double(6, 6, 18, 18));
                g2.draw(new Line2D.Double(18, 6, 6, 18));
            }

            case SMILE -> {
                g2.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g2.draw(new Line2D.Double(9, 10, 9, 10.2));
                g2.draw(new Line2D.Double(15, 10, 15, 10.2));
                g2.draw(new Arc2D.Double(7.5, 8.5, 9, 9, 200, 140, Arc2D.OPEN));
            }

            case TROPHY -> {
                Path2D cup = new Path2D.Float();
                cup.moveTo(7, 4);
                cup.lineTo(17, 4);
                cup.lineTo(17, 9);
                cup.curveTo(17, 12, 14.8, 14, 12, 14);
                cup.curveTo(9.2, 14, 7, 12, 7, 9);
                cup.closePath();
                g2.draw(cup);
                g2.draw(new Arc2D.Double(2.5, 5, 5, 5, 90, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(16.5, 5, 5, 5, 90, -180, Arc2D.OPEN));
                g2.draw(new Line2D.Double(12, 14, 12, 18));
                g2.draw(new Line2D.Double(8, 20, 16, 20));
            }

            case FLAME -> {
                Path2D f = new Path2D.Float();
                f.moveTo(12, 3);
                f.curveTo(12, 7, 17.5, 9, 17.5, 14.5);
                f.curveTo(17.5, 18, 15, 20.5, 12, 20.5);
                f.curveTo(9, 20.5, 6.5, 18, 6.5, 14.5);
                f.curveTo(6.5, 12, 8, 10.5, 8.5, 8.5);
                f.curveTo(10, 9.5, 11.5, 8, 12, 3);
                f.closePath();
                g2.draw(f);
            }

            case STAR -> {
                Path2D st = new Path2D.Float();
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 9 : 4;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    double px = 12 + r * Math.cos(a);
                    double py = 12.5 + r * Math.sin(a);
                    if (i == 0) {
                        st.moveTo(px, py);
                    } else {
                        st.lineTo(px, py);
                    }
                }
                st.closePath();
                g2.draw(st);
            }

            case KEY -> {
                g2.draw(new Ellipse2D.Double(3.5, 8.5, 7, 7));
                g2.draw(new Line2D.Double(10.5, 12, 20.5, 12));
                g2.draw(new Line2D.Double(17, 12, 17, 15.5));
                g2.draw(new Line2D.Double(20.5, 12, 20.5, 14.5));
            }

            case INFO -> {
                g2.draw(new Ellipse2D.Double(3, 3, 18, 18));
                g2.draw(new Line2D.Double(12, 11, 12, 16.5));
                g2.draw(new Line2D.Double(12, 7.7, 12, 7.8));
            }

            case ALERT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(12, 3.5);
                p.lineTo(21.5, 20);
                p.lineTo(2.5, 20);
                p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Double(12, 9.5, 12, 14));
                g2.draw(new Line2D.Double(12, 16.9, 12, 17));
            }

            case ARROW_LEFT -> {
                g2.draw(new Line2D.Double(19, 12, 5, 12));
                Path2D p = new Path2D.Float();
                p.moveTo(11, 6);
                p.lineTo(5, 12);
                p.lineTo(11, 18);
                g2.draw(p);
            }

            case COMMENT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(4, 5.5);
                p.lineTo(20, 5.5);
                p.lineTo(20, 16);
                p.lineTo(11, 16);
                p.lineTo(6.5, 20);
                p.lineTo(6.5, 16);
                p.lineTo(4, 16);
                p.closePath();
                g2.draw(p);
            }

            case SHARE -> {
                g2.draw(new Line2D.Double(8.5, 11, 15.5, 7));
                g2.draw(new Line2D.Double(8.5, 13, 15.5, 17));
                g2.fill(new Ellipse2D.Double(15.5, 3.5, 5, 5));
                g2.fill(new Ellipse2D.Double(3.5, 9.5, 5, 5));
                g2.fill(new Ellipse2D.Double(15.5, 15.5, 5, 5));
            }

            case DOWNLOAD -> {
                g2.draw(new Line2D.Double(12, 4, 12, 15));
                Path2D arrow = new Path2D.Float();
                arrow.moveTo(7, 11);
                arrow.lineTo(12, 16);
                arrow.lineTo(17, 11);
                g2.draw(arrow);
                g2.draw(new Line2D.Double(5, 20, 19, 20));
            }

            case SEND -> {
                Path2D p = new Path2D.Float();
                p.moveTo(21, 3);
                p.lineTo(3, 10.5);
                p.lineTo(10.5, 13.5);
                p.lineTo(13.5, 21);
                p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Double(21, 3, 10.5, 13.5));
            }

            case IMAGE -> {
                g2.draw(new RoundRectangle2D.Double(3.5, 4.5, 17, 15, 3, 3));
                g2.fill(new Ellipse2D.Double(7, 8, 3, 3));
                Path2D p = new Path2D.Float();
                p.moveTo(4, 17);
                p.lineTo(9.5, 12.5);
                p.lineTo(13, 15.5);
                p.lineTo(16, 13);
                p.lineTo(20, 17);
                g2.draw(p);
            }

            case EDIT -> {
                Path2D p = new Path2D.Float();
                p.moveTo(4, 20);
                p.lineTo(4.8, 15.6);
                p.lineTo(16.5, 3.9);
                p.lineTo(20.1, 7.5);
                p.lineTo(8.4, 19.2);
                p.closePath();
                g2.draw(p);
                g2.draw(new Line2D.Double(14.5, 6, 18, 9.5));
            }

            case TRASH -> {
                g2.draw(new Line2D.Double(4, 7, 20, 7));
                g2.draw(new RoundRectangle2D.Double(6, 7, 12, 13, 3, 3));
                g2.draw(new Line2D.Double(9, 4, 15, 4));
                g2.draw(new Line2D.Double(10, 11, 10, 16));
                g2.draw(new Line2D.Double(14, 11, 14, 16));
            }

            case GIFT -> {
                g2.draw(new RoundRectangle2D.Double(4, 10, 16, 10, 2, 2));
                g2.draw(new RoundRectangle2D.Double(3, 7, 18, 3.5, 1.5, 1.5));
                g2.draw(new Line2D.Double(12, 7, 12, 20));
                g2.draw(new Ellipse2D.Double(7.5, 3, 4.5, 4));
                g2.draw(new Ellipse2D.Double(12, 3, 4.5, 4));
            }

            case FLAG -> {
                g2.draw(new Line2D.Double(5, 3, 5, 21));
                Path2D p = new Path2D.Float();
                p.moveTo(5, 4);
                p.lineTo(19, 4);
                p.lineTo(16, 9);
                p.lineTo(19, 14);
                p.lineTo(5, 14);
                g2.draw(p);
            }
        }

        g2.dispose();
    }

    private static Shape heart() {
        Path2D p = new Path2D.Float();
        p.moveTo(12, 20.5);
        p.curveTo(4, 14.5, 2.5, 10.5, 2.5, 7.8);
        p.curveTo(2.5, 5.2, 4.5, 3.5, 7, 3.5);
        p.curveTo(9.2, 3.5, 11, 4.8, 12, 6.5);
        p.curveTo(13, 4.8, 14.8, 3.5, 17, 3.5);
        p.curveTo(19.5, 3.5, 21.5, 5.2, 21.5, 7.8);
        p.curveTo(21.5, 10.5, 20, 14.5, 12, 20.5);
        p.closePath();
        return p;
    }
}
