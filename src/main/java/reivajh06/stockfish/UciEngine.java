package reivajh06.stockfish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Minimal UCI client around a Stockfish process. */
class UciEngine implements AutoCloseable {

	private final Process process;
	private final BufferedReader in;
	private final BufferedWriter out;

	UciEngine(Path executable) throws IOException {
		process = new ProcessBuilder(executable.toString())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();
		in = process.inputReader();
		out = process.outputWriter();
		send("uci");
		waitFor("uciok");
	}

	void setOption(String name, Object value) throws IOException {
		send("setoption name %s value %s".formatted(name, value));
	}

	void waitReady() throws IOException {
		send("isready");
		waitFor("readyok");
	}

	/**
	 * Result of one search.
	 *
	 * @param move  the best move in UCI notation (e.g. e2e4, e7e8q) or null when there is no legal move
	 * @param stats how hard the engine worked: depth, nodes, nodes per second and time in ms. Deliberately no
	 *              evaluation and no expected line: the player can see these numbers and they would give away the plan.
	 */
	record Search(String move, Map<String, String> stats) {
	}

	private static final Set<String> SAFE_STATS = Set.of("depth", "nodes", "nps", "time");

	Search search(String fen, long moveTimeMs) throws IOException {
		send("position fen " + fen);
		send("go movetime " + moveTimeMs);

		Map<String, String> stats = new LinkedHashMap<>();
		String line;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("info ") && line.contains(" pv ")) {
				// A complete iteration: remember its numbers, the last one is the deepest.
				String[] tokens = line.split("\\s+");
				for (int i = 1; i < tokens.length - 1; i++)
					if (SAFE_STATS.contains(tokens[i])) stats.put(tokens[i], tokens[i + 1]);
			} else if (line.startsWith("bestmove")) {
				String[] parts = line.split("\\s+");
				boolean none = parts.length < 2 || parts[1].equals("(none)") || parts[1].equals("0000");
				return new Search(none ? null : parts[1], stats);
			}
		}
		throw new IOException("Stockfish closed unexpectedly");
	}

	private void send(String command) throws IOException {
		out.write(command);
		out.newLine();
		out.flush();
	}

	private String waitFor(String prefix) throws IOException {
		String line;
		while ((line = in.readLine()) != null) {
			if (line.startsWith(prefix)) return line;
		}
		throw new IOException("Stockfish closed unexpectedly");
	}

	@Override
	public void close() {
		try {
			send("quit");
		} catch (IOException ignored) {
		}
		process.destroy();
	}
}
