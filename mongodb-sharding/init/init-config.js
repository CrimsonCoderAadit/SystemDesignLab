// =====================================================================
// init-config.js
// Initializes the Config Server Replica Set (cfgRS).
// Run this INSIDE the `configsvr` container:
//   docker exec -it configsvr mongosh /scripts/init-config.js
// =====================================================================

function isReplSetInitialized() {
  try {
    rs.status();
    return true;
  } catch (e) {
    return false;
  }
}

if (isReplSetInitialized()) {
  print("[init-config] cfgRS is already initialized. Skipping rs.initiate().");
} else {
  print("[init-config] Initializing config server replica set 'cfgRS' ...");
  const result = rs.initiate({
    _id: "cfgRS",
    configsvr: true,
    members: [{ _id: 0, host: "configsvr:27017" }],
  });
  printjson(result);
}

// Wait until this node becomes PRIMARY (myState === 1) before returning.
let attempts = 0;
while (rs.status().myState !== 1 && attempts < 30) {
  print("[init-config] Waiting for cfgRS PRIMARY election...");
  sleep(1000);
  attempts++;
}

if (rs.status().myState === 1) {
  print("[init-config] cfgRS is PRIMARY and ready.");
} else {
  print("[init-config] WARNING: cfgRS did not reach PRIMARY state in time.");
}

printjson(rs.status().members.map((m) => ({ name: m.name, stateStr: m.stateStr })));
