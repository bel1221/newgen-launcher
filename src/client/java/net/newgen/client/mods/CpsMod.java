package net.newgen.client.mods;

import java.util.ArrayDeque;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.newgen.client.core.HudMod;
import net.newgen.client.core.Setting;

public class CpsMod extends HudMod {
	private final Setting.Bool rightButton = bool("right_button", "Show right button", true);
	private final Setting.Bool label = bool("label", "Show \"CPS\" label", true);

	// Times (ms) of the clicks made in the last second.
	private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
	private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();
	private boolean leftWasDown, rightWasDown;

	public CpsMod() {
		super("cps", "CPS", "Clicks per second", 0f, 0.09f);
	}

	@Override
	protected List<String> lines(Minecraft mc) {
		// shortcut: buttons are sampled once per frame, so a press+release inside one frame is missed
		// (matters only at very low FPS). Upgrade to a mouse-button mixin if exact counts are needed.
		long now = System.currentTimeMillis();
		boolean left = mc.mouseHandler.isLeftPressed();
		boolean right = mc.mouseHandler.isRightPressed();
		if (left && !leftWasDown) leftClicks.add(now);
		if (right && !rightWasDown) rightClicks.add(now);
		leftWasDown = left;
		rightWasDown = right;
		while (!leftClicks.isEmpty() && now - leftClicks.peek() > 1000) leftClicks.poll();
		while (!rightClicks.isEmpty() && now - rightClicks.peek() > 1000) rightClicks.poll();

		String text = String.valueOf(leftClicks.size());
		if (rightButton.value) text += " | " + rightClicks.size();
		if (label.value) text += " CPS";
		return List.of(text);
	}
}
