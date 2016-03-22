package net.Animes4Me.Proxy;
	import java.util.Arrays;
	import net.md_5.bungee.api.ChatColor;
	import net.md_5.bungee.api.CommandSender;
	import net.md_5.bungee.api.chat.TextComponent;
	import net.md_5.bungee.api.connection.ProxiedPlayer;
	import net.md_5.bungee.api.event.ChatEvent;
	import net.md_5.bungee.api.plugin.Command;
	import net.md_5.bungee.api.plugin.Listener;
	import net.md_5.bungee.api.plugin.TabExecutor;
	import net.md_5.bungee.event.EventHandler;

	public class SupportCommand extends Command implements Listener, TabExecutor {
		private Iterable<String> list;
		
		public SupportCommand() {
			super("support", "telegramsupport.help", "sup");
			list = Arrays.asList("join", "leave", "help", "forward", "toggle", "reload", "info", "version", "author", "");
			Plugin.instance.getProxy().getPluginManager().registerListener(Plugin.instance, this);
		}
	
		@Override
		public void execute(CommandSender sender, String[] args) {
			if(sender instanceof ProxiedPlayer){
				ProxiedPlayer p = (ProxiedPlayer) sender;
				
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
								for(ProxiedPlayer temp : Plugin.instance.getProxy().getPlayers()){
									if(temp.hasPermission("telegramsupport.toggle")){
										sendMessage(temp, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.toggle_cmd"));
									}
								}
							}else{
								sendMessage(p, Plugin.instance.getConfig_String("messages.prefix") + Plugin.instance.getConfig_String("messages.no_perm"));
							}
							break;
						case "forward":
							if(p.hasPermission("telegramsupport.forward")){
								Plugin.instance.config.set("forward", Plugin.instance.config.getBoolean("forward"));
								for(ProxiedPlayer temp : Plugin.instance.getProxy().getPlayers()){
									if(temp.hasPermission("telegramsupport.forward")){
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
									Plugin.instance.loadConfig();
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
								sendMessage(p, "&6Library: Bungeecord");
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
		}
	
		@Override
	    public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) { return list; }
				
		public void sendMessage(ProxiedPlayer p, String msg) {
			p.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', msg)));
		}
		
		@EventHandler
	    public void onChat(ChatEvent e) {
			if(e.getSender() instanceof ProxiedPlayer){
				if(Plugin.instance.supportchat.contains((ProxiedPlayer) e.getSender())){
					if(!e.getMessage().startsWith("/")){
						e.setCancelled(true);
						
						Plugin.instance.telegram.send(((ProxiedPlayer) e.getSender()).getName() + ": " + e.getMessage());
						
						for(ProxiedPlayer temp : Plugin.instance.supportchat){
							sendMessage(temp, Plugin.instance.getConfig_String("messages.prefix") + e.getMessage());
						}
					}
				}else if((!e.getMessage().startsWith("/")) && Plugin.instance.config.getBoolean("forward")){
					Plugin.instance.telegram.send(((ProxiedPlayer) e.getSender()).getName() + ": " + e.getMessage());
				}
			}
		}
		
	}
