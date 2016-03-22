package net.Animes4Me.Proxy;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

public class Metrics {

	private final static int REVISION = 7;
	private static final String BASE_URL = "http://report.mcstats.org";
	private static final String REPORT_URL = "/plugin/%s";
	private static final int PING_INTERVAL = 15;
	private final Plugin plugin;
	private final Set<Graph> graphs = Collections.synchronizedSet(new HashSet<Graph>());
	private final Properties properties = new Properties();
	private final File configurationFile;
	private final String guid;
	private final boolean debug;
	private final Object optOutLock = new Object();
	private ScheduledTask thread = null;

	
	public Metrics(final Plugin plugin) throws IOException {
		if (plugin == null) {
			throw new IllegalArgumentException("Plugin cannot be null");
		}

		this.plugin = plugin;

		configurationFile = getConfigFile();

		if (!configurationFile.exists()) {
			if (configurationFile.getPath().contains("/")
					|| configurationFile.getPath().contains("\\")) {
				File parent = new File(configurationFile.getParent());
				if (!parent.exists()) {
					parent.mkdir();
				}
			}

			configurationFile.createNewFile(); // config file
			properties.put("opt-out", "false");
			properties.put("guid", UUID.randomUUID().toString());
			properties.put("debug", "false");
			properties.store(new FileOutputStream(configurationFile),
					"http://mcstats.org");
		} else {
			properties.load(new FileInputStream(configurationFile));
		}

		guid = properties.getProperty("guid");
		debug = Boolean.parseBoolean(properties.getProperty("debug"));
	}

	public Graph createGraph(final String name) {
		if (name == null) {
			throw new IllegalArgumentException("Graph name cannot be null");
		}

		final Graph graph = new Graph(name);
		graphs.add(graph);
		return graph;
	}

	public void addGraph(final Graph graph) {
		if (graph == null) {
			throw new IllegalArgumentException("Graph cannot be null");
		}

		graphs.add(graph);
	}

	public boolean start() {
		synchronized (optOutLock) {
			if (isOptOut()) {
				return false;
			}

			if (thread != null) {
				return true;
			}

			thread = ProxyServer.getInstance().getScheduler()
					.runAsync(plugin, new Runnable() {

						private boolean firstPost = true;

						private long nextPost = 0L;

						public void run() {
							while (thread != null) {
								if (nextPost == 0L
										|| System.currentTimeMillis() > nextPost) {
									try {
										synchronized (optOutLock) {
											if (isOptOut() && thread != null) {
												ScheduledTask temp = thread;
												thread = null;
												for (Graph graph : graphs) {
													graph.onOptOut();
												}
												temp.cancel();
												;
												return;
											}
										}

										postPlugin(!firstPost);

										firstPost = false;
										nextPost = System.currentTimeMillis() + (PING_INTERVAL * 60 * 1000);
									} catch (IOException e) {
										if (debug) {
											System.out.println("[Metrics] "
													+ e.getMessage());
										}
									}
								}

								try {
									Thread.sleep(100L);
								} catch (InterruptedException e) {}
							}
						}
					});

			return true;
		}
	}

	public boolean isOptOut() {
		synchronized (optOutLock) {
			try {
				properties.load(new FileInputStream(configurationFile));
			} catch (IOException ex) {
				if (debug) {
					ProxyServer.getInstance().getLogger()
							.log(Level.INFO, "[Metrics] " + ex.getMessage());
				}
				return true;
			}

			return Boolean.parseBoolean(properties.getProperty("opt-out"));
		}
	}

	public void enable() throws IOException {
		synchronized (optOutLock) {
			
			if (isOptOut()) {
				properties.setProperty("opt-out", "false");
				properties.store(new FileOutputStream(configurationFile),
						"http://mcstats.org");
			}

			if (thread == null) {
				start();
			}
		}
	}

	public void disable() throws IOException {
		synchronized (optOutLock) {

			if (!isOptOut()) {
				properties.setProperty("opt-out", "true");
				properties.store(new FileOutputStream(configurationFile),
						"http://mcstats.org");
			}

			if (thread != null) {
				thread.cancel();
				thread = null;
			}
		}
	}

	public File getConfigFile() {
		return new File(new File("plugins", "PluginMetrics"), "config.properties");
	}

	private void postPlugin(final boolean isPing) throws IOException {
		PluginDescription description = plugin.getDescription();
		String pluginName = description.getName();
		boolean onlineMode = ProxyServer.getInstance().getConfigurationAdapter().getBoolean("online_mode", true);
		String pluginVersion = description.getVersion();
		String serverVersion = ProxyServer.getInstance().getVersion();
		int playersOnline = ProxyServer.getInstance().getPlayers().size();

		StringBuilder json = new StringBuilder(1024);
		json.append('{');

		appendJSONPair(json, "guid", guid);
		appendJSONPair(json, "plugin_version", pluginVersion);
		appendJSONPair(json, "server_version", serverVersion);
		appendJSONPair(json, "players_online", Integer.toString(playersOnline));

		String osname = System.getProperty("os.name");
		String osarch = System.getProperty("os.arch");
		String osversion = System.getProperty("os.version");
		String java_version = System.getProperty("java.version");
		int coreCount = Runtime.getRuntime().availableProcessors();

		if (osarch.equals("amd64")) {
			osarch = "x86_64";
		}

		appendJSONPair(json, "osname", osname);
		appendJSONPair(json, "osarch", osarch);
		appendJSONPair(json, "osversion", osversion);
		appendJSONPair(json, "cores", Integer.toString(coreCount));
		appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
		appendJSONPair(json, "java_version", java_version);

		if (isPing) {
			appendJSONPair(json, "ping", "1");
		}

		if (graphs.size() > 0) {
			synchronized (graphs) {
				json.append(',');
				json.append('"');
				json.append("graphs");
				json.append('"');
				json.append(':');
				json.append('{');

				boolean firstGraph = true;

				final Iterator<Graph> iter = graphs.iterator();

				while (iter.hasNext()) {
					Graph graph = iter.next();

					StringBuilder graphJson = new StringBuilder();
					graphJson.append('{');

					for (Plotter plotter : graph.getPlotters()) {
						appendJSONPair(graphJson, plotter.getColumnName(),
								Integer.toString(plotter.getValue()));
					}

					graphJson.append('}');

					if (!firstGraph) {
						json.append(',');
					}

					json.append(escapeJSON(graph.getName()));
					json.append(':');
					json.append(graphJson);

					firstGraph = false;
				}

				json.append('}');
			}
		}

		json.append('}');

		URL url = new URL(BASE_URL + String.format(REPORT_URL, urlEncode(pluginName)));
		URLConnection connection;

		if (isMineshafterPresent()) {
			connection = url.openConnection(Proxy.NO_PROXY);
		} else {
			connection = url.openConnection();
		}

		byte[] uncompressed = json.toString().getBytes();
		byte[] compressed = gzip(json.toString());

		connection.addRequestProperty("User-Agent", "MCStats/" + REVISION);
		connection.addRequestProperty("Content-Type", "application/json");
		connection.addRequestProperty("Content-Encoding", "gzip");
		connection.addRequestProperty("Content-Length",
				Integer.toString(compressed.length));
		connection.addRequestProperty("Accept", "application/json");
		connection.addRequestProperty("Connection", "close");

		connection.setDoOutput(true);

		if (debug) {
			System.out.println("[Metrics] Prepared request for " + pluginName
					+ " uncompressed=" + uncompressed.length + " compressed="
					+ compressed.length);
		}

		OutputStream os = connection.getOutputStream();
		os.write(compressed);
		os.flush();

		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		String response = reader.readLine();

		os.close();
		reader.close();

		if (response == null || response.startsWith("ERR")
				|| response.startsWith("7")) {
			if (response == null) {
				response = "null";
			} else if (response.startsWith("7")) {
				response = response
						.substring(response.startsWith("7,") ? 2 : 1);
			}

			throw new IOException(response);
		} else {
			if (response.equals("1")
					|| response.contains("This is your first update this hour")) {
				synchronized (graphs) {
					final Iterator<Graph> iter = graphs.iterator();

					while (iter.hasNext()) {
						final Graph graph = iter.next();

						for (Plotter plotter : graph.getPlotters()) {
							plotter.reset();
						}
					}
				}
			}
		}
	}

	public static byte[] gzip(String input) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = null;

		try {
			gzos = new GZIPOutputStream(baos);
			gzos.write(input.getBytes("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (gzos != null)
				try {
					gzos.close();
				} catch (IOException ignore) {
				}
		}

		return baos.toByteArray();
	}

	private boolean isMineshafterPresent() {
		try {
			Class.forName("mineshafter.MineServer");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static void appendJSONPair(StringBuilder json, String key,
			String value) throws UnsupportedEncodingException {
		boolean isValueNumeric = false;

		try {
			if (value.equals("0") || !value.endsWith("0")) {
				Double.parseDouble(value);
				isValueNumeric = true;
			}
		} catch (NumberFormatException e) {
			isValueNumeric = false;
		}

		if (json.charAt(json.length() - 1) != '{') {
			json.append(',');
		}

		json.append(escapeJSON(key));
		json.append(':');

		if (isValueNumeric) {
			json.append(value);
		} else {
			json.append(escapeJSON(value));
		}
	}

	private static String escapeJSON(String text) {
		StringBuilder builder = new StringBuilder();

		builder.append('"');
		for (int index = 0; index < text.length(); index++) {
			char chr = text.charAt(index);

			switch (chr) {
			case '"':
			case '\\':
				builder.append('\\');
				builder.append(chr);
				break;
			case '\b':
				builder.append("\\b");
				break;
			case '\t':
				builder.append("\\t");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\r':
				builder.append("\\r");
				break;
			default:
				if (chr < ' ') {
					String t = "000" + Integer.toHexString(chr);
					builder.append("\\u" + t.substring(t.length() - 4));
				} else {
					builder.append(chr);
				}
				break;
			}
		}
		builder.append('"');

		return builder.toString();
	}

	private static String urlEncode(final String text) throws UnsupportedEncodingException {
		return URLEncoder.encode(text, "UTF-8");
	}

	public static class Graph {

		private final String name;

		private final Set<Plotter> plotters = new LinkedHashSet<Plotter>();

		private Graph(final String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void addPlotter(final Plotter plotter) {
			plotters.add(plotter);
		}

		public void removePlotter(final Plotter plotter) {
			plotters.remove(plotter);
		}

		public Set<Plotter> getPlotters() {
			return Collections.unmodifiableSet(plotters);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(final Object object) {
			if (!(object instanceof Graph)) {
				return false;
			}

			final Graph graph = (Graph) object;
			return graph.name.equals(name);
		}

		protected void onOptOut() {
		}
	}

	public static abstract class Plotter {

		private final String name;

		public Plotter() {
			this("Default");
		}

		public Plotter(final String name) {
			this.name = name;
		}

		public abstract int getValue();

		public String getColumnName() {
			return name;
		}

		public void reset() {
		}

		@Override
		public int hashCode() {
			return getColumnName().hashCode();
		}

		@Override
		public boolean equals(final Object object) {
			if (!(object instanceof Plotter)) {
				return false;
			}

			final Plotter plotter = (Plotter) object;
			return plotter.name.equals(name)
					&& plotter.getValue() == getValue();
		}
	}
}
