package net.newgen.client.mods.hud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Pure maths and state for the Wave 3 HUD 2 mods (no Minecraft classes, so it is unit-tested). */
final class HudLogic2 {
	private HudLogic2() {
	}

	/** "E: 12/345, SD: 12" (LevelRenderer.getEntityStatistics) to {rendered, total}; null if it does not parse. */
	static int[] parseEntityStats(String s) {
		if (s == null) return null;
		int colon = s.indexOf(':'), slash = s.indexOf('/'), comma = s.indexOf(',');
		if (colon < 0 || slash < colon) return null;
		int end = comma > slash ? comma : s.length();
		try {
			return new int[]{Integer.parseInt(s.substring(colon + 1, slash).trim()), Integer.parseInt(s.substring(slash + 1, end).trim())};
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** A short label for a key name, at most 3 characters: "1" stays, "Left Shift" = "LS", "Mouse Button 4" = "MB4". */
	static String shortKey(String name) {
		if (name == null) return "";
		name = name.trim();
		if (name.length() <= 3) return name;
		StringBuilder out = new StringBuilder();
		for (String word : name.split("[\\s_-]+")) {
			if (word.isEmpty()) continue;
			if (word.chars().allMatch(Character::isDigit)) out.append(word);
			else out.append(Character.toUpperCase(word.charAt(0)));
		}
		String s = out.toString();
		return s.length() > 3 ? s.substring(0, 3) : s;
	}

	/**
	 * Where a world direction lands on the screen: camera yaw/pitch in degrees (Minecraft convention), vertical FOV in degrees,
	 * screen size. Returns {x, y} in screen pixels, or null when the direction points behind the camera.
	 */
	static double[] project(double dx, double dy, double dz, double yaw, double pitch, double fov, double width, double height) {
		double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
		double fx = -Math.sin(y) * Math.cos(p), fy = -Math.sin(p), fz = Math.cos(y) * Math.cos(p);
		double rx = -Math.cos(y), ry = 0, rz = -Math.sin(y);
		// up = right x forward
		double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
		double cz = dx * fx + dy * fy + dz * fz;
		if (cz <= 1e-6) return null;
		double cx = dx * rx + dy * ry + dz * rz, cy = dx * ux + dy * uy + dz * uz;
		double t = Math.tan(Math.toRadians(fov) / 2);
		double ndcX = cx / cz / (t * width / height), ndcY = cy / cz / t;
		return new double[]{width / 2 * (1 + ndcX), height / 2 * (1 - ndcY)};
	}

	/** Successful block placements over time: per second, per minute, total, with an optional automatic reset. */
	static final class PlaceCounter {
		private final ArrayDeque<Long> times = new ArrayDeque<>();
		private int total;
		private long since = -1;

		void place(long now) {
			if (since < 0) since = now;
			times.addLast(now);
			total++;
			prune(now);
		}

		/** Clears the count when the interval (ms, 0 = never) has run out since the last reset. */
		void autoReset(long now, long interval) {
			if (since < 0) since = now;
			if (interval > 0 && now - since >= interval) reset(now);
		}

		void reset(long now) {
			times.clear();
			total = 0;
			since = now;
		}

		private void prune(long now) {
			while (!times.isEmpty() && now - times.peekFirst() >= 60_000) times.removeFirst();
		}

		int within(long now, long window) {
			prune(now);
			int n = 0;
			for (long t : times) if (now - t < window) n++;
			return n;
		}

		int total() {
			return total;
		}
	}

	/** The mouse strokes dot: moved by the turn of each frame, pulled back to the centre, with a trail of past places. */
	static final class MouseTrail {
		double x, y;
		final List<double[]> points = new ArrayList<>();

		/** dx/dy: turn this frame (degrees), already scaled; half: the dot stays inside +-half; pull: 0..1 per 1/60 s. */
		void update(double dx, double dy, double seconds, double pull, double half, int length) {
			x = clamp(x + dx, half);
			y = clamp(y + dy, half);
			double keep = Math.pow(1 - Math.max(0, Math.min(1, pull)), seconds * 60);
			x *= keep;
			y *= keep;
			points.add(new double[]{x, y});
			while (points.size() > Math.max(1, length)) points.remove(0);
		}

		private static double clamp(double v, double half) {
			return Math.max(-half, Math.min(half, v));
		}
	}
}
