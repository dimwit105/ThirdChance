package com.blaxout1213.SecondChanceReborn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

public class DownCounterTask implements Runnable
{

	private SecondChanceReborn plugin;
	private static double forgiveTime;
	private static HashMap<Player, Double> downs = new HashMap<Player, Double>();
	public DownCounterTask(SecondChanceReborn plugin, double forgiveTime)
	{
		this.plugin = plugin;
		DownCounterTask.forgiveTime = forgiveTime;
	}
	@Override
	public void run()
	{
		Iterator<Entry<Player, Double>> it = downs.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<Player, Double> pair = it.next();
			Player p = (Player) pair.getKey();
			Double time = (Double) pair.getValue();
			downs.put(p, time - 1);
			if(time <= 0)
			{
				it.remove();
			}
		}
	}
	
	public static void addDowns(Player p, Double t)
	{
		downs.put(p, downs.getOrDefault(p, 0D) + t*forgiveTime);
	}
	public static int getDowns(Player p)
	{
		return (int) Math.ceil(downs.getOrDefault(p, 0D) / forgiveTime);
	}
	public static void resetDowns(Player p)
	{
		downs.remove(p);
	}
}
