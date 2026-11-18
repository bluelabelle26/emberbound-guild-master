package com.emberbound;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class CovenantScroll extends Overlay
{
    private static final Color INK = new Color(53, 33, 24);
    private static final Color PAPER = new Color(183, 151, 103);
    private static final Color DARK_PAPER = new Color(158, 119, 76);
    private static final Color WAX = new Color(126, 36, 29);
    private static final Font CALLIGRAPHY = calligraphy();
    private static final Font TITLE = CALLIGRAPHY.deriveFont(23f);

    static Font calligraphy()
    {
        Font font = new Font("Lucida Calligraphy", Font.PLAIN, 23);
        return font.getFamily().equals(Font.DIALOG) ? new Font(Font.SERIF, Font.ITALIC, 23) : font;
    }
    private static final Font BODY = CALLIGRAPHY.deriveFont(14f);
    private static final Font LABEL = CALLIGRAPHY.deriveFont(Font.BOLD, 14f);

    private final Client client;
    final Overlay envelope = new Envelope();
    private volatile List<Card> cards = Collections.emptyList();
    private volatile List<Hit> hits = Collections.emptyList();
    private volatile Rectangle surface = new Rectangle();
    private volatile Completion completion;
    private volatile boolean open;
    private volatile boolean reviewing;
    private volatile boolean promotion;
    private volatile boolean audience;
    private volatile int selected = -1;
    private volatile boolean refusing;
    private volatile String warning = "";
    private volatile String letter = "";
    private volatile Runnable decline;
    private boolean captured;
    private volatile Runnable closeFreelance;

    static final class Card
    {
        final String title;
        final String rank;
        final String target;
        final String reward;
        final String detail;
        final BufferedImage icon;
        final Runnable accept;
        final java.util.function.BooleanSupplier canAccept;

        Card(String title, String rank, String target, String reward, String detail, BufferedImage icon, Runnable accept)
        {
            this(title, rank, target, reward, detail, icon, accept, () -> true);
        }

        Card(String title, String rank, String target, String reward, String detail, BufferedImage icon, Runnable accept,
            java.util.function.BooleanSupplier canAccept)
        {
            this.title = title;
            this.rank = rank;
            this.target = target;
            this.reward = reward;
            this.detail = detail;
            this.icon = icon;
            this.accept = accept;
            this.canAccept = canAccept;
        }
    }

    private static final class Hit
    {
        final Rectangle area;
        final Runnable action;

        Hit(Rectangle area, Runnable action)
        {
            this.area = area;
            this.action = action;
        }
    }

    CovenantScroll(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    void showSlate(List<Card> next, String nextLetter, String nextWarning, Runnable onDecline)
    {
        clear();
        letter = nextLetter;
        warning = nextWarning;
        decline = onDecline;
        cards = List.copyOf(next);
    }

    void showFreelance(Card card, Runnable onClose)
    {
        clear();
        closeFreelance = onClose;
        cards = List.of(card);
        open = true;
        reviewing = true;
        selected = 0;
    }

    void showPromotion(List<Card> trials, String message, Runnable onClose)
    {
        clear();
        promotion = true;
        letter = message;
        closeFreelance = onClose;
        cards = List.copyOf(trials);
        open = true;
    }

    void showAudience(String message, Runnable onClose)
    {
        clear();
        audience = true;
        letter = message;
        closeFreelance = onClose;
        open = true;
    }

    private void closeSpecialView()
    {
        Runnable onClose = closeFreelance;
        clear();
        if (onClose != null)
        {
            onClose.run();
        }
    }

    void clear()
    {
        closeFreelance = null;
        promotion = false;
        audience = false;
        cards = Collections.emptyList();
        hits = Collections.emptyList();
        surface = new Rectangle();
        open = false;
        reviewing = false;
        refusing = false;
        selected = -1;
    }

    void reopenSlate()
    {
        open = !cards.isEmpty();
    }

    final KeyListener keyboard = new KeyListener()
    {
        private boolean escapeHeld;

        @Override
        public void keyPressed(KeyEvent event)
        {
            if (event.getKeyCode() != KeyEvent.VK_ESCAPE)
            {
                return;
            }
            if (escapeHeld)
            {
                event.consume();
                return;
            }
            if (!open || client.getGameState() != GameState.LOGGED_IN)
            {
                return;
            }
            escapeHeld = true;
            event.consume();
            hits = Collections.emptyList();
            if (audience || (reviewing && !promotion && closeFreelance != null))
            {
                closeSpecialView();
            }
            else if (reviewing || refusing)
            {
                reviewing = false;
                refusing = false;
                selected = -1;
            }
            else if (promotion)
            {
                closeSpecialView();
            }
            else
            {
                open = false;
            }
        }

        @Override
        public void keyReleased(KeyEvent event)
        {
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE && escapeHeld)
            {
                escapeHeld = false;
                event.consume();
            }
        }

        @Override
        public void keyTyped(KeyEvent event)
        {
            if (escapeHeld && event.getKeyChar() == KeyEvent.VK_ESCAPE)
            {
                event.consume();
            }
        }

        @Override
        public void focusLost()
        {
            escapeHeld = false;
        }
    };

    final MouseAdapter input = new MouseAdapter()
    {
        @Override
        public MouseEvent mousePressed(MouseEvent event)
        {
            captured = false;
            if (event.isAltDown() || client.getGameState() != GameState.LOGGED_IN || (cards.isEmpty() && !audience))
            {
                return event;
            }
            if (!open)
            {
                if (event.getButton() == MouseEvent.BUTTON1 && envelope.getBounds().contains(event.getPoint()))
                {
                    captured = true;
                    event.consume();
                    open = true;
                }
                return event;
            }
            if (!surface.contains(event.getPoint()))
            {
                return event;
            }
            captured = true;
            event.consume();
            if (event.getButton() == MouseEvent.BUTTON1)
            {
                for (Hit hit : hits)
                {
                    if (hit.area.contains(event.getPoint()))
                    {
                        hit.action.run();
                        break;
                    }
                }
            }
            return event;
        }

        @Override
        public MouseEvent mouseReleased(MouseEvent event)
        {
            if (captured)
            {
                event.consume();
            }
            return event;
        }

        @Override
        public MouseEvent mouseClicked(MouseEvent event)
        {
            if (captured)
            {
                captured = false;
                event.consume();
            }
            return event;
        }
    };

    private static final class Completion
    {
        final String objective;
        final String parameters;
        final String payout;
        long firstPaintNanos;

        Completion(String objective, String parameters, String payout)
        {
            this.objective = objective;
            this.parameters = parameters;
            this.payout = payout;
        }
    }

    void showCompletion(String objective, String parameters, String payout)
    {
        completion = new Completion(objective, parameters, payout);
    }

    void clearCompletion()
    {
        completion = null;
    }

    private void paintCompletion(Graphics2D original)
    {
        Completion receipt = completion;
        if (receipt == null)
        {
            return;
        }
        int width = Math.min(346, client.getCanvasWidth() - 24);
        if (width < 200)
        {
            return;
        }
        long now = System.nanoTime();
        if (receipt.firstPaintNanos == 0)
        {
            receipt.firstPaintNanos = now;
        }
        double elapsed = (now - receipt.firstPaintNanos) / 1_000_000_000.0;
        double fadeStart = .40 + 6;
        double fadeEnd = fadeStart + .45;
        if (elapsed >= fadeEnd)
        {
            return;
        }
        Graphics2D g = (Graphics2D) original.create();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float opacity = (float) Math.min(1, Math.min(elapsed / .24, (fadeEnd - elapsed) / .45));
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0, opacity)));
            int x = (client.getCanvasWidth() - width) / 2;
            int y = 52 - (int) (20 * (1 - Math.min(1, elapsed / .24)));
            g.setColor(PAPER);
            g.fillRoundRect(x + 10, y + 4, width - 20, 154, 10, 10);
            paintRoll(g, x, y, width);
            paintRoll(g, x, y + 146, width);
            cardText(g, receipt.objective, x, y + 48, width, TITLE);
            cardText(g, receipt.parameters, x, y + 73, width, BODY);
            cardText(g, receipt.payout, x, y + 96, width, LABEL);
            if (elapsed >= .26)
            {
                int centerX = x + width / 2;
                double settle = Math.min(1, (elapsed - .26) / .14);
                int stampY = y + 113 - (int) (7 * (1 - settle));
                g.setColor(WAX);
                g.fillOval(centerX - 66, stampY, 132, 27);
                g.setColor(new Color(238, 181, 111));
                g.drawOval(centerX - 63, stampY + 2, 126, 23);
                g.setFont(LABEL);
                String stamp = "COMPLETED";
                g.drawString(stamp, centerX - g.getFontMetrics().stringWidth(stamp) / 2, stampY + 18);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    @Override
    public Dimension render(Graphics2D original)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }
        List<Card> slate = cards;
        if (!open || (slate.isEmpty() && !audience))
        {
            paintCompletion(original);
            return null;
        }
        Graphics2D g = (Graphics2D) original.create();
        List<Hit> controls = new ArrayList<>();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(BODY);
            int canvasWidth = client.getCanvasWidth();
            int canvasHeight = client.getCanvasHeight();
            int width = Math.min(reviewing ? 440 : 360, canvasWidth - 28);
            if (width < 280 || canvasHeight < 330)
            {
                surface = new Rectangle();
                hits = Collections.emptyList();
                return null;
            }
            Rectangle area = new Rectangle(Math.max(14, (canvasWidth - width) / 2 - 48),
                Math.max(4, (canvasHeight - 326) / 2 - 30), width, 326);
            if (audience)
            {
                area = new Rectangle(area.x, area.y + 102, area.width, 152);
                paintCard(g, area, false);
                g.setColor(INK);
                g.setFont(TITLE);
                g.drawString("Guild Master", area.x + 18, area.y + 31);
                paragraph(g, letter, area.x + 18, area.y + 55, area.width - 36);
                button(g, new Rectangle(area.x + area.width - 87, area.y + 114, 70, 27), "Close", controls,
                    this::closeSpecialView);
            }
            else if (reviewing)
            {
                paintReview(g, area, controls, slate);
            }
            else
            {
                paintScroll(g, area, controls);
            }
            surface = area;
            hits = List.copyOf(controls);
            paintCompletion(original);
        }
        finally
        {
            g.dispose();
        }
        return null;
    }

    private final class Envelope extends Overlay
    {
        Envelope()
        {
            setPosition(OverlayPosition.TOP_LEFT);
            setLayer(OverlayLayer.ABOVE_WIDGETS);
            setPriority(PRIORITY_HIGH);
            setMovable(true);
        }

        @Override
        public Dimension render(Graphics2D g)
        {
            if (open || cards.isEmpty() || client.getGameState() != GameState.LOGGED_IN)
            {
                return null;
            }
            g.setColor(DARK_PAPER);
            g.fillRoundRect(0, 0, 34, 24, 4, 4);
            g.setColor(INK);
            g.drawRoundRect(0, 0, 33, 23, 4, 4);
            g.drawLine(1, 1, 17, 14);
            g.drawLine(32, 1, 17, 14);
            g.setColor(WAX);
            g.fillOval(12, 10, 10, 10);
            g.setFont(LABEL);
            g.setColor(PAPER);
            g.drawString("Contract", 41, 17);
            return new Dimension(103, 24);
        }
    }

    private void paintScroll(Graphics2D g, Rectangle area, List<Hit> controls)
    {
        int x = area.x;
        int y = area.y;
        int w = area.width;
        int h = area.height;
        java.awt.geom.Path2D parchment = new java.awt.geom.Path2D.Double();
        parchment.moveTo(x + 15, y + 16);
        parchment.quadTo(x + 7, y + h / 2, x + 16, y + h - 19);
        parchment.lineTo(x + w - 16, y + h - 19);
        parchment.quadTo(x + w - 7, y + h / 2, x + w - 15, y + 16);
        parchment.closePath();
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(java.awt.geom.AffineTransform.getTranslateInstance(3, 4).createTransformedShape(parchment));
        g.setPaint(new java.awt.LinearGradientPaint(x + 12, y, x + w - 12, y,
            new float[]{0f, .18f, .76f, 1f}, new Color[]{DARK_PAPER, PAPER, PAPER, DARK_PAPER}));
        g.fill(parchment);
        g.setColor(new Color(141, 94, 51));
        g.draw(parchment);
        paintRoll(g, x + 2, y + 2, w - 4);
        paintRoll(g, x + 2, y + h - 20, w - 4);
        g.setColor(INK);
        g.setFont(TITLE);
        g.drawString(promotion ? "Writ of Promotion" : "Emberbound Covenant", x + 24, y + 44);
        button(g, new Rectangle(x + w - 48, y + 23, 24, 23), "X", controls, () ->
        {
            if (promotion)
            {
                closeSpecialView();
                return;
            }
            open = false;
            reviewing = false;
            refusing = false;
            selected = -1;
        });

        if (refusing)
        {
            paragraph(g, warning, x + 24, y + 77, w - 48);
            button(g, new Rectangle(x + 24, y + 253, 142, 29), "Confirm decline", controls, decline);
            button(g, new Rectangle(x + 174, y + 253, 74, 29), "Back", controls, () -> refusing = false);
            return;
        }

        paragraph(g, letter, x + 24, y + 79, w - 48);
        g.setFont(CALLIGRAPHY.deriveFont(20f));
        g.setColor(WAX);
        String signature = "Guild Master";
        g.drawString(signature, x + w - g.getFontMetrics().stringWidth(signature) - 26, y + 146);
        button(g, new Rectangle(x + 24, y + 210, 170, 34), promotion ? "Review Trials" : "Review Contracts", controls, () -> reviewing = true);
        if (promotion)
        {
            button(g, new Rectangle(x + 24, y + 254, 124, 28), "Close Letter", controls, this::closeSpecialView);
        }
        else
        {
            button(g, new Rectangle(x + 24, y + 254, 124, 28), "Decline Slate", controls, () -> refusing = true);
        }
    }

    private void paintReview(Graphics2D g, Rectangle area, List<Hit> controls, List<Card> slate)
    {
        int x = area.x;
        int y = area.y;
        int width = area.width;
        if (selected >= 0 && selected < slate.size())
        {
            Card card = slate.get(selected);
            Rectangle frame = new Rectangle(x + 12, y + 20, width - 24, 270);
            paintCard(g, frame, promotion);
            paintSkill(g, card, frame.x + 35, y + 55);
            g.setFont(TITLE);
            g.setColor(INK);
            g.drawString(card.title, frame.x + 57, y + 62);
            g.setFont(LABEL);
            g.drawString(card.rank + "  /  " + (promotion ? card.reward : card.reward + " favour"), frame.x + 20, y + 96);
            g.drawString("Required: " + card.target, frame.x + 20, y + 122);
            paragraph(g, card.detail, frame.x + 20, y + 148, frame.width - 40);
            button(g, new Rectangle(frame.x + 20, y + 247, 122, 29), slate.size() == 1 && !promotion ? "Close" : "Back to Cards", controls, () ->
            {
                if (slate.size() == 1 && !promotion)
                {
                    if (closeFreelance != null)
                    {
                        closeSpecialView();
                    }
                    else
                    {
                        open = false;
                        reviewing = false;
                        selected = -1;
                        hits = Collections.emptyList();
                    }
                }
                else
                {
                    selected = -1;
                }
            });
            if (card.accept != null)
            {
                if (card.canAccept.getAsBoolean())
                {
                    button(g, new Rectangle(frame.x + frame.width - 104, y + 247, 84, 29), "Confirm", controls, card.accept);
                }
                else
                {
                    g.drawString("Open bank to confirm", frame.x + frame.width - 175, y + 267);
                }
            }
            return;
        }

        int gap = 12;
        int cardWidth = (width - gap * (slate.size() - 1)) / slate.size();
        for (int index = 0; index < slate.size(); index++)
        {
            Card card = slate.get(index);
            int chosen = index;
            int left = x + index * (cardWidth + gap);
            Rectangle frame = new Rectangle(left, y + 53, cardWidth, 183);
            paintCard(g, frame, promotion);
            cardText(g, card.rank, left, y + 80, cardWidth, LABEL);
            paintSkill(g, card, left + cardWidth / 2, y + 109);
            cardText(g, card.title, left, y + 145, cardWidth, LABEL);
            cardText(g, card.target, left, y + 166, cardWidth, BODY);
            cardText(g, promotion ? card.reward : card.reward + " favour", left, y + 185, cardWidth, BODY);
            cardText(g, "Review", left, y + 217, cardWidth, LABEL);
            controls.add(new Hit(frame, () -> selected = chosen));
        }
        button(g, new Rectangle(x + (width - 128) / 2, y + 253, 128, 29), "Back to Letter", controls, () -> reviewing = false);
    }

    private static void paintCard(Graphics2D g, Rectangle frame, boolean goldTrim)
    {
        int x = frame.x;
        int y = frame.y;
        int w = frame.width;
        int h = frame.height;
        java.awt.geom.Path2D edge = new java.awt.geom.Path2D.Double();
        edge.moveTo(x + 8, y + 1);
        edge.quadTo(x + w * .47, y - 2, x + w - 9, y + 2);
        edge.lineTo(x + w - 2, y + 8);
        edge.quadTo(x + w + 1, y + h * .48, x + w - 2, y + h - 8);
        edge.lineTo(x + w - 9, y + h - 2);
        edge.quadTo(x + w * .44, y + h + 1, x + 8, y + h - 2);
        edge.lineTo(x + 2, y + h - 8);
        edge.quadTo(x - 1, y + h * .54, x + 2, y + 8);
        edge.closePath();
        g.setColor(new Color(0, 0, 0, 115));
        g.fill(java.awt.geom.AffineTransform.getTranslateInstance(2, 3).createTransformedShape(edge));
        g.setPaint(new java.awt.LinearGradientPaint(x, y, x + w, y + h,
            new float[]{0f, .19f, .69f, 1f}, new Color[]{DARK_PAPER, PAPER, PAPER, DARK_PAPER}));
        g.fill(edge);
        g.setColor(new Color(98, 57, 31));
        g.draw(edge);
        if (goldTrim)
        {
            g.setStroke(new java.awt.BasicStroke(6f));
            g.setColor(new Color(255, 187, 54, 85));
            g.draw(edge);
            g.setStroke(new java.awt.BasicStroke(2f));
            g.setPaint(new java.awt.GradientPaint(x, y, new Color(255, 224, 128), x + w, y + h, new Color(174, 103, 29)));
            g.draw(edge);
            g.setStroke(new java.awt.BasicStroke(1f));
        }
        g.setColor(new Color(105, 68, 40, 125));
        for (int side : new int[]{x + 8, x + w - 9})
        {
            int direction = side == x + 8 ? 1 : -1;
            g.drawLine(side, y + 10, side + direction * 12, y + 10);
            g.drawLine(side, y + 10, side, y + 22);
            g.drawLine(side, y + h - 10, side + direction * 12, y + h - 10);
            g.drawLine(side, y + h - 10, side, y + h - 22);
        }
    }

    private static void paintSkill(Graphics2D g, Card card, int centerX, int centerY)
    {
        if (card.icon == null)
        {
            g.setColor(WAX);
            g.fillOval(centerX - 10, centerY - 10, 20, 20);
            g.setColor(new Color(247, 200, 89));
            g.drawOval(centerX - 9, centerY - 9, 18, 18);
            return;
        }
        g.drawImage(card.icon, centerX - card.icon.getWidth() / 2, centerY - card.icon.getHeight() / 2, null);
    }

    private static void cardText(Graphics2D g, String text, int x, int y, int width, Font font)
    {
        g.setColor(INK);
        g.setFont(font);
        while (g.getFontMetrics().stringWidth(text) > width - 10 && g.getFont().getSize() > 10)
        {
            g.setFont(g.getFont().deriveFont((float) g.getFont().getSize() - 1));
        }
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static void paintRoll(Graphics2D g, int x, int y, int width)
    {
        Color edge = new Color(116, 70, 37);
        g.setPaint(new java.awt.GradientPaint(x, y, PAPER, x, y + 17, edge));
        g.fillRoundRect(x, y, width, 18, 14, 14);
        g.setColor(edge);
        g.drawRoundRect(x, y, width - 1, 17, 14, 14);
        g.drawArc(x + width - 13, y + 2, 9, 13, -75, 150);
        g.drawArc(x + 4, y + 2, 9, 13, 105, 150);
    }

    private static void paragraph(Graphics2D g, String text, int x, int y, int width)
    {
        g.setFont(BODY);
        g.setColor(INK);
        String line = "";
        for (String word : text.split(" "))
        {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && g.getFontMetrics().stringWidth(next) > width)
            {
                g.drawString(line, x, y);
                y += 16;
                line = word;
            }
            else
            {
                line = next;
            }
        }
        g.drawString(line, x, y);
    }

    private static void button(Graphics2D g, Rectangle rect, String label, List<Hit> controls, Runnable action)
    {
        g.setColor(DARK_PAPER);
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 6, 6);
        g.setColor(INK);
        g.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, 6, 6);
        g.setFont(LABEL);
        g.drawString(label, rect.x + 7, rect.y + (rect.height + g.getFontMetrics().getAscent()) / 2 - 2);
        controls.add(new Hit(rect, action));
    }
}

