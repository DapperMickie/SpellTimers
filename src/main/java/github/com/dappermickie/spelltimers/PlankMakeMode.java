package github.com.dappermickie.spelltimers;

public enum PlankMakeMode
{
	ACTIVE("Active"),
	AFK("AFK"),
	BOTH("Both");

	private final String name;

	PlankMakeMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
