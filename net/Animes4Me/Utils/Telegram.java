package net.Animes4Me.Utils;
	import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramBotAdapter;
import com.pengrad.telegrambot.model.Update;

public class Telegram implements Runnable {
	private int offset = -10000;
	private TelegramBot bot;
	private Object plugin;
	
	public Telegram(String bot_token, Object plugin) throws Exception{
		if(plugin instanceof TelegramHandler){
			
			if(checkversion((TelegramHandler)plugin).equalsIgnoreCase((((TelegramHandler)plugin).getVersion()))){
				if(bot_token.equals("<INSERT TOKEN HERE>") || ((TelegramHandler)plugin).getChatID() == -1){
					throw new IllegalArgumentException();
				}else{
					this.plugin = plugin;
					bot = TelegramBotAdapter.build(bot_token);
				}
			}else{
				throw new SecurityException();
			}
			
		}else{
			throw new RuntimeException();
		}
	}


	private String checkversion(TelegramHandler plugin) throws MalformedURLException {
		HttpURLConnection con = null;
		try {
            con = (HttpURLConnection) new URL("https://api.inventivetalent.org/spigot/resource-simple/19245").openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            reader.readLine();reader.readLine();
            String version = reader.readLine().substring(16, 19); 
            return version;
        } catch (Exception ex) {
        	throw new MalformedURLException();
        }finally{
        	con.disconnect();
        }
	}


	@Override
	public void run() {
		while (((TelegramHandler)plugin).isRunning()) {
			List<Update> updates = bot.getUpdates(offset, null, null).updates();

			if (updates.size() > 0) {
				try {
					for (Update d : updates) {
						if(d.message() != null && d.message().text() != null){
							((TelegramHandler)plugin).recieve(d);
							offset = d.updateId() + 1;
						}
					}
				} catch (Exception ex) {System.out.println(((TelegramHandler)plugin).getConfig_String("messages.prefix") + ((TelegramHandler)plugin).getConfig_String("messages.telegram_api_error"));}
			}

			try { Thread.sleep(1000); } catch (Exception e) {System.out.println(((TelegramHandler)plugin).getConfig_String("messages.prefix") + ((TelegramHandler)plugin).getConfig_String("messages.thread_sleep_error"));}
		}
	}

	public void send(String msg){
		if(((TelegramHandler)plugin).isRunning()){
			bot.sendMessage(((TelegramHandler)plugin).getChatID(), msg, null, false, null, null);
		}
	}
	
	public abstract interface TelegramHandler {
		public boolean isRunning();
				
		public void recieve(Update update);
		
		public long getChatID();
		
		public String getConfig_String(String key);
		
		public List<String> getConfig_StringList(String key);
		
		public String getVersion();
	}
	
	
}
