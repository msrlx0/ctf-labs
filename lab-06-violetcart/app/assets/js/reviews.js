function violetReviewProof(value) {
  window.violetProof = value || "review-proof";
  const node = document.querySelector("[data-review-proof]");
  if (node) {
    node.textContent = "Local review proof set";
  }
}

window.violetReviewProof = violetReviewProof;
