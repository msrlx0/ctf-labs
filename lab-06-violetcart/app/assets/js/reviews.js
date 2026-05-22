function violetReviewProof(value) {
  window.violetProof = value || "review-proof";
  const node = document.querySelector("[data-review-proof]");
  if (node) {
    node.textContent = "Review renderer QA proof accepted.";
  }
}

window.violetReviewProof = violetReviewProof;
