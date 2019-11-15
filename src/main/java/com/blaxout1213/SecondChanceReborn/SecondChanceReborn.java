package com.blaxout1213.SecondChanceReborn;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class SecondChanceReborn extends JavaPlugin
{
	private EventListener ev;
	File f = new File("plugins/ThirdChance/messages.yml");
	YamlConfiguration messages = YamlConfiguration.loadConfiguration(f);
	File messagesFile = null;
	public int revived;
	public int deadNow;
	
	public void onEnable()
	{
		loadConfig();
		this.ev = new EventListener(this);
		BukkitScheduler scheduler = getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, new BleedoutTask(this, ev), 0L, getConfig().getLong("BleedingOptions.Health.Decay")*20);
		scheduler.scheduleSyncRepeatingTask(this, new DownCounterTask(this, getConfig().getLong("CooldownOptions.DownResetTime")*60), 0L, 20L);
		if(getConfig().getString("BleedingOptions.Health.DownPunishmentType").equals("PERSISTENT"))
		{
			scheduler.scheduleSyncRepeatingTask(this, new HealBleedoutTask((double)getConfig().getLong("CooldownOptions.DownResetTime")), 0L, 120L);
		}

	}

	public void onDisable()
	{
	}

	public void loadConfig()
	{
		getConfig().addDefault("GeneralOptions.OverKill", -20);
		getConfig().addDefault("GeneralOptions.BleedThrough", false);
		
		getConfig().addDefault("DownedOptions.CrawlSpeed", 0.07D);
		getConfig().addDefault("DownedOptions.Glow", true);
		getConfig().addDefault("DownedOptions.Resistance", 3.1D);
		getConfig().addDefault("DownedOptions.RemoveRegeneration", true);
		getConfig().addDefault("DownedOptions.MobInvisibility", false);
		getConfig().addDefault("DownedOptions.Damage.Outgoing", 0.0D);
		getConfig().addDefault("DownedOptions.Damage.Incoming", 0.5D);

		
		getConfig().addDefault("ReviveOptions.Absorption.Enabled", true);
		getConfig().addDefault("ReviveOptions.Absorption.Length", 7);
		getConfig().addDefault("ReviveOptions.Absorption.Power", 1);
		getConfig().addDefault("ReviveOptions.ThrowRevive", false);
		getConfig().addDefault("ReviveOptions.KillRevive", false);
		getConfig().addDefault("ReviveOptions.SelfReviveChance", 0.05D);
		
		getConfig().addDefault("ReviveOptions.Health.Flat", 6D);
		getConfig().addDefault("ReviveOptions.Health.Percentage.Enabled", false);
		getConfig().addDefault("ReviveOptions.Health.Percentage.Amount", 0.3D);
		
		getConfig().addDefault("ReviveOptions.Delay.Type", "REGENERATION");
		getConfig().addDefault("ReviveOptions.Delay.Time", 20);
		getConfig().addDefault("ReviveOptions.Delay.Power", 1);
		
		getConfig().addDefault("CooldownOptions.Cooldown.Enabled", false);
		getConfig().addDefault("CooldownOptions.Cooldown.Time", 2.0D);
		getConfig().addDefault("CooldownOptions.Cooldown.CanceledByHeal", false);
		getConfig().addDefault("CooldownOptions.DownResetTime", 15.0D);
		
		
		getConfig().addDefault("BleedingOptions.Health.Downed", 20.0D);
		getConfig().addDefault("BleedingOptions.Health.Decay", 3L);
		getConfig().addDefault("BleedingOptions.Health.BaseDamage", 1.0D);
		
		getConfig().addDefault("BleedingOptions.Health.DownIncreaseMultiplier", 1D);
		getConfig().addDefault("BleedingOptions.Health.DownPunishmentType", "DAMAGE");
		
		getConfig().addDefault("BleedingOptions.Health.Percentage.Enabled", false);
		getConfig().addDefault("BleedingOptions.Health.Percentage.Downed", 1D);
		getConfig().addDefault("BleedingOptions.Health.Percentage.BaseDamage", 0.05D);
		
		messages.addDefault("Announcements.Enabled", true);
		messages.addDefault("Announcements.BleedOutMessagesEnabled", true);
		messages.addDefault("Announcements.DeathAnnounce", false);
		messages.addDefault("Announcements.Range", 256);
		messages.addDefault("Announcements.Title.Enabled", false);
		messages.addDefault("Announcements.Title.FadeIn", 0.5);
		messages.addDefault("Announcements.Title.Time", 5);
		messages.addDefault("Announcements.Title.FadeOut", 0.5);
		messages.addDefault("Announcements.Messages.Downed", "$4%p has been incapacitated!");
		messages.addDefault("Announcements.Messages.Revived", "$1%p has been revived!");
		messages.addDefault("Announcements.Messages.SelfRevive", "$1%p is too stubborn to die!");
		messages.addDefault("Announcements.Messages.Death", "$8No one revived %p.");
		messages.addDefault("Announcements.Messages.ReviveIncoming", "You will be revived in %s seconds");
		messages.addDefault("Announcements.Messages.ReviveHeal", "You will be revived when you reach full health");
		messages.addDefault("Announcements.Messages.ReviveCancelled", "Damage taken, revive has been cancelled");
		messages.addDefault("Announcements.Messages.ReviveOther", "%p is being revived!");
		
		messages.addDefault("Announcements.Messages.BleedOut.CONTACT", "%p bled out after hugging a cactus.");
		messages.addDefault("Announcements.Messages.BleedOut.DRAGON_BREATH", "%p bled out after being burnt by the dragon.");
		messages.addDefault("Announcements.Messages.BleedOut.DROWNING", "%p couldn't expel the water from their lungs.");
		messages.addDefault("Announcements.Messages.BleedOut.ENTITY_ATTACK", "%p bled out after being attacked.");
		messages.addDefault("Announcements.Messages.BleedOut.EXPLODE", "%p bled out after an explosion.");
		messages.addDefault("Announcements.Messages.BleedOut.FALL", "%p bled out after falling.");
		messages.addDefault("Announcements.Messages.BleedOut.FALLING_BLOCK", "%p bled out after being crushed.");
		messages.addDefault("Announcements.Messages.BleedOut.FIRE", "%p finally burned away.");
		messages.addDefault("Announcements.Messages.BleedOut.FLY_INTO_WALL", "%p bled out after smashing a wall.");
		messages.addDefault("Announcements.Messages.BleedOut.HOT_FLOOR", "%p bled out after stepping on magma.");
		messages.addDefault("Announcements.Messages.BleedOut.LAVA", "%p finally burned away after a lava encounter.");
		messages.addDefault("Announcements.Messages.BleedOut.LIGHTNING", "%p bled out after being struck by lightning.");
		messages.addDefault("Announcements.Messages.BleedOut.MAGIC", "%p bled out after magic happened.");
		messages.addDefault("Announcements.Messages.BleedOut.POISON", "%p succomed to the poison.");
		messages.addDefault("Announcements.Messages.BleedOut.PROJECTILE", "%p bled out after being shot.");
		messages.addDefault("Announcements.Messages.BleedOut.STARVATION", "%p finally starved.");
		messages.addDefault("Announcements.Messages.BleedOut.SUFFOCATION", "%p couldn't expel the blocks from their lungs.");
		messages.addDefault("Announcements.Messages.BleedOut.SUICIDE", "%p finally ended it.");
		messages.addDefault("Announcements.Messages.BleedOut.THORNS", "%p bled out after hitting thorny armor.");
		messages.addDefault("Announcements.Messages.BleedOut.VOID", "%p had the void stare back at it.");
		messages.addDefault("Announcements.Messages.BleedOut.WITHER", "%p finally withered away.");
		messages.addDefault("Announcements.Messages.BleedOut.UNKNOWN", "%p bled out.");
		messages.addDefault("Announcements.Messages.BleedOut.Killer", "%k finally killed %p.");
		
		//getConfig().options().copyDefaults(true);
		//saveConfig();
		//reloadConfig();
		//messages.options().copyDefaults(true);
		//save();
		if(!f.exists())
		{
			saveResource("messages.yml", false);
		}
		saveDefaultConfig();
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if ((sender instanceof Player))
		{
			if (cmd.getName().equalsIgnoreCase("Giveup"))
			{
				if (this.ev.isDown((Player) sender))
				{
					((Player) sender).setHealth(0.0D);
				} else
				{
					sender.sendMessage("You still have hope!");
				}
				return true;
			}
		}
		if (cmd.getName().equalsIgnoreCase("Thirdchance"))
		{
			sender.sendMessage("Third chance version: 2.2");
			return true;
		}
		return false;
	}

	public void save()
	{
		try
		{
			this.messages.save("plugins/ThirdChance/messages.yml");
			getLogger().log(Level.INFO, "Messages Saved");
		}
		catch (IOException e)
		{
			getLogger().log(Level.SEVERE, "Failed to save!");
			e.printStackTrace();
		}
	}
	/*
	public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        final InputStream defConfigStream = getResource("messages.yml");
        if (defConfigStream == null) {
            return;
        }

        messages.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
	}
	public void saveMessages() 
	{
        try {
            getConfig().save(messagesFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + messagesFile, ex);
        }
	}
	public FileConfiguration getMessages() 
	{
        if (messages == null) {
            reloadConfig();
        }
        return messages;
	}
	public void saveDefaultConfig() 
	{
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
	}
	*/
}
