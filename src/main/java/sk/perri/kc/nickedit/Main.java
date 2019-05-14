package sk.perri.kc.nickedit;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor
{
    // TAB / NICK / CHAT

    public static Main self;
    Database db;
    Map<String, Map<String, String >> groups = new HashMap<>();
    Map<String, String> players = new HashMap<>();

    @Override
    public void onEnable()
    {
        self = this;

        if(!getDataFolder().exists())
            getDataFolder().mkdir();

        getConfig().options().copyDefaults(true);
        saveConfig();

        db = new Database();
        groups = db.loadFormat();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nickedit").setExecutor(this);
        getLogger().info("Plugin sa zapol");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        String group = findGroup(event.getPlayer());
        players.put(event.getPlayer().getName().toLowerCase(), group);
        if(group != null)
        {
            event.getPlayer().setPlayerListName(groups.get(group).get("tabPrefix")+event.getPlayer().getName()+
                    groups.get(group).get("tabSuffix"));

            event.getPlayer().setCustomName(groups.get(group).get("nickPrefix")+event.getPlayer().getName()+
                    groups.get(group).get("nickSuffix"));
            event.getPlayer().setCustomNameVisible(true);
            registerTeams(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        players.remove(event.getPlayer().getName().toLowerCase());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        String g = null;
        if(players.containsKey(event.getPlayer().getName().toLowerCase()))
            g = players.get(event.getPlayer().getName().toLowerCase());
        else
            g = findGroup(event.getPlayer().getPlayer());

        if(g != null)
        {
            TextComponent head = new TextComponent(groups.get(g).get("chatPrefix")+event.getPlayer().getName()+
                    groups.get(g).get("chatSuffix"));
            head.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pm "+
                    event.getPlayer().getName()));

            // TODO chatHover placeholders
            List<String> hover = Arrays.asList(groups.get(g).get("chatHover").split("\\\\"));
            TextComponent[] hc = new TextComponent[hover.size()];
            for(int i = 0; i < hover.size(); i++)
            {
                hc[i] = new TextComponent(hover.get(i).replace("%group%", g)+"\n");
            }
            head.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hc));

            head.addExtra(groups.get(g).get("chatColor")+ String.join(" "+groups.get(g).get("chatColor"), event.getMessage().split(" ")));
            event.getRecipients().forEach(r -> r.spigot().sendMessage(head));
            event.setCancelled(true);
        }
    }

    public String findGroup(Player player)
    {
        final int[] currid = {1};
        final String[] currg = {null};

        groups.forEach((n, g) ->
        {
            Permission per = new Permission("ne.groups."+n, PermissionDefault.FALSE);

            if(player.hasPermission(per) && Integer.parseInt(g.get("id")) > currid[0])
            {
                currg[0] = n;
                currid[0] = Integer.parseInt(g.get("id"));
            }

            if(currg[0] == null && Integer.parseInt(g.get("id")) == 1)
                currg[0] = n;
        });

        return currg[0];
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if(!sender.isOp())
            return true;

        if(args.length > 0)
        {
            if(args[0].equalsIgnoreCase("reload"))
            {
                reloadConfig();
                db = new Database();
                groups = db.loadFormat();

                getServer().getOnlinePlayers().forEach(p ->
                {
                    String group = findGroup(p);
                    players.put(p.getName().toLowerCase(), group);
                    if(group != null)
                    {
                        p.setPlayerListName(groups.get(group).get("tabPrefix")+p.getName()+
                                groups.get(group).get("tabSuffix"));

                        p.setCustomName(groups.get(group).get("nickPrefix")+p.getName()+
                                groups.get(group).get("nickSuffix"));
                        p.setCustomNameVisible(true);
                        registerTeams(p);
                    }
                });
                sender.sendMessage("§7Config reloadovany");
                return true;
            }

            if(args[0].equalsIgnoreCase("groups"))
            {
                groups.keySet().forEach(g -> sender.sendMessage("§6"+g));
                return true;
            }
        }
        return true;
    }

    public void registerTeams(Player player)
    {
        Scoreboard sb = player.getScoreboard();
        groups.forEach((n, g) ->
        {
            if(sb.getTeam(n) == null)
            {
                Team t = sb.registerNewTeam(n);
                t.setPrefix(g.get("nickPrefix"));
                t.setSuffix(g.get("nickSuffix"));
                t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            }
        });

        getServer().getOnlinePlayers().forEach(p ->
        {
            if(players.containsKey(p.getName().toLowerCase()))
                sb.getTeam(players.get(p.getName().toLowerCase())).addEntry(p.getName());
        });

        player.setScoreboard(sb);
    }
}
