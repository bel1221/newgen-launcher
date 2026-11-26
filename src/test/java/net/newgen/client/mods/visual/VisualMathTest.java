package net.newgen.client.mods.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Zoom wheel and FOV factor maths (no game needed). */
class VisualMathTest {
	@Test
	void scrollZoomStepsAndClamps() {
		assertEquals(5, VisualMath.scrollZoom(4, 1, 1.1, 50), 1e-9);
		assertEquals(3.2, VisualMath.scrollZoom(4, -1, 1.1, 50), 1e-9);
		assertEquals(50, VisualMath.scrollZoom(45, 3, 1.1, 50), 1e-9);
		assertEquals(1.1, VisualMath.scrollZoom(1.2, -5, 1.1, 50), 1e-9);
		assertEquals(4, VisualMath.scrollZoom(4, 0, 1.1, 50), 1e-9);
		// A smooth-scrolling touchpad sends fractions of a notch: the zoom moves by that fraction.
		assertEquals(4 * Math.pow(1.25, 0.3), VisualMath.scrollZoom(4, 0.3, 1.1, 50), 1e-9);
	}

	@Test
	void easingCurvesRunFromZeroToOne() {
		for (String c : new String[] {"Linear", "Ease in", "Ease out", "Ease in-out"}) {
			assertEquals(0, VisualMath.ease(c, 0), 1e-6, c);
			assertEquals(1, VisualMath.ease(c, 1), 1e-6, c);
			assertEquals(1, VisualMath.ease(c, 2), 1e-6, c);
		}
		assertEquals(0.25f, VisualMath.ease("Ease in", 0.5f), 1e-6);
		assertEquals(0.75f, VisualMath.ease("Ease out", 0.5f), 1e-6);
		assertEquals(0.5f, VisualMath.ease("Ease in-out", 0.5f), 1e-6);
	}

	@Test
	void fovFactorsOfOneMatchVanilla() {
		// Sprinting: speed ratio 1.3 → (1.3 + 1) / 2.
		assertEquals(1.15f, VisualMath.fovModifier(false, 1.3f, 1.3f, 0, 1, 1, 1, 1, 1), 1e-5);
		// Flying and walking: 1.1.
		assertEquals(1.1f, VisualMath.fovModifier(true, 1, 1, 0, 1, 1, 1, 1, 1), 1e-5);
		// Full bow pull: 1 - 0.15.
		assertEquals(0.85f, VisualMath.fovModifier(false, 1, 1, 1, 1, 1, 1, 1, 1), 1e-5);
		// Sprinting with Speed I (1.2 * 1.3).
		assertEquals((1.56f + 1) / 2, VisualMath.fovModifier(false, 1.56f, 1.3f, 0, 1, 1, 1, 1, 1), 1e-5);
	}

	@Test
	void fovFactorsScaleEachCause() {
		// No sprint effect, but Speed I still counts.
		assertEquals((1.2f + 1) / 2, VisualMath.fovModifier(false, 1.56f, 1.3f, 0, 1, 0, 1, 1, 1), 1e-5);
		// No speed effect, sprint still counts.
		assertEquals(1.15f, VisualMath.fovModifier(false, 1.56f, 1.3f, 0, 1, 1, 0, 1, 1), 1e-5);
		// Slowness halved: ratio 0.85 → 0.925.
		assertEquals((0.925f + 1) / 2, VisualMath.fovModifier(false, 0.85f, 1, 0, 1, 1, 1, 0.5, 1), 1e-5);
		// Everything off: 1.
		assertEquals(1f, VisualMath.fovModifier(true, 1.56f, 1.3f, 1, 0, 0, 0, 0, 0), 1e-5);
		// Double bow.
		assertEquals(0.7f, VisualMath.fovModifier(false, 1, 1, 1, 1, 1, 1, 1, 2), 1e-5);
	}
}
