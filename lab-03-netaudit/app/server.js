const express = require("express");
const cookieParser = require("cookie-parser");
const path = require("path");

const authRoutes = require("./routes/auth");
const toolsRoutes = require("./routes/tools");
const supportRoutes = require("./routes/support");
const internalRoutes = require("./routes/internal");

const app = express();
const PORT = 3000;

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

app.use(express.static(path.join(__dirname, "public")));

app.use("/api/auth", authRoutes);
app.use("/api/tools", toolsRoutes);
app.use("/api/support", supportRoutes);
app.use("/api/internal", internalRoutes);

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

app.listen(PORT, () => {
  console.log(`NetAudit running on port ${PORT}`);
});
