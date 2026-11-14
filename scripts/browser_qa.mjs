// Local browser QA using an already installed Chrome with remote debugging.
// Start Chrome headless with --remote-debugging-port=9222 and an isolated profile.
import fs from "node:fs/promises";
const base = process.env.QA_BASE_URL || "http://127.0.0.1:3000";
let browser;
const debugPort = process.env.QA_DEBUG_PORT || "9222";
for (let attempt = 0; attempt < 120; attempt++) {
  try {
    browser = await (
      await fetch(`http://127.0.0.1:${debugPort}/json/version`)
    ).json();
    break;
  } catch {
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
}
if (!browser)
  throw new Error(
    "The isolated browser debugging endpoint did not become available.",
  );
const socket = new WebSocket(browser.webSocketDebuggerUrl);
await new Promise((resolve, reject) => {
  socket.onopen = resolve;
  socket.onerror = reject;
});
let nextId = 0;
const pending = new Map();
const errors = [];
socket.onmessage = ({ data }) => {
  const message = JSON.parse(data);
  if (message.id) {
    const job = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) job.reject(new Error(JSON.stringify(message.error)));
    else if (message.result?.exceptionDetails)
      job.reject(
        new Error(
          message.result.exceptionDetails.exception?.description ||
            message.result.exceptionDetails.text,
        ),
      );
    else job.resolve(message.result);
  }
  if (message.method === "Runtime.exceptionThrown")
    errors.push(message.params.exceptionDetails.text);
};
function call(method, params = {}, sessionId) {
  return new Promise((resolve, reject) => {
    const id = ++nextId;
    pending.set(id, { resolve, reject });
    socket.send(
      JSON.stringify({
        id,
        method,
        params,
        ...(sessionId ? { sessionId } : {}),
      }),
    );
  });
}
let { targetId } = await call("Target.createTarget", { url: "about:blank" });
let { sessionId } = await call("Target.attachToTarget", {
  targetId,
  flatten: true,
});
await call("Page.enable", {}, sessionId);
await call("Runtime.enable", {}, sessionId);
await fs.mkdir(".tmp/screenshots", { recursive: true });
const roleTabs = {
  admin: ['overview', 'merchants', 'users', 'activity', 'dispatch', 'deliveries', 'pricing', 'reports', 'access', 'profile'],
  customer: ['overview', 'send', 'track', 'history', 'settings', 'notifications'],
  merchant: ['reports', 'overview', 'shipments', 'create-shipment', 'bulk-upload', 'tracking', 'history', 'profile', 'api', 'notifications', 'settings'],
  courier: ['overview', 'deliveries', 'route', 'history', 'profile', 'notifications'],
};
const liveCases = Object.entries(roleTabs).flatMap(([role, tabs]) =>
  tabs.map(tab => [`${role}-${tab}`, `/${role}/dashboard/${tab}`, 1440, role]));
liveCases.push(...Object.keys(roleTabs).map(role => [`${role}-mobile`, `/${role}/dashboard/overview`, 390, role]));
liveCases.push(['customer-booking-mobile', '/customer/dashboard/send', 390, 'customer']);
let currentRole;
let browserContextId;
const evaluate = async (expression) =>
  (
    await call(
      "Runtime.evaluate",
      { expression, returnByValue: true },
      sessionId,
    )
  ).result.value;
const requestedCases = process.env.QA_CASES?.split(",");
const cases = liveCases.filter(
  ([name]) => !requestedCases || requestedCases.includes(name),
);
for (const [name, path, width, role] of cases) {
  if (role && currentRole !== role) {
    await call("Target.closeTarget", { targetId });
    if (browserContextId)
      await call("Target.disposeBrowserContext", { browserContextId });
    ({ browserContextId } = await call("Target.createBrowserContext"));
    ({ targetId } = await call("Target.createTarget", {
      url: "about:blank",
      browserContextId,
    }));
    ({ sessionId } = await call("Target.attachToTarget", {
      targetId,
      flatten: true,
    }));
    await call("Page.enable", {}, sessionId);
    await call("Runtime.enable", {}, sessionId);
    await call("Page.navigate", { url: base + "/login" }, sessionId);
    for (let attempt = 0; attempt < 60; attempt++) {
      if (
        await evaluate("Boolean(document.querySelector('input[type=email]'))")
      )
        break;
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
    const password =
      role === "admin"
        ? process.env.SWIFTDROP_ADMIN_PASSWORD || "Admin@SwiftDrop2026!"
        : role === "courier"
          ? "Courier@123!"
          : role === "merchant"
            ? "Merchant@123!"
            : "Customer@123!";
    await evaluate(`(() => {
      const set = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      for (const [selector,value] of [['input[type=email]',${JSON.stringify(role === "merchant" ? "merchant.approved@swiftdrop.io" : role + "@swiftdrop.io")}],['input[type=password]',${JSON.stringify(password)}]]) {
        const input=document.querySelector(selector);set.call(input,value);input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));
      }
    })()`);
    await new Promise(resolve => setTimeout(resolve, 250));
    await evaluate("document.querySelector('form').requestSubmit()");
    for (let attempt = 0; attempt < 100; attempt++) {
      if (await evaluate(`location.pathname.startsWith('/${role}/dashboard')`))
        break;
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
    if (!(await evaluate(`location.pathname.startsWith('/${role}/dashboard')`)))
      throw new Error("Live UI login failed for " + role);
    currentRole = role;
  }
  await call(
    "Emulation.setDeviceMetricsOverride",
    { width, height: 1000, deviceScaleFactor: 1, mobile: width < 600 },
    sessionId,
  );
  await call("Page.navigate", { url: base + path }, sessionId);
  await new Promise((resolve) => setTimeout(resolve, 2200));
  for (let attempt = 0; attempt < 120; attempt++) {
    if (
      await evaluate(
        "document.body.innerText.length>200 && !document.body.innerText.includes('Loading your workspace') && !document.querySelector('[aria-label=\"Loading delivery workspace\"],.MuiLinearProgress-indeterminate')",
      )
    )
      break;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  await call(
    "Runtime.evaluate",
    { expression: "document.fonts.ready", awaitPromise: true },
    sessionId,
  );
  const result = await call(
    "Runtime.evaluate",
    {
      expression:
        "JSON.stringify({title:document.title,heading:document.querySelector('main h1,main h2,h1,h2')?.textContent,overflow:document.documentElement.scrollWidth>innerWidth,alerts:[...document.querySelectorAll('.MuiAlert-standardError,.MuiAlert-standardWarning')].map(e=>e.textContent),body:document.body.innerText.slice(0,160)})",
      returnByValue: true,
    },
    sessionId,
  );
  const screenshot = await call(
    "Page.captureScreenshot",
    { format: "png", captureBeyondViewport: false },
    sessionId,
  );
  await fs.writeFile(
    `.tmp/screenshots/${name}.png`,
    Buffer.from(screenshot.data, "base64"),
  );
  console.log(name, result.result.value);
  const summary = JSON.parse(result.result.value);
  if (summary.overflow || summary.alerts.length)
    errors.push(`${name}: layout overflow or API error`);
}
console.log("Browser runtime exceptions:", JSON.stringify(errors));
await call("Target.closeTarget", { targetId });
if (browserContextId)
  await call("Target.disposeBrowserContext", { browserContextId });
socket.close();
if (errors.length) process.exitCode = 1;
