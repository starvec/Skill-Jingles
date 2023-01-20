package com.skilljingles;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Skill Jingles")
public interface SkillJinglesConfig extends Config
{
	@ConfigItem(
			keyName = "volume",
			name = "Jingle Volume",
			description = "The volume to play skill jingles at (0-100)"
	)
	default int volume()
	{
		return 50;
	}

	@ConfigItem(
			keyName = "playOnUnmute",
			name = "Play While Music Not Muted",
			description = "Play jingles even when the in-game music is enabled?"
	)
	default boolean playOnUnmute() {return false;}

	@ConfigItem(
			keyName = "testMode",
			name = "Volume Test Mode",
			description = "Allows you to test your jingle volume by playing jingles whenever you gain xp or a stat changes"
	)
	default boolean testMode() {return false;}
}
