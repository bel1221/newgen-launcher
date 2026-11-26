package net.newgen.client.mods.controls;

/** One press switches it on, the next press switches it off. Feed it the key state once per tick or frame. */
public final class ToggleState {
	private boolean on, wasDown;

	/** Returns the state after this key reading. */
	public boolean update(boolean down) {
		if (down && !wasDown) on = !on;
		wasDown = down;
		return on;
	}

	public boolean on() {
		return on;
	}

	public void set(boolean value) {
		on = value;
	}
}
