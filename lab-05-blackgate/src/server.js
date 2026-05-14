const path = require("path");
const express = require("express");
const session = require("express-session");
const cookieParser = require("cookie-parser");

const authRoutes = require("./routes/auth");
const dashboardRoutes = require("./routes/dashboard");
const ticketRoutes = require("./routes/tickets");
const assetRoutes = require("./routes/assets");
const healthRoutes = require("./routes/health");
const apiRoutes = require("./routes/api");
const debugRoutes = require("./routes/debug");
const securityRoutes = require("./routes/security");
const contextRoutes = require("./routes/context");
const operatorRoutes = require("./routes/operator");
const gatewayRoutes = require("./routes/gateway");

const app = express();
const port = Number(process.env.PORT || 3000);

app.set("view engine", "ejs");
app.set("views", path.join(__dirname, "views"));

app.use("/static", express.static(path.join(__dirname, "public")));
app.use(cookieParser());
app.use(express.urlencoded({ extended: false }));
app.use(express.json({ limit: "64kb" }));

app.use(session({
  name: "blackgate.sid",
  secret: process.env.SESSION_SECRET || "blackgate-phase4-local-session",
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: "lax",
    maxAge: 1000 * 60 * 60 * 4
  }
}));

app.use((req, res, next) => {
  res.locals.currentUser = req.session.user || null;
  res.locals.currentPath = req.path;
  res.locals.appName = "BlackGate Operations Console";

  res.renderPage = (view, options = {}) => {
    const data = {
      ...res.locals,
      ...options
    };

    app.render(view, data, (viewError, body) => {
      if (viewError) {
        return next(viewError);
      }

      return res.render("layout", {
        ...data,
        body
      });
    });
  };

  next();
});

app.get("/", (req, res) => {
  if (req.session.user) {
    return res.redirect("/dashboard");
  }

  return res.redirect("/login");
});

app.use(authRoutes);
app.use(securityRoutes);
app.use(dashboardRoutes);
app.use(gatewayRoutes);
app.use(ticketRoutes);
app.use(assetRoutes);
app.use(healthRoutes);
app.use(contextRoutes);
app.use(operatorRoutes);
app.use(apiRoutes);
app.use(debugRoutes);

app.use((req, res) => {
  return res.status(404).renderPage("error", {
    title: "Not found",
    statusCode: 404,
    message: "The requested BlackGate route was not found."
  });
});

app.use((err, req, res, next) => {
  if (res.headersSent) {
    return next(err);
  }

  if (req.path.startsWith("/api") || req.path.startsWith("/debug")) {
    const isBadRequest = err.status === 400 || err.type === "entity.parse.failed";

    return res.status(isBadRequest ? 400 : 500).json({
      error: isBadRequest ? "bad_request" : "internal_error",
      message: isBadRequest ? "Invalid JSON body." : "BlackGate could not complete the request."
    });
  }

  if (typeof res.renderPage !== "function") {
    return res.status(500).send("BlackGate could not complete the request.");
  }

  return res.status(500).renderPage("error", {
    title: "Internal error",
    statusCode: 500,
    message: "BlackGate could not complete the request."
  });
});

app.listen(port, () => {
  console.log(`BlackGate listening on port ${port}`);
});
