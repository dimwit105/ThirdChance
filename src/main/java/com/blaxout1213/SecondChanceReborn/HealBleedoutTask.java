package com.blaxout1213.SecondChanceReborn;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

public class HealBleedoutTask implements Runnable
{
	public static HashMap<Player, Double> lowestHP = new HashMap<Player, Double>();
	private Double healthPerSix;
	public HealBleedoutTask(Double timeToHealTwenty)
	{
		this.healthPerSix = 20/(timeToHealTwenty*10);
	}
	@Override
	public void run()
	{
		HashMap<Player, Double> temp = new HashMap<Player, Double>();
		for(Entry<Player, Double> entry : lowestHP.entrySet())
		{
			temp.put(entry.getKey(), entry.getValue() + healthPerSix);
		}
		HealBleedoutTask.lowestHP.putAll(temp);
	}

}
