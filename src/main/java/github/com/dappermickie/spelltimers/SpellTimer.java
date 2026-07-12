package github.com.dappermickie.spelltimers;

import lombok.Getter;

class SpellTimer
{
	@Getter
	private final String spellName;

	@Getter
	private final String readyAction;

	@Getter
	private final int totalTicks;

	@Getter
	private int ticksRemaining;

	@Getter
	private int hideAfterReadyTicks;

	@Getter
	private boolean notificationSent;

	SpellTimer(String spellName, String readyAction, int ticksRemaining, int hideAfterReadyTicks)
	{
		this.spellName = spellName;
		this.readyAction = readyAction;
		this.totalTicks = ticksRemaining;
		this.ticksRemaining = ticksRemaining;
		this.hideAfterReadyTicks = hideAfterReadyTicks;
	}

	void tick()
	{
		if (ticksRemaining > 0)
		{
			ticksRemaining--;
		}
	}

	boolean isExpired()
	{
		return ticksRemaining == 0;
	}

	void markNotificationSent()
	{
		notificationSent = true;
	}

	void decrementHideAfterReadyTicks()
	{
		if (hideAfterReadyTicks > 0)
		{
			hideAfterReadyTicks--;
		}
	}
}
