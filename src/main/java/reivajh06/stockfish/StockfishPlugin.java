package reivajh06.stockfish;

import reivajh06.plugins.Plugin;
import reivajh06.plugins.PluginConfiguration;
import reivajh06.plugins.PluginResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Archchess plugin that plays with a local Stockfish installation. */
public class StockfishPlugin implements Plugin {

	private final Path executable;
	private UciEngine engine;
	private long moveTimeMs = 1000;

	/** Required by ServiceLoader: the executable is located in start(). */
	public StockfishPlugin() {
		this(null);
	}

	public StockfishPlugin(Path executable) {
		this.executable = executable;
	}

	@Override
	public String name() {
		return "Stockfish";
	}

	@Override
	public PluginType type() {
		return PluginType.CPU;
	}

	@Override
	public void start() {
		try {
			engine = new UciEngine(executable != null ? executable : StockfishLocator.locate(new String[0]));
			sendParameters(new PluginConfiguration(PluginConfiguration.Difficulty.MAX, moveTimeMs));
		} catch (IOException e) {
			throw new IllegalStateException("Could not start Stockfish: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isReady() {
		return engine != null;
	}

	@Override
	public synchronized PluginResponse requestMove(String fen) {
		try {
			UciEngine.Search search = engine.search(fen, moveTimeMs);
			String move = search.move();
			if (move == null) return new PluginResponse(Map.of("error", "no legal moves"), Map.of());

			Map<String, String> message = new LinkedHashMap<>();
			message.put("from", move.substring(0, 2));
			message.put("to", move.substring(2, 4));
			if (move.length() > 4) message.put("promote_to", promotion(move.charAt(4)));
			return new PluginResponse(message, search.stats());
		} catch (IOException e) {
			return new PluginResponse(Map.of("error", String.valueOf(e.getMessage())), Map.of());
		}
	}

	/** Engines pick the whole move on their own: the legal moves are only needed by AI plugins. */
	@Override
	public PluginResponse requestMove(String fen, String legalMoves) {
		return requestMove(fen);
	}

	@Override
	public synchronized boolean sendParameters(PluginConfiguration configuration) {
		try {
			if (configuration.moveTimeMs() > 0) moveTimeMs = configuration.moveTimeMs();
			int skill = switch (configuration.difficulty()) {
				case EASY -> 1;
				case MEDIUM -> 8;
				case HARD -> 15;
				case MAX -> 20;
			};
			engine.setOption("Skill Level", skill);
			engine.waitReady();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public void stop() {
		if (engine != null) engine.close();
	}

	private static String promotion(char c) {
		return switch (c) {
			case 'q' -> "queen";
			case 'r' -> "rook";
			case 'b' -> "bishop";
			case 'n' -> "knight";
			default -> throw new IllegalArgumentException("Unknown promotion: " + c);
		};
	}
}
