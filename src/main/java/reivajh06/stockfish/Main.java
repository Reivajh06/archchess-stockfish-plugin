package reivajh06.stockfish;

import reivajh06.plugins.PluginConfiguration;
import reivajh06.plugins.PluginResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Archchess protocol over stdin/stdout. Every reply line starts with "$archchess:".
 * <pre>
 *   name                      -> $archchess:Stockfish
 *   move FEN                  -> $archchess:{"from":"e2","to":"e4"[,"promote_to":"queen"]}
 *   config DIFFICULTY [ms]    -> $archchess:{"ok":"true"}
 *   exit                      -> terminates
 * </pre>
 * Errors are reported as {"error":"..."}.
 */
public class Main {

	private static final String PREFIX = "$archchess:";

	public static void main(String[] args) throws Exception {
		PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
		StockfishPlugin plugin;
		try {
			plugin = new StockfishPlugin(StockfishLocator.locate(args));
			plugin.start();
		} catch (RuntimeException e) {
			out.println(PREFIX + toJson(Map.of("error", String.valueOf(e.getMessage()))));
			return;
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		String line;
		while ((line = in.readLine()) != null) {
			line = line.strip();
			if (line.equals("exit")) break;
			if (line.equals("name")) {
				out.println(PREFIX + plugin.name());
			} else if (line.startsWith("move ")) {
				PluginResponse response = plugin.requestMove(line.substring(5).strip());
				out.println(PREFIX + withDetails(response));
			} else if (line.startsWith("config ")) {
				String[] p = line.split("\\s+");
				try {
					var difficulty = PluginConfiguration.Difficulty.valueOf(p[1].toUpperCase());
					long ms = p.length > 2 ? Long.parseLong(p[2]) : 0;
					boolean ok = plugin.sendParameters(new PluginConfiguration(difficulty, ms));
					out.println(PREFIX + toJson(Map.of("ok", String.valueOf(ok))));
				} catch (RuntimeException e) {
					out.println(PREFIX + toJson(Map.of("error", "invalid config")));
				}
			}
		}
		plugin.stop();
	}

	private static String toJson(Map<String, String> map) {
		return map.entrySet().stream()
				.map(e -> "\"%s\":\"%s\"".formatted(escape(e.getKey()), escape(e.getValue())))
				.collect(Collectors.joining(",", "{", "}"));
	}

	/** The move as JSON, plus a "details" object when the plugin reported any. */
	private static String withDetails(PluginResponse response) {
		String json = toJson(response.message());
		if (response.details() == null || response.details().isEmpty()) return json;
		return json.substring(0, json.length() - 1) + ",\"details\":" + toJson(response.details()) + "}";
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", " ");
	}
}
