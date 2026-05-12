const fs = require("fs");
const path = require("path");
const { createClient } = require("redis");

const REDIS_URL = process.env.REDIS_URL || "redis://redis:6379";
const WORKER_TOKEN = process.env.WORKER_TOKEN || "worker-secret-41b8d0aa";
const OUTPUT_DIR = "/shared";

const client = createClient({ url: REDIS_URL });

function outputPath(name, fallback) {
  return path.join(OUTPUT_DIR, path.basename(String(name || fallback)));
}

function writeOutput(file, content) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
  fs.writeFileSync(file, content);
}

async function processJob(raw) {
  let job;

  try {
    job = JSON.parse(raw);
  } catch {
    console.log("[worker] discarded malformed job");
    return;
  }

  if (job.type === "report.export") {
    const output = outputPath(job.output, "report.txt");
    const content = [
      "SentinelCore exported report",
      `job_id=${job.id || "unknown"}`,
      `source=${job.source || "daily-summary"}`,
      `generated_at=${new Date().toISOString()}`,
      ""
    ].join("\n");

    writeOutput(output, content);
    console.log(`[worker] report.export wrote ${output}`);
    return;
  }

  if (job.type === "token.debug") {
    const output = outputPath(job.output, "worker-token.txt");
    const content = [
      "SentinelCore worker debug token export",
      `WORKER_TOKEN=${WORKER_TOKEN}`,
      "flag{worker_queue_poisoned}",
      ""
    ].join("\n");

    writeOutput(output, content);
    console.log(`[worker] token.debug wrote ${output}`);
    return;
  }

  if (job.type === "file.read") {
    const output = outputPath(job.output, "worker-file-read.txt");
    const source = String(job.source || "");

    try {
      const content = fs.readFileSync(source, "utf8");
      writeOutput(output, content);
      console.log(`[worker] file.read read ${source} and wrote ${output}`);
    } catch (error) {
      writeOutput(output, `error=${error.message}\n`);
      console.log(`[worker] file.read failed for ${source}: ${error.message}`);
    }

    return;
  }

  const output = outputPath(job.output, "unknown-job.txt");
  writeOutput(output, `unsupported job type=${job.type}\n`);
  console.log(`[worker] unsupported job type ${job.type}`);
}

async function main() {
  await client.connect();
  console.log("[worker] connected to Redis");
  console.log("[worker] waiting for jobs on sentinel:jobs");

  while (true) {
    const result = await client.brPop("sentinel:jobs", 0);

    if (result && result.element) {
      await processJob(result.element);
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
