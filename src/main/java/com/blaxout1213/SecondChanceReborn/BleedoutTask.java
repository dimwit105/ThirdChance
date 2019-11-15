package com.blaxout1213.SecondChanceReborn;

import java.util.ArrayList;

import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class BleedoutTask implements Runnable
{

    private SecondChanceReborn plugin;
	private EventListener ev;
	private static ArrayList<Player> downed = new ArrayList<Player>();
	private static ArrayList<Player> remove = new ArrayList<Player>();

	public BleedoutTask(SecondChanceReborn plugin, EventListener ev) 
    {
        this.plugin = plugin;
        this.ev = ev;
        plugin.getServer().getOnlinePlayers();
    }

	public void run()
	{
		downed.removeAll(remove);
		remove.clear();
		for(Player player : downed)
		{
			if(remove.contains(player))
			{
				continue;
			}
			double damage = plugin.getConfig().getDouble("BleedingOptions.Health.BaseDamage");
			if(plugin.getConfig().getBoolean("BleedingOptions.Health.Percentage.Enabled"))
			{
				damage = plugin.getConfig().getDouble("BleedingOptions.Health.Percentage.BaseDamage") * player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			}
			if(player.hasPotionEffect(PotionEffectType.REGENERATION) && player.getHealth() >= player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() && (plugin.getConfig().getString("ReviveOptions.Delay.Type").equals("REGENERATION")))
			{
				ev.revive(player, "heal");
				//remove.add(player);
				continue;
			}
			//"BleedingOptions.Health.DownIncreaseMultiplier"
			if(!player.hasPotionEffect(PotionEffectType.REGENERATION))
			{
				if(plugin.getConfig().getString("BleedingOptions.Health.DownPunishmentType").equals("DAMAGE"))
				{
					damage = damage * Math.max((DownCounterTask.getDowns(player) + 1)*plugin.getConfig().getDouble("BleedingOptions.Health.DownIncreaseMultiplier"), 1);
				}
				
				if (ev.rand.nextDouble() < plugin.getConfig().getDouble("ReviveOptions.SelfReviveChance") && player.getHealth() <= damage && plugin.getConfig().getDouble("ReviveOptions.SelfReviveChance") > 0.0D)
				{
					ev.revive(player, "chance");
					//remove.add(player);
					continue;
					
				}
				else 
				{
					if(player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE))
					{
						double divisor = 0.2*(player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1);
						damage = damage / Math.max(0.2, 1-divisor);
					}
					int protectionLevels = 0;
					for(int i = 0; i < player.getInventory().getArmorContents().length; i++)
					{
						if(player.getInventory().getArmorContents()[i] != null && player.getInventory().getArmorContents()[i].getEnchantments().containsKey(Enchantment.PROTECTION_ENVIRONMENTAL))
						{
							protectionLevels = protectionLevels + player.getInventory().getArmorContents()[i].getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
						}
					}
					double enchantmentDivisor = 1.0 - Math.min(20, protectionLevels)*0.04;
					player.damage(damage/enchantmentDivisor);
				}
			}
		}
	}
	
	public static void remove(Player p)
	{
		remove.add(p);
	}
	public static void add(Player p)
	{
		downed.add(p);
	}
	public static boolean has(Player p)
	{
		return downed.contains(p) && !remove.contains(p);
	}
}
