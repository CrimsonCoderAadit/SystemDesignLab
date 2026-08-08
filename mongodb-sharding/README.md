# MongoDB Horizontal Sharding Lab

**Experiment:** Design and simulate Horizontal Database Sharding using MongoDB.

This project deploys a complete, working MongoDB **sharded cluster** with Docker
Compose on macOS (Docker Desktop): a config server replica set, two shard
replica sets, and a `mongos` query router. It then enables sharding on a
`College` database, shards a `Student` collection on `RollNo`, loads 150
sample student records, and demonstrates how MongoDB spreads those documents
across the two shards.

Everything in this repository has been executed end-to-end (containers
booted, replica sets initialized, shards added, data loaded, queries run,
distribution verified, stop/restart tested) — the "Expected Output" sections
below are **real captured output**, not fabricated examples.

---

## 1. Objectives

- Deploy a MongoDB sharded cluster using Docker Compose.
- Configure horizontal database sharding.
- Distribute data across multiple shards.
- Analyze scalability and query performance.

## 2. Architecture

```
                         ┌─────────────────────┐
                         │   Client / mongosh   │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   mongos (router)    │   port 27017
                         │   container: mongos   │
                         └──────────┬───────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 ▼                                      ▼
     ┌───────────────────────┐            ┌───────────────────────┐
     │  shard1RS (shard1)    │            │  shard2RS (shard2)    │
     │  --shardsvr           │            │  --shardsvr           │
     │  port 27018 (host)    │            │  port 27020 (host)    │
     └───────────────────────┘            └───────────────────────┘

                 ┌───────────────────────┐
                 │  cfgRS (configsvr)    │   port 27019 (host)
                 │  --configsvr          │   stores cluster metadata
                 └───────────────────────┘
```

All four services are single-node replica sets (`cfgRS`, `shard1RS`,
`shard2RS`) plus one `mongos` router — the minimum topology needed to
demonstrate real sharding behaviour on a laptop.

## 3. Project Structure

```
mongodb-sharding/
│
├── docker-compose.yml          # 4 services: configsvr, shard1, shard2, mongos
├── README.md                   # this file
│
├── init/
│   ├── init-config.js          # rs.initiate() for the config server (cfgRS)
│   ├── init-shard1.js          # rs.initiate() for shard1RS
│   ├── init-shard2.js          # rs.initiate() for shard2RS
│   └── setup-sharding.js       # sh.addShard(), sh.enableSharding(), sh.shardCollection()
│
├── scripts/
│   ├── insertStudents.js       # generates + inserts 150 student documents
│   ├── queries.js              # findOne, find, count, sort, range, aggregation
│   └── verifyDistribution.js   # sh.status(), getShardDistribution(), stats()
│
└── sample-data/
    └── students.json           # 150 reference student records (JSON)
```

`init/` is mounted **read-only** into `configsvr`, `shard1`, `shard2`, and
`mongos` at `/scripts`. `scripts/` and `sample-data/` are mounted read-only
into `mongos` only, at `/app-scripts` and `/sample-data` respectively —
because all application-level reads/writes must go **through mongos**, never
directly against a shard.

---

## 4. Prerequisites

- **macOS** with **Docker Desktop** installed and running.
  - Download: https://www.docker.com/products/docker-desktop/
  - Verify installation:
    ```bash
    docker --version
    docker compose version
    ```
  - Recommended Docker Desktop resources: at least 4 GB RAM allocated
    (Settings → Resources) since we run 4 MongoDB processes simultaneously.
- No local MongoDB installation is required — `mongosh` is used **inside**
  the containers (the `mongo:7.0` image bundles `mongosh`).
- MongoDB Docker image used: **`mongo:7.0`** (official image from Docker
  Hub) for all four services — config server, both shards, and mongos.

---

## 5. Full Command Sequence (run in this exact order)

Run every command below from inside the `mongodb-sharding/` directory.

```bash
# 0. Move into the project directory
cd mongodb-sharding

# 1. Build and start all 4 containers in the background
docker compose up -d

# 2. Confirm all 4 containers are running
docker ps

# 3. Initialize the Config Server replica set (cfgRS)
docker exec -it configsvr mongosh /scripts/init-config.js

# 4. Initialize Shard 1 replica set (shard1RS)
docker exec -it shard1 mongosh /scripts/init-shard1.js

# 5. Initialize Shard 2 replica set (shard2RS)
docker exec -it shard2 mongosh /scripts/init-shard2.js

# 5b. Confirm mongos itself has finished starting up before using it.
#     mongos takes a few seconds longer than the mongod processes because
#     it has to connect to the config server on startup. If this prints
#     "MongoNetworkError: connect ECONNREFUSED", wait a few seconds and
#     re-run it until you see { ok: 1, ... }.
docker exec -it mongos mongosh --eval "db.adminCommand({ ping: 1 })"

# 6. Through mongos: add both shards, enable sharding on "College",
#    and shard the "Student" collection on { RollNo: "hashed" }
docker exec -it mongos mongosh /scripts/setup-sharding.js

# 7. Load 150 sample student records into College.Student (via mongos)
docker exec -it mongos mongosh /app-scripts/insertStudents.js

# 8. Run example queries through mongos
docker exec -it mongos mongosh /app-scripts/queries.js

# 9. Verify how documents are distributed across the two shards
docker exec -it mongos mongosh /app-scripts/verifyDistribution.js

# 10. (Optional) Open an interactive mongosh session against mongos
docker exec -it mongos mongosh

# 11. Stop the containers (keeps data in named volumes)
docker compose stop

# 12. Restart later without losing data or needing to re-initialize
docker compose start

# 13. Fully tear down (containers + network); add -v to also delete data volumes
docker compose down
docker compose down -v   # use this to reset the lab to a clean slate
```

Steps 3–6 only need to be run **once** — after that, the replica sets and
sharding configuration persist in the named Docker volumes across
`docker compose stop` / `docker compose start` cycles. You only need to
re-run them after `docker compose down -v` (which deletes the volumes).

---

## 6. Step-by-Step Explanation

### Step 1–2: Start containers

`docker compose up -d` reads `docker-compose.yml` and starts four
containers on a private bridge network `shardnet`:

| Service     | Container name | Role                     | Host port → Container port |
|-------------|-----------------|--------------------------|-----------------------------|
| `configsvr` | `configsvr`     | Config Server (cfgRS)    | 27019 → 27017               |
| `shard1`    | `shard1`        | Shard Server (shard1RS)  | 27018 → 27017               |
| `shard2`    | `shard2`        | Shard Server (shard2RS)  | 27020 → 27017               |
| `mongos`    | `mongos`        | Query Router             | 27017 → 27017               |

All four use `restart: unless-stopped`, so if Docker Desktop restarts, the
cluster comes back up automatically.

### Step 3–5: Initialize replica sets

Every MongoDB shard (and the config server) **must** run as a replica set,
even with a single member, because `mongos` and the sharding protocol
require replica-set semantics (elections, oplog, `rs.status()`) to track
each shard's primary. `init-config.js`, `init-shard1.js`, and
`init-shard2.js` each call `rs.initiate()` once (they are idempotent — safe
to re-run) and then poll `rs.status()` until the node becomes `PRIMARY`.

### Step 6: `setup-sharding.js` (run via `mongos`)

This script, run against `mongos`, performs four actions:

1. `sh.addShard("shard1RS/shard1:27017")` and
   `sh.addShard("shard2RS/shard2:27017")` — registers both shard replica
   sets with the cluster.
2. `sh.enableSharding("College")` — marks the `College` database as
   shardable.
3. `collegeDB.Student.createIndex({ Department: 1 })` — a supporting
   secondary index used by department-based queries later (not the shard
   key, just a normal query-performance index).
4. `sh.shardCollection("College.Student", { RollNo: "hashed" })` — shards
   the `Student` collection using a **hashed index on `RollNo`**.

### Step 7: `insertStudents.js`

Generates 150 deterministic student documents (`RollNo` 1–150, cycling
department, year, and a pseudo-random CGPA) and inserts them into
`College.Student` **through `mongos`**, which routes each document to the
correct shard based on the hash of its `RollNo`.

### Step 8: `queries.js`

Runs `findOne`, `find` by department, `countDocuments`, `sort`, a range
query, and two aggregation pipelines — all issued to `mongos`, which
transparently scatters/gathers across both shards as needed.

### Step 9: `verifyDistribution.js`

Prints `sh.status()`, `getShardDistribution()`, a trimmed `stats()`
summary, and a chunk-count-per-shard breakdown pulled from
`config.chunks` — the direct evidence that documents live on two
different physical shards.

---

## 7. Sample Data (`sample-data/students.json`)

150 pre-generated reference records with the schema:

```json
{
  "RollNo": 1,
  "Name": "Aarav Sharma",
  "Department": "CSE",
  "Year": 1,
  "CGPA": 5.37
}
```

- **RollNo**: sequential integer, 1–150 (also the shard key).
- **Name**: generated from first/last name pools.
- **Department**: cycles through `CSE`, `ECE`, `IT`, `EEE`, `MECH` (30
  students each).
- **Year**: cycles 1–4.
- **CGPA**: deterministic pseudo-random value between 5.00 and 9.99.

This file is provided as a readable reference / for use with `mongoimport`
if you prefer:

```bash
docker exec -it mongos mongoimport --host localhost --port 27017 \
  --db College --collection Student --file /sample-data/students.json --jsonArray
```

`scripts/insertStudents.js` generates the **same** data programmatically
inside `mongosh` (no file I/O needed), so the normal lab flow (Step 7 above)
does not depend on this file — it is there for reference and as an
alternative loading method.

---

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Cannot connect to the Docker daemon` | Docker Desktop isn't running | Open Docker Desktop, wait for the whale icon to settle, retry |
| `docker exec` says container not found | Containers not started, or wrong name | Run `docker ps` to confirm names are `configsvr`, `shard1`, `shard2`, `mongos` |
| `setup-sharding.js` hangs or errors on `addShard` | Ran before replica sets finished electing a PRIMARY | Re-run `init-config.js` / `init-shard1.js` / `init-shard2.js` first and confirm they print `... is PRIMARY and ready.` |
| `MongoServerSelectionError` connecting to `mongos` | `mongos` started before `configsvr` was reachable | `docker compose restart mongos` (Compose's `depends_on` only waits for container start, not for `mongod` to be ready) |
| `getShardDistribution()` / `sh.status()` print nothing when run from a script file | mongosh does not auto-print return values in **script mode** (only in the interactive shell or `--eval`) | Already handled in `verifyDistribution.js` by wrapping calls in `print(...)` |
| Port already in use (`27017`/`27018`/`27019`/`27020`) | Another MongoDB or previous lab run is using that port | Stop the conflicting process, or edit the `ports:` mapping in `docker-compose.yml` |
| Data looks wrong after re-running `insertStudents.js` | This is expected — the script clears `College.Student` first | It's idempotent by design; re-run any time to reset the dataset |
| Want a totally clean slate | Old replica set / sharding state persists in volumes | `docker compose down -v` then repeat the full command sequence from Step 1 |

---

## 9. Viva / Conceptual Questions

**Q1. What is horizontal sharding?**
Horizontal sharding (a.k.a. horizontal partitioning) splits a single
logical collection's **rows/documents** across multiple physical database
servers (shards), based on a shard key. Each shard holds a disjoint subset
of the documents, but together they present one logical collection to the
application. This contrasts with vertical partitioning (splitting by
columns/fields) and with simply scaling up one server (vertical scaling).

**Q2. Why is `RollNo` a good shard key?**
`RollNo` is unique (high cardinality) and present on every document, so it
divides the data evenly and every query that filters on it can be routed
directly to the owning shard (a "targeted" query). Its one weakness is that
raw `RollNo` values are **monotonically increasing** — with an ascending
range-based shard key, every new insert would land in the same "last"
chunk on the same shard, creating a write hotspot. This project sidesteps
that by sharding on `{ RollNo: "hashed" }`: MongoDB stores documents by the
hash of `RollNo`, which distributes both storage **and** write load evenly
across shards, at the cost of no longer supporting efficient range
queries directly on `RollNo` (a range query like "RollNo 50–60" must now be
scattered to all shards — see `queries.js` Query 5, which still works
correctly, just non-targeted).

**Q3. What does `mongos` do?**
`mongos` is the **query router**. It is the only component client
applications ever talk to. It holds no data itself; instead it caches the
cluster's chunk metadata (from the config servers) and, for every query,
decides which shard(s) to forward it to, then merges the results before
returning them to the client. It also routes writes to the correct shard
based on the shard key.

**Q4. What does the Config Server do?**
The config server (here, replica set `cfgRS`) stores the cluster's
**metadata**: which shards exist, the sharding configuration for each
database/collection, the shard key, and the full chunk map (which ranges
of the shard key live on which shard). Every `mongos` instance reads this
metadata to know how to route queries. Since MongoDB 3.4+, the config
server itself must be a replica set (`--configsvr`), for high availability
of this critical metadata.

**Q5. What does a Shard Server do?**
A shard server stores the actual **data** — a subset of the documents in
a sharded collection, determined by the shard key ranges (chunks)
currently assigned to it. In production each shard is itself a replica set
for redundancy (as modeled here with `shard1RS`/`shard2RS`, even though
each has only one member in this lab).

**Q6. What happens when another shard is added?**
After `sh.addShard()` registers a new shard, the cluster's **balancer**
automatically migrates chunks from existing shards to the new one in the
background until data is roughly evenly distributed again. Clients
experience no downtime — `mongos` continues routing queries correctly
throughout the migration, because it only switches a chunk's "owner" in
its routing table once the migration is confirmed complete.

**Q7. Difference between replication and sharding?**

| | Replication | Sharding |
|---|---|---|
| Purpose | High availability / redundancy | Horizontal scalability |
| Data on each node | **Full copy** of the same data | **Different subset** (partition) of the data |
| Solves | Failover, read scaling, durability | Storage limits, write throughput, dataset size |
| In this lab | Each shard (`shard1RS`, `shard2RS`) and the config server (`cfgRS`) are themselves single-node replica sets | The `College.Student` collection is split across `shard1RS` and `shard2RS` |

They are complementary, not alternatives: real deployments shard **and**
replicate (each shard is a replica set), exactly as modeled structurally
here.

**Q8. Advantages of sharding (vs. a standalone server)**
- **Horizontal scalability**: add more shards to hold more data than any
  single machine's disk could fit.
- **Write throughput**: writes are spread across multiple primaries
  instead of bottlenecking on one.
- **Larger working set in RAM**: each shard only needs to cache its own
  subset of data/indexes, improving cache hit rates.
- **Parallelism**: queries that touch multiple shards can be executed
  concurrently across them.
- **Elastic growth**: shards can be added later without downtime, and the
  balancer redistributes data automatically.

**Q9. Disadvantages of sharding**
- **Operational complexity**: more moving parts (config servers, multiple
  replica sets, `mongos` routers) than a single standalone server.
- **Shard key choice is hard to change** later — a poor choice (e.g., low
  cardinality, monotonic without hashing) can cause hotspots or uneven
  distribution that are expensive to fix after the fact.
- **Cross-shard operations are costlier**: queries that can't be targeted
  to one shard (no shard key in the filter) must scatter-gather across
  all shards, and multi-document transactions across shards have more
  overhead than on a single replica set.
- **More infrastructure/cost**: more containers/VMs, more monitoring, more
  network hops for the same logical database.

---

## 10. Expected Output (captured from a real run)

### `sh.status()` (excerpt, run via `docker exec -it mongos mongosh --eval "sh.status()"`)

```
shardingVersion
{ _id: 1, clusterId: ObjectId('6a72d40bd3cb855e6fe86774') }
---
shards
[
  {
    _id: 'shard1RS',
    host: 'shard1RS/shard1:27017',
    state: 1,
    topologyTime: Timestamp({ t: 1785910302, i: 1 })
  },
  {
    _id: 'shard2RS',
    host: 'shard2RS/shard2:27017',
    state: 1,
    topologyTime: Timestamp({ t: 1785910302, i: 5 })
  }
]
---
active mongoses
[ { '7.0.39': 1 } ]
---
autosplit
{ 'Currently enabled': 'yes' }
---
balancer
{
  'Currently enabled': 'yes',
  'Currently running': 'no',
  'Failed balancer rounds in last 5 attempts': 0,
  'Migration Results for the last 24 hours': 'No recent migrations'
}
---
databases
[
  {
    database: { _id: 'College', primary: 'shard2RS', partitioned: false, ... },
    collections: {
      'College.Student': {
        shardKey: { RollNo: 'hashed' },
        unique: false,
        chunkMetadata: [
          { shard: 'shard1RS', nChunks: 2 },
          { shard: 'shard2RS', nChunks: 2 }
        ],
        chunks: [
          { min: { RollNo: MinKey() }, max: { RollNo: Long('-4611686018427387902') }, 'on shard': 'shard2RS' },
          { min: { RollNo: Long('-4611686018427387902') }, max: { RollNo: Long('0') }, 'on shard': 'shard2RS' },
          { min: { RollNo: Long('0') }, max: { RollNo: Long('4611686018427387902') }, 'on shard': 'shard1RS' },
          { min: { RollNo: Long('4611686018427387902') }, max: { RollNo: MaxKey() }, 'on shard': 'shard1RS' }
        ]
      }
    }
  }
]
```

`sh.shardCollection()` with a hashed key pre-splits the hash space into 4
equal chunks and assigns 2 to each shard — this is why distribution is
already balanced immediately after sharding, before the balancer even has
to move anything.

### `db.Student.find()` (first 2 documents, run via `queries.js`)

```json
{
  "_id": ObjectId("6a72d423710ba2575458a1ff"),
  "RollNo": 1,
  "Name": "Aarav Sharma",
  "Department": "CSE",
  "Year": 1,
  "CGPA": 5.37
}
{
  "_id": ObjectId("6a72d423710ba2575458a200"),
  "RollNo": 2,
  "Name": "Vivaan Sharma",
  "Department": "ECE",
  "Year": 2,
  "CGPA": 5.74
}
```

### `db.Student.getShardDistribution()`

```
Shard shard1RS at shard1RS/shard1:27017
{
  data: '8KiB',
  docs: 82,
  chunks: 2,
  'estimated data per chunk': '4KiB',
  'estimated docs per chunk': 41
}
---
Shard shard2RS at shard2RS/shard2:27017
{
  data: '6KiB',
  docs: 68,
  chunks: 2,
  'estimated data per chunk': '3KiB',
  'estimated docs per chunk': 34
}
---
Totals
{
  data: '14KiB',
  docs: 150,
  chunks: 4,
  'Shard shard1RS': [ '54.75 % data', '54.66 % docs in cluster', '100B avg obj size on shard' ],
  'Shard shard2RS': [ '45.24 % data', '45.33 % docs in cluster', '99B avg obj size on shard' ]
}
```

This is the clearest, most direct proof of horizontal distribution: of the
150 total documents, **82 physically reside on `shard1RS`** and **68 on
`shard2RS`** — roughly a 55/45 split, which is expected for hashed
sharding on a modest sample size (it converges closer to 50/50 as the
dataset grows).

### `countDocuments()`

```js
> db.getSiblingDB("College").Student.countDocuments()
150
```

### Per-department counts (`queries.js`)

```
CSE: 30
ECE: 30
IT: 30
EEE: 30
MECH: 30
```

---

## 11. Cleaning Up

```bash
# Stop containers, keep data for next session
docker compose stop

# Stop and remove containers + network, keep data volumes
docker compose down

# Full reset: remove containers, network, AND data volumes
docker compose down -v
```

After `docker compose down -v`, the cluster is back to a blank slate — you
must repeat the full command sequence in Section 5 from Step 1 (including
re-running the `init-*.js` and `setup-sharding.js` scripts) to use it
again.
