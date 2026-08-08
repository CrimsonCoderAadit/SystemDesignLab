// =====================================================================
// queries.js
// Example read queries executed through mongos against College.Student.
//
// Run this INSIDE the `mongos` container:
//   docker exec -it mongos mongosh /app-scripts/queries.js
// =====================================================================

const collegeDB = db.getSiblingDB("College");

print("\n===== 1. Find ONE student by RollNo =====");
printjson(collegeDB.Student.findOne({ RollNo: 42 }));

print("\n===== 2. Find students BY DEPARTMENT (CSE), first 5 =====");
collegeDB.Student.find({ Department: "CSE" })
  .limit(5)
  .forEach((doc) => printjson(doc));

print("\n===== 3. COUNT total students =====");
print(`Total students: ${collegeDB.Student.countDocuments()}`);

print("\n===== 3b. COUNT students per department =====");
const departments = ["CSE", "ECE", "IT", "EEE", "MECH"];
departments.forEach((dept) => {
  const c = collegeDB.Student.countDocuments({ Department: dept });
  print(`  ${dept}: ${c}`);
});

print("\n===== 4. SORT students by CGPA descending, top 5 =====");
collegeDB.Student.find()
  .sort({ CGPA: -1 })
  .limit(5)
  .forEach((doc) => printjson(doc));

print("\n===== 5. RANGE QUERY: students with RollNo between 50 and 60 =====");
collegeDB.Student.find({ RollNo: { $gte: 50, $lte: 60 } })
  .sort({ RollNo: 1 })
  .forEach((doc) => printjson(doc));

print("\n===== 5b. RANGE QUERY: students with CGPA >= 8.5 (count) =====");
print(
  `Students with CGPA >= 8.5: ${collegeDB.Student.countDocuments({ CGPA: { $gte: 8.5 } })}`
);

print("\n===== 6. AGGREGATION: average CGPA and headcount per department =====");
collegeDB.Student.aggregate([
  {
    $group: {
      _id: "$Department",
      averageCGPA: { $avg: "$CGPA" },
      totalStudents: { $sum: 1 },
    },
  },
  { $sort: { _id: 1 } },
]).forEach((doc) => printjson(doc));

print("\n===== 6b. AGGREGATION: top student per department (highest CGPA) =====");
collegeDB.Student.aggregate([
  { $sort: { Department: 1, CGPA: -1 } },
  {
    $group: {
      _id: "$Department",
      topStudent: { $first: "$Name" },
      RollNo: { $first: "$RollNo" },
      CGPA: { $first: "$CGPA" },
    },
  },
  { $sort: { _id: 1 } },
]).forEach((doc) => printjson(doc));

print("\n[queries] Done.");
