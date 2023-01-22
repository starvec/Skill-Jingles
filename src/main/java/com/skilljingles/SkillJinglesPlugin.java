package com.skilljingles;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.*;
import net.runelite.api.events.GameTick;
import javax.sound.sampled.*;
import javax.sound.sampled.DataLine.Info;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioSystem.*;

@Slf4j
@PluginDescriptor(
	name = "Skill Jingles"
)
public class SkillJinglesPlugin extends Plugin
{
	// file names for each skill jingle
	private final String[] jingleFiles = {"attack.ogg", "defence.ogg", "strength.ogg", "hitpoints.ogg", "ranged.ogg", "prayer.ogg", "magic.ogg", "cooking.ogg", "woodcutting.ogg", "fletching.ogg", "fishing.ogg", "firemaking.ogg", "crafting.ogg", "smithing.ogg", "mining.ogg", "herblore.ogg", "agility.ogg", "thieving.ogg", "slayer.ogg", "farming.ogg", "runecraft.ogg", "hunter,ogg", "construction.ogg",
			"attack2.ogg", "defence2.ogg", "strength2.ogg", "hitpoints2.ogg", "ranged2.ogg", "prayer2.ogg", "magic2.ogg", "cooking2.ogg", "woodcutting2.ogg", "fletching2.ogg", "fishing2.ogg", "firemaking2.ogg", "crafting2.ogg", "smithing2.ogg", "mining2.ogg", "herblore2.ogg", "agility2.ogg", "thieving2.ogg", "slayer2.ogg", "farming2.ogg", "runecraft2.ogg", "hunter2.ogg", "construction2.ogg"};

	// index offset to get from one version of a jingle to another
	private final int specialJingleOffset = 23;

	// holds the players current skill levels
	// used to detect when a skill level increases
	private int[] skillLevels = new int[23];

	// if false, the skillLevels array has not been set up with the player's skill levels
	private boolean initSkillLevels = false;

	// contains which version of a skill's jingle should be played at each level
	// for more information, visit https://oldschool.runescape.wiki/w/Jingles
	private boolean[][] skillJingleVersion = new boolean[23][99];

	@Inject
	private Client client;

	@Inject
	private SkillJinglesConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Skill Jingles plugin started!");
		initJingleVersions("jingle_versions.csv");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Skill Jingles plugin stopped!");
	}

	@Provides
	SkillJinglesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillJinglesConfig.class);
	}

	// check for stat changes and play jingles only when volume test mode is enabled
	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		if (config.testMode() && initSkillLevels)
		{
			Skill skill = statChanged.getSkill();

			int jingleIndex = skill.ordinal();
			int level = client.getRealSkillLevel(skill);

			if (skillJingleVersion[skill.ordinal()][level]) {
				jingleIndex += specialJingleOffset;
			}

			playJingle(jingleFiles[jingleIndex]);
		}
	}

	// check for jingles that need to be played and play them
	@Subscribe
	public void onGameTick(GameTick event)
	{
		for (int s = 0; s < 23; s++) {
			Skill thisSkill = Skill.values()[s];

			int newSkillLevel = client.getRealSkillLevel(thisSkill);

			// if the skill levels array has already been initialized
			if (initSkillLevels)
			{
				if (newSkillLevel != skillLevels[s])
				{
					log.info(thisSkill.getName() + " increased from " + skillLevels[s] + " to " + newSkillLevel);
					skillLevels[s] = newSkillLevel;

					if (config.playOnUnmute() || client.getMusicVolume() == 0) {
						int jingleIndex = s;

						if (skillJingleVersion[Skill.ATTACK.ordinal()][newSkillLevel]) {
							jingleIndex += specialJingleOffset;
						}

						log.info("Playing jingle " + jingleFiles[jingleIndex]);
						playJingle(jingleFiles[jingleIndex]);
					}
				}
			}
			// else if the skill level array has not yet been initialized
			else {
				skillLevels[s] = newSkillLevel;
			}

		}
		initSkillLevels = true;
	}

	// plays the provided audio resource
	private void playJingle(String file)
	{
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				InputStream stream = getClass().getClassLoader().getResourceAsStream(file);

				try (final AudioInputStream in = getAudioInputStream(stream)) {

					final AudioFormat outFormat = getOutFormat(in.getFormat());
					final Info info = new Info(SourceDataLine.class, outFormat);

					try (final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {

						Thread.sleep(600);

						if (line != null) {
							line.open(outFormat);

							if (line.isControlSupported(FloatControl.Type.MASTER_GAIN))
							{
								FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
								double gain = Math.min(Math.max(config.volume()/100D, 0D), 100D);
								float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
								gainControl.setValue(dB);
							}
							else
								log.info("Was not able to set SourceDataLine volume");

							line.start();
							stream(getAudioInputStream(outFormat, in), line);
							line.drain();
							line.stop();
						}
					} catch (InterruptedException e) {
						log.info(e.getMessage());
					}

				} catch (UnsupportedAudioFileException
						 | LineUnavailableException
						 | IOException e) {
					log.info(e.getMessage());
				}
			}
		});
		t1.start();
	}

	private AudioFormat getOutFormat(AudioFormat inFormat) {
		final int ch = inFormat.getChannels();
		final float rate = inFormat.getSampleRate();
		return new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
	}

	private void stream(AudioInputStream in, SourceDataLine line)
			throws IOException {
		final byte[] buffer = new byte[65536];
		for (int n = 0; n != -1; n = in.read(buffer, 0, buffer.length)) {
			line.write(buffer, 0, n);
		}
	}

	// reads the file containing which version of a skill's jingles should be played at each level and writes that information to the skillJingleVersions array
	private void initJingleVersions(String jingleVersionPath) throws IOException {
		InputStream stream = getClass().getClassLoader().getResourceAsStream(jingleVersionPath);
		if (stream != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));

			String line;
			String[] tempArr;

			int s = 0;
			br.readLine(); // skip headers
			while ((line = br.readLine()) != null) {
				tempArr = line.split(",");
				for (int l = 1; l <= 99; l++) {
					skillJingleVersion[s][l - 1] = (tempArr[l].equals("1"));
				}
				s++;
			}
		}
		else {
			log.info("Could not read jingle versions configuration, InputStream was null");
		}
	}
}
