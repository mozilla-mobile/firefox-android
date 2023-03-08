"use strict";

/**
 * Bug 1819678 - cnki.net - Cannot use search field
 * WebCompat issue #115777 - https://webcompat.com/issues/115777
 *
 * This patch ensures that the search input never has the [disabled]
 * attribute, so that users may tap/click on it to search.
 *
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=1819678 for details.
 */

console.info(
  "search input disabled attribute was removed for compatibility reasons. See https://webcompat.com/issues/115777 for details."
);

const SELECTOR = `.searchimg[disabled]`;

function check(target) {
  if (target.nodeName === "INPUT" && target.matches(SELECTOR)) {
    target.removeAttribute("disabled");
    return true;
  }
  return false;
}

new MutationObserver(mutations => {
  for (const { addedNodes, target, attributeName } of mutations) {
    if (attributeName === "disabled") {
      check(target);
    } else {
      addedNodes?.forEach(node => {
        if (!check(node)) {
          node
            .querySelectorAll?.(SELECTOR)
            ?.forEach(n => n.removeAttribute("disabled"));
        }
      });
    }
  }
}).observe(document, { attributes: true, childList: true, subtree: true });
