// =====================================================================
// verifyDistribution.js
// Inspects how the College.Student collection's documents/chunks are
// physically spread across shard1RS and shard2RS.
//
// Run this INSIDE the `mongos` container:
//   docker exec -it mongos mongosh /app-scripts/verifyDistribution.js
// =====================================================================

const collegeDB = db.getSiblingDB("College");

print("\n===== sh.status() =====");
// Cluster-wide overview: registered shards, databases with sharding
// enabled, the sharded collections, their shard key, and how many
// chunks live on each shard.
// NOTE: sh.status() normally relies on mongosh's interactive
// auto-print of its return value. When run from a script file (as
// opposed to the interactive shell or --eval), that auto-print does
// not happen, so we wrap it in print() to force the output.
print(sh.status());

print("\n===== db.Student.getShardDistribution() =====");
// Per-shard document counts, data size, and estimated average object
// size for THIS collection specifically. This is the clearest,
// most direct evidence of horizontal distribution across shards.
// Same script-mode auto-print caveat as sh.status() above -- wrap in
// print() so the output actually appears.
print(collegeDB.Student.getShardDistribution());

print("\n===== db.Student.stats() (summary) =====");
// stats() returns a large, low-level WiredTiger storage document.
// We print only the fields relevant to this lab: overall counts/sizes
// plus the per-shard breakdown, which is the second piece of direct
// evidence (alongside getShardDistribution()) that data physically
// lives on two different shards.
const fullStats = collegeDB.Student.stats();
printjson({
  ns: fullStats.ns,
  sharded: fullStats.sharded,
  count: fullStats.count,
  size: fullStats.size,
  avgObjSize: fullStats.avgObjSize,
  shards: Object.fromEntries(
    Object.entries(fullStats.shards || {}).map(([shardName, s]) => [
      shardName,
      { count: s.count, size: s.size, avgObjSize: s.avgObjSize },
    ])
  ),
});

print("\n===== Chunk count per shard (from config metadata) =====");
const configDB = db.getSiblingDB("config");
const collEntry = configDB.collections.findOne({ _id: "College.Student" });
if (collEntry) {
  const uuid = collEntry.uuid;
  configDB.chunks
    .aggregate([
      { $match: { uuid: uuid } },
      { $group: { _id: "$shard", chunkCount: { $sum: 1 } } },
      { $sort: { _id: 1 } },
    ])
    .forEach((doc) => printjson(doc));
} else {
  print("College.Student not found in config.collections yet.");
}

print("\n[verifyDistribution] Done.");
print(
  "[verifyDistribution] Interpretation: getShardDistribution() and the chunk"
);
print(
  "counts above show that documents are NOT all on one server -- MongoDB has"
);
print(
  "hashed RollNo, spread the resulting ranges into chunks, and balanced those"
);
print("chunks across shard1RS and shard2RS.");
