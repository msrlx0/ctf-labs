const fs = require("fs");
const path = require("path");

const requiredFiles = [
  "data/app.log",
  "data/system.log",
  "data/audit.log",
  "flags/flag1.txt",
  "flags/flag2.txt",
  "flags/flag4.txt",
  "flags/flag_time.txt",
  "flags/root.txt"
];

const missing = requiredFiles.filter(file => {
  return !fs.existsSync(path.join(__dirname, file));
});

if (missing.length > 0) {
  console.error("Missing lab files:");
  missing.forEach(file => console.error(`- ${file}`));
  process.exitCode = 1;
} else {
  console.log("NetAudit lab seed files are present.");
}
