package github.com.dappermickie.spelltimers;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;

@ConfigGroup("spell-timer")
public interface SpellTimerConfig extends Config
{
	@ConfigSection(
		name = "Plugin Settings",
		description = "General Spell Timers settings.",
		position = 0
	)
	String pluginSettings = "pluginSettings";

	@ConfigSection(
		name = "Spell Settings",
		description = "Spell-specific timer settings.",
		position = 1
	)
	String spellSettings = "spellSettings";

	@ConfigItem(
		keyName = "showHumidify",
		name = "Humidify",
		description = "Show a bank-now timer after casting Humidify",
		position = 1,
		section = spellSettings
	)
	default boolean showHumidify()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSuperglassMake",
		name = "Superglass Make",
		description = "Show a bank-now timer after casting Superglass Make",
		position = 2,
		section = spellSettings
	)
	default boolean showSuperglassMake()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDegrime",
		name = "Degrime",
		description = "Show a bank-now timer after casting Degrime",
		position = 3,
		section = spellSettings
	)
	default boolean showDegrime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSpinFlax",
		name = "Spin Flax",
		description = "Show a recast timer after casting Spin Flax",
		position = 4,
		section = spellSettings
	)
	default boolean showSpinFlax()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTanLeather",
		name = "Tan Leather",
		description = "Show a recast timer after casting Tan Leather",
		position = 5,
		section = spellSettings
	)
	default boolean showTanLeather()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlankMake",
		name = "Plank Make",
		description = "Show a timer after casting Plank Make",
		position = 6,
		section = spellSettings
	)
	default boolean showPlankMake()
	{
		return true;
	}

	@ConfigItem(
		keyName = "plankMakeMode",
		name = "Plank Make mode",
		description = "Active shows the 3-tick manual rhythm; AFK estimates passive auto-cast completion; Both shows both",
		position = 7,
		section = spellSettings
	)
	default PlankMakeMode plankMakeMode()
	{
		return PlankMakeMode.ACTIVE;
	}

	@ConfigItem(
		keyName = "showStringJewellery",
		name = "String Jewellery",
		description = "Show an inventory-finished timer after casting String Jewellery",
		position = 8,
		section = spellSettings
	)
	default boolean showStringJewellery()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBakePie",
		name = "Bake Pie",
		description = "Show an inventory-finished timer after casting Bake Pie",
		position = 9,
		section = spellSettings
	)
	default boolean showBakePie()
	{
		return true;
	}

	@ConfigItem(
		keyName = "readyTimeoutSeconds",
		name = "Ready timeout",
		description = "Seconds to keep a ready timer on screen after the next action is available",
		position = 1,
		section = pluginSettings
	)
	default int readyTimeoutSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "timeRemainingMode",
		name = "Show time remaining",
		description = "Choose whether the overlay shows ticks or seconds remaining",
		position = 2,
		section = pluginSettings
	)
	default TimeRemainingMode timeRemainingMode()
	{
		return TimeRemainingMode.SECONDS;
	}

	@ConfigItem(
		keyName = "enableNotification",
		name = "Ready notification",
		description = "Configures the notification sent when the spell timer is ready or done",
		position = 3,
		section = pluginSettings
	)
	default Notification readyNotification()
	{
		return Notification.OFF;
	}
}
