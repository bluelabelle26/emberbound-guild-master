package com.emberbound;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GuildMasterLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(GuildMasterPlugin.class);
        RuneLite.main(args);
    }
}
