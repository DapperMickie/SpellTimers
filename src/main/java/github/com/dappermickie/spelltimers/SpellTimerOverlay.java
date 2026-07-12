package github.com.dappermickie.spelltimers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

class SpellTimerOverlay extends OverlayPanel
{
	private static final int PANEL_WIDTH = 204;
	private static final int PANEL_HEIGHT = 70;
	private static final int ICON_SIZE = 32;
	private static final int PADDING = 12;
	private static final int ARC = 8;
	private static final int ICON_X_OFFSET = 2;

	private static final Color PANEL_BACKGROUND = new Color(18, 22, 26, 222);
	private static final Color PANEL_BORDER = new Color(255, 255, 255, 45);
	private static final Color TRACK = new Color(255, 255, 255, 36);
	private static final Color TEXT = new Color(245, 247, 250);
	private static final Color MUTED_TEXT = new Color(190, 198, 208);
	private static final Color IN_PROGRESS = new Color(235, 168, 55);
	private static final Color CAST_READY = new Color(77, 208, 121);
	private static final Color DONE = new Color(72, 191, 227);

	private final SpellTimerPlugin plugin;
	private final SpriteManager spriteManager;
	private final TimerComponent timerComponent = new TimerComponent();
	private final Map<Integer, BufferedImage> icons = new HashMap<>();
	private final Set<Integer> requestedIcons = new HashSet<>();

	@Inject
	SpellTimerOverlay(SpellTimerPlugin plugin, SpriteManager spriteManager)
	{
		this.plugin = plugin;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.TOP_LEFT);
		setResizable(false);
		setMinimumSize(PANEL_WIDTH);
		panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
		panelComponent.setBorder(new Rectangle(0, 0, 0, 0));
		panelComponent.setBackgroundColor(null);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SpellTimer timer = plugin.getActiveTimer();
		if (timer == null)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		timerComponent.setTimer(timer);
		panelComponent.getChildren().add(timerComponent);
		return super.render(graphics);
	}

	private BufferedImage getIcon(SpellTimer timer)
	{
		int spriteId = getSpriteId(timer.getSpellName());
		BufferedImage icon = icons.get(spriteId);
		if (icon == null && requestedIcons.add(spriteId))
		{
			spriteManager.getSpriteAsync(spriteId, 0, image -> icons.put(spriteId, image));
		}

		return icon;
	}

	private static int getSpriteId(String spellName)
	{
		switch (spellName)
		{
			case "Humidify":
				return SpriteID.LunarMagicOn.HUMIDIFY;
			case "Superglass Make":
				return SpriteID.LunarMagicOn.SUPERGLASS_MAKE;
			case "Degrime":
				return SpriteID.MagicNecroOn.DEGRIME;
			case "Spin Flax":
				return SpriteID.LunarMagicOn.SPIN_FLAX;
			case "Tan Leather":
				return SpriteID.LunarMagicOn.TAN_LEATHER;
			case "Plank Make":
				return SpriteID.LunarMagicOn.PLANK_MAKE;
			case "String Jewellery":
				return SpriteID.LunarMagicOn.STRING_JEWELLERY;
			case "Bake Pie":
				return SpriteID.LunarMagicOn.BAKE_PIE;
			default:
				return SpriteID.LunarMagicOn.SPELLBOOK_SWAP;
		}
	}

	private final class TimerComponent implements LayoutableRenderableEntity
	{
		private final Rectangle bounds = new Rectangle();
		private Point preferredLocation = new Point();
		private SpellTimer timer;

		private void setTimer(SpellTimer timer)
		{
			this.timer = timer;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			if (timer == null)
			{
				return null;
			}

			Object oldAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			Object oldTextAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
			Object oldFractionalMetrics = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
			Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
			Font oldFont = graphics.getFont();
			Color oldColor = graphics.getColor();
			Stroke oldStroke = graphics.getStroke();

			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			int x = preferredLocation.x;
			int y = preferredLocation.y;
			Color stateColor = getStateColor(timer);
			String status = getStatusText(timer);
			String action = timer.isExpired() ? plugin.getReadyText(timer) : "Working";
			String time = timer.isExpired() ? "Ready" : plugin.formatTimeRemaining(timer);

			graphics.setColor(PANEL_BACKGROUND);
			graphics.fillRoundRect(x, y, PANEL_WIDTH, PANEL_HEIGHT, ARC, ARC);
			graphics.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 36));
			graphics.fillRoundRect(x + 1, y + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, ARC, ARC);
			graphics.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 120));
			graphics.fillRoundRect(x, y, 4, PANEL_HEIGHT, ARC, ARC);
			graphics.setColor(PANEL_BORDER);
			graphics.setStroke(new BasicStroke(1f));
			graphics.drawRoundRect(x, y, PANEL_WIDTH - 1, PANEL_HEIGHT - 1, ARC, ARC);

			drawIcon(graphics, timer, x + PADDING + ICON_X_OFFSET, y + 17, stateColor);

			int textX = x + PADDING + ICON_X_OFFSET + ICON_SIZE + 12;
			int textWidth = PANEL_WIDTH - textX + x - PADDING;
			graphics.setFont(oldFont.deriveFont(Font.BOLD, 14f));
			drawShadowedClippedString(graphics, timer.getSpellName(), textX, y + 19,
				textWidth - graphics.getFontMetrics().stringWidth(status) - 8);

			graphics.setFont(oldFont.deriveFont(Font.BOLD, 10.5f));
			graphics.setColor(stateColor);
			drawShadowedRightAligned(graphics, status, x + PANEL_WIDTH - PADDING, y + 19, stateColor);

			graphics.setFont(oldFont.deriveFont(12f));
			graphics.setColor(MUTED_TEXT);
			drawShadowedClippedString(graphics, action, textX, y + 38, textWidth - 44, MUTED_TEXT);
			graphics.setColor(TEXT);
			drawShadowedRightAligned(graphics, time, x + PANEL_WIDTH - PADDING, y + 38, TEXT);

			drawProgress(graphics, textX, y + 52, PANEL_WIDTH - (textX - x) - PADDING, 7, stateColor);

			restoreRenderingHint(graphics, RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
			restoreRenderingHint(graphics, RenderingHints.KEY_TEXT_ANTIALIASING, oldTextAntialiasing);
			restoreRenderingHint(graphics, RenderingHints.KEY_FRACTIONALMETRICS, oldFractionalMetrics);
			restoreRenderingHint(graphics, RenderingHints.KEY_INTERPOLATION, oldInterpolation);
			graphics.setFont(oldFont);
			graphics.setColor(oldColor);
			graphics.setStroke(oldStroke);

			Dimension dimension = new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
			bounds.setLocation(preferredLocation);
			bounds.setSize(dimension);
			return dimension;
		}

		private void restoreRenderingHint(Graphics2D graphics, RenderingHints.Key key, Object value)
		{
			if (value == null)
			{
				graphics.getRenderingHints().remove(key);
				return;
			}

			graphics.setRenderingHint(key, value);
		}

		private void drawIcon(Graphics2D graphics, SpellTimer timer, int x, int y, Color stateColor)
		{
			graphics.setColor(new Color(0, 0, 0, 90));
			graphics.fillRoundRect(x - 4, y - 4, ICON_SIZE + 8, ICON_SIZE + 8, ARC, ARC);
			graphics.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 90));
			graphics.drawRoundRect(x - 4, y - 4, ICON_SIZE + 8, ICON_SIZE + 8, ARC, ARC);

			BufferedImage icon = getIcon(timer);
			if (icon != null)
			{
				graphics.drawImage(icon, x + 1, y + 1, ICON_SIZE - 2, ICON_SIZE - 2, null);
				return;
			}

			graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 18f));
			graphics.setColor(stateColor);
			String initial = timer.getSpellName().substring(0, 1);
			FontMetrics metrics = graphics.getFontMetrics();
			drawShadowedString(graphics, initial, x + (ICON_SIZE - metrics.stringWidth(initial)) / 2, y + 23, stateColor);
		}

		private void drawProgress(Graphics2D graphics, int x, int y, int width, int height, Color stateColor)
		{
			double progress = timer.isExpired()
				? 1d
				: (double) (timer.getTotalTicks() - timer.getTicksRemaining()) / timer.getTotalTicks();
			int filled = (int) Math.round(width * Math.max(0d, Math.min(1d, progress)));

			graphics.setColor(TRACK);
			graphics.fillRoundRect(x, y, width, height, height, height);
			graphics.setColor(stateColor);
			graphics.fillRoundRect(x, y, filled, height, height, height);
		}

		private Color getStateColor(SpellTimer timer)
		{
			if (!timer.isExpired())
			{
				return IN_PROGRESS;
			}

			return "Bank".equals(timer.getReadyAction()) ? DONE : CAST_READY;
		}

		private String getStatusText(SpellTimer timer)
		{
			if (!timer.isExpired())
			{
				return "IN PROGRESS";
			}

			return "Bank".equals(timer.getReadyAction()) ? "DONE" : "CAST";
		}

		private void drawShadowedClippedString(Graphics2D graphics, String text, int x, int y, int maxWidth)
		{
			drawShadowedClippedString(graphics, text, x, y, maxWidth, TEXT);
		}

		private void drawShadowedClippedString(Graphics2D graphics, String text, int x, int y, int maxWidth, Color color)
		{
			String clipped = clipText(graphics, text, maxWidth);
			drawShadowedString(graphics, clipped, x, y, color);
		}

		private String clipText(Graphics2D graphics, String text, int maxWidth)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			if (metrics.stringWidth(text) <= maxWidth)
			{
				return text;
			}

			String ellipsis = "...";
			int ellipsisWidth = metrics.stringWidth(ellipsis);
			String clipped = text;
			while (!clipped.isEmpty() && metrics.stringWidth(clipped) + ellipsisWidth > maxWidth)
			{
				clipped = clipped.substring(0, clipped.length() - 1);
			}
			return clipped + ellipsis;
		}

		private void drawShadowedRightAligned(Graphics2D graphics, String text, int rightX, int y, Color color)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			drawShadowedString(graphics, text, rightX - metrics.stringWidth(text), y, color);
		}

		private void drawShadowedString(Graphics2D graphics, String text, int x, int y, Color color)
		{
			graphics.setColor(new Color(0, 0, 0, 190));
			graphics.drawString(text, x + 1, y + 1);
			graphics.setColor(color);
			graphics.drawString(text, x, y);
		}

		@Override
		public void setPreferredLocation(Point preferredLocation)
		{
			this.preferredLocation = preferredLocation;
		}

		@Override
		public void setPreferredSize(Dimension preferredSize)
		{
			// Fixed-size overlay component.
		}

		@Override
		public Rectangle getBounds()
		{
			return bounds;
		}
	}
}
