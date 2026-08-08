// =====================================================================
// insertStudents.js
// Creates the "College" database (implicitly, on first write) and
// bulk-inserts student records into the sharded "Student" collection,
// then verifies the insertion.
//
// Run this INSIDE the `mongos` container (so writes are routed to the
// correct shards by the query router):
//   docker exec -it mongos mongosh /app-scripts/insertStudents.js
// =====================================================================

const collegeDB = db.getSiblingDB("College");

const DEPARTMENTS = ["CSE", "ECE", "IT", "EEE", "MECH"];

const FIRST_NAMES = [
  "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan",
  "Krishna", "Ishaan", "Ananya", "Diya", "Saanvi", "Aadhya", "Kiara",
  "Myra", "Anika", "Navya", "Riya", "Sara", "Rohan", "Karthik", "Nikhil",
  "Varun", "Siddharth", "Meera", "Priya", "Sneha", "Pooja", "Divya",
];

const LAST_NAMES = [
  "Sharma", "Verma", "Iyer", "Nair", "Reddy", "Rao", "Gupta", "Menon",
  "Pillai", "Kulkarni", "Patel", "Joshi", "Chatterjee", "Bose", "Mehta",
  "Agarwal", "Bhat", "Das", "Krishnan", "Naidu",
];

const TOTAL_STUDENTS = 150;

function buildStudents(count) {
  const students = [];
  for (let i = 1; i <= count; i++) {
    const department = DEPARTMENTS[(i - 1) % DEPARTMENTS.length];
    const year = ((i - 1) % 4) + 1; // Year 1..4
    const firstName = FIRST_NAMES[(i - 1) % FIRST_NAMES.length];
    const lastName = LAST_NAMES[Math.floor((i - 1) / FIRST_NAMES.length) % LAST_NAMES.length];

    // Deterministic pseudo-random CGPA in the range 5.00 - 9.99
    const cgpa = parseFloat((5 + ((i * 37) % 500) / 100).toFixed(2));

    students.push({
      RollNo: i,
      Name: `${firstName} ${lastName}`,
      Department: department,
      Year: year,
      CGPA: cgpa,
    });
  }
  return students;
}

print(`[insertStudents] Generating ${TOTAL_STUDENTS} student records ...`);
const students = buildStudents(TOTAL_STUDENTS);
print(`[insertStudents] Generated ${students.length} records. Sample record:`);
printjson(students[0]);

print("[insertStudents] Clearing any existing documents in College.Student (idempotent re-run) ...");
collegeDB.Student.deleteMany({});

print("[insertStudents] Inserting documents into College.Student via mongos ...");
const insertResult = collegeDB.Student.insertMany(students, { ordered: false });
const insertedCount = insertResult.insertedIds
  ? Object.keys(insertResult.insertedIds).length
  : students.length;
print(`[insertStudents] insertMany() reported ${insertedCount} inserted documents.`);

// ---- Verification -----------------------------------------------------
const countAfter = collegeDB.Student.countDocuments();
print(`[insertStudents] Verification: College.Student now contains ${countAfter} documents.`);

if (countAfter === TOTAL_STUDENTS) {
  print("[insertStudents] SUCCESS: document count matches expected total.");
} else {
  print(
    `[insertStudents] WARNING: expected ${TOTAL_STUDENTS} documents but found ${countAfter}.`
  );
}

print("[insertStudents] Sample of inserted documents:");
collegeDB.Student.find().limit(3).forEach((doc) => printjson(doc));
