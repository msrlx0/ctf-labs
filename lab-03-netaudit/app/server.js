const express = require("express");
const cookieParser = require("cookie-parser");
const path = require("path");

const authRoutes = require("./routes/auth");
const assetsRoutes = require("./routes/assets");
const toolsRoutes = require("./routes/tools");
const supportRoutes = require("./routes/support");
const internalRoutes = require("./routes/internal");

const app = express();
const PORT = 3000;

app.disable("x-powered-by");

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

app.use(express.static(path.join(__dirname, "public")));

app.use("/api", (req, res, next) => {
  res.set("Cache-Control", "no-store");
  next();
});

app.use("/api/auth", authRoutes);
app.use("/api/assets", assetsRoutes);
app.use("/api/tools", toolsRoutes);
app.use("/api/support", supportRoutes);
app.use("/api/internal", internalRoutes);

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.use("/api", (req, res) => {
  res.status(404).json({
    ok: false,
    error: "Not found"
  });
});

app.listen(PORT, () => {
  console.log(`NetAudit running on port ${PORT}`);
});
