package github.com.dappermickie.spelltimers;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SpellTimerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SpellTimerPlugin.class);
		RuneLite.main(args);
	}
}
