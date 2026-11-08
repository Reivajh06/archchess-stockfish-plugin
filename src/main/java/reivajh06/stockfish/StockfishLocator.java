package reivajh06.stockfish;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds the Stockfish executable. The plugin is a single jar: Stockfish is bundled inside it and
 * extracted once to the temp folder. Order:
 * 1. first program argument (Archchess passes an empty one), 2. STOCKFISH_PATH env var,
 * 3. -Dstockfish.path (these three only override the bundled engine), 4. the bundled engine.
 */
final class StockfishLocator {

	private static final String BUNDLED_WINDOWS = "/engines/windows/stockfish.exe";

	private StockfishLocator() {
	}

	static Path locate(String[] args) {
		List<String> overrides = new ArrayList<>();
		if (args.length > 0) overrides.add(args[0]);
		overrides.add(System.getenv("STOCKFISH_PATH"));
		overrides.add(System.getProperty("stockfish.path"));

		for (String candidate : overrides) {
			if (candidate == null || candidate.isBlank()) continue;
			Path path = Path.of(candidate.strip());
			if (Files.isRegularFile(path)) return path;
		}

		Path bundled = extractBundled();
		if (bundled != null) return bundled;

		throw new IllegalStateException("No Stockfish bundled for this operating system. Set STOCKFISH_PATH to an executable.");
	}

	/** Extracts the bundled engine to <tmp>/archchess-stockfish/ (once: later calls reuse the file). */
	private static Path extractBundled() {
		if (!System.getProperty("os.name").toLowerCase().contains("win")) return null;

		URL resource = StockfishLocator.class.getResource(BUNDLED_WINDOWS);
		if (resource == null) return null;

		try {
			long size = resource.openConnection().getContentLengthLong();
			Path dir = Path.of(System.getProperty("java.io.tmpdir"), "archchess-stockfish");
			Files.createDirectories(dir);
			Path target = dir.resolve("stockfish-" + size + ".exe");
			if (Files.isRegularFile(target) && Files.size(target) == size) return target;

			Path partial = Files.createTempFile(dir, "stockfish", ".part");
			try (InputStream in = resource.openStream()) {
				Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
			}
			try {
				Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				// Another process extracted it first (and may be running it): use that copy.
				Files.deleteIfExists(partial);
				if (!Files.isRegularFile(target)) throw e;
			}
			return target;
		} catch (IOException e) {
			throw new IllegalStateException("Could not extract the bundled Stockfish: " + e.getMessage(), e);
		}
	}
}
