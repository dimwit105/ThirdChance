package com.blaxout1213.SecondChanceReborn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EventListener implements Listener
{
	//public ArrayList<String> incapacitated = new ArrayList<String>();
	ArrayList<String> cooldown = new ArrayList<String>();
	//HashMap<String, Double> downs = new HashMap<String, Double>();
	HashMap<String, DamageCause> lastHit = new HashMap<String, DamageCause>();
	HashMap<String, Entity> downer = new HashMap<String, Entity>();
	HashMap<Player, Float> speed = new HashMap<Player, Float>();
	private SecondChanceReborn plugin;
	private long coolDownTime;
	//private EventPriority onEntityDamage = EventPriority.valueOf(plugin.getConfig().getString("EventPriorities.EntityDamageEvent"));
	public static int revived2;
	public static int deadNow2;
	YamlConfiguration messages;
	Random rand = new Random();
	
	public EventListener(SecondChanceReborn plugin)
	{
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		messages = plugin.messages;
		this.plugin = plugin;
		this.coolDownTime = (long) ((plugin.getConfig().getDouble("CooldownOptions.Cooldown.Time") * 20.0D * 60.0D));
	}

	@EventHandler
	public void stopThoseBlocks(BlockPlaceEvent event)
	{
		if (BleedoutTask.has(event.getPlayer()))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void stopThoseBlocks(BlockBreakEvent event)
	{
		if (BleedoutTask.has(event.getPlayer()))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onUseBucket(PlayerBucketEmptyEvent event)
	{
		if(BleedoutTask.has(event.getPlayer()))
		{
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onShootArrow(EntityShootBowEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			if(BleedoutTask.has((Player)event.getEntity()))
			{
				event.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onEntityAttack(EntityDamageByEntityEvent event)
	{
		if ((event.getDamager() instanceof Player))
		{
			Player p = (Player) event.getDamager();
			if (BleedoutTask.has(p))
			{
				if(this.plugin.getConfig().getDouble("DownedOptions.Damage.Outgoing") <= 0.0D)
				{
					event.setCancelled(true);
				}
				event.setDamage(event.getDamage() * this.plugin.getConfig().getDouble("DownedOptions.Damage.Outgoing"));
				if ((event.getEntity() instanceof LivingEntity))
				{
					LivingEntity attacked = (LivingEntity) event.getEntity();
					if ((attacked.getHealth() - event.getFinalDamage() <= 0.0D) && (this.plugin.getConfig().getBoolean("ReviveOptions.KillRevive")))
					{
						revive(p, "kill");
					}
				}
			}
		}
		if(event.getEntity() instanceof Player)
		{
			Player p = (Player) event.getEntity();
			attemptDown(event, p);
		}
	}
	
	public void attemptDown(EntityDamageEvent event, Player p)
	{
		if ((!this.cooldown.contains(p.getName()) && plugin.getConfig().getBoolean("CooldownOptions.Cooldown.Enabled")) || !plugin.getConfig().getBoolean("CooldownOptions.Cooldown.Enabled"))
		{
			if ((!event.isCancelled()) && (p.getHealth() - event.getFinalDamage() <= 0.0D) && (p.getHealth() - event.getFinalDamage() >= this.plugin.getConfig().getInt("GeneralOptions.OverKill")) && (!BleedoutTask.has(p)) && DownCounterTask.getDowns(p) < p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
			{
				event.setCancelled(true);
				if(event instanceof EntityDamageByEntityEvent)
				{
					downer.put(p.getName(), ((EntityDamageByEntityEvent)event).getDamager());
				}
				lastHit.put(p.getName(), event.getCause());
				Incappacitate(p, p.getHealth() - event.getFinalDamage());
			}
		}
	}
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFallDamage(EntityDamageEvent event)
	{
		if(event.getEntity() instanceof Player) {
			if(BleedoutTask.has((Player) event.getEntity()) && event.getCause() == DamageCause.FALL) {
				event.setCancelled(true); }}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent event)
	{
		if ((event.getEntity() instanceof Player))
		{
			Player p = (Player) event.getEntity();
			attemptDown(event, p);
			if(BleedoutTask.has(p))
			{
				if((event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK || event.getCause() == DamageCause.PROJECTILE))
				{
					if(p.hasPotionEffect(PotionEffectType.REGENERATION))
					{
						p.removePotionEffect(PotionEffectType.REGENERATION);
						p.sendMessage(messages.getString("Announcements.Messages.ReviveCancelled"));
					}
					
					event.setDamage(event.getDamage() * this.plugin.getConfig().getDouble("DownedOptions.Damage.Incoming"));
				}
				if(HealBleedoutTask.lowestHP.getOrDefault(p, Double.MAX_VALUE) > p.getHealth() - event.getDamage())
				{
					HealBleedoutTask.lowestHP.put(p, p.getHealth() - event.getDamage());
				}
			}
			if (p.getHealth() <= 0.0D)
			{
				resetPlayer(p);
			}
		}
	}

	@EventHandler
	public void onRightClickEntity(PlayerInteractEntityEvent event)
	{
		if ((event.getRightClicked() instanceof Player))
		{
			Player p = (Player) event.getRightClicked();
			Player ep = event.getPlayer();
			String s = plugin.getConfig().getString("ReviveOptions.Delay.Type");
			int time = plugin.getConfig().getInt("ReviveOptions.Delay.Time");
			if (BleedoutTask.has(p) && !BleedoutTask.has(ep) && ep.getGameMode() != GameMode.SPECTATOR)
			{
				if(s.equals("REGENERATION"))
				{
					if(!p.hasPotionEffect(PotionEffectType.REGENERATION))
					{
						p.sendMessage(messages.getString("Announcements.Messages.ReviveHeal"));
						message(p, messages.getString("Announcements.Messages.ReviveOther").replace("%p", p.getName()));
					}
					p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * (time), this.plugin.getConfig().getInt("ReviveOptions.Delay.Power")));
				}
				else if(s.equals("TIME"))
				{
					if(!p.hasPotionEffect(PotionEffectType.REGENERATION))
					{
						p.sendMessage(messages.getString("Announcements.Messages.ReviveIncoming").replace("%s", Integer.toString(time)));
						message(p, messages.getString("Announcements.Messages.ReviveOther").replace("%p", p.getName()));
					}
					
					p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * (time+1), this.plugin.getConfig().getInt("ReviveOptions.Delay.Power")));

					this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
					{
						public void run()
						{
							if(p.hasPotionEffect(PotionEffectType.REGENERATION))
							{
								revive(p, "heal");
							}
						}
					}, time*20);
				}
				else
				{
					revive(p, "rightclick");
				}	
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onDeath(PlayerDeathEvent event)
	{
		Player p = event.getEntity();
		if (BleedoutTask.has(p))
		{
			DamageCause dc = lastHit.remove(p.getName());
			Entity e = downer.remove(p.getName());
			if(p.getLastDamageCause().getCause() == DamageCause.CUSTOM && messages.getBoolean("Announcements.BleedOutMessagesEnabled"))
			{
				if(e != null)
				{
					event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.Killer").replace("%k", e.getName()).replace("%p", p.getName()).replace("$", "§"));
					p.incrementStatistic(Statistic.ENTITY_KILLED_BY, e.getType());
					if(e instanceof Player) {((Player) e).incrementStatistic(Statistic.KILL_ENTITY, e.getType());}
				}
				else
				{
					switch(dc)
					{
						case BLOCK_EXPLOSION:
						case ENTITY_EXPLOSION:
							event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.EXPLODE").replace("%p", p.getName()).replace("$", "§")); break;
						
						case CONTACT: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.CONTACT").replace("%p", p.getName()).replace("$", "§")); break;
						case DRAGON_BREATH: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.DRAGON_BREATH").replace("%p", p.getName()).replace("$", "§")); break;
						case DROWNING: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.DROWNING").replace("%p", p.getName()).replace("$", "§")); break;
						
						case ENTITY_ATTACK:
						case ENTITY_SWEEP_ATTACK:
							event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.ENTITY_ATTACK").replace("%p", p.getName()).replace("$", "§")); break;
						
						case FALL: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.FALL").replace("%p", p.getName()).replace("$", "§")); break;
						case FALLING_BLOCK: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.FALLING_BLOCK").replace("%p", p.getName()).replace("$", "§")); break;
						
						case FIRE_TICK:
						case FIRE:
							event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.FIRE").replace("%p", p.getName()).replace("$", "§")); break;
						
						case FLY_INTO_WALL: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.FLY_INTO_WALL").replace("%p", p.getName()).replace("$", "§")); break;
						case HOT_FLOOR: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.HOT_FLOOR").replace("%p", p.getName()).replace("$", "§")); break;
						case LAVA: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.LAVA").replace("%p", p.getName()).replace("$", "§")); break;
						case LIGHTNING: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.LIGHTNING").replace("%p", p.getName()).replace("$", "§")); break;
						case MAGIC: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.MAGIC").replace("%p", p.getName()).replace("$", "§")); break;
						case POISON: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.POISON").replace("%p", p.getName()).replace("$", "§")); break;
						case PROJECTILE: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.PROJECTILE").replace("%p", p.getName()).replace("$", "§")); break;
						case STARVATION: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.STARVATION").replace("%p", p.getName()).replace("$", "§")); break;
						case SUFFOCATION: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.SUFFOCATION").replace("%p", p.getName()).replace("$", "§")); break;
						case SUICIDE: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.SUICIDE").replace("%p", p.getName()).replace("$", "§")); break;
						case THORNS: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.THORNS").replace("%p", p.getName()).replace("$", "§")); break;
						case VOID: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.VOID").replace("%p", p.getName()).replace("$", "§")); break;
						case WITHER: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.WITHER").replace("%p", p.getName()).replace("$", "§")); break;
						default: event.setDeathMessage(messages.getString("Announcements.Messages.BleedOut.UNKNOWN").replace("%p", p.getName()).replace("$", "§")); break;
					}
				}
				
			}
			if (messages.getBoolean("Announcements.Enabled") && messages.getBoolean("Announcements.DeathAnnounce"))
			{
				message(p, messages.getString("Announcements.Messages.Death").replace("%p", p.getName()));
			}
			resetPlayer(p);
		}
	}

	@EventHandler
	public void monsterInvisibility(EntityTargetEvent event)
	{
		if ((event.getTarget() instanceof Player))
		{
			Player target = (Player) event.getTarget();
			if ((BleedoutTask.has(target)) && (this.plugin.getConfig().getBoolean("DownedOptions.MobInvisibility")))
			{
				if ((event.getEntity() instanceof Monster))
				{
					Monster mob = (Monster) event.getEntity();
					mob.setTarget(null);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void stopHealing(EntityRegainHealthEvent event)
	{
		if ((event.getEntity() instanceof Player))
		{
			Player p = (Player) event.getEntity();
			if (BleedoutTask.has(p))
			{
				if ((event.getRegainReason().equals(EntityRegainHealthEvent.RegainReason.MAGIC)) && (this.plugin.getConfig().getBoolean("ReviveOptions.ThrowRevive")))
				{
					event.setCancelled(true);
					revive(p, "heal");
				}
				if(plugin.getConfig().getString("ReviveOptions.Delay.Type").equals("REGENERATION") && p.getHealth() + event.getAmount() >= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
				{
					event.setCancelled(true);
					revive(p, "heal");
				}
				else if(!event.getRegainReason().equals(RegainReason.MAGIC_REGEN))
				{
					event.setCancelled(true);
				}
			}
			else if ((this.cooldown.contains(p.getName())) && (this.plugin.getConfig().getBoolean("CooldownOptions.Cooldown.Enabled.CanceledByHeal")))
			{
				this.cooldown.remove(p.getName());
			}
		}
	}

	@EventHandler
	public void logoutBlocker(PlayerQuitEvent event)
	{
		Player p = event.getPlayer();
		if (BleedoutTask.has(p))
		{
			p.setHealth(0.0D);
		}
	}

	@EventHandler
	public void respawnSafety(PlayerRespawnEvent event)
	{
		Player player = event.getPlayer();
		resetPlayer(player);
		HealBleedoutTask.lowestHP.remove(player);
		DownCounterTask.resetDowns(player);
		this.cooldown.remove(player.getName());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void noItems(PlayerItemConsumeEvent event)
	{
		if (BleedoutTask.has(event.getPlayer()))
		{
			event.setCancelled(true);
		}
	}

	public void resetPlayer(Player player)
	{
		BleedoutTask.remove(player);
		Float playerSpeed = speed.remove(player);
		if(playerSpeed != null) { player.setWalkSpeed(playerSpeed); }
		player.removePotionEffect(PotionEffectType.BLINDNESS);
		player.removePotionEffect(PotionEffectType.JUMP);
		if(plugin.getConfig().getBoolean("DownedOptions.Glow")) { player.removePotionEffect(PotionEffectType.GLOWING); }
		player.setSneaking(false);
	}

	public void giveUp(Player player)
	{
		if (BleedoutTask.has(player))
		{
			player.setHealth(0.0D);
		}
	}

	public void Incappacitate(final Player player, double damage)
	{
		BleedoutTask.add(player);
		player.setSneaking(true);
		//double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		//double percentage = this.plugin.getConfig().getDouble("BleedingOptions.Health.Percentage.Downed");
		double downedHealth = plugin.getConfig().getBoolean("BleedingOptions.Health.Percentage.Enabled") ? player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*this.plugin.getConfig().getDouble("BleedingOptions.Health.Percentage.Downed") : this.plugin.getConfig().getDouble("BleedingOptions.Health.Downed");
		if(downedHealth > HealBleedoutTask.lowestHP.getOrDefault(player, downedHealth) && plugin.getConfig().getString("BleedingOptions.Health.DownPunishmentType").equals("PERSISTENT"))
		{
			downedHealth = HealBleedoutTask.lowestHP.get(player);
		}
		if(this.plugin.getConfig().getBoolean("GeneralOptions.BleedThrough"))
		{
			downedHealth -= (Math.abs(damage)*this.plugin.getConfig().getDouble("DownedOptions.Damage.Incoming"));
		}
		if(plugin.getConfig().getBoolean("DownedOptions.RemoveRegeneration"))
		{
			player.removePotionEffect(PotionEffectType.REGENERATION);
		}
		if (messages.getBoolean("Announcements.Enabled"))
		{
			message(player, messages.getString("Announcements.Messages.Downed").replace("%p", player.getName()));
		}
		if (this.plugin.getConfig().getBoolean("DownedOptions.MobInvisibility"))
		{
			ArrayList<Entity> mobs = (ArrayList<Entity>) player.getNearbyEntities(20.0D, 20.0D, 20.0D);
			for (int i = 0; i < mobs.size(); i++)
			{
				if ((mobs.get(i) instanceof Monster))
				{
					Monster monster = (Monster) mobs.get(i);
					monster.setTarget(null);
				}
			}
		}

		if(plugin.getConfig().getString("BleedingOptions.Health.DownPunishmentType").equals("HEALTH"))
		{
			player.setHealth(downedHealth - (DownCounterTask.getDowns(player) * this.plugin.getConfig().getDouble("BleedingOptions.Health.BaseDamage") * this.plugin.getConfig().getDouble("BleedingOptions.Health.DownIncreaseMultiplier")));
		}
		else
		{
			player.setHealth(downedHealth);
		}
		
		player.addPotionEffect(PotionEffectType.JUMP.createEffect(Integer.MAX_VALUE, -5));
		player.addPotionEffect(PotionEffectType.BLINDNESS.createEffect(Integer.MAX_VALUE, 0));
		if(plugin.getConfig().getDouble("DownedOptions.Resistance") > 0D) 
		{
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, (int)plugin.getConfig().getDouble("DownedOptions.Resistance") * 20, 4, false));
		}
		if(plugin.getConfig().getBoolean("DownedOptions.Glow"))
		{
			player.addPotionEffect(PotionEffectType.GLOWING.createEffect(Integer.MAX_VALUE, 0));
		}
		speed.put(player, player.getWalkSpeed());
		player.setWalkSpeed((float) EventListener.this.plugin.getConfig().getDouble("DownedOptions.CrawlSpeed"));
		this.cooldown.add(player.getName());
		BleedoutTask.add(player);

		if(plugin.getConfig().getBoolean("CooldownOptions.Cooldown.Enabled"))
		{
			this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
			{
				public void run()
				{
					EventListener.this.cooldown.remove(player.getName());
					//downs.remove(player.getName());
				}
			}, this.coolDownTime);
		}

	}

	public void revive(Player p, String reason)
	{
		if (BleedoutTask.has(p))
		{
			resetPlayer(p);
			DownCounterTask.addDowns(p, 1D);
			if (this.plugin.getConfig().getBoolean("ReviveOptions.Absorption.Enabled"))
			{
				p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * this.plugin.getConfig().getInt("ReviveOptions.Absorption.Length"), this.plugin.getConfig().getInt("ReviveOptions.Absorption.Power")));
			}
			p.removePotionEffect(PotionEffectType.REGENERATION);
			double reviveHealth = plugin.getConfig().getBoolean("ReviveOptions.Health.Percentage.Enabled") ? p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*this.plugin.getConfig().getDouble("ReviveOptions.Health.Percentage.Amount") : this.plugin.getConfig().getDouble("ReviveOptions.Health.Flat");
			p.setHealth(reviveHealth);
			if (messages.getBoolean("Announcements.Enabled"))
			{
				String message = "";
				switch(reason)
				{
					case "chance": message = messages.getString("Announcements.Messages.SelfRevive").replace("%p", p.getName()); break;
					//case "rightclick": message = messages.getString("Announcements.Messages.SelfRevive").replace("%p", p.getName()); break;
					default: message = messages.getString("Announcements.Messages.Revived").replace("%p", p.getName()); break;
				}
				message(p, message);
			}
			
		}
	}

	void message(Player p, String message)
	{
		message = message.replace("$", "§");
		int fadeIn = (int) Math.floor(20*messages.getDouble("Announcements.Title.FadeIn"));
		int time = (int) Math.floor(20*messages.getDouble("Announcements.Title.Time"));
		int fadeOut = (int) Math.floor(20*messages.getDouble("Announcements.Title.FadeOut"));
		final String fmessage = message;
		if (messages.getInt("Announcements.Range") > 0)
		{
			if(messages.getBoolean("Announcements.Title.Enabled"))
			{
				p.sendTitle("", message, fadeIn, time, fadeOut);
			}
			else
			{
				p.sendMessage(message);
			}
			int range = messages.getInt("Announcements.Range");
			List<Entity> mobs = p.getNearbyEntities(range, range, range);
			for (int i = 0; i < mobs.size(); i++)
			{
				if ((mobs.get(i) instanceof Player))
				{
					Player mep = (Player) mobs.get(i);
					if(messages.getBoolean("Announcements.Title.Enabled"))
					{
						mep.sendTitle("", message, fadeIn, time, fadeOut);
					}
					else
					{
						mep.sendMessage(message);
					}
				}
			}
		}
		else
		{
			if(messages.getBoolean("Announcements.Title.Enabled"))
			{
				Bukkit.getOnlinePlayers().forEach(player -> player.sendTitle("", fmessage, fadeIn, time, fadeOut));
			}
			else
			{
				Bukkit.broadcastMessage(message);
			}
		}
	}
	public boolean isDown(Player p)
	{
		if (BleedoutTask.has(p))
		{
			return true;
		}
		return false;
	}
}
