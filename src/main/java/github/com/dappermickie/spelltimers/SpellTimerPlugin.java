package github.com.dappermickie.spelltimers;

import com.google.inject.Provides;
import java.util.Locale;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import static net.runelite.api.ScriptID.XPDROPS_SETDROPSIZE;

@Slf4j
@PluginDescriptor(
	name = "Spell Timers",
	description = "Shows timers for skilling spells with delayed or batched results",
	tags = {"magic", "skilling", "timer", "lunar", "arceuus"}
)
public class SpellTimerPlugin extends Plugin
{
	private static final int STANDARD_CAST_TICKS = 3;
	private static final int HUMIDIFY_TICKS = 3;
	private static final int SUPERGLASS_MAKE_TICKS = 3;
	private static final int DEGRIME_TICKS = 3;
	private static final int SPIN_FLAX_TICKS = 5;
	private static final int TAN_LEATHER_TICKS = 3;
	private static final int PLANK_MAKE_ACTIVE_TICKS = 3;
	private static final int PLANK_MAKE_AFK_TICKS_PER_LOG = 6;
	private static final int STRING_JEWELLERY_TICKS_PER_ITEM = 3;
	private static final int BAKE_PIE_TICKS_PER_ITEM = 3;
	private static final int PENDING_CAST_TIMEOUT_TICKS = 5;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SpellTimerOverlay overlay;

	@Inject
	private SpellTimerConfig config;

	@Inject
	private Notifier notifier;

	@Getter
	private SpellTimer activeTimer;

	private PendingSpell pendingSpell;

	private int pendingCastTicksRemaining;

	private int activeTimerStartTick = -1;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		log.debug("Spell Timers started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		activeTimer = null;
		pendingSpell = null;
		pendingCastTicksRemaining = 0;
		activeTimerStartTick = -1;
		log.debug("Spell Timers stopped");
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		clearReadyBankTimer(event);

		if (!isCastMenu(event))
		{
			return;
		}

		String target = clean(event.getMenuTarget());
		String option = clean(event.getMenuOption());
		String combined = option + " " + target;

		if (config.showHumidify() && containsSpell(combined, "Humidify"))
		{
			queueTimer("Humidify", "Bank", HUMIDIFY_TICKS);
			return;
		}

		if (config.showSuperglassMake() && containsSpell(combined, "Superglass Make"))
		{
			queueTimer("Superglass Make", "Bank", SUPERGLASS_MAKE_TICKS);
			return;
		}

		if (config.showDegrime() && containsSpell(combined, "Degrime"))
		{
			queueTimer("Degrime", "Bank", DEGRIME_TICKS);
			return;
		}

		if (config.showSpinFlax() && containsSpell(combined, "Spin Flax"))
		{
			queueTimer("Spin Flax", nextBatchAction(countInventoryItems("flax"), 5), SPIN_FLAX_TICKS);
			return;
		}

		if (config.showTanLeather() && containsSpell(combined, "Tan Leather"))
		{
			queueTimer("Tan Leather", nextBatchAction(countMatchingInventoryItems(SpellTimerPlugin::isUntannedHide), 5), TAN_LEATHER_TICKS);
			return;
		}

		if (config.showPlankMake() && containsSpell(combined, "Plank Make"))
		{
			startPlankMakeTimer();
			return;
		}

		if (config.showStringJewellery() && containsSpell(combined, "String Jewellery"))
		{
			int items = countMatchingInventoryItems(SpellTimerPlugin::isStringJewelleryInput);
			queueTimer("String Jewellery", "Bank", Math.max(STANDARD_CAST_TICKS, items * STRING_JEWELLERY_TICKS_PER_ITEM));
			return;
		}

		if (config.showBakePie() && containsSpell(combined, "Bake Pie"))
		{
			int items = countMatchingInventoryItems(SpellTimerPlugin::isUncookedPie);
			queueTimer("Bake Pie", "Bank", Math.max(STANDARD_CAST_TICKS, items * BAKE_PIE_TICKS_PER_ITEM));
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (pendingSpell != null && pendingCastTicksRemaining > 0)
		{
			pendingCastTicksRemaining--;
			if (pendingCastTicksRemaining == 0)
			{
				pendingSpell = null;
			}
		}

		if (activeTimer == null)
		{
			return;
		}

		if (client.getTickCount() == activeTimerStartTick)
		{
			return;
		}

		activeTimer.tick();
		if (activeTimer.isExpired() && !activeTimer.isNotificationSent())
		{
			notifyReady(activeTimer);
			activeTimer.markNotificationSent();
		}

		if (activeTimer.isExpired() && activeTimer.getHideAfterReadyTicks() > 0)
		{
			activeTimer.decrementHideAfterReadyTicks();
			if (activeTimer.getHideAfterReadyTicks() == 0)
			{
				activeTimer = null;
			}
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (pendingSpell == null || event.getSkill() != Skill.MAGIC || event.getXp() <= 0)
		{
			return;
		}

		startPendingTimer();
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (pendingSpell == null || event.getScriptId() != XPDROPS_SETDROPSIZE)
		{
			return;
		}

		int[] intStack = client.getIntStack();
		int intStackSize = client.getIntStackSize();
		if (intStackSize < 4)
		{
			return;
		}

		Widget xpDrop = client.getWidget(intStack[intStackSize - 4]);
		if (!isMagicXpDrop(xpDrop))
		{
			return;
		}

		startPendingTimer();
	}

	private void startPendingTimer()
	{
		startTimer(pendingSpell.spellName, pendingSpell.readyAction, pendingSpell.ticks);
		pendingSpell = null;
		pendingCastTicksRemaining = 0;
	}

	private static boolean isMagicXpDrop(Widget xpDrop)
	{
		if (xpDrop == null || xpDrop.getChildren() == null)
		{
			return false;
		}

		Widget[] children = xpDrop.getChildren();
		for (int i = 1; i < children.length; i++)
		{
			Widget child = children[i];
			if (child != null && child.getSpriteId() == SpriteID.Staticons.MAGIC)
			{
				return true;
			}
		}

		return false;
	}

	private void startPlankMakeTimer()
	{
		PlankMakeMode mode = config.plankMakeMode();
		int logCount = countMatchingInventoryItems(SpellTimerPlugin::isPlankMakeLog);

		if (mode == PlankMakeMode.AFK)
		{
			queueTimer("Plank Make", "Bank", Math.max(PLANK_MAKE_ACTIVE_TICKS, logCount * PLANK_MAKE_AFK_TICKS_PER_LOG));
			return;
		}

		if (mode == PlankMakeMode.BOTH)
		{
			int afkTicks = Math.max(PLANK_MAKE_ACTIVE_TICKS, logCount * PLANK_MAKE_AFK_TICKS_PER_LOG);
			queueTimer("Plank Make", "Cast / AFK " + formatTicks(afkTicks), PLANK_MAKE_ACTIVE_TICKS);
			return;
		}

		queueTimer("Plank Make", nextBatchAction(logCount, 1), PLANK_MAKE_ACTIVE_TICKS);
	}

	private void queueTimer(String spellName, String readyAction, int ticks)
	{
		pendingSpell = new PendingSpell(spellName, readyAction, Math.max(1, ticks));
		pendingCastTicksRemaining = PENDING_CAST_TIMEOUT_TICKS;
	}

	private void startTimer(String spellName, String readyAction, int ticks)
	{
		activeTimer = new SpellTimer(spellName, readyAction, Math.max(1, ticks), getReadyTimeoutTicks());
		activeTimerStartTick = client.getTickCount();
	}

	private void notifyReady(SpellTimer timer)
	{
		String action = getReadyText(timer);
		String message = timer.getSpellName() + ": " + action;
		notifier.notify(config.readyNotification(), message);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			activeTimer = null;
			pendingSpell = null;
			pendingCastTicksRemaining = 0;
			activeTimerStartTick = -1;
		}
	}

	private int getReadyTimeoutTicks()
	{
		return Math.max(1, (int) Math.ceil(config.readyTimeoutSeconds() / 0.6));
	}

	String getReadyText(SpellTimer timer)
	{
		String action = timer.getReadyAction();
		if ("Bank".equals(action))
		{
			return "Bank now";
		}

		if ("Cast".equals(action))
		{
			return "Cast again";
		}

		return action;
	}

	String formatTimeRemaining(SpellTimer timer)
	{
		if (config.timeRemainingMode() == TimeRemainingMode.TICKS)
		{
			return timer.getTicksRemaining() + "t";
		}

		return formatTicks(timer.getTicksRemaining());
	}

	private void clearReadyBankTimer(MenuOptionClicked event)
	{
		if (activeTimer == null || !activeTimer.isExpired() || !"Bank".equals(activeTimer.getReadyAction()))
		{
			return;
		}

		String option = clean(event.getMenuOption());
		if (option.equals("bank") || option.startsWith("bank "))
		{
			activeTimer = null;
		}
	}

	private static boolean isCastMenu(MenuOptionClicked event)
	{
		String option = clean(event.getMenuOption());
		return option.equals("cast") || option.startsWith("cast ");
	}

	private static boolean containsSpell(String text, String spellName)
	{
		return text.toLowerCase(Locale.ROOT).contains(spellName.toLowerCase(Locale.ROOT));
	}

	private static String clean(String text)
	{
		if (text == null)
		{
			return "";
		}

		return text.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
	}

	private String nextBatchAction(int itemCount, int itemsPerCast)
	{
		return itemCount > itemsPerCast ? "Cast" : "Bank";
	}

	private int countInventoryItems(String exactName)
	{
		return countMatchingInventoryItems(name -> name.equalsIgnoreCase(exactName));
	}

	private int countMatchingInventoryItems(ItemNamePredicate predicate)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return 0;
		}

		int count = 0;
		for (Item item : inventory.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}

			ItemComposition composition = client.getItemDefinition(item.getId());
			String name = composition.getName();
			if (name != null && predicate.test(name))
			{
				count += Math.max(1, item.getQuantity());
			}
		}

		return count;
	}

	private static boolean isUntannedHide(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.equals("cowhide")
			|| lower.equals("green dragonhide")
			|| lower.equals("blue dragonhide")
			|| lower.equals("red dragonhide")
			|| lower.equals("black dragonhide");
	}

	private static boolean isPlankMakeLog(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.equals("logs")
			|| lower.equals("oak logs")
			|| lower.equals("teak logs")
			|| lower.equals("mahogany logs");
	}

	private static boolean isStringJewelleryInput(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.startsWith("unstrung ")
			|| lower.equals("salve amulet");
	}

	private static boolean isUncookedPie(String name)
	{
		return name.toLowerCase(Locale.ROOT).startsWith("uncooked ");
	}

	static String formatTicks(int ticks)
	{
		double seconds = ticks * 0.6;
		return String.format(Locale.US, "%.1fs", seconds);
	}

	@Provides
	SpellTimerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpellTimerConfig.class);
	}

	private interface ItemNamePredicate
	{
		boolean test(String name);
	}

	private static final class PendingSpell
	{
		private final String spellName;
		private final String readyAction;
		private final int ticks;

		private PendingSpell(String spellName, String readyAction, int ticks)
		{
			this.spellName = spellName;
			this.readyAction = readyAction;
			this.ticks = ticks;
		}
	}
}
