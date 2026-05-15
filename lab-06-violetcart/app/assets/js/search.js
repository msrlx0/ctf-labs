function violetSearchNotice(message) {
  const node = document.querySelector("[data-search-proof]");
  if (node) {
    node.textContent = message;
  }
}

window.violetSearchNotice = violetSearchNotice;
