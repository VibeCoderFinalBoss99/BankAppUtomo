/**
 * UTOMO BANK - Cloud Functions (Layer 2 - Blaze Plan Required)
 * 
 * Note: These functions require the Firebase Blaze plan and Google Cloud Identity Platform
 * for Auth blocking functions. Layer 1 (Spark Free Plan) works independently without them.
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

// Helper: Server-side Audit Log Writer
async function writeAuditLog({ actorUid, actorEmail, actorRole, action, targetType, targetId, before, after, reason }) {
  try {
    await db.collection('audit_logs').add({
      actorUid: actorUid || 'system',
      actorEmail: actorEmail || 'system@bankutomo.local',
      actorRole: actorRole || 'system',
      action,
      targetType,
      targetId: targetId || '',
      before: before || null,
      after: after || null,
      reason: reason || 'Action executed via Cloud Functions',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  } catch (err) {
    console.error('Failed to write audit log:', err);
  }
}

// ============================================================
// 1. CALLABLE: setStaffRole (Superadmin Only)
// ============================================================
exports.setStaffRole = functions.https.onCall(async (data, context) => {
  // Enforce superadmin caller
  if (!context.auth || context.auth.token.role !== 'superadmin') {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only a superadmin can assign staff roles.'
    );
  }

  const { targetUid, role, reason } = data;
  const ALLOWED_ROLES = ['superadmin', 'support_agent', 'card_operator', 'viewer'];

  if (!targetUid || !role || !ALLOWED_ROLES.includes(role)) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      `Valid targetUid and role (${ALLOWED_ROLES.join(', ')}) are required.`
    );
  }

  // Get current user and staff doc
  let userRecord;
  try {
    userRecord = await admin.auth().getUser(targetUid);
  } catch (err) {
    throw new functions.https.HttpsError('not-found', `User ${targetUid} not found.`);
  }

  const staffRef = db.collection('staff').doc(targetUid);
  const staffSnap = await staffRef.get();
  const beforeState = staffSnap.exists ? staffSnap.data() : null;

  // Set Auth Custom Claim
  await admin.auth().setCustomUserClaims(targetUid, { role });

  // Update staff doc
  const afterState = {
    email: userRecord.email,
    name: userRecord.displayName || userRecord.email.split('@')[0],
    role,
    active: true,
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  };
  if (!staffSnap.exists) {
    afterState.createdAt = admin.firestore.FieldValue.serverTimestamp();
  }
  await staffRef.set(afterState, { merge: true });

  // Audit log
  await writeAuditLog({
    actorUid: context.auth.uid,
    actorEmail: context.auth.token.email,
    actorRole: 'superadmin',
    action: 'SET_STAFF_ROLE',
    targetType: 'staff',
    targetId: targetUid,
    before: beforeState,
    after: afterState,
    reason: reason || 'Assigned via Admin Panel callable function'
  });

  return { success: true, targetUid, role };
});

// ============================================================
// 2. CALLABLE: suspendUser (Superadmin Only)
// ============================================================
exports.suspendUser = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== 'superadmin') {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only a superadmin can suspend customer accounts.'
    );
  }

  const { targetUid, reason } = data;
  if (!targetUid) {
    throw new functions.https.HttpsError('invalid-argument', 'targetUid is required.');
  }

  // Disable in Firebase Auth
  await admin.auth().updateUser(targetUid, { disabled: true });
  // Revoke refresh tokens to instantly terminate active sessions
  await admin.auth().revokeRefreshTokens(targetUid);

  const userDocRef = db.collection('users').doc(targetUid);
  const snap = await userDocRef.get();
  const beforeState = snap.exists ? snap.data() : null;

  await userDocRef.set({
    status: 'suspended',
    statusReason: reason || 'Account suspended by banking security administration',
    statusUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    statusUpdatedBy: context.auth.uid
  }, { merge: true });

  await writeAuditLog({
    actorUid: context.auth.uid,
    actorEmail: context.auth.token.email,
    actorRole: 'superadmin',
    action: 'SUSPEND_USER',
    targetType: 'user',
    targetId: targetUid,
    before: beforeState,
    after: { status: 'suspended', reason },
    reason: reason || 'Administrative account suspension'
  });

  return { success: true, targetUid, status: 'suspended' };
});

// ============================================================
// 3. CALLABLE: reactivateUser (Superadmin Only)
// ============================================================
exports.reactivateUser = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== 'superadmin') {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Only a superadmin can reactivate customer accounts.'
    );
  }

  const { targetUid, reason } = data;
  if (!targetUid) {
    throw new functions.https.HttpsError('invalid-argument', 'targetUid is required.');
  }

  await admin.auth().updateUser(targetUid, { disabled: false });

  const userDocRef = db.collection('users').doc(targetUid);
  const snap = await userDocRef.get();
  const beforeState = snap.exists ? snap.data() : null;

  await userDocRef.set({
    status: 'active',
    statusReason: reason || 'Account reactivated by bank administrator',
    statusUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    statusUpdatedBy: context.auth.uid
  }, { merge: true });

  await writeAuditLog({
    actorUid: context.auth.uid,
    actorEmail: context.auth.token.email,
    actorRole: 'superadmin',
    action: 'REACTIVATE_USER',
    targetType: 'user',
    targetId: targetUid,
    before: beforeState,
    after: { status: 'active', reason },
    reason: reason || 'Administrative account reactivation'
  });

  return { success: true, targetUid, status: 'active' };
});

// ============================================================
// 4. AUTH BLOCKING FUNCTIONS (Identity Platform Required)
// ============================================================
async function checkMaintenanceAuth(user, context) {
  // Check if user is staff
  const customClaims = context.auth ? context.auth.token : {};
  if (customClaims.role && ['superadmin', 'support_agent', 'card_operator', 'viewer'].includes(customClaims.role)) {
    return; // Staff always allowed
  }

  // Check tester bypass
  const bypassDoc = await db.collection('maintenance_bypass').doc(user.uid).get();
  if (bypassDoc.exists) {
    return; // Tester allowed
  }

  // Check maintenance doc
  const maintSnap = await db.collection('config').doc('maintenance').get();
  if (maintSnap.exists) {
    const data = maintSnap.data();
    if (data.enabled === true) {
      const now = new Date();
      const startsAt = data.startsAt ? data.startsAt.toDate() : null;
      const endsAt = data.endsAt ? data.endsAt.toDate() : null;

      const isStarted = !startsAt || now >= startsAt;
      const isNotEnded = !endsAt || now < endsAt;

      if (isStarted && isNotEnded) {
        const msg = (data.message && (data.message.en || data.message.id)) ||
          'UTOMO Bank systems are currently undergoing scheduled maintenance.';
        throw new functions.https.HttpsError(
          'unavailable',
          `MAINTENANCE: ${msg}`
        );
      }
    }
  }
}

// 2nd gen / Identity Platform blocking functions
exports.beforeUserSignedIn = functions.auth.user().beforeSignIn(async (user, context) => {
  await checkMaintenanceAuth(user, context);
});

exports.beforeUserCreated = functions.auth.user().beforeCreate(async (user, context) => {
  await checkMaintenanceAuth(user, context);
});

// ============================================================
// 5. FIRESTORE TRIGGERS: FCM PUSH NOTIFICATIONS
// ============================================================

// Trigger on ticket message: Notify customer if staff replies
exports.onTicketMessageCreated = functions.firestore
  .document('tickets/{ticketId}/messages/{messageId}')
  .onCreate(async (snap, context) => {
    const msg = snap.data();
    if (msg.senderType !== 'staff') return;

    const { ticketId } = context.params;
    const ticketDoc = await db.collection('tickets').doc(ticketId).get();
    if (!ticketDoc.exists) return;

    const ticket = ticketDoc.data();
    const userId = ticket.userId;
    const lang = ticket.customerLanguage || 'id';

    const userDoc = await db.collection('users').doc(userId).get();
    const fcmToken = userDoc.exists ? userDoc.data().fcmToken : null;

    // Create in-app notification record
    await db.collection('users').doc(userId).collection('notifications').add({
      type: 'ticket_reply',
      params: {
        ticketId,
        subject: ticket.subject || 'Support Ticket',
        staffName: msg.senderName || 'Staff'
      },
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    if (!fcmToken) return;

    const title = lang === 'en' ? 'Support Reply Received' : 'Balasan Layanan Pelanggan';
    const body = lang === 'en' 
      ? `Staff has replied to your ticket: "${ticket.subject || 'Inquiry'}"`
      : `Staf telah membalas tiket bantuan Anda: "${ticket.subject || 'Pertanyaan'}"`;

    try {
      await admin.messaging().send({
        token: fcmToken,
        notification: { title, body },
        data: {
          type: 'ticket_reply',
          ticketId: ticketId
        }
      });
    } catch (e) {
      console.warn('FCM dispatch failed:', e.message);
    }
  });

// Trigger on card status update: Notify customer
exports.onCardStatusUpdated = functions.firestore
  .document('users/{userId}/cards/{cardId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    if (before.status === after.status) return;

    const { userId, cardId } = context.params;
    const userDoc = await db.collection('users').doc(userId).get();
    const userData = userDoc.exists ? userDoc.data() : {};
    const lang = userData.language || 'id';
    const fcmToken = userData.fcmToken;

    let notifType = 'card_updated';
    let title = lang === 'en' ? 'Card Status Update' : 'Pembaruan Status Kartu';
    let body = '';

    if (after.status === 'blocked') {
      notifType = 'card_blocked';
      title = lang === 'en' ? 'Card Blocked' : 'Kartu Diblokir';
      body = lang === 'en'
        ? `Your card ending in ${after.cardNumber ? after.cardNumber.slice(-4) : '****'} has been blocked.`
        : `Kartu Anda berakhiran ${after.cardNumber ? after.cardNumber.slice(-4) : '****'} telah diblokir demi keamanan.`;
    } else if (after.status === 'frozen') {
      notifType = 'card_frozen';
      title = lang === 'en' ? 'Card Frozen' : 'Kartu Dibekukan';
      body = lang === 'en'
        ? `Your card ending in ${after.cardNumber ? after.cardNumber.slice(-4) : '****'} has been temporarily frozen.`
        : `Kartu Anda berakhiran ${after.cardNumber ? after.cardNumber.slice(-4) : '****'} telah dibekukan sementara.`;
    } else if (after.status === 'active') {
      notifType = 'card_unblocked';
      title = lang === 'en' ? 'Card Unblocked' : 'Kartu Diaktifkan';
      body = lang === 'en'
        ? `Your card ending in ${after.cardNumber ? after.cardNumber.slice(-4) : '****'} is now active.`
        : `Kartu Anda berakhiran ${after.cardNumber ? after.cardNumber.slice(-4) : '****'} kini telah aktif kembali.`;
    }

    // In-app notification
    await db.collection('users').doc(userId).collection('notifications').add({
      type: notifType,
      params: {
        cardId,
        last4: after.cardNumber ? after.cardNumber.replace(/\s+/g, '').slice(-4) : '0000',
        reason: after.statusReasonCode || 'administrative'
      },
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    if (!fcmToken) return;

    try {
      await admin.messaging().send({
        token: fcmToken,
        notification: { title, body },
        data: {
          type: notifType,
          cardId: cardId
        }
      });
    } catch (e) {
      console.warn('FCM dispatch failed:', e.message);
    }
  });
