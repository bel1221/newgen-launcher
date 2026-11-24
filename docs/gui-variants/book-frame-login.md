# The book login (login screen until 2026-11-25)

Until 2026-11-25 the login screen was the in-game LOG IN book: a see-through frame with two pages in the middle of the window and a small LOG IN title frame on its top edge. Left page: ACCOUNT, PASSWORD and the LOG IN button. Right page: the logo and, under it, the offline note or a login error. It was replaced by the login bar (README "Branding"). Its markup, its CSS rules and its two lines of script are kept here, as they were, so it can be put back.

**To put it back:** in `src/renderer/index.html` put the markup below in place of the `<main id="login-view" ...>` block and delete the `<img class="logo" ...>` line in `#titlebar` (the book carries its own logo); paste the CSS below after the last rule of `src/renderer/style.css`; and in `init()` in `src/renderer/renderer.js` put the script lines below in place of the `if (!info.authEnabled) { ... }` block.

The Play frame showed the logo too, as its first line under the title. To have that back as well, add `<img class="logo" src="assets/lockup.png" alt="NewGen">` under the `<h1>` in `<main id="play-view">`; its size rule is the last line of the CSS below.

## Markup (`src/renderer/index.html`)

```html
  <!-- Login view: the in-game LOG IN book, as a see-through frame with two pages.
       Left page: the form. Right page: the logo and, below it, the offline note or a login error. -->
  <main id="login-view" class="book">
    <h1 class="plaque">Log in</h1>
    <form id="login-form" class="page">
      <label>Account <input id="username" autocomplete="username" maxlength="16" spellcheck="false" required></label>
      <label id="password-row">Password <input id="password" type="password" autocomplete="current-password"></label>
      <button type="submit" id="login-btn">Log in</button>
    </form>
    <div class="page">
      <img class="logo" src="assets/lockup.png" alt="NewGen">
      <p id="offline-note" class="hidden">Offline mode: no password needed.</p>
      <p id="login-error" class="error" role="alert"></p>
    </div>
  </main>
```

## CSS (`src/renderer/style.css`)

The first three rules were not rules of their own: `--orange-hi` was a variable in `:root`, and `.book` and `.page` were names in the selector lists of "Shared pixel construction" and "Frames". The rest is word for word.

```css
/* ---------- Login: the book (docs/gui-variants/book-frame-login.md) ---------- */
.book { --orange-hi: #fff6cc; } /* LOG IN title lettering */
.book, .page {
  border: 0;
  box-shadow:
    inset var(--px) var(--px) 0 var(--hi, transparent),
    inset calc(-1 * var(--px)) calc(-1 * var(--px)) 0 var(--lo, transparent),
    calc(-1 * var(--px)) 0 0 var(--o), var(--px) 0 0 var(--o),
    0 calc(-1 * var(--px)) 0 var(--o), 0 var(--px) 0 var(--o);
}
.page { --hi: var(--line); --lo: var(--line); background: var(--glass); }
.logo { display: block; margin: 0 auto; } /* smooth cartoon art, so scaled normally */

/* The book carries the dark outline; each of its two pages carries its own light line. The gap
   between the pages is the spine: one more piece of the outline, drawn as the book's background. */
.book {
  position: relative; display: grid; grid-template-columns: 1fr 1fr; gap: var(--px); width: 522px;
  --o: var(--cover); --line: var(--frame);
  background: linear-gradient(var(--o), var(--o)) center / var(--px) 100% no-repeat;
}
/* The whole login book uses the Minecraft font. It is drawn on an 8px grid, so the size is
   16px (one font pixel = one GUI pixel). 24px would land off the grid. */
.book, .book :is(label, input, button, .plaque) { font-family: "Minecraft", "Pixelify Sans", sans-serif; }
.book .plaque { color: var(--orange-hi); }
.page {
  /* as tall as the form with both fields, so the book is the same size in offline mode */
  min-height: 236px; padding: 24px;
  display: flex; flex-direction: column; justify-content: center; gap: 16px;
  --o: transparent; /* the outline round the pages is the book's */
}
.book .logo { height: 50px; }
label { display: block; }
/* Typing fields, drawn the way Minecraft draws its own: a dark see-through box with a solid line. */
input {
  display: block; width: 100%; margin-top: calc(3 * var(--px)); padding: 8px 10px;
  --o: var(--page-edge); background: var(--well);
  color: var(--white); font-size: 16px; line-height: 20px;
}
.page p { font-size: 16px; line-height: 20px; }

.book button {
  padding: calc(6 * var(--px));
  --o: var(--blue-out); --hi: var(--blue-hi); --lo: var(--blue-lo);
  background: var(--blue); color: var(--white); text-shadow: var(--px) var(--px) 0 var(--blue-out);
}
/* Half-pixel nudges: these centred labels are an odd number of pixels wide, so centring would put
   them half a pixel off and blur the pixel font. 1px of extra padding lands them on a whole pixel.
   Re-check if the wording of a label changes. */
.book button { padding-left: calc(6 * var(--px) + 1px); }
.book .plaque { padding-left: calc(8 * var(--px) + 1px); }
.book button:active:not(:disabled) { --hi: var(--blue-lo); --lo: var(--blue-hi); }
.book button:disabled { --hi: var(--blue-lo); --lo: var(--blue-lo); background: var(--blue-lo); color: var(--page-edge); }

.panel .logo { height: 44px; } /* only for the logo in the Play frame, see above */
```

## Script (`src/renderer/renderer.js`, in `init()`)

```js
  if (!info.authEnabled) {
    $('password-row').classList.add('hidden');
    $('offline-note').classList.remove('hidden');
  }
```
