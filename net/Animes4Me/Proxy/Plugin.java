package net.Animes4Me.Proxy;
	import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.pengrad.telegrambot.model.Update;

import net.Animes4Me.Utils.Telegram;
import net.Animes4Me.Utils.Telegram.TelegramHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

	public class Plugin extends net.md_5.bungee.api.plugin.Plugin implements TelegramHandler {
		protected static Plugin instance; 
		protected Configuration config;
		protected Telegram telegram;
		protected ArrayList<ProxiedPlayer> supportchat;
		protected boolean isrunning = true;
		protected SupportCommand cmd;
		
		public void onEnable(){
			instance = this;
			
			try{
				loadConfig();
				System.out.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.config_loaded"))));
			}catch(NullPointerException | IOException ex){
				System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.config_load_error"))));
				ex.printStackTrace();
			}
			
			System.out.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.bungeecord"))));
			
			try {
		        Metrics metrics = new Metrics(this);
		        metrics.start();
		        System.out.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.metrics_loaded"))));
		    } catch (IOException ex) {
		    	System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.metrics_error"))));
		    	ex.printStackTrace();
		    }
			
			supportchat = new ArrayList<ProxiedPlayer>();
			cmd = new SupportCommand();
			getProxy().getPluginManager().registerCommand(this, cmd);
			
			try {
				telegram = new Telegram(config.getString("token"), this);
				getProxy().getScheduler().runAsync(this, telegram);
		    } catch (IllegalArgumentException ex) {
		    	System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.telegram_config_error"))));
		    } catch (SecurityException ex) {
		    	System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.spigot_update_available"))));
		    } catch (MalformedURLException ex) {
		    	System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.spigot_update_api_error"))));	
			} catch (Exception e) {
				System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.telegram_api_error"))));
				e.printStackTrace();
			}
		}
	
		public void loadConfig() throws IOException {
			if (!getDataFolder().exists()){
	            getDataFolder().mkdir();
			}
	
	        File file = new File(getDataFolder(), "config.yml");
	
	        if (!file.exists()) {
	            InputStream in = getResourceAsStream("config.yml");
	            Files.copy(in, file.toPath());
	        }
	        
	        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
		}
	
		@Override
		public boolean isRunning() { return isrunning; }
		
		@Override
		public String getVersion() { return getDescription().getVersion(); }
	
		@Override
		public void recieve(Update update) {
			if(update.message().text().startsWith("/")){
				String cmd = update.message().text().substring(1, update.message().text().length()).split(" ")[0].toLowerCase();
				if(config.getString("permissions." + update.message().from().username() + "." + cmd) != null){
					telegram.send("Commands for Bungee Comming soon ...");
				}else{
					telegram.send(config.getString("messages.no_permission").replace("<USERNAME>", update.message().from().username()).replace("<CMD>", cmd));
				}
			}else{
				for(ProxiedPlayer p : supportchat){
					cmd.sendMessage(p, ChatColor.translateAlternateColorCodes('&', "&8[&6" + update.message().from().username()+ "&8] &r" + update.message().text()));
				}
			}
		}
	
		@Override
		public String getConfig_String(String key) { return config.getString(key); }
	
		@Override
		public List<String> getConfig_StringList(String key) { return config.getStringList(key); }
	
		@Override
		public long getChatID() { return config.getLong("chatid"); }
		
	}
