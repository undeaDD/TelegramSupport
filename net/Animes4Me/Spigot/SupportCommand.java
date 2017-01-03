package net.Animes4Me.Spigot;
	import java.util.Arrays;
	import java.util.List;
	import org.bukkit.ChatColor;
	import org.bukkit.command.Command;
	import org.bukkit.command.CommandExecutor;
	import org.bukkit.command.CommandSender;
	import org.bukkit.command.TabExecutor;
	import org.bukkit.entity.Player;
	import org.bukkit.event.EventHandler;
	import org.bukkit.event.Listener;
	import org.bukkit.event.player.AsyncPlayerChatEvent;

	public class SupportCommand implements CommandExecutor, Listener, TabExecutor {
		private List<String> list;
		
		public SupportCommand() {
			list = Arrays.asList("join", "leave", "help", "forward", "toggle", "reload", "info", "version", "author", "");
			Plugin.instance.getServer().getPluginManager().registerEvents(this, Plugin.instance);
		}
	
		@Override
		public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) { return list; }

		public void sendMessage(Player p, String msg) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
		}
		
		@Override
		public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
			if(sender instanceof Player){
				Player p = (Player) sender;
				
				if(args.length == 1){
					switch(args[0].toLowerCase()){
						case "join":
							if(p.hasPermission("telegramsupport.join")){
								Plugin.instance.supportchat.add(p);
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.support_join"));
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "leave":
							if(p.hasPermission("telegramsupport.leave")){
								Plugin.instance.supportchat.remove(p);
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.support_left"));
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "toggle":
							if(p.hasPermission("telegramsupport.toggle")){
								Plugin.instance.isrunning = false;
								for(Player temp : Plugin.instance.getServer().getOnlinePlayers()){
									if(temp.hasPermission("TelegramSupport.perm.support.toggle")){
										sendMessage(temp, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.toggle_cmd"));
									}
								}
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "forward":
							if(p.hasPermission("telegramsupport.forward")){
								Plugin.instance.config.set("forward", !Plugin.instance.config.getBoolean("forward"));
								for(Player temp : Plugin.instance.getServer().getOnlinePlayers()){
									if(temp.hasPermission("TelegramSupport.perm.support.forward")){
										sendMessage(temp, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.forward_cmd"));
									}
								}
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "reload":
							if(p.hasPermission("telegramsupport.reload")){
								try{
									Plugin.instance.reloadConfig();
									Plugin.instance.config = Plugin.instance.getConfig();
									sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.reload_cmd"));
								}catch(Exception ex){
									System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.config_load_error"))));
									ex.printStackTrace();
								}
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "info":
						case "version":
						case "author":
							if(p.hasPermission("telegramsupport.help")){
								sendMessage(p, "&8------ [&3TelegramSupport&8] ------");
								sendMessage(p, "&3Author: &6undeaD_D");
								sendMessage(p, "&3Version: &6" + Plugin.instance.getDescription().getVersion());
								sendMessage(p, "&3Special thanks to: &6RoWe_ and Proklos");
								sendMessage(p, "&6Library: Spigot");
								sendMessage(p, "&6Copyright 2016 animes4me.net");
								sendMessage(p, "&8-----------------------------");
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "help":
						default:
							if(p.hasPermission("telegramsupport.help")){
								for(String s : Plugin.instance.getConfig_StringList("messages.help_cmd")){
									sendMessage(p, s);
								}
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
					}
				}else{
					sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.wrong_cmd"));
				}
				
			}
			return true;
		}

		@EventHandler
	    public void onChat(AsyncPlayerChatEvent e) {
			if(Plugin.instance.supportchat.contains(e.getPlayer())){
				if(!e.getMessage().startsWith("/")){
					e.setCancelled(true);
					
					Plugin.instance.telegram.send(e.getPlayer().getName() + ": " + e.getMessage());
					
					for(Player temp : Plugin.instance.supportchat){
						sendMessage(temp, Plugin.instance.getConfig_String("messages.prefix") + e.getMessage());
					}
				}
			}else if((!e.getMessage().startsWith("/")) && Plugin.instance.config.getBoolean("forward")){
				Plugin.instance.telegram.send(e.getPlayer().getName() + ": " + e.getMessage());
			}
		}

	}
