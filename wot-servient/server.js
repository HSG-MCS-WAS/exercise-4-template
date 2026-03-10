const { Servient } = require("@node-wot/core");
const { HttpServer } = require("@node-wot/binding-http");

const PORT = 1180;
const TD_PORT = 1181;
const DASHBOARD_PORT = 5000;

// In-memory device state
const initialState = {
    wristband: "asleep",
    eventPortal: "none",
    lights: "off",
    blinds: "lowered",
    mattress: "idle",
    coffeeMachine: "idle",
};

const state = { ...initialState };

// Track how many wake-up methods are active
let methodCounter = 0;

function checkWakeUp() {
    if (methodCounter >= 3 && state.wristband === "asleep") {
        state.wristband = "awake";
        notifyListeners();
    }
}

function resetAll() {
    Object.assign(state, { ...initialState });
    methodCounter = 0;
    notifyListeners();
}

// Exposed WoT Things (populated in main())
const things = {};

// Event listeners for state changes
const listeners = [];
// Track previous state to only emit WoT property changes for actually changed properties
let previousState = { ...state };

function notifyListeners() {
    // Determine which properties actually changed
    const changed = {};
    for (const key of Object.keys(state)) {
        if (state[key] !== previousState[key]) {
            changed[key] = true;
        }
    }
    previousState = { ...state };

    // Notify SSE listeners (dashboard)
    for (const res of listeners) {
        res.write(`data: ${JSON.stringify(state)}\n\n`);
    }

    // Only emit WoT property changes for properties that actually changed
    if (changed.wristband && things.wristband) things.wristband.emitPropertyChange("ownerState");
    if (changed.eventPortal && things.eventPortal) things.eventPortal.emitPropertyChange("upcoming");
    if (changed.lights && things.lights) things.lights.emitPropertyChange("state");
    if (changed.blinds && things.blinds) things.blinds.emitPropertyChange("state");
    if (changed.mattress && things.mattress) things.mattress.emitPropertyChange("state");
    if (changed.coffeeMachine && things.coffeeMachine) things.coffeeMachine.emitPropertyChange("state");
}

function wrapWriteHandler(handler) {
    return async (value) => {
        await handler(value);
        notifyListeners();
    };
}

async function main() {
    const servient = new Servient();
    servient.addServer(new HttpServer({ port: PORT }));

    const WoT = await servient.start();

    // --- Wristband ---
    const wristband = await WoT.produce({
        title: "wristband",
        properties: {
            ownerState: {
                type: "string",
                readOnly: true,
                observable: true,
            },
        },
    });
    wristband.setPropertyReadHandler("ownerState", async () => state.wristband);
    await wristband.expose();
    things.wristband = wristband;

    // --- Event Portal ---
    const eventPortal = await WoT.produce({
        title: "event-portal",
        properties: {
            upcoming: {
                type: "string",
                readOnly: false,
                observable: true,
            },
        },
    });
    eventPortal.setPropertyReadHandler("upcoming", async () => state.eventPortal);
    eventPortal.setPropertyWriteHandler("upcoming", wrapWriteHandler(async (value) => {
        state.eventPortal = await value.value();
    }));
    await eventPortal.expose();
    things.eventPortal = eventPortal;

    // --- Lights ---
    const lights = await WoT.produce({
        title: "lights",
        properties: {
            state: {
                type: "string",
                enum: ["on", "off"],
                readOnly: false,
                observable: true,
            },
        },
    });
    lights.setPropertyReadHandler("state", async () => state.lights);
    lights.setPropertyWriteHandler("state", wrapWriteHandler(async (value) => {
        const newVal = await value.value();
        const oldVal = state.lights;
        state.lights = newVal;
        if (oldVal === "off" && newVal === "on") { methodCounter++; checkWakeUp(); }
        if (oldVal === "on" && newVal === "off") { methodCounter = Math.max(0, methodCounter - 1); }
    }));
    await lights.expose();
    things.lights = lights;

    // --- Blinds ---
    const blinds = await WoT.produce({
        title: "blinds",
        properties: {
            state: {
                type: "string",
                enum: ["raised", "lowered"],
                readOnly: false,
                observable: true,
            },
        },
    });
    blinds.setPropertyReadHandler("state", async () => state.blinds);
    blinds.setPropertyWriteHandler("state", wrapWriteHandler(async (value) => {
        const newVal = await value.value();
        const oldVal = state.blinds;
        state.blinds = newVal;
        if (oldVal === "lowered" && newVal === "raised") { methodCounter++; checkWakeUp(); }
        if (oldVal === "raised" && newVal === "lowered") { methodCounter = Math.max(0, methodCounter - 1); }
    }));
    await blinds.expose();
    things.blinds = blinds;

    // --- Mattress ---
    const mattress = await WoT.produce({
        title: "mattress",
        properties: {
            state: {
                type: "string",
                enum: ["idle", "vibrating"],
                readOnly: false,
                observable: true,
            },
        },
    });
    mattress.setPropertyReadHandler("state", async () => state.mattress);
    mattress.setPropertyWriteHandler("state", wrapWriteHandler(async (value) => {
        const newVal = await value.value();
        const oldVal = state.mattress;
        state.mattress = newVal;
        if (oldVal === "idle" && newVal === "vibrating") { methodCounter++; checkWakeUp(); }
        if (oldVal === "vibrating" && newVal === "idle") { methodCounter = Math.max(0, methodCounter - 1); }
    }));
    await mattress.expose();
    things.mattress = mattress;

    // --- Coffee Machine ---
    const coffeeMachine = await WoT.produce({
        title: "coffee-machine",
        properties: {
            state: {
                type: "string",
                enum: ["idle", "grinding", "brewing", "ready", "cleaning"],
                readOnly: false,
                observable: true,
            },
        },
    });
    coffeeMachine.setPropertyReadHandler("state", async () => state.coffeeMachine);
    coffeeMachine.setPropertyWriteHandler("state", wrapWriteHandler(async (value) => {
        const newVal = await value.value();
        if (["idle", "grinding", "brewing", "ready", "cleaning"].includes(newVal)) {
            state.coffeeMachine = newVal;
        }
    }));
    await coffeeMachine.expose();
    things.coffeeMachine = coffeeMachine;

    // --- Dashboard HTTP server ---
    const http = require("http");
    const fs = require("fs");
    const path = require("path");

    const corsHeaders = {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, PUT, POST, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "*",
    };

    // --- TD Server (serves Thing Descriptions for agents) ---
    const tdServer = http.createServer((req, res) => {
        if (req.method === "OPTIONS") {
            res.writeHead(204, corsHeaders);
            res.end();
            return;
        }
        if (req.url.startsWith("/tds/") && req.url.endsWith(".ttl")) {
            const filePath = path.join(__dirname, req.url);
            try {
                const ttl = fs.readFileSync(filePath, "utf8");
                res.writeHead(200, { "Content-Type": "text/turtle", ...corsHeaders });
                res.end(ttl);
            } catch (e) {
                res.writeHead(404, { "Content-Type": "text/plain", ...corsHeaders });
                res.end("Not Found");
            }
            return;
        }
        res.writeHead(404, { "Content-Type": "text/plain" });
        res.end("Not Found");
    });
    tdServer.listen(TD_PORT, () => {
        console.log(`TD Server running on http://localhost:${TD_PORT}`);
    });

    // Thing Directory listing
    const thingDirectory = [
        { title: "wristband", tdUrl: `http://localhost:${TD_PORT}/tds/wristband.ttl` },
        { title: "event-portal", tdUrl: `http://localhost:${TD_PORT}/tds/event-portal.ttl` },
        { title: "lights", tdUrl: `http://localhost:${TD_PORT}/tds/lights.ttl` },
        { title: "blinds", tdUrl: `http://localhost:${TD_PORT}/tds/blinds.ttl` },
        { title: "mattress", tdUrl: `http://localhost:${TD_PORT}/tds/mattress.ttl` },
        { title: "coffee-machine", tdUrl: `http://localhost:${TD_PORT}/tds/coffee-machine.ttl` },
    ];

    const dashboard = http.createServer((req, res) => {
        // Handle CORS preflight
        if (req.method === "OPTIONS") {
            res.writeHead(204, corsHeaders);
            res.end();
            return;
        }
        if (req.url === "/events") {
            res.writeHead(200, {
                "Content-Type": "text/event-stream",
                "Cache-Control": "no-cache",
                Connection: "keep-alive",
                ...corsHeaders,
            });
            res.write(`data: ${JSON.stringify(state)}\n\n`);
            listeners.push(res);
            req.on("close", () => {
                const idx = listeners.indexOf(res);
                if (idx !== -1) listeners.splice(idx, 1);
            });
            return;
        }
        if (req.url === "/api/state") {
            res.writeHead(200, { "Content-Type": "application/json", ...corsHeaders });
            res.end(JSON.stringify(state));
            return;
        }
        if (req.url === "/api/trigger-event" && req.method === "POST") {
            let body = "";
            req.on("data", (chunk) => { body += chunk; });
            req.on("end", () => {
                try {
                    const value = JSON.parse(body);
                    state.eventPortal = value;
                    notifyListeners();
                    res.writeHead(200, { "Content-Type": "application/json", ...corsHeaders });
                    res.end(JSON.stringify({ ok: true }));
                } catch (e) {
                    res.writeHead(400, { "Content-Type": "application/json", ...corsHeaders });
                    res.end(JSON.stringify({ error: e.message }));
                }
            });
            return;
        }
        if (req.url === "/api/reset" && req.method === "POST") {
            resetAll();
            res.writeHead(200, { "Content-Type": "application/json", ...corsHeaders });
            res.end(JSON.stringify({ ok: true }));
            return;
        }
        if (req.url === "/directory") {
            res.writeHead(200, { "Content-Type": "application/json", ...corsHeaders });
            res.end(JSON.stringify(thingDirectory));
            return;
        }
        // Serve dashboard HTML
        const html = fs.readFileSync(path.join(__dirname, "dashboard.html"), "utf8");
        res.writeHead(200, { "Content-Type": "text/html", ...corsHeaders });
        res.end(html);
    });

    dashboard.listen(DASHBOARD_PORT, () => {
        console.log(`\nDashboard running on http://localhost:${DASHBOARD_PORT}`);
    });

    console.log(`WoT Device Simulator running on http://localhost:${PORT}`);
    console.log("Device states:", JSON.stringify(state, null, 2));
    console.log("\nExposed Things:");
    console.log(`  Wristband:    http://localhost:${PORT}/wristband`);
    console.log(`  Event Portal: http://localhost:${PORT}/event-portal`);
    console.log(`  Lights:       http://localhost:${PORT}/lights`);
    console.log(`  Blinds:       http://localhost:${PORT}/blinds`);
    console.log(`  Mattress:     http://localhost:${PORT}/mattress`);
    console.log(`  Coffee:       http://localhost:${PORT}/coffee-machine`);
    console.log(`\nThing Directory: http://localhost:${DASHBOARD_PORT}/directory`);
}

main().catch(console.error);
