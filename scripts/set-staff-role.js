/**
 * UTOMO BANK - Back-Office Staff Role Provisioning Script
 * 
 * Usage:
 *   node scripts/set-staff-role.js <email> <role> [serviceAccountKeyPath]
 * 
 * Example:
 *   node scripts/set-staff-role.js admin@bankutomo.co.id superadmin ./serviceAccountKey.json
 * 
 * Allowed Roles:
 *   - superadmin
 *   - support_agent
 *   - card_operator
 *   - viewer
 */

const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

const ALLOWED_ROLES = ['superadmin', 'support_agent', 'card_operator', 'viewer'];

async function main() {
  const args = process.argv.slice(2);
  const email = args[0];
  const role = args[1];
  const keyPathArg = args[2];

  console.log('====================================================');
  console.log('   UTOMO BANK - Back-Office Role Provisioning Tool   ');
  console.log('====================================================\n');

  if (!email || !role) {
    console.error('Error: Missing required arguments.\n');
    console.log('Usage:');
    console.log('  node scripts/set-staff-role.js <email> <role> [serviceAccountKeyPath]\n');
    console.log('Allowed Roles:');
    ALLOWED_ROLES.forEach(r => console.log(`  - ${r}`));
    process.exit(1);
  }

  if (!ALLOWED_ROLES.includes(role)) {
    console.error(`Error: Invalid role "${role}".`);
    console.log('Allowed Roles:');
    ALLOWED_ROLES.forEach(r => console.log(`  - ${r}`));
    process.exit(1);
  }

  // Resolve service account key path
  let serviceAccountPath = keyPathArg || process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!serviceAccountPath) {
    const defaultCandidate = path.join(__dirname, 'service-account.json');
    if (fs.existsSync(defaultCandidate)) {
      serviceAccountPath = defaultCandidate;
    }
  }

  if (!serviceAccountPath || !fs.existsSync(serviceAccountPath)) {
    console.error('Error: Firebase Service Account key file not found.');
    console.error('Please download your Service Account JSON from:');
    console.error('  Firebase Console -> Project Settings -> Service accounts -> Generate new private key');
    console.error('And provide the path as an argument or set GOOGLE_APPLICATION_CREDENTIALS environment variable.\n');
    console.error('Example:');
    console.error(`  node scripts/set-staff-role.js ${email} ${role} ./serviceAccountKey.json`);
    process.exit(1);
  }

  try {
    const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
    console.log(`[1/4] Connected to Firebase project: ${serviceAccount.project_id}`);
  } catch (err) {
    console.error('Error initializing Firebase Admin SDK with service account:', err.message);
    process.exit(1);
  }

  try {
    console.log(`[2/4] Looking up user by email: ${email}...`);
    const user = await admin.auth().getUserByEmail(email);
    console.log(`      Found UID: ${user.uid}`);

    console.log(`[3/4] Assigning custom claim: { role: "${role}" }...`);
    await admin.auth().setCustomUserClaims(user.uid, { role });
    console.log('      Custom claims updated successfully.');

    console.log(`[4/4] Writing staff document to Firestore: staff/${user.uid}...`);
    const db = admin.firestore();
    const staffRef = db.collection('staff').doc(user.uid);
    const existingDoc = await staffRef.get();

    const staffData = {
      email: user.email,
      name: user.displayName || user.email.split('@')[0],
      role: role,
      active: true,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    if (!existingDoc.exists) {
      staffData.createdAt = admin.firestore.FieldValue.serverTimestamp();
    }

    await staffRef.set(staffData, { merge: true });

    // Also write an audit log entry for this action
    await db.collection('audit_logs').add({
      actorUid: 'cli_setup_script',
      actorEmail: 'system@bankutomo.local',
      actorRole: 'system',
      action: 'SET_STAFF_ROLE',
      targetType: 'staff',
      targetId: user.uid,
      before: existingDoc.exists ? existingDoc.data() : null,
      after: staffData,
      reason: 'Assigned via CLI set-staff-role script',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log('\nSUCCESS: Staff role successfully provisioned!');
    console.log('----------------------------------------------------');
    console.log(`User:  ${user.email} (${user.uid})`);
    console.log(`Role:  ${role}`);
    console.log('----------------------------------------------------');
    console.log('Note: If the user is currently logged in to the admin panel,');
    console.log('they should sign out and sign back in to refresh their ID token.\n');
    process.exit(0);
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      console.error(`\nError: User with email "${email}" does not exist in Firebase Authentication.`);
      console.error('Please create the user first in Firebase Console or via the app, then re-run this script.');
    } else {
      console.error('\nError setting staff role:', err.message);
    }
    process.exit(1);
  }
}

main();
