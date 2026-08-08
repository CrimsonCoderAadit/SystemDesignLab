// =====================================================================
// setup-sharding.js
// Connects through mongos, adds both shards to the cluster, enables
// sharding on the "College" database, and shards the "Student"
// collection on RollNo (hashed).
//
// Run this INSIDE the `mongos` container:
//   docker exec -it mongos mongosh /scripts/setup-sharding.js
// =====================================================================

print("[setup-sharding] Checking currently registered shards ...");
const shardStatus = db.adminCommand({ listShards: 1 });
const existingShards = shardStatus.shards.map((s) => s._id);
printjson(existingShards);

if (!existingShards.includes("shard1RS")) {
  print("[setup-sharding] Adding shard1RS (shard1:27017) ...");
  printjson(sh.addShard("shard1RS/shard1:27017"));
} else {
  print("[setup-sharding] shard1RS is already registered.");
}

if (!existingShards.includes("shard2RS")) {
  print("[setup-sharding] Adding shard2RS (shard2:27017) ...");
  printjson(sh.addShard("shard2RS/shard2:27017"));
} else {
  print("[setup-sharding] shard2RS is already registered.");
}

print("[setup-sharding] Enabling sharding on database 'College' ...");
printjson(sh.enableSharding("College"));

const collegeDB = db.getSiblingDB("College");

// A supporting (non-shard-key) index used by department-based queries
// in scripts/queries.js. Not required for sharding itself, but keeps
// those reads efficient once data is spread across shards.
print("[setup-sharding] Creating supporting index on Department ...");
collegeDB.Student.createIndex({ Department: 1 });

// RollNo is unique and monotonically increasing, so we shard using a
// HASHED index on RollNo. sh.shardCollection() creates the hashed
// index automatically. Hashing avoids the "hot shard" problem that a
// plain ascending shard key would cause with sequential RollNo values
// (all new inserts would otherwise land on a single shard/chunk).
print("[setup-sharding] Sharding College.Student on { RollNo: 'hashed' } ...");
printjson(sh.shardCollection("College.Student", { RollNo: "hashed" }));

print("[setup-sharding] Current cluster status:");
printjson(sh.status());

print("[setup-sharding] Done. Shards added, sharding enabled, College.Student sharded on RollNo (hashed).");
