// Window size (README "Window"). The page is laid out for exactly WIDTH x HEIGHT. On a screen whose
// work area is smaller, main.js shrinks the window in proportion and zooms the page by the same
// factor, so the layout stays the same and only gets smaller.
const WIDTH = 1280;
const HEIGHT = 720;
const MARGIN = 16; // free space left beside the window, both sides together

// area: the screen's work area, { width, height }. Returns 1, or less if the window has to shrink.
function fitScale(area) {
  return Math.min(1, (area.width - MARGIN) / WIDTH, (area.height - MARGIN) / HEIGHT);
}

module.exports = { WIDTH, HEIGHT, fitScale };

// Self-check: node src/fit.js
if (require.main === module) {
  const assert = require('assert');
  assert.strictEqual(fitScale({ width: 1920, height: 1040 }), 1); // 1920x1080 at 100%
  assert.strictEqual(fitScale({ width: 2560, height: 1400 }), 1); // never larger than the layout
  // 1920x1080 at 150%, 1366x768, and two smaller ones: the shrunk window fits both ways
  for (const [width, height] of [[1280, 680], [1366, 728], [1024, 728], [800, 560]]) {
    const s = fitScale({ width, height });
    assert(s > 0 && s < 1, `${width}x${height}: scale ${s}`);
    assert(Math.floor(WIDTH * s) <= width - MARGIN && Math.floor(HEIGHT * s) <= height - MARGIN, `${width}x${height}: does not fit`);
  }
  console.log('fit.js: ok');
}
