package net.Animes4Me.Spigot;
	import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import net.Animes4Me.Utils.Telegram;
import net.Animes4Me.Utils.Telegram.TelegramHandler;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.pengrad.telegrambot.model.Update;

	public class Plugin extends org.bukkit.plugin.java.JavaPlugin implements TelegramHandler {
		protected static Plugin instance; 
		protected FileConfiguration config;
		protected Telegram telegram;
		protected ArrayList<Player> supportchat;
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
			
			System.out.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.spigot"))));
			
			try {
		        Metrics metrics = new Metrics(this);
		        metrics.start();
		    } catch (IOException ex) {
		    	System.err.println(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',getConfig_String("messages.prefix") + getConfig_String("messages.metrics_error"))));
		    	ex.printStackTrace();
		    }
			
			supportchat = new ArrayList<Player>();
			cmd = new SupportCommand();
			getCommand("support").setExecutor(cmd);
			getCommand("support").setTabCompleter(cmd);
			
			try {
				telegram = new Telegram(config.getString("token"), this);
				getServer().getScheduler().runTaskAsynchronously(this, telegram);
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
	
		private void loadConfig() throws IOException {
			config = getConfig();
			config.options().copyDefaults(true);
			saveConfig();
		}
	
		@Override
		public boolean isRunning() { return isrunning; }

		@Override
		public String getVersion() { return getDescription().getVersion(); }
		
		@Override
		public void recieve(Update update) {
			if(update.message().text().startsWith("/")){
				String cmd = update.message().text().substring(1, update.message().text().length()).split(" ")[0].toLowerCase();
				if(config.getString("permissions." + update.message().from().username() + ".*") != null || config.getString("permissions." + update.message().from().username() + "." + cmd) != null ){
					boolean result = getServer().dispatchCommand(getServer().getConsoleSender(), update.message().text().substring(1, update.message().text().length()));
					if(result){
						telegram.send(config.getString("messages.cmd_success").replace("<CMD>", update.message().text()));
					}else{
						telegram.send(config.getString("messages.cmd_error").replace("<CMD>", update.message().text()));
					}
				}else{
					telegram.send(config.getString("messages.no_permission").replace("<USERNAME>", update.message().from().username()).replace("<CMD>", cmd));
				}
			}else{
				for(Player p : supportchat){
					cmd.sendMessage(p, "&8[&6" + update.message().from().username()+ "&8] &r" + update.message().text());
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
