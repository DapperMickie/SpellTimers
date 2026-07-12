package github.com.dappermickie.spelltimers;

public enum TimeRemainingMode
{
	TICKS("Ticks"),
	SECONDS("Seconds");

	private final String name;

	TimeRemainingMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
