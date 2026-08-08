// =====================================================================
// init-shard2.js
// Initializes the Shard 2 Replica Set (shard2RS).
// Run this INSIDE the `shard2` container:
//   docker exec -it shard2 mongosh /scripts/init-shard2.js
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
  print("[init-shard2] shard2RS is already initialized. Skipping rs.initiate().");
} else {
  print("[init-shard2] Initializing shard replica set 'shard2RS' ...");
  const result = rs.initiate({
    _id: "shard2RS",
    members: [{ _id: 0, host: "shard2:27017" }],
  });
  printjson(result);
}

let attempts = 0;
while (rs.status().myState !== 1 && attempts < 30) {
  print("[init-shard2] Waiting for shard2RS PRIMARY election...");
  sleep(1000);
  attempts++;
}

if (rs.status().myState === 1) {
  print("[init-shard2] shard2RS is PRIMARY and ready.");
} else {
  print("[init-shard2] WARNING: shard2RS did not reach PRIMARY state in time.");
}

printjson(rs.status().members.map((m) => ({ name: m.name, stateStr: m.stateStr })));
