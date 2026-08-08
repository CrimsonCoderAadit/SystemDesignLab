// =====================================================================
// init-shard1.js
// Initializes the Shard 1 Replica Set (shard1RS).
// Run this INSIDE the `shard1` container:
//   docker exec -it shard1 mongosh /scripts/init-shard1.js
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
  print("[init-shard1] shard1RS is already initialized. Skipping rs.initiate().");
} else {
  print("[init-shard1] Initializing shard replica set 'shard1RS' ...");
  const result = rs.initiate({
    _id: "shard1RS",
    members: [{ _id: 0, host: "shard1:27017" }],
  });
  printjson(result);
}

let attempts = 0;
while (rs.status().myState !== 1 && attempts < 30) {
  print("[init-shard1] Waiting for shard1RS PRIMARY election...");
  sleep(1000);
  attempts++;
}

if (rs.status().myState === 1) {
  print("[init-shard1] shard1RS is PRIMARY and ready.");
} else {
  print("[init-shard1] WARNING: shard1RS did not reach PRIMARY state in time.");
}

printjson(rs.status().members.map((m) => ({ name: m.name, stateStr: m.stateStr })));
