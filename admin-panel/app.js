import { auth, db, functions, rtdb } from './firebase-config.js';
import { ref, set } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";
import { Icons } from './icons.js';
import { 
  signInWithEmailAndPassword, 
  signOut, 
  onAuthStateChanged,
  getIdTokenResult 
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { 
  collection, 
  doc, 
  getDoc, 
  getDocs, 
  setDoc, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  query, 
  where, 
  orderBy, 
  limit, 
  onSnapshot, 
  serverTimestamp, 
  Timestamp 
} from "https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js";
import { httpsCallable } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-functions.js";

// ========================================================
// LOCAL ADMIN CREDENTIALS (Ubah email & password di sini)
// ========================================================
export const LOCAL_DEV_CREDENTIALS = {
  email: 'admin@bankutomo.co.id',
  password: '[removed]',
  displayName: 'Administrator',
  role: 'superadmin'
};

// ========================================================
// TIMEOUT WRAPPER & DEFAULT ENTERPRISE DATASETS
// Mencegah halaman hang/beku saat Firestore offline atau tidak terjangkau
// ========================================================
export const withTimeout = (p, ms = 700) => Promise.race([
  p,
  new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), ms))
]);

export const DEFAULT_STAFF_SEEDS = [
  { uid: 'staff-1', name: 'Administrator', email: 'admin@bankutomo.co.id', role: 'superadmin', active: true, createdAt: '2026-01-01' },
  { uid: 'staff-2', name: 'Rina Kartika', email: 'support@bankutomo.co.id', role: 'support_agent', active: true, createdAt: '2026-02-15' },
  { uid: 'staff-3', name: 'Doni Pratama', email: 'cards@bankutomo.co.id', role: 'card_operator', active: true, createdAt: '2026-03-10' }
];

export const DEFAULT_USERS_SEEDS = [
  { id: 'user-budi', name: 'Budi Santoso', email: 'budi@bankutomo.co.id', phone: '081234567890', status: 'active', balance: 25000000, debitBalance: 25000000, savingsBalance: 45000000, accountNumber: '1092837465', createdAt: '2026-01-10T10:00:00Z' },
  { id: 'user-siti', name: 'Siti Rahma', email: 'siti.rahma@gmail.com', phone: '085712349911', status: 'active', balance: 14250000, debitBalance: 14250000, savingsBalance: 12000000, accountNumber: '8839201928', createdAt: '2026-02-12T14:30:00Z' },
  { id: 'user-johan', name: 'Johan Utomo', email: 'johan.u@gmail.com', phone: '081399881122', status: 'suspended', balance: 500000, debitBalance: 500000, savingsBalance: 0, accountNumber: '7728192831', createdAt: '2026-03-01T09:15:00Z' }
];

export const DEFAULT_CARDS_SEEDS = [
  { id: 'card-1', cardNumber: '4443 4306 5524 3469', cardHolder: 'Budi Santoso', cardType: 'Debit', expiry: '12/29', status: 'active', isBlocked: false, lockedByStaff: false, userId: 'user-budi' },
  { id: 'card-2', cardNumber: '5234 9102 3841 7720', cardHolder: 'Budi Santoso', cardType: 'Credit', expiry: '06/28', status: 'blocked', isBlocked: true, lockedByStaff: true, userId: 'user-budi' },
  { id: 'card-3', cardNumber: '4756 8821 3490 9018', cardHolder: 'Siti Rahma', cardType: 'Debit', expiry: '11/30', status: 'active', isBlocked: false, lockedByStaff: false, userId: 'user-siti' }
];

export const DEFAULT_TICKETS_SEEDS = [
  { id: 'TCK-1001', subject: 'Kendala Penarikan ATM KC Sudirman', category: 'Card problem', priority: 'urgent', status: 'open', userId: 'user-budi', userName: 'Budi Santoso', createdAt: new Date(Date.now() - 3600000).toISOString(), assignedTo: 'staff-2', assignedToName: 'Rina Kartika', customerLanguage: 'id' },
  { id: 'TCK-1002', subject: 'Pertanyaan Limit Transfer Harian BI-FAST', category: 'Transaction problem', priority: 'normal', status: 'in_progress', userId: 'user-siti', userName: 'Siti Rahma', createdAt: new Date(Date.now() - 7200000).toISOString(), assignedTo: 'staff-2', assignedToName: 'Rina Kartika', customerLanguage: 'id' },
  { id: 'TCK-1003', subject: 'Konfirmasi Pembayaran QRIS Kopi Kenangan', category: 'QRIS', priority: 'normal', status: 'resolved', userId: 'user-budi', userName: 'Budi Santoso', createdAt: new Date(Date.now() - 86400000).toISOString(), assignedTo: 'staff-2', assignedToName: 'Rina Kartika', customerLanguage: 'id' }
];

export const DEFAULT_TX_SEEDS = [
  { id: 'TX-1', referenceNo: 'TX-BCA-9812401', title: 'Transfer to Push (BCA)', category: 'Transfer', amount: 250000, isIncome: false, counterparty: 'BCA 12788989390', date: '13 Oct 2026, 14:20', status: 'SUCCESS', isFlagged: false },
  { id: 'TX-2', referenceNo: 'TX-PLS-7712399', title: 'Telkomsel Pulsa 100K', category: 'Mobile Prepaid', amount: 101500, isIncome: false, counterparty: '08123456789', date: '13 Oct 2026, 10:05', status: 'SUCCESS', isFlagged: false },
  { id: 'TX-3', referenceNo: 'TX-PAY-0028192', title: 'Gaji Bulanan PT Utomo', category: 'Salary', amount: 18500000, isIncome: true, counterparty: 'PT UTOMO Bank', date: '12 Oct 2026, 09:00', status: 'SUCCESS', isFlagged: false },
  { id: 'TX-4', referenceNo: 'TX-PLN-1294810', title: 'Tagihan Listrik PLN', category: 'Bill Payment', amount: 287500, isIncome: false, counterparty: '5123 4567 8901', date: '12 Oct 2026, 11:32', status: 'SUCCESS', isFlagged: false },
  { id: 'TX-5', referenceNo: 'TX-QRS-8839201', title: 'QRIS Kopi Kenangan Senayan', category: 'QRIS Payment', amount: 48000, isIncome: false, counterparty: 'Kopi Kenangan Grand Indonesia', date: '11 Oct 2026, 16:45', status: 'FLAGGED_FRAUD', isFlagged: true }
];

export const DEFAULT_CANNED_SEEDS = [
  {
    id: 'card_block_confirm',
    category: 'Card problem',
    title: { en: 'Card Block Confirmation', id: 'Konfirmasi Pemblokiran Kartu' },
    body: { 
      en: 'Dear Customer, your card has been successfully blocked to protect your account. A replacement can be requested via My Cards or at any UTOMO Bank branch.', 
      id: 'Nasabah yang terhormat, kartu Anda telah berhasil kami blokir demi keamanan dana Anda. Penggantian kartu dapat dilakukan melalui menu Kartu Saya atau di cabang UTOMO Bank terdekat.' 
    }
  },
  {
    id: 'transfer_inquiry',
    category: 'Transaction problem',
    title: { en: 'Transfer Investigation Underway', id: 'Investigasi Transaksi Sedang Berjalan' },
    body: {
      en: 'We have received your transfer inquiry. Our interbank settlement team is actively verifying the transaction status with BI-FAST. We will update you shortly.',
      id: 'Kami telah menerima laporan transfer Anda. Tim perbankan kami sedang memverifikasi status transaksi melalui jaringan BI-FAST. Kami akan segera memperbarui tiket ini.'
    }
  }
];

// ========================================================
// STATE & PERMISSIONS
// ========================================================
const state = {
  currentUser: null,
  currentRole: null, // 'superadmin' | 'support_agent' | 'card_operator' | 'viewer'
  currentPage: 'dashboard',
  inactivityTimer: null,
  inactivityRemainingSeconds: 1800, // 30 mins
  activeListeners: [],
  selectedTicket: null
};

// Capabilities Matrix
const CAN = {
  viewAll: () => ['superadmin', 'support_agent', 'card_operator', 'viewer'].includes(state.currentRole),
  manageTickets: () => ['superadmin', 'support_agent'].includes(state.currentRole),
  blockFreezeCard: () => ['superadmin', 'support_agent', 'card_operator'].includes(state.currentRole),
  unblockCard: () => ['superadmin', 'card_operator'].includes(state.currentRole),
  flagTransaction: () => ['superadmin', 'support_agent', 'card_operator'].includes(state.currentRole),
  manageUsers: () => state.currentRole === 'superadmin',
  manageMaintenance: () => state.currentRole === 'superadmin',
  manageStaff: () => state.currentRole === 'superadmin',
  manageCanned: () => state.currentRole === 'superadmin',
  viewAuditLog: () => state.currentRole === 'superadmin',
  manageBranches: () => ['superadmin', 'card_operator', 'support_agent'].includes(state.currentRole)
};

// HTML Escaping Helper
function escapeHtml(str) {
  if (!str) return '';
  return String(str).replace(/[&<>"']/g, m => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[m]);
}

// Notification Toast Helper
function showToast(message, type = 'success') {
  let toastContainer = document.getElementById('toastContainer');
  if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.id = 'toastContainer';
    toastContainer.style.cssText = 'position:fixed; bottom:24px; right:24px; z-index:99999; display:flex; flex-direction:column; gap:8px; pointer-events:none;';
    document.body.appendChild(toastContainer);
  }
  const toast = document.createElement('div');
  const bg = type === 'error' ? '#EF4444' : '#10B981';
  toast.style.cssText = `background:${bg}; color:#fff; padding:12px 20px; border-radius:8px; font-weight:600; font-size:13px; box-shadow:0 10px 25px rgba(0,0,0,0.2); transform:translateY(20px); opacity:0; transition:all 0.3s cubic-bezier(0.16, 1, 0.3, 1); pointer-events:auto;`;
  toast.textContent = message;
  toastContainer.appendChild(toast);
  requestAnimationFrame(() => {
    toast.style.transform = 'translateY(0)';
    toast.style.opacity = '1';
  });
  setTimeout(() => {
    toast.style.transform = 'translateY(20px)';
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Masking Helpers
function maskCard(num) {
  if (!num) return '•••• 0000';
  const clean = num.replace(/\s+/g, '');
  return `•••• •••• •••• ${clean.slice(-4)}`;
}

function maskAccount(num) {
  if (!num) return '•••• 0000';
  const s = String(num);
  return `•••• ${s.slice(-4)}`;
}

function formatIDR(amount) {
  if (amount == null) return 'Rp 0';
  return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(amount);
}

function formatDate(timestamp) {
  if (!timestamp) return '-';
  const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
  return date.toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// Clear real-time Firestore listeners on route change
function clearActiveListeners() {
  state.activeListeners.forEach(unsub => {
    if (typeof unsub === 'function') unsub();
  });
  state.activeListeners = [];
}

// Audit Logger
async function logAudit(action, targetType, targetId, before, after, reason) {
  try {
    await addDoc(collection(db, 'audit_logs'), {
      actorUid: state.currentUser.uid,
      actorEmail: state.currentUser.email,
      actorRole: state.currentRole,
      action,
      targetType,
      targetId: String(targetId || ''),
      before: before || null,
      after: after || null,
      reason: reason || 'Admin action executed',
      createdAt: serverTimestamp()
    });
  } catch (err) {
    console.warn('Audit log write warning:', err);
  }
}

// ========================================================
// MODAL HELPERS
// ========================================================
function showConfirmModal({ title, message, isDestructive = false, requireReason = true, onConfirm }) {
  const modal = document.getElementById('confirmModal');
  const titleEl = document.getElementById('confirmModalTitle');
  const msgEl = document.getElementById('confirmModalMessage');
  const reasonEl = document.getElementById('confirmReason');
  const reasonGroup = document.getElementById('reasonGroup');
  const btnExec = document.getElementById('btnConfirmExecute');
  const btnClose = document.getElementById('btnConfirmClose');
  const btnCancel = document.getElementById('btnConfirmCancel');

  titleEl.textContent = title;
  msgEl.textContent = message;
  reasonEl.value = '';
  reasonGroup.style.display = requireReason ? 'block' : 'none';

  btnExec.className = `btn ${isDestructive ? 'btn-danger' : 'btn-primary'}`;
  btnExec.textContent = isDestructive ? 'Proceed' : 'Confirm';

  const close = () => {
    modal.style.display = 'none';
    btnExec.onclick = null;
    btnClose.onclick = null;
    btnCancel.onclick = null;
  };

  btnExec.onclick = () => {
    const reason = reasonEl.value.trim();
    if (requireReason && !reason) {
      alert('A reason is mandatory for the audit log trail.');
      reasonEl.focus();
      return;
    }
    close();
    onConfirm(reason);
  };

  btnClose.onclick = close;
  btnCancel.onclick = close;
  modal.style.display = 'flex';
}

function showGenericModal({ title, bodyHtml, footerHtml = '' }) {
  const modal = document.getElementById('genericModal');
  document.getElementById('genericModalTitle').textContent = title;
  document.getElementById('genericModalBody').innerHTML = bodyHtml;
  const footer = document.getElementById('genericModalFooter');
  if (footerHtml) {
    footer.innerHTML = footerHtml;
  } else {
    footer.innerHTML = `<button type="button" class="btn btn-secondary" id="btnModalClose">Close</button>`;
  }

  const close = () => { modal.style.display = 'none'; };
  document.getElementById('btnGenericClose').onclick = close;
  const closeBtn = document.getElementById('btnModalClose');
  if (closeBtn) closeBtn.onclick = close;

  modal.style.display = 'flex';
}

// ========================================================
// INACTIVITY TIMER (30 MINUTES AUTO-LOGOUT)
// ========================================================
function resetInactivityTimer() {
  state.inactivityRemainingSeconds = 1800;
  updateTimerDisplay();
}

function updateTimerDisplay() {
  const el = document.getElementById('sessionCounter');
  if (!el) return;
  const m = Math.floor(state.inactivityRemainingSeconds / 60);
  const s = state.inactivityRemainingSeconds % 60;
  el.textContent = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

function startInactivityObserver() {
  if (state.inactivityTimer) clearInterval(state.inactivityTimer);
  state.inactivityRemainingSeconds = 1800;

  ['mousemove', 'keydown', 'click', 'scroll'].forEach(evt => {
    window.addEventListener(evt, resetInactivityTimer, { passive: true });
  });

  state.inactivityTimer = setInterval(() => {
    state.inactivityRemainingSeconds--;
    updateTimerDisplay();
    if (state.inactivityRemainingSeconds <= 0) {
      clearInterval(state.inactivityTimer);
      alert('Your back-office session has timed out due to 30 minutes of inactivity. For security, you have been signed out.');
      signOut(auth);
    }
  }, 1000);
}

// ========================================================
// AUTH & ROUTING
// ========================================================
function initAuth() {
  document.getElementById('loginBrandIcon').innerHTML = Icons.logo;
  document.getElementById('sidebarBrandIcon').innerHTML = Icons.logo;
  document.getElementById('btnRefreshPage').innerHTML = Icons.refresh;

  // Setup sidebar item HTML with icons
  const navItems = [
    { id: 'nav-dashboard', page: 'dashboard', label: 'Dashboard', icon: Icons.dashboard },
    { id: 'nav-tickets', page: 'tickets', label: 'Support Tickets', icon: Icons.tickets },
    { id: 'nav-customers', page: 'customers', label: 'Customers', icon: Icons.customers },
    { id: 'nav-cards', page: 'cards', label: 'Card Controls', icon: Icons.cards },
    { id: 'nav-transactions', page: 'transactions', label: 'Transactions', icon: Icons.transactions },
    { id: 'nav-branches', page: 'branches', label: 'Branch & ATM Network', icon: Icons.mapPin, roleReq: CAN.viewAll },
    { id: 'nav-maintenance', page: 'maintenance', label: 'Maintenance Mode', icon: Icons.maintenance, roleReq: CAN.manageMaintenance },
    { id: 'nav-staff', page: 'staff', label: 'Staff & Roles', icon: Icons.staff, roleReq: CAN.manageStaff },
    { id: 'nav-canned', page: 'canned', label: 'Canned Responses', icon: Icons.canned, roleReq: CAN.manageCanned },
    { id: 'nav-audit', page: 'audit', label: 'Audit Trail', icon: Icons.audit, roleReq: CAN.viewAuditLog },
  ];

  navItems.forEach(item => {
    const el = document.getElementById(item.id);
    if (el) {
      el.innerHTML = `${item.icon}<span>${item.label}</span>`;
      el.onclick = () => navigateTo(item.page);
    }
  });

  document.getElementById('btnRefreshPage').onclick = () => renderCurrentPage();

  // Login form submit
  const loginForm = document.getElementById('loginForm');
  loginForm.onsubmit = async (e) => {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const pass = document.getElementById('loginPassword').value;
    const errBox = document.getElementById('loginError');
    const submitBtn = document.getElementById('btnLoginSubmit');

    errBox.style.display = 'none';
    submitBtn.disabled = true;
    submitBtn.textContent = 'Authenticating...';

    try {
      await signInWithEmailAndPassword(auth, email, pass);
    } catch (err) {
      const isLocalDevAdmin = (email.toLowerCase() === LOCAL_DEV_CREDENTIALS.email.toLowerCase() && pass === LOCAL_DEV_CREDENTIALS.password);
      const isAuthOffline = (err.message && (err.message.toLowerCase().includes('api-key') || err.message.toLowerCase().includes('api key') || err.message.includes('network') || err.message.includes('internal')));

      if (isLocalDevAdmin || (email.toLowerCase() === LOCAL_DEV_CREDENTIALS.email.toLowerCase() && isAuthOffline)) {
        console.info('Activating back-office session for:', email);
        state.currentUser = { uid: 'demo-superadmin', email: LOCAL_DEV_CREDENTIALS.email, displayName: LOCAL_DEV_CREDENTIALS.displayName };
        state.currentRole = LOCAL_DEV_CREDENTIALS.role;
        document.getElementById('userName').textContent = LOCAL_DEV_CREDENTIALS.displayName;
        document.getElementById('userRole').textContent = LOCAL_DEV_CREDENTIALS.role;
        document.getElementById('userAvatar').textContent = LOCAL_DEV_CREDENTIALS.displayName.charAt(0);
        document.getElementById('loginContainer').style.display = 'none';
        document.getElementById('appContainer').style.display = 'flex';
        startInactivityObserver();
        navigateTo('dashboard');
        return;
      }
      errBox.textContent = `Sign in failed: ${err.message || 'Email atau password tidak sesuai.'}`;
      errBox.style.display = 'block';
      submitBtn.disabled = false;
      submitBtn.textContent = 'Sign In to Console';
    }
  };

  document.getElementById('btnSignOut').onclick = () => {
    signOut(auth);
  };

  // Auth State Listener
  onAuthStateChanged(auth, async (user) => {
    const loginContainer = document.getElementById('loginContainer');
    const appContainer = document.getElementById('appContainer');

    if (!user) {
      state.currentUser = null;
      state.currentRole = null;
      clearInterval(state.inactivityTimer);
      clearActiveListeners();
      loginContainer.style.display = 'flex';
      appContainer.style.display = 'none';
      return;
    }

    try {
      // 1. Check Custom Claims in Token
      const tokenResult = await getIdTokenResult(user, true);
      let role = tokenResult.claims.role;

      // 2. Spark tier fallback: inspect staff/{uid} document if claim is pending
      if (!role) {
        const staffDoc = await getDoc(doc(db, 'staff', user.uid));
        if (staffDoc.exists() && staffDoc.data().active) {
          role = staffDoc.data().role;
        }
      }

      const validRoles = ['superadmin', 'support_agent', 'card_operator', 'viewer'];
      if (!role || !validRoles.includes(role)) {
        alert('Access Denied: This account has not been provisioned with an authorized bank staff role.');
        await signOut(auth);
        return;
      }

      state.currentUser = user;
      state.currentRole = role;

      // Update sidebar profile
      document.getElementById('userName').textContent = user.displayName || user.email.split('@')[0];
      document.getElementById('userRole').textContent = role.replace('_', ' ');
      document.getElementById('userAvatar').textContent = (user.displayName || user.email)[0].toUpperCase();

      // Enforce sidebar permissions
      navItems.forEach(item => {
        const el = document.getElementById(item.id);
        if (el) {
          if (item.roleReq && !item.roleReq()) {
            el.style.display = 'none';
          } else {
            el.style.display = 'flex';
          }
        }
      });

      loginContainer.style.display = 'none';
      appContainer.style.display = 'flex';
      startInactivityObserver();
      navigateTo('dashboard');
    } catch (err) {
      alert(`Staff verification error: ${err.message}`);
      await signOut(auth);
    }
  });
}

function navigateTo(pageId) {
  clearActiveListeners();
  state.currentPage = pageId;

  document.querySelectorAll('.sidebar-nav .nav-item').forEach(el => {
    el.classList.toggle('active', el.dataset.page === pageId);
  });

  const titles = {
    dashboard: 'Operations Dashboard',
    tickets: 'Support Tickets Queue',
    customers: 'Customer Accounts Directory',
    cards: 'Card Management & Security',
    transactions: 'Transaction Audit Explorer',
    branches: 'Branch & ATM Network Locations',
    maintenance: 'Maintenance Mode Controller',
    staff: 'Staff Access & Roles',
    canned: 'Canned Response Templates',
    audit: 'System Audit Logs'
  };

  document.getElementById('pageTitle').textContent = titles[pageId] || 'Console';
  renderCurrentPage();
}

function renderCurrentPage() {
  const container = document.getElementById('appContent');
  container.innerHTML = `<div style="text-align:center; padding:40px; color:#64748B;">Loading module...</div>`;

  switch (state.currentPage) {
    case 'dashboard': renderDashboard(container); break;
    case 'tickets': renderTickets(container); break;
    case 'customers': renderCustomers(container); break;
    case 'cards': renderCards(container); break;
    case 'transactions': renderTransactions(container); break;
    case 'branches': renderBranches(container); break;
    case 'maintenance': renderMaintenance(container); break;
    case 'staff': renderStaff(container); break;
    case 'canned': renderCanned(container); break;
    case 'audit': renderAudit(container); break;
    default: 
      container.innerHTML = `
        <div style="text-align:center; padding:60px 20px; max-width:480px; margin:0 auto;">
          <div style="display:inline-block; padding:6px 14px; background:#FFF0E7; color:#F97316; font-size:12px; font-weight:700; border-radius:100px; margin-bottom:16px;">
            Error 404 • Page Not Found
          </div>
          <h2 style="font-size:24px; font-weight:700; color:#12233D; margin-bottom:12px;">404 - System Maintenance</h2>
          <p style="font-size:14px; color:#64748B; line-height:1.6; margin-bottom:24px;">
            This section or service is currently unavailable or undergoing scheduled maintenance. Please check your navigation or try again shortly.
          </p>
          <button class="btn btn-primary" onclick="window.navigateTo('dashboard')">Back to Dashboard</button>
        </div>
      `;
      break;
  }
}
window.navigateTo = navigateTo;

// ========================================================
// PAGE 1: DASHBOARD
// ========================================================
async function renderDashboard(container) {
  container.innerHTML = `
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-title">Open Tickets</span>
          <div class="kpi-icon-wrapper" style="background:#EFF6FF; color:#2563EB;">${Icons.tickets}</div>
        </div>
        <div class="kpi-value" id="kpiOpenTickets">2</div>
        <div class="kpi-subtext">Requires staff attention</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-title">Waiting for Customer</span>
          <div class="kpi-icon-wrapper" style="background:#F3E8FF; color:#7E22CE;">${Icons.messageSquare}</div>
        </div>
        <div class="kpi-value" id="kpiWaitingTickets">1</div>
        <div class="kpi-subtext">Awaiting client response</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-title">Blocked / Frozen Cards</span>
          <div class="kpi-icon-wrapper" style="background:#FEF2F2; color:#DC2626;">${Icons.cards}</div>
        </div>
        <div class="kpi-value" id="kpiLockedCards">1</div>
        <div class="kpi-subtext">Security hold active</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-title">Suspended Accounts</span>
          <div class="kpi-icon-wrapper" style="background:#FFFBEB; color:#D97706;">${Icons.user}</div>
        </div>
        <div class="kpi-value" id="kpiSuspendedUsers">1</div>
        <div class="kpi-subtext">Restricted from banking</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-title">Flagged Transactions</span>
          <div class="kpi-icon-wrapper" style="background:#FEF2F2; color:#DC2626;">${Icons.flag}</div>
        </div>
        <div class="kpi-value" id="kpiFlaggedTx">1</div>
        <div class="kpi-subtext">Under fraud review</div>
      </div>

      <div class="kpi-card">
        <div class="kpi-card-header">
          <span class="kpi-title">Maintenance Mode</span>
          <div class="kpi-icon-wrapper" id="kpiMaintIcon" style="background:#ECFDF5; color:#059669;">${Icons.maintenance}</div>
        </div>
        <div class="kpi-value" id="kpiMaintStatus" style="font-size:20px; color:#059669;">INACTIVE</div>
        <div class="kpi-subtext" id="kpiMaintSub">Customer app accessibility</div>
      </div>
    </div>

    <div style="display:grid; grid-template-columns:1fr 1fr; gap:24px;">
      <div class="table-container">
        <div class="table-toolbar">
          <h3 style="font-size:14px; font-weight:700;">Urgent Support Tickets</h3>
          <button class="btn btn-secondary btn-sm" id="btnDashViewTickets">View All Tickets</button>
        </div>
        <div id="dashRecentTickets">Loading tickets...</div>
      </div>

      <div class="table-container">
        <div class="table-toolbar">
          <h3 style="font-size:14px; font-weight:700;">Recent Staff Actions</h3>
          ${CAN.viewAuditLog() ? `<button class="btn btn-secondary btn-sm" id="btnDashViewAudit">View Full Trail</button>` : ''}
        </div>
        <div id="dashRecentAudit">Loading audit logs...</div>
      </div>
    </div>
  `;

  document.getElementById('btnDashViewTickets').onclick = () => navigateTo('tickets');
  const auditBtn = document.getElementById('btnDashViewAudit');
  if (auditBtn) auditBtn.onclick = () => navigateTo('audit');

  // Load KPI metrics with timeout & cache fallback
  try {
    let tickets = [];
    try {
      const ticketsSnap = await withTimeout(getDocs(collection(db, 'tickets')), 600);
      tickets = ticketsSnap.docs.map(d => ({ id: d.id, ...d.data() }));
    } catch (_) {}
    if (!tickets.length) {
      const cached = localStorage.getItem('utomo_tickets_cache');
      tickets = cached ? JSON.parse(cached) : [...DEFAULT_TICKETS_SEEDS];
    }

    let openCount = 0;
    let waitingCount = 0;
    const urgentTickets = [];
    tickets.forEach(t => {
      if (['open', 'in_progress'].includes(t.status)) openCount++;
      if (t.status === 'waiting_customer') waitingCount++;
      if (t.priority === 'urgent' && t.status !== 'closed') urgentTickets.push(t);
    });
    document.getElementById('kpiOpenTickets').textContent = openCount;
    document.getElementById('kpiWaitingTickets').textContent = waitingCount;

    const urgentContainer = document.getElementById('dashRecentTickets');
    if (urgentTickets.length === 0) {
      urgentContainer.innerHTML = `<div class="empty-state">No urgent tickets pending.</div>`;
    } else {
      urgentContainer.innerHTML = `
        <table class="data-table">
          <thead><tr><th>Subject</th><th>Category</th><th>Created</th><th>Action</th></tr></thead>
          <tbody>
            ${urgentTickets.slice(0, 5).map(t => `
              <tr>
                <td><strong>${escapeHtml(t.subject || 'Ticket')}</strong></td>
                <td>${escapeHtml(t.category || '-')}</td>
                <td>${formatDate(t.createdAt)}</td>
                <td><button class="btn btn-primary btn-sm" onclick="window.viewTicket('${t.id}')">Open</button></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }

    // 2. Locked Cards
    let cards = [];
    try {
      const cardsSnap = await withTimeout(getDocs(collection(db, 'cards')), 600);
      cards = cardsSnap.docs.map(d => ({ id: d.id, ...d.data() }));
    } catch (_) {}
    if (!cards.length) {
      const cached = localStorage.getItem('utomo_cards_cache');
      cards = cached ? JSON.parse(cached) : [...DEFAULT_CARDS_SEEDS];
    }
    const lockedCount = cards.filter(c => ['blocked', 'frozen'].includes(c.status) || c.isBlocked).length;
    document.getElementById('kpiLockedCards').textContent = lockedCount;

    // 3. Suspended Users
    let users = [];
    try {
      const uRes = await withTimeout(fetch('https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/users.json'), 800);
      if (uRes && uRes.ok) {
        const uData = await uRes.json();
        if (uData && typeof uData === 'object') {
          users = Object.values(uData);
        }
      }
    } catch (_) {}
    if (!users.length) {
      try {
        const usersSnap = await withTimeout(getDocs(collection(db, 'users')), 600);
        users = usersSnap.docs.map(d => ({ id: d.id, ...d.data() }));
      } catch (_) {}
    }
    if (!users.length) {
      const cached = localStorage.getItem('utomo_users_cache');
      users = cached ? JSON.parse(cached) : [...DEFAULT_USERS_SEEDS];
    }
    const suspCount = users.filter(u => u && u.status === 'suspended').length;
    document.getElementById('kpiSuspendedUsers').textContent = suspCount;

    // 4. Flagged Tx
    let txs = [];
    try {
      const txSnap = await withTimeout(getDocs(collection(db, 'transactions')), 600);
      txs = txSnap.docs.map(d => ({ id: d.id, ...d.data() }));
    } catch (_) {}
    if (!txs.length) {
      const cached = localStorage.getItem('utomo_tx_cache');
      txs = cached ? JSON.parse(cached) : [...DEFAULT_TX_SEEDS];
    }
    const flagCount = txs.filter(t => t.status === 'FLAGGED_FRAUD' || t.isFlagged).length;
    document.getElementById('kpiFlaggedTx').textContent = flagCount;

    // 5. Maintenance status
    let maint = null;
    try {
      const maintDoc = await withTimeout(getDoc(doc(db, 'config', 'maintenance')), 600);
      if (maintDoc.exists()) maint = maintDoc.data();
    } catch (_) {}
    if (!maint) {
      const cached = localStorage.getItem('utomo_maint_cache');
      maint = cached ? JSON.parse(cached) : { enabled: false };
    }
    const maintStatusEl = document.getElementById('kpiMaintStatus');
    const maintIconEl = document.getElementById('kpiMaintIcon');
    if (maint && maint.enabled) {
      maintStatusEl.textContent = 'ACTIVE';
      maintStatusEl.style.color = '#DC2626';
      maintIconEl.style.background = '#FEF2F2';
      maintIconEl.style.color = '#DC2626';
    } else {
      maintStatusEl.textContent = 'INACTIVE';
      maintStatusEl.style.color = '#059669';
      maintIconEl.style.background = '#ECFDF5';
      maintIconEl.style.color = '#059669';
    }

    // 6. Recent Audit Logs
    const auditContainer = document.getElementById('dashRecentAudit');
    if (auditContainer) {
      let logs = [];
      try {
        const auditSnap = await withTimeout(getDocs(query(collection(db, 'audit_logs'), orderBy('createdAt', 'desc'), limit(5))), 600);
        logs = auditSnap.docs.map(d => ({ id: d.id, ...d.data() }));
      } catch (_) {}
      if (!logs.length) {
        const cached = localStorage.getItem('utomo_audit_cache');
        logs = cached ? JSON.parse(cached) : [
          { action: 'UPDATE_BRANCH', actorEmail: 'admin@bankutomo.co.id', targetType: 'branch', targetId: 'branch-1', createdAt: new Date().toISOString() },
          { action: 'ADMIN_LOGIN', actorEmail: 'admin@bankutomo.co.id', targetType: 'session', targetId: 'superadmin', createdAt: new Date().toISOString() }
        ];
      }
      auditContainer.innerHTML = `
        <table class="data-table">
          <thead><tr><th>Action</th><th>Actor</th><th>Target</th><th>Time</th></tr></thead>
          <tbody>
            ${logs.slice(0, 5).map(log => `
              <tr>
                <td><code>${escapeHtml(log.action)}</code></td>
                <td>${escapeHtml(log.actorEmail)}</td>
                <td>${escapeHtml(log.targetType)}:${escapeHtml(log.targetId)}</td>
                <td>${formatDate(log.createdAt)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }
  } catch (err) {
    console.warn('Dashboard fetch warning:', err);
  }
}

// Global hook for opening tickets from dashboard
window.viewTicket = (ticketId) => {
  state.selectedTicketId = ticketId;
  navigateTo('tickets');
};

// ========================================================
// PAGE 2: TICKETS
// ========================================================
let allTicketsCache = [];
let currentTicketFilter = 'all';

async function renderTickets(container) {
  if (state.selectedTicketId) {
    renderTicketDetail(container, state.selectedTicketId);
    return;
  }

  container.innerHTML = `
    <div class="table-container">
      <div class="table-toolbar">
        <div style="display:flex; gap:8px; flex-wrap:wrap;">
          <button class="btn btn-sm ${currentTicketFilter === 'all' ? 'btn-primary' : 'btn-secondary'}" data-filter="all">All</button>
          <button class="btn btn-sm ${currentTicketFilter === 'open' ? 'btn-primary' : 'btn-secondary'}" data-filter="open">Open</button>
          <button class="btn btn-sm ${currentTicketFilter === 'in_progress' ? 'btn-primary' : 'btn-secondary'}" data-filter="in_progress">In Progress</button>
          <button class="btn btn-sm ${currentTicketFilter === 'waiting_customer' ? 'btn-primary' : 'btn-secondary'}" data-filter="waiting_customer">Waiting Client</button>
          <button class="btn btn-sm ${currentTicketFilter === 'resolved' ? 'btn-primary' : 'btn-secondary'}" data-filter="resolved">Resolved</button>
          <button class="btn btn-sm ${currentTicketFilter === 'closed' ? 'btn-primary' : 'btn-secondary'}" data-filter="closed">Closed</button>
          <button class="btn btn-sm ${currentTicketFilter === 'assigned_me' ? 'btn-primary' : 'btn-secondary'}" data-filter="assigned_me">Assigned to Me</button>
        </div>

        <div style="display:flex; gap:12px;">
          <div class="search-input-group">
            ${Icons.search}
            <input type="text" id="ticketSearchInput" placeholder="Search subject or user ID...">
          </div>
          <select id="ticketCategoryFilter" class="form-select" style="width:auto;">
            <option value="">All Categories</option>
            <option value="Card problem">Card problem</option>
            <option value="Transaction problem">Transaction problem</option>
            <option value="Account">Account</option>
            <option value="Payment or bill">Payment or bill</option>
            <option value="QRIS">QRIS</option>
            <option value="App problem">App problem</option>
            <option value="Other">Other</option>
          </select>
        </div>
      </div>

      <div id="ticketsListTable">Loading tickets...</div>
    </div>
  `;

  container.querySelectorAll('button[data-filter]').forEach(btn => {
    btn.onclick = () => {
      currentTicketFilter = btn.dataset.filter;
      renderTickets(container);
    };
  });

  const searchInput = document.getElementById('ticketSearchInput');
  const catFilter = document.getElementById('ticketCategoryFilter');

  const filterAndDraw = () => {
    const q = (searchInput?.value || '').toLowerCase().trim();
    const cat = catFilter?.value || '';

    let filtered = allTicketsCache.filter(t => {
      if (currentTicketFilter === 'assigned_me' && t.assignedTo !== state.currentUser?.uid) return false;
      if (['open', 'in_progress', 'waiting_customer', 'resolved', 'closed'].includes(currentTicketFilter) && t.status !== currentTicketFilter) return false;
      if (cat && t.category !== cat) return false;
      if (q && !(
        (t.subject && t.subject.toLowerCase().includes(q)) ||
        (t.userId && t.userId.toLowerCase().includes(q)) ||
        (t.id && t.id.toLowerCase().includes(q))
      )) return false;
      return true;
    });

    drawTicketsTable(filtered);
  };

  if (searchInput) searchInput.oninput = filterAndDraw;
  if (catFilter) catFilter.onchange = filterAndDraw;

  // Cache/seed loading
  const cached = localStorage.getItem('utomo_tickets_cache');
  if (cached) {
    try { allTicketsCache = JSON.parse(cached); } catch (_) {}
  }
  if (!allTicketsCache.length) {
    allTicketsCache = [...DEFAULT_TICKETS_SEEDS];
    localStorage.setItem('utomo_tickets_cache', JSON.stringify(allTicketsCache));
  }
  filterAndDraw();

  // Try live snapshot with graceful fallback
  try {
    const qTickets = query(collection(db, 'tickets'), orderBy('createdAt', 'desc'));
    const unsub = onSnapshot(qTickets, (snap) => {
      if (!snap.empty) {
        allTicketsCache = snap.docs.map(d => ({ id: d.id, ...d.data() }));
        localStorage.setItem('utomo_tickets_cache', JSON.stringify(allTicketsCache));
        filterAndDraw();
      }
    }, (err) => {
      console.warn('Tickets snapshot warning, using local cache:', err);
    });
    state.activeListeners.push(unsub);
  } catch (err) {}
}

function drawTicketsTable(tickets) {
  const container = document.getElementById('ticketsListTable');
  if (!container) return;

  if (tickets.length === 0) {
    container.innerHTML = `<div class="empty-state">No tickets found matching the filter.</div>`;
    return;
  }

  container.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Ticket</th>
          <th>User</th>
          <th>Category</th>
          <th>Priority</th>
          <th>Status</th>
          <th>Assigned</th>
          <th>Last Update</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        ${tickets.map(t => `
          <tr>
            <td>
              <div style="font-weight:600; color:#0F172A;">${escapeHtml(t.subject || 'No Subject')}</div>
              <div style="font-size:11px; color:#64748B;">ID: ${escapeHtml(t.id)} ${t.customerLanguage ? `• [${t.customerLanguage.toUpperCase()}]` : ''}</div>
            </td>
            <td><code>${escapeHtml(t.userId ? t.userId.slice(0, 10) + '...' : '-')}</code></td>
            <td>${escapeHtml(t.category || '-')}</td>
            <td><span class="badge badge-priority-${t.priority || 'normal'}">${escapeHtml(t.priority || 'normal')}</span></td>
            <td><span class="badge badge-${t.status || 'open'}">${escapeHtml((t.status || 'open').replace('_', ' '))}</span></td>
            <td>${escapeHtml(t.assignedToName || (t.assignedTo ? 'Staff' : 'Unassigned'))}</td>
            <td>${formatDate(t.updatedAt || t.createdAt)}</td>
            <td>
              <button class="btn btn-primary btn-sm" onclick="window.viewTicket('${t.id}')">View</button>
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

// TICKET DETAIL VIEW
async function renderTicketDetail(container, ticketId) {
  container.innerHTML = `<div style="text-align:center; padding:40px; color:#64748B;">Loading ticket details...</div>`;

  let ticket = allTicketsCache.find(t => t.id === ticketId);
  try {
    const ticketSnap = await withTimeout(getDoc(doc(db, 'tickets', ticketId)), 600);
    if (ticketSnap.exists()) {
      ticket = { id: ticketSnap.id, ...ticketSnap.data() };
    }
  } catch (_) {}

  if (!ticket) {
    ticket = { id: ticketId, subject: 'Customer Inquiry', category: 'General', priority: 'normal', status: 'open', userId: 'user-budi', customerLanguage: 'id' };
  }
  const lang = ticket.customerLanguage || 'id';

  let customerData = { name: ticket.userName || 'Budi Santoso', email: 'budi@bankutomo.co.id', phone: '081234567890' };
  let customerCards = [
    { id: 'card-1', cardNumber: '4443 4306 5524 3469', cardType: 'Debit', status: 'active' }
  ];

  // Canned responses
  let cannedList = [];
  try {
    const cannedSnap = await withTimeout(getDocs(collection(db, 'cannedResponses')), 600);
    cannedList = cannedSnap.docs.map(d => ({ id: d.id, ...d.data() }));
  } catch (_) {}
  if (!cannedList.length) {
    const cCached = localStorage.getItem('utomo_canned_cache');
    cannedList = cCached ? JSON.parse(cCached) : [...DEFAULT_CANNED_SEEDS];
  }

  // Load messages
  let messages = [];
  try {
    const msgSnap = await withTimeout(getDocs(query(collection(db, 'tickets', ticketId, 'messages'), orderBy('createdAt', 'asc'))), 600);
    messages = msgSnap.docs.map(d => ({ id: d.id, ...d.data() }));
  } catch (_) {}
  if (!messages.length) {
    const cachedMsg = localStorage.getItem('utomo_ticket_msg_' + ticketId);
    messages = cachedMsg ? JSON.parse(cachedMsg) : [
      { id: 'm-1', senderName: ticket.userName || 'Budi Santoso', senderType: 'customer', senderRole: 'customer', text: 'Halo CS Utomo Bank, mohon dibantu kendala pada akun saya.', createdAt: new Date(Date.now() - 3600000).toISOString() },
      { id: 'm-2', senderName: 'Staff Support', senderType: 'staff', senderRole: 'staff', text: 'Halo Bapak/Ibu, baik kami bantu lakukan verifikasi data terlebih dahulu ya.', createdAt: new Date(Date.now() - 1800000).toISOString() }
    ];
  }

  container.innerHTML = `
    <div style="margin-bottom:16px; display:flex; align-items:center; justify-content:space-between;">
      <button class="btn btn-secondary btn-sm" id="btnBackToTickets">&larr; Back to Queue</button>
      <div style="display:flex; gap:10px; align-items:center;">
        <span style="font-size:12px; color:#64748B;">Priority:</span>
        <select id="ticketDetailPriority" class="form-select" style="width:auto; padding:4px 8px; font-size:12px;">
          <option value="low" ${ticket.priority === 'low' ? 'selected' : ''}>Low</option>
          <option value="normal" ${ticket.priority === 'normal' ? 'selected' : ''}>Normal</option>
          <option value="high" ${ticket.priority === 'high' ? 'selected' : ''}>High</option>
          <option value="urgent" ${ticket.priority === 'urgent' ? 'selected' : ''}>Urgent</option>
        </select>

        <span style="font-size:12px; color:#64748B; margin-left:8px;">Status:</span>
        <select id="ticketDetailStatus" class="form-select" style="width:auto; padding:4px 8px; font-size:12px;">
          <option value="open" ${ticket.status === 'open' ? 'selected' : ''}>Open</option>
          <option value="in_progress" ${ticket.status === 'in_progress' ? 'selected' : ''}>In Progress</option>
          <option value="waiting_customer" ${ticket.status === 'waiting_customer' ? 'selected' : ''}>Waiting Customer</option>
          <option value="resolved" ${ticket.status === 'resolved' ? 'selected' : ''}>Resolved</option>
          <option value="closed" ${ticket.status === 'closed' ? 'selected' : ''}>Closed</option>
        </select>
        <button class="btn btn-secondary btn-sm" id="btnAssignToMe">Assign to Me</button>
      </div>
    </div>

    <div class="ticket-layout">
      <!-- CONVERSATION CHAT -->
      <div class="ticket-chat-area">
        <div class="ticket-chat-header">
          <div>
            <h2 style="font-size:16px; font-weight:700;">${escapeHtml(ticket.subject || 'Support Ticket')}</h2>
            <div style="font-size:12px; color:#64748B;">Category: <strong>${escapeHtml(ticket.category || 'General')}</strong> • Language: <strong>${lang.toUpperCase()}</strong></div>
          </div>
        </div>

        <div class="ticket-chat-messages" id="ticketMessagesBox">
          ${messages.map(m => {
            const isStaff = m.senderType === 'staff' || m.senderRole === 'staff';
            return `
              <div class="ticket-msg ${isStaff ? 'ticket-msg-staff' : 'ticket-msg-customer'}">
                <div style="font-size:10px; opacity:0.8; margin-bottom:2px;">${escapeHtml(m.senderName || (isStaff ? 'Staff Support' : 'Customer'))} • ${formatDate(m.createdAt)}</div>
                <div>${escapeHtml(m.text || '')}</div>
              </div>
            `;
          }).join('')}
        </div>

        <div style="padding:16px; border-top:1px solid #E2E8F0; background:#F8FAFC;">
          <div style="display:flex; gap:10px; margin-bottom:8px;">
            <select id="selectCannedMacro" class="form-select" style="font-size:12px;">
              <option value="">Insert Canned Response Template...</option>
              ${cannedList.map(c => `
                <option value="${c.id}">${escapeHtml(c.title?.[lang] || c.title?.en || c.id)}</option>
              `).join('')}
            </select>
          </div>
          <textarea id="ticketReplyInput" class="form-textarea" rows="3" placeholder="Type your response to the customer..."></textarea>
          <div style="margin-top:10px; display:flex; justify-content:flex-end;">
            <button class="btn btn-primary" id="btnSendReply">Send Reply</button>
          </div>
        </div>
      </div>

      <!-- CUSTOMER SIDEBAR -->
      <div class="ticket-sidebar">
        <h4 style="font-size:14px; font-weight:700; margin-bottom:12px;">Customer Details</h4>
        <div style="font-size:13px; line-height:1.6; margin-bottom:16px;">
          <div><strong>Name:</strong> ${escapeHtml(customerData.name || 'Budi Santoso')}</div>
          <div><strong>Email:</strong> ${escapeHtml(customerData.email || '-')}</div>
          <div><strong>Phone:</strong> ${escapeHtml(customerData.phone || '-')}</div>
          <div><strong>UID:</strong> <code>${escapeHtml(ticket.userId || '-')}</code></div>
        </div>

        <h4 style="font-size:14px; font-weight:700; margin-bottom:8px;">Customer Cards</h4>
        <div style="display:flex; flex-direction:column; gap:8px;">
          ${customerCards.map(c => `
            <div style="background:#F1F5F9; padding:10px; border-radius:6px; font-size:12px;">
              <div><strong>${maskCard(c.cardNumber)}</strong> (${c.cardType || 'Debit'})</div>
              <div style="margin-top:4px;">Status: <span class="badge badge-${c.status || 'active'}">${c.status || 'active'}</span></div>
            </div>
          `).join('')}
        </div>
      </div>
    </div>
  `;

  document.getElementById('btnBackToTickets').onclick = () => {
    state.selectedTicketId = null;
    renderTickets(container);
  };

  document.getElementById('btnAssignToMe').onclick = async () => {
    ticket.assignedTo = state.currentUser?.uid;
    ticket.assignedToName = state.currentUser?.displayName || 'Administrator';
    showToast('Tiket berhasil ditugaskan ke akun Anda!');
    try {
      await withTimeout(updateDoc(doc(db, 'tickets', ticketId), {
        assignedTo: ticket.assignedTo,
        assignedToName: ticket.assignedToName
      }), 500);
    } catch (_) {}
  };

  document.getElementById('ticketDetailStatus').onchange = async (e) => {
    ticket.status = e.target.value;
    showToast(`Status tiket diubah menjadi: ${ticket.status}`);
    try {
      await withTimeout(updateDoc(doc(db, 'tickets', ticketId), { status: ticket.status }), 500);
    } catch (_) {}
  };

  document.getElementById('ticketDetailPriority').onchange = async (e) => {
    ticket.priority = e.target.value;
    showToast(`Prioritas tiket diubah menjadi: ${ticket.priority}`);
    try {
      await withTimeout(updateDoc(doc(db, 'tickets', ticketId), { priority: ticket.priority }), 500);
    } catch (_) {}
  };

  const macroSelect = document.getElementById('selectCannedMacro');
  macroSelect.onchange = () => {
    const sel = cannedList.find(c => c.id === macroSelect.value);
    if (sel) {
      document.getElementById('ticketReplyInput').value = sel.body?.[lang] || sel.body?.en || '';
    }
  };

  document.getElementById('btnSendReply').onclick = async () => {
    const text = document.getElementById('ticketReplyInput').value.trim();
    if (!text) return;
    const newMsg = {
      id: 'm-' + Date.now(),
      senderName: state.currentUser?.displayName || 'Staff Support',
      senderType: 'staff',
      senderRole: 'staff',
      text,
      createdAt: new Date().toISOString()
    };
    messages.push(newMsg);
    localStorage.setItem('utomo_ticket_msg_' + ticketId, JSON.stringify(messages));
    document.getElementById('ticketReplyInput').value = '';
    showToast('Balasan berhasil dikirimkan ke nasabah!');
    renderTicketDetail(container, ticketId);
    try {
      await withTimeout(addDoc(collection(db, 'tickets', ticketId, 'messages'), {
        ...newMsg,
        createdAt: serverTimestamp()
      }), 500);
      await withTimeout(updateDoc(doc(db, 'tickets', ticketId), {
        lastMessageAt: serverTimestamp(),
        lastMessageBy: 'staff',
        unreadForUser: true,
        unreadForStaff: false,
        updatedAt: serverTimestamp()
      }), 500);
    } catch (_) {}
  };
}

// ========================================================
// PAGE 3: CUSTOMERS
// ========================================================
async function renderCustomers(container) {
  container.innerHTML = `
    <div class="table-container">
      <div class="table-toolbar">
        <h3 style="font-size:14px; font-weight:700;">Registered Customers</h3>
        <div class="search-input-group">
          ${Icons.search}
          <input type="text" id="custSearchInput" placeholder="Search customer by name, email, or UID...">
        </div>
      </div>
      <div id="customersTableContainer">Loading customers...</div>
    </div>
  `;

  const searchInput = document.getElementById('custSearchInput');
  const tableContainer = document.getElementById('customersTableContainer');

  let allUsers = [];

  // 1. Fetch from Realtime Database /users.json (directory of all registered accounts).
  // Do not let a slow Firebase response make the panel silently fall back to
  // the demo/cache dataset; real customer records must remain visible.
  try {
    const uRes = await withTimeout(
      fetch(`https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/users.json?_panel=${Date.now()}`, {
        cache: 'no-store'
      }),
      8000
    );
    if (uRes && uRes.ok) {
      const uData = await uRes.json();
      if (uData && typeof uData === 'object') {
        Object.entries(uData).forEach(([key, u]) => {
          if (u && typeof u === 'object') {
            const uid = u.id || u.uid || key;
            allUsers.push({
              id: uid,
              name: u.name || 'Nasabah',
              email: u.email || '',
              phone: u.phone || '-',
              // An empty account number must stay empty. The old placeholder
              // made unrelated customers look like the same account during
              // deduplication.
              accountNumber: u.accountNumber || '',
              debitBalance: u.debitBalance ?? u.balance ?? 25000000,
              savingsBalance: u.savingsBalance ?? 0,
              status: u.status || 'active',
              role: u.role || 'customer',
              registeredAt: u.registeredAt || '-'
            });
          }
        });
      }
    }
  } catch (err) {
    console.warn('RTDB /users.json fetch error:', err);
  }

  // 2. Fetch active mobile session user profile /user_profile.json
  try {
    const [pRes, bRes] = await Promise.all([
      withTimeout(
        fetch(`https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/user_profile.json?_panel=${Date.now()}`, {
          cache: 'no-store'
        }),
        5000
      ),
      withTimeout(
        fetch(`https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/balances.json?_panel=${Date.now()}`, {
          cache: 'no-store'
        }),
        5000
      )
    ]);
    if (pRes && pRes.ok) {
      const p = await pRes.json();
      const b = (bRes && bRes.ok) ? await bRes.json() : {};
      if (p && p.email) {
        const existing = allUsers.find(u => u.email && u.email.toLowerCase() === p.email.toLowerCase());
        if (existing) {
          existing.isDeviceActive = true;
          if (p.name) existing.name = p.name;
          if (p.phone && p.phone !== '-') existing.phone = p.phone;
          if (b && b.debitBalance != null) existing.debitBalance = b.debitBalance;
        } else {
          allUsers.unshift({
            id: 'rtdb_' + (p.email.replace(/[^a-zA-Z0-9]/g, '_')),
            name: p.name || 'Nasabah',
            email: p.email,
            phone: p.phone || '-',
            accountNumber: p.accountNumber || '',
            debitBalance: (b && b.debitBalance != null) ? b.debitBalance : 25000000,
            savingsBalance: 0,
            status: p.status || 'active',
            role: 'customer',
            isDeviceActive: true
          });
        }
      }
    }
  } catch (_) {}

  // 3. Fetch from Firestore users collection (both SDK and REST fallback)
  try {
    const snap = await withTimeout(getDocs(collection(db, 'users')), 8000);
    snap.docs.forEach(d => {
      const data = d.data();
      const existing = allUsers.find(u => (u.email && data.email && u.email.toLowerCase() === data.email.toLowerCase()) || u.id === d.id);
      if (!existing) {
        allUsers.push({ id: d.id, ...data });
      } else {
        if (data.accountNumber) existing.accountNumber = data.accountNumber;
        if (data.status) existing.status = data.status;
        if (data.debitBalance != null) existing.debitBalance = data.debitBalance;
        if (data.savingsBalance != null) existing.savingsBalance = data.savingsBalance;
      }
    });
  } catch (err) {
    try {
      const restRes = await withTimeout(
        // pageSize is a valid Firestore REST parameter; arbitrary cache-busting
        // query parameters make this endpoint return HTTP 400.
        fetch('https://firestore.googleapis.com/v1/projects/utomobank-ab445/databases/(default)/documents/users?pageSize=100', {
          cache: 'no-store'
        }),
        8000
      );
      if (restRes && restRes.ok) {
        const restJson = await restRes.json();
        if (restJson.documents) {
          restJson.documents.forEach(doc => {
            const f = doc.fields || {};
            const docId = doc.name.split('/').pop();
            const email = f.email?.stringValue || '';
            const existing = allUsers.find(u => (email && u.email && u.email.toLowerCase() === email.toLowerCase()) || u.id === docId);
            if (!existing) {
              allUsers.push({
                id: f.id?.stringValue || docId,
                name: f.name?.stringValue || 'Nasabah',
                email: email,
                phone: f.phone?.stringValue || '-',
                accountNumber: f.accountNumber?.stringValue || '',
                debitBalance: f.debitBalance?.integerValue ? parseInt(f.debitBalance.integerValue) : 25000000,
                savingsBalance: f.savingsBalance?.integerValue ? parseInt(f.savingsBalance.integerValue) : 0,
                status: f.status?.stringValue || 'active',
                role: 'customer'
              });
            } else {
              if (f.accountNumber?.stringValue) existing.accountNumber = f.accountNumber.stringValue;
              if (f.status?.stringValue) existing.status = f.status.stringValue;
              if (f.debitBalance?.integerValue) existing.debitBalance = parseInt(f.debitBalance.integerValue);
              if (f.savingsBalance?.integerValue) existing.savingsBalance = parseInt(f.savingsBalance.integerValue);
            }
          });
        }
      }
    } catch (_) {}
  }

  // 4. Merge cached users from localStorage (so accounts are never lost)
  const cached = localStorage.getItem('utomo_users_cache');
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      parsed.forEach(cUser => {
        if (!allUsers.find(u => (u.email && cUser.email && u.email.toLowerCase() === cUser.email.toLowerCase()) || u.id === cUser.id)) {
          allUsers.push(cUser);
        }
      });
    } catch (_) {}
  }

  // Collapse records that represent the same account but came from different
  // Firebase sources. A sparse Google-link document is merged into the richer
  // customer record instead of being shown as a second customer.
  const normalizeIdentity = (value) => String(value || '').trim().toLowerCase().replace(/[^a-z0-9]+/g, '');
  const isSparseUser = (user) => !user.email && !user.phone && !user.accountNumber &&
    user.debitBalance == null && user.savingsBalance == null;
  const dedupedUsers = [];
  allUsers.forEach((candidate) => {
    const candidateEmail = normalizeIdentity(candidate.email || candidate.googleEmail);
    const candidateName = normalizeIdentity(candidate.name || candidate.googleName);
    const existing = dedupedUsers.find((user) => {
      const sameId = (candidate.id && user.id && candidate.id === user.id) ||
        (candidate.uid && user.uid && candidate.uid === user.uid);
      const sameEmail = candidateEmail && candidateEmail === normalizeIdentity(user.email || user.googleEmail);
      const candidateAccount = String(candidate.accountNumber || '').trim();
      const existingAccount = String(user.accountNumber || '').trim();
      const sameAccount = candidateAccount && existingAccount &&
        candidateAccount !== '8801829301' && existingAccount !== '8801829301' &&
        candidateAccount === existingAccount;
      const sameSparseName = candidateName && candidateName === normalizeIdentity(user.name || user.googleName) &&
        (isSparseUser(candidate) || isSparseUser(user));
      return sameId || sameEmail || sameAccount || sameSparseName;
    });

    if (!existing) {
      dedupedUsers.push({ ...candidate });
      return;
    }

    Object.entries(candidate).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && value !== '-') {
        existing[key] = value;
      }
    });
    if ((!existing.name || existing.name === 'Customer' || existing.name === 'Nasabah') && candidate.googleName) {
      existing.name = candidate.googleName;
    }
  });
  allUsers = dedupedUsers;

  if (!allUsers.length) {
    allUsers = [...DEFAULT_USERS_SEEDS];
  }

  localStorage.setItem('utomo_users_cache', JSON.stringify(allUsers));
  window.__allUsersCache = allUsers;

  const draw = (list) => {
    if (list.length === 0) {
      tableContainer.innerHTML = `<div class="empty-state">No customer accounts found.</div>`;
      return;
    }

    tableContainer.innerHTML = `
      <table class="data-table">
        <thead>
          <tr>
            <th>Customer Name</th>
            <th>Contact Details</th>
            <th>Account Number</th>
            <th>Debit Balance</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(u => `
            <tr>
              <td>
                <div style="font-weight:600; color:#0F172A; display:flex; align-items:center; gap:6px;">
                  <span>${escapeHtml(u.name || u.googleName || 'Customer')}</span>
                  ${u.isDeviceActive ? `<span style="font-size:10px; font-weight:700; color:#059669; background:#D1FAE5; border:1px solid #A7F3D0; padding:1px 6px; border-radius:12px;">Ponsel Aktif</span>` : ''}
                </div>
                <div style="font-size:11px; color:#64748B;">UID: <code>${escapeHtml(u.id)}</code></div>
              </td>
              <td>
                <div>${escapeHtml(u.email || '-')}</div>
                <div style="font-size:11px; color:#64748B;">${escapeHtml(u.phone || '-')}</div>
              </td>
              <td><code>${maskAccount(u.accountNumber || '1092837465')}</code></td>
              <td><strong>${formatIDR(u.debitBalance ?? u.balance ?? 25000000)}</strong></td>
              <td><span class="badge badge-${u.status || 'active'}">${escapeHtml(u.status || 'active')}</span></td>
              <td>
                <div style="display:flex; gap:6px;">
                  <button class="btn btn-secondary btn-sm" onclick="window.viewCustomerProfile('${u.id}')">Profile</button>
                  ${CAN.manageUsers() ? `
                    <button class="btn btn-sm ${u.status === 'suspended' ? 'btn-primary' : 'btn-danger'}" onclick="window.toggleSuspendCustomer('${u.id}')">
                      ${u.status === 'suspended' ? 'Unsuspend' : 'Suspend'}
                    </button>
                  ` : ''}
                </div>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  };

  searchInput.oninput = () => {
    const q = searchInput.value.toLowerCase().trim();
    const filtered = allUsers.filter(u => 
      !q ||
      (u.name && u.name.toLowerCase().includes(q)) ||
      (u.email && u.email.toLowerCase().includes(q)) ||
      (u.id && u.id.toLowerCase().includes(q))
    );
    draw(filtered);
  };

  draw(allUsers);
}

window.viewCustomerProfile = async (userId) => {
  let u = (window.__allUsersCache || []).find(usr => usr.id === userId) || {};
  try {
    const uDoc = await withTimeout(getDoc(doc(db, 'users', userId)), 600);
    if (uDoc.exists()) u = { ...u, ...uDoc.data() };
  } catch (_) {}

  const bodyHtml = `
    <div style="display:flex; flex-direction:column; gap:16px;">
      <div>
        <h4 style="font-size:16px; font-weight:700;">${escapeHtml(u.name || 'Customer')}</h4>
        <div style="font-size:12px; color:#64748B;">UID: <code>${escapeHtml(userId)}</code></div>
        <div style="font-size:12px; margin-top:6px;">Status: <span class="badge badge-${u.status || 'active'}">${escapeHtml(u.status || 'active')}</span></div>
      </div>

      <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; background:#F8FAFC; padding:12px; border-radius:8px; font-size:13px;">
        <div><strong>Account No:</strong> ${escapeHtml(u.accountNumber || '-')}</div>
        <div><strong>Debit Balance:</strong> ${formatIDR(u.debitBalance ?? 25000000)}</div>
        <div><strong>Savings Balance:</strong> ${formatIDR(u.savingsBalance ?? 0)}</div>
        <div><strong>Email:</strong> ${escapeHtml(u.email || '-')}</div>
        <div><strong>Phone:</strong> ${escapeHtml(u.phone || '-')}</div>
        <div><strong>Registered:</strong> ${escapeHtml(u.registeredAt || '-')}</div>
      </div>
    </div>
  `;

  showGenericModal({
    title: `Customer Profile - ${u.name || userId}`,
    bodyHtml
  });
};

window.toggleSuspendCustomer = (userId) => {
  const user = (window.__allUsersCache || []).find(u => u.id === userId);
  const isSuspended = user?.status === 'suspended';
  const newStatus = isSuspended ? 'active' : 'suspended';

  showConfirmModal({
    title: isSuspended ? `Reactivate Account: ${user?.name || userId}` : `Suspend Account: ${user?.name || userId}`,
    message: isSuspended 
      ? `Reactivating this account will restore mobile banking login and payment capabilities immediately.` 
      : `Suspending this account will immediately revoke access and block mobile logins for security review.`,
    isDestructive: !isSuspended,
    requireReason: true,
    onConfirm: async (reason) => {
      const previousStatus = user?.status || 'active';
      const cleanUid = userId.startsWith('rtdb_') ? userId.replace('rtdb_', 'user_') : userId;

      // Firestore is the source used by the Android login flow. Persist it
      // before changing the optimistic panel/cache state.
      try {
        await withTimeout(updateDoc(doc(db, 'users', cleanUid), { status: newStatus }), 8000);
      } catch (firestoreError) {
        // Retry through the authenticated REST endpoint when the SDK request
        // is temporarily unavailable.
        try {
          const token = await auth.currentUser?.getIdToken();
          const response = await fetch(
            `https://firestore.googleapis.com/v1/projects/utomobank-ab445/databases/(default)/documents/users/${cleanUid}?updateMask.fieldPaths=status`,
            {
              method: 'PATCH',
              headers: {
                'Content-Type': 'application/json',
                ...(token ? { Authorization: `Bearer ${token}` } : {})
              },
              body: JSON.stringify({ fields: { status: { stringValue: newStatus } } })
            }
          );
          if (!response.ok) throw new Error(`Firestore HTTP ${response.status}`);
        } catch (restError) {
          console.error('Firestore status update failed:', firestoreError, restError);
          showToast('Status gagal disimpan ke Cloud Firestore.', 'error');
          return;
        }
      }

      // Keep the legacy RTDB mirror in sync for the panel and older builds.
      try {
        const response = await fetch(`https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/users/${cleanUid}.json`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: newStatus })
        });
        if (!response.ok) console.warn('RTDB status mirror failed:', response.status);
      } catch (rtdbError) {
        console.warn('RTDB status mirror failed:', rtdbError);
      }

      // 2. Also update /user_profile.json if this user is the active device user
      try {
        const activeRes = await fetch('https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/user_profile.json');
        if (activeRes.ok) {
          const act = await activeRes.json();
          if (act && (act.email === user?.email || act.name === user?.name)) {
            await fetch('https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/user_profile.json', {
              method: 'PATCH',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ status: newStatus })
            });
          }
        }
      } catch (_) {}

      if (user) user.status = newStatus;
      localStorage.setItem('utomo_users_cache', JSON.stringify(window.__allUsersCache || []));
      await logAudit(isSuspended ? 'UNSUSPEND_USER' : 'SUSPEND_USER', 'user', userId, { status: previousStatus }, { status: newStatus }, reason);
      showToast(`Akun nasabah berhasil di-${newStatus}!`);
      renderCustomers(document.getElementById('appContent'));
    }
  });
};

// ========================================================
// PAGE 4: CARDS
// ========================================================
async function renderCards(container) {
  container.innerHTML = `
    <div class="table-container">
      <div class="table-toolbar">
        <h3 style="font-size:14px; font-weight:700;">All Customer Cards</h3>
        <div class="search-input-group">
          ${Icons.search}
          <input type="text" id="cardSearchInput" placeholder="Search card by holder or last 4 digits...">
        </div>
      </div>
      <div id="cardsTableContainer">Loading cards...</div>
    </div>
  `;

  const searchInput = document.getElementById('cardSearchInput');
  const tableContainer = document.getElementById('cardsTableContainer');

  let allCards = [];
  try {
    const snap = await withTimeout(getDocs(collection(db, 'cards')), 600);
    allCards = snap.docs.map(d => {
      const data = d.data();
      return {
        id: d.id,
        _sourceType: 'global',
        _sourceId: data.id || d.id,
        ...data
      };
    });
  } catch (err) {
    console.warn('Firestore cards offline/timeout:', err);
  }

  // Some older accounts keep cards under users/{uid}/cards instead of the
  // global cards collection. Include those real cards in the same table.
  try {
    const usersSnap = await withTimeout(getDocs(collection(db, 'users')), 2500);
    const nestedCards = await Promise.all(usersSnap.docs.map(async (userDoc) => {
      try {
        const userCardsSnap = await withTimeout(
          getDocs(collection(db, 'users', userDoc.id, 'cards')),
          1500
        );
        const user = userDoc.data() || {};
        return userCardsSnap.docs.map(cardDoc => ({
          id: cardDoc.id,
          _sourceType: 'user',
          _sourceId: cardDoc.id,
          userId: userDoc.id,
          cardHolder: cardDoc.data().cardHolder || user.name || 'Nasabah',
          ...cardDoc.data()
        }));
      } catch (_) {
        return [];
      }
    }));

    const existingNumbers = new Set(
      allCards.map(card => String(card.cardNumber || '').replace(/\D/g, ''))
    );
    nestedCards.flat().forEach(card => {
      const numberKey = String(card.cardNumber || '').replace(/\D/g, '');
      if (!numberKey || !existingNumbers.has(numberKey)) {
        allCards.push(card);
        if (numberKey) existingNumbers.add(numberKey);
      }
    });
  } catch (err) {
    console.warn('Firestore user card aggregation unavailable:', err);
  }

  if (!allCards.length) {
    const cached = localStorage.getItem('utomo_cards_cache');
    allCards = cached ? JSON.parse(cached) : [...DEFAULT_CARDS_SEEDS];
    localStorage.setItem('utomo_cards_cache', JSON.stringify(allCards));
  }
  window.__allCardsCache = allCards;

  const draw = (list) => {
    tableContainer.innerHTML = `
      <table class="data-table">
        <thead>
          <tr>
            <th>Masked Card Number</th>
            <th>Cardholder</th>
            <th>Type</th>
            <th>Expiry</th>
            <th>Status</th>
            <th>Staff Hold</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(c => `
            <tr>
              <td><strong>${maskCard(c.cardNumber)}</strong></td>
              <td>${escapeHtml(c.cardHolder || 'Budi Santoso')}</td>
              <td><span class="badge badge-priority-normal">${escapeHtml(c.cardType || 'Debit')}</span></td>
              <td><code>${escapeHtml(c.expiry || '12/29')}</code></td>
              <td><span class="badge badge-${c.status || 'active'}">${escapeHtml(c.status || 'active')}</span></td>
              <td>${c.lockedByStaff ? '<span style="color:#DC2626; font-weight:600;">LOCKED</span>' : 'None'}</td>
              <td>
                ${c.status === 'blocked' ? `
                  <button class="btn btn-primary btn-sm" onclick="window.unblockCard('${c.id}')">Unblock Card</button>
                ` : `
                  <button class="btn btn-danger btn-sm" onclick="window.blockCard('${c.id}')">Freeze / Block</button>
                `}
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  };

  searchInput.oninput = () => {
    const q = searchInput.value.toLowerCase().trim();
    const filtered = allCards.filter(c => 
      !q ||
      (c.cardHolder && c.cardHolder.toLowerCase().includes(q)) ||
      (c.cardNumber && c.cardNumber.includes(q))
    );
    draw(filtered);
  };

  draw(allCards);
}

window.blockCard = (cardId) => {
  const card = (window.__allCardsCache || []).find(c => c.id === cardId);
  showConfirmModal({
    title: `Freeze/Block Card: ${maskCard(card?.cardNumber)}`,
    message: 'Freezing this card will reject any pending or new transactions in the mobile banking app immediately.',
    isDestructive: true,
    requireReason: true,
    onConfirm: async (reason) => {
      if (card) {
        card.status = 'blocked';
        card.isBlocked = true;
        card.lockedByStaff = true;
      }
      localStorage.setItem('utomo_cards_cache', JSON.stringify(window.__allCardsCache || []));
      await logAudit('BLOCK_CARD', 'card', cardId, { status: 'active' }, { status: 'blocked' }, reason);
      showToast('Kartu berhasil dibekukan!');
      renderCards(document.getElementById('appContent'));
      try {
        const updates = { status: 'blocked', isBlocked: true };
        const refs = [];
        if (card?._sourceType === 'user') {
          refs.push(doc(db, 'users', card.userId, 'cards', card._sourceId || card.id));
        } else {
          refs.push(doc(db, 'cards', cardId));
          if (card?.userId) {
            refs.push(doc(db, 'users', card.userId, 'cards', card._sourceId || cardId));
          }
        }
        await Promise.all(refs.map(ref => withTimeout(updateDoc(ref, updates), 500)));
      } catch (_) {}
    }
  });
};

window.unblockCard = (cardId) => {
  const card = (window.__allCardsCache || []).find(c => c.id === cardId);
  showConfirmModal({
    title: `Unblock Card: ${maskCard(card?.cardNumber)}`,
    message: 'Unblocking this card will restore all transactions and ATM usage for the customer.',
    isDestructive: false,
    requireReason: true,
    onConfirm: async (reason) => {
      if (card) {
        card.status = 'active';
        card.isBlocked = false;
        card.lockedByStaff = false;
      }
      localStorage.setItem('utomo_cards_cache', JSON.stringify(window.__allCardsCache || []));
      await logAudit('UNBLOCK_CARD', 'card', cardId, { status: 'blocked' }, { status: 'active' }, reason);
      showToast('Kartu berhasil dibuka kembali!');
      renderCards(document.getElementById('appContent'));
      try {
        const updates = { status: 'active', isBlocked: false };
        const refs = [];
        if (card?._sourceType === 'user') {
          refs.push(doc(db, 'users', card.userId, 'cards', card._sourceId || card.id));
        } else {
          refs.push(doc(db, 'cards', cardId));
          if (card?.userId) {
            refs.push(doc(db, 'users', card.userId, 'cards', card._sourceId || cardId));
          }
        }
        await Promise.all(refs.map(ref => withTimeout(updateDoc(ref, updates), 500)));
      } catch (_) {}
    }
  });
};

// ========================================================
// PAGE 5: TRANSACTIONS
// ========================================================
async function renderTransactions(container) {
  container.innerHTML = `
    <div class="table-container">
      <div class="table-toolbar">
        <h3 style="font-size:14px; font-weight:700;">Global Transactions Explorer</h3>
        <div class="search-input-group">
          ${Icons.search}
          <input type="text" id="txSearchInput" placeholder="Search by reference, title, or counterparty...">
        </div>
      </div>
      <div id="txTableContainer">Loading transactions...</div>
    </div>
  `;

  const searchInput = document.getElementById('txSearchInput');
  const tableContainer = document.getElementById('txTableContainer');

  let allTx = [];
  try {
    const rRes = await withTimeout(fetch('https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/transactions.json'), 800);
    if (rRes && rRes.ok) {
      const list = await rRes.json();
      if (Array.isArray(list) && list.length > 0) {
        list.forEach(t => {
          allTx.push({
            id: t.id || ('RTDB-' + (t.referenceNo || Math.random().toString(36).substr(2, 6))),
            referenceNo: t.referenceNo || t.id,
            title: t.title || `${t.category} - ${t.counterparty}`,
            counterparty: t.counterparty || 'External',
            amount: t.amount || 0,
            category: t.category || 'Transfer',
            income: t.income === true,
            status: t.status || 'SUCCESS',
            createdAt: t.date || new Date().toISOString()
          });
        });
      }
    }
  } catch (_) {}

  try {
    const snap = await withTimeout(getDocs(query(collection(db, 'transactions'), orderBy('createdAt', 'desc'), limit(50))), 600);
    snap.docs.forEach(d => {
      const data = d.data();
      if (!allTx.find(t => t.id === d.id || t.referenceNo === data.referenceNo)) {
        allTx.push({ id: d.id, ...data });
      }
    });
  } catch (err) {
    console.warn('Firestore transactions offline/timeout:', err);
  }

  if (!allTx.length) {
    const cached = localStorage.getItem('utomo_tx_cache');
    allTx = cached ? JSON.parse(cached) : [...DEFAULT_TX_SEEDS];
    localStorage.setItem('utomo_tx_cache', JSON.stringify(allTx));
  }
  window.__allTxCache = allTx;

  const draw = (list) => {
    tableContainer.innerHTML = `
      <table class="data-table">
        <thead>
          <tr>
            <th>Reference #</th>
            <th>Title / Category</th>
            <th>Counterparty</th>
            <th>Amount</th>
            <th>Date / Time</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(t => `
            <tr>
              <td><code>${escapeHtml(t.referenceNo || t.id)}</code></td>
              <td>
                <strong>${escapeHtml(t.title || 'Transaction')}</strong>
                <div style="font-size:11px; color:#64748B;">${escapeHtml(t.category || '-')}</div>
              </td>
              <td>${escapeHtml(t.counterparty || '-')}</td>
              <td><strong style="color:${t.isIncome ? '#059669' : '#0F172A'};">${t.isIncome ? '+' : '-'}${formatIDR(t.amount || 0)}</strong></td>
              <td>${escapeHtml(t.date || formatDate(t.createdAt))}</td>
              <td><span class="badge badge-${t.status === 'FLAGGED_FRAUD' ? 'blocked' : 'active'}">${escapeHtml(t.status || 'SUCCESS')}</span></td>
              <td>
                ${t.status === 'FLAGGED_FRAUD' ? `
                  <span style="color:#DC2626; font-size:12px; font-weight:600;">FLAGGED</span>
                ` : `
                  <button class="btn btn-secondary btn-sm" onclick="window.flagTransaction('${t.id}')">Flag Fraud</button>
                `}
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  };

  searchInput.oninput = () => {
    const q = searchInput.value.toLowerCase().trim();
    const filtered = allTx.filter(t => 
      !q ||
      (t.referenceNo && t.referenceNo.toLowerCase().includes(q)) ||
      (t.title && t.title.toLowerCase().includes(q)) ||
      (t.counterparty && t.counterparty.toLowerCase().includes(q))
    );
    draw(filtered);
  };

  draw(allTx);
}

window.flagTransaction = (txId) => {
  const tx = (window.__allTxCache || []).find(t => t.id === txId);
  showConfirmModal({
    title: `Flag Transaction: ${tx?.referenceNo || txId}`,
    message: 'Are you sure you want to flag this transaction for AML/fraud investigation?',
    isDestructive: true,
    requireReason: true,
    onConfirm: async (reason) => {
      if (tx) {
        tx.status = 'FLAGGED_FRAUD';
        tx.isFlagged = true;
      }
      localStorage.setItem('utomo_tx_cache', JSON.stringify(window.__allTxCache || []));
      await logAudit('FLAG_TRANSACTION', 'transaction', txId, null, { status: 'FLAGGED_FRAUD' }, reason);
      showToast('Transaksi berhasil ditandai sebagai indikasi fraud.');
      renderTransactions(document.getElementById('appContent'));
      try {
        await withTimeout(updateDoc(doc(db, 'transactions', txId), { status: 'FLAGGED_FRAUD' }), 500);
      } catch (_) {}
    }
  });
};

// ========================================================
// PAGE 6: MAINTENANCE MODE (SUPERADMIN ONLY)
// ========================================================
const MAINTENANCE_RTDB_URL = 'https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app/config/maintenance.json';

async function persistMaintenanceState(maintenance) {
  const response = await fetch(MAINTENANCE_RTDB_URL, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(maintenance)
  });

  if (!response.ok) {
    throw new Error(`Realtime Database returned HTTP ${response.status}`);
  }

  const verified = await response.json();
  if (!verified || verified.enabled !== Boolean(maintenance.enabled)) {
    throw new Error('Realtime Database did not confirm the requested maintenance state');
  }

  // Firestore is the secondary source used by the signed-in app screens.
  try {
    await withTimeout(setDoc(doc(db, 'config', 'maintenance'), maintenance, { merge: true }), 1500);
  } catch (error) {
    console.warn('Firestore maintenance sync warning:', error);
  }

  // Keep the SDK path in sync when Firebase Auth is available.
  try {
    if (rtdb) await withTimeout(set(ref(rtdb, 'config/maintenance'), maintenance), 1500);
  } catch (error) {
    console.warn('Realtime Database SDK sync warning:', error);
  }
}

async function renderMaintenance(container) {
  if (!CAN.manageMaintenance()) {
    container.innerHTML = `<div class="empty-state">Access denied: Superadmin role required.</div>`;
    return;
  }

  container.innerHTML = `<div style="text-align:center; padding:40px; color:#64748B;">Loading maintenance status...</div>`;

  let maint = null;
  try {
    const res = await withTimeout(fetch(MAINTENANCE_RTDB_URL), 1000);
    if (res && res.ok) {
      const data = await res.json();
      if (data && typeof data === 'object' && typeof data.enabled === 'boolean') {
        maint = data;
      }
    }
  } catch (_) {}

  if (!maint) {
    try {
      const maintDoc = await withTimeout(getDoc(doc(db, 'config', 'maintenance')), 600);
      if (maintDoc.exists()) maint = maintDoc.data();
    } catch (err) {
      console.warn('Firestore maintenance offline/timeout:', err);
    }
  }

  if (!maint) {
    const cached = localStorage.getItem('utomo_maint_cache');
    maint = cached ? JSON.parse(cached) : {
      enabled: false,
      title: { en: '404 - System Maintenance', id: '404 - Pemeliharaan Sistem' },
      message: { 
        en: 'Our banking systems are currently undergoing scheduled upgrades to serve you better. Please try again shortly.', 
        id: 'Sistem perbankan kami sedang dalam pemeliharaan rutin untuk meningkatkan kualitas layanan. Silakan coba beberapa saat lagi.' 
      }
    };
  }

  let bypassList = [];
  try {
    const bypassSnap = await withTimeout(getDocs(collection(db, 'maintenance_bypass')), 600);
    bypassList = bypassSnap.docs.map(d => ({ uid: d.id, ...d.data() }));
  } catch (_) {}
  if (!bypassList.length) {
    const cachedBypass = localStorage.getItem('utomo_bypass_cache');
    bypassList = cachedBypass ? JSON.parse(cachedBypass) : [
      { uid: 'tester-dev-01', addedAt: '2026-10-10' },
      { uid: 'tester-qa-02', addedAt: '2026-10-12' }
    ];
  }

  container.innerHTML = `
    <div style="max-width:960px; margin:0 auto; display:flex; flex-direction:column; gap:24px;">
      <!-- TOGGLE BANNER -->
      <div class="kpi-card" style="border-left:6px solid ${maint.enabled ? '#DC2626' : '#059669'};">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <h2 style="font-size:18px; font-weight:700;">Maintenance Mode Status: <span style="color:${maint.enabled ? '#DC2626' : '#059669'};">${maint.enabled ? 'ACTIVE' : 'INACTIVE'}</span></h2>
            <p style="font-size:12px; color:#64748B; margin-top:4px;">
              ${maint.enabled 
                ? 'All customer banking requests and mobile logins are currently BLOCKED, except for staff and whitelisted testers.' 
                : 'Banking services are running normally and accepting all customer transactions.'}
            </p>
          </div>
          <button class="btn ${maint.enabled ? 'btn-secondary' : 'btn-danger'}" id="btnToggleMaintenance">
            ${maint.enabled ? 'Disable Maintenance Mode' : 'Enable Maintenance Mode'}
          </button>
        </div>
      </div>

      <!-- CONFIGURATION & PREVIEWS -->
      <div class="table-container" style="padding:24px;">
        <h3 style="font-size:15px; font-weight:700; margin-bottom:16px;">Localized Downtime Notice Messages</h3>
        <div style="display:grid; grid-template-columns:1fr 1fr; gap:24px;">
          <!-- ENGLISH -->
          <div>
            <div style="font-weight:700; font-size:13px; color:#2563EB; margin-bottom:8px;">English (EN)</div>
            <div class="form-group">
              <label class="form-label">Title</label>
              <input type="text" id="maintTitleEn" class="form-input" value="${escapeHtml(maint.title?.en || '')}">
            </div>
            <div class="form-group">
              <label class="form-label">Message</label>
              <textarea id="maintMsgEn" class="form-textarea" rows="4">${escapeHtml(maint.message?.en || '')}</textarea>
            </div>
          </div>

          <!-- INDONESIAN -->
          <div>
            <div style="font-weight:700; font-size:13px; color:#059669; margin-bottom:8px;">Bahasa Indonesia (ID)</div>
            <div class="form-group">
              <label class="form-label">Judul</label>
              <input type="text" id="maintTitleId" class="form-input" value="${escapeHtml(maint.title?.id || '')}">
            </div>
            <div class="form-group">
              <label class="form-label">Pesan</label>
              <textarea id="maintMsgId" class="form-textarea" rows="4">${escapeHtml(maint.message?.id || '')}</textarea>
            </div>
          </div>
        </div>

        <div style="margin-top:16px; display:flex; justify-content:flex-end;">
          <button class="btn btn-primary" id="btnSaveMaintContent">Save Localized Notice</button>
        </div>
      </div>

      <!-- TESTER BYPASS WHITELIST -->
      <div class="table-container" style="padding:24px;">
        <h3 style="font-size:15px; font-weight:700; margin-bottom:12px;">Tester Bypass Whitelist</h3>
        <p style="font-size:12px; color:#64748B; margin-bottom:16px;">These tester UIDs can log into the mobile app and test features even when Maintenance Mode is strictly active.</p>

        <div style="display:flex; gap:10px; margin-bottom:16px;">
          <input type="text" id="inputNewBypassUid" class="form-input" placeholder="Enter Tester Firebase UID...">
          <button class="btn btn-primary btn-sm" id="btnAddBypassUid">Whitelist UID</button>
        </div>

        <table class="data-table">
          <thead><tr><th>Tester UID</th><th>Added Date</th><th>Action</th></tr></thead>
          <tbody>
            ${bypassList.map(b => `
              <tr>
                <td><code>${escapeHtml(b.uid)}</code></td>
                <td>${b.addedAt || 'Active'}</td>
                <td><button class="btn btn-secondary btn-sm" onclick="window.removeBypass('${b.uid}')">Remove</button></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    </div>
  `;

  document.getElementById('btnToggleMaintenance').onclick = () => {
    const willEnable = !maint.enabled;
    showConfirmModal({
      title: willEnable ? 'ACTIVATE SYSTEM MAINTENANCE?' : 'DEACTIVATE MAINTENANCE?',
      message: willEnable 
        ? 'Activating maintenance will lock out customer app logins. Are you sure you wish to proceed?' 
        : 'Deactivating will restore public access to Utomo Bank mobile services immediately.',
      isDestructive: willEnable,
      requireReason: true,
      onConfirm: async (reason) => {
        try {
          const nextMaint = { ...maint, enabled: willEnable };
          await persistMaintenanceState(nextMaint);
          maint = nextMaint;
          localStorage.setItem('utomo_maint_cache', JSON.stringify(maint));
          await logAudit(willEnable ? 'ENABLE_MAINTENANCE' : 'DISABLE_MAINTENANCE', 'system_config', 'maintenance', null, maint, reason);
          showToast(willEnable ? 'Mode pemeliharaan telah DIAKTIFKAN.' : 'Mode pemeliharaan telah DINONAKTIFKAN.');
          renderMaintenance(container);
        } catch (error) {
          showToast(`Gagal menyimpan status maintenance: ${error.message}`, 'error');
        }
      }
    });
  };

  document.getElementById('btnSaveMaintContent').onclick = async () => {
    maint.title = {
      en: document.getElementById('maintTitleEn').value.trim(),
      id: document.getElementById('maintTitleId').value.trim()
    };
    maint.message = {
      en: document.getElementById('maintMsgEn').value.trim(),
      id: document.getElementById('maintMsgId').value.trim()
    };
    try {
      await persistMaintenanceState(maint);
      localStorage.setItem('utomo_maint_cache', JSON.stringify(maint));
      await logAudit('UPDATE_MAINTENANCE_NOTICE', 'system_config', 'maintenance', null, maint, 'Updated localized downtime text');
      showToast('Pesan downtime berhasil disimpan!');
    } catch (error) {
      showToast(`Gagal menyimpan pesan maintenance: ${error.message}`, 'error');
    }
  };

  document.getElementById('btnAddBypassUid').onclick = async () => {
    const uid = document.getElementById('inputNewBypassUid').value.trim();
    if (!uid) return;
    bypassList.push({ uid, addedAt: new Date().toISOString() });
    localStorage.setItem('utomo_bypass_cache', JSON.stringify(bypassList));
    showToast(`Tester UID "${uid}" berhasil dimasukkan ke whitelist bypass!`);
    renderMaintenance(container);
    try {
      await withTimeout(setDoc(doc(db, 'maintenance_bypass', uid), { active: true, addedAt: serverTimestamp() }), 500);
    } catch (_) {}
  };
}

window.removeBypass = async (uid) => {
  const cached = localStorage.getItem('utomo_bypass_cache');
  let list = cached ? JSON.parse(cached) : [];
  list = list.filter(b => b.uid !== uid);
  localStorage.setItem('utomo_bypass_cache', JSON.stringify(list));
  showToast(`UID "${uid}" berhasil dihapus dari whitelist.`);
  renderMaintenance(document.getElementById('appContent'));
  try {
    await withTimeout(deleteDoc(doc(db, 'maintenance_bypass', uid)), 500);
  } catch (_) {}
};

// ========================================================
// PAGE 7: STAFF & ROLES (SUPERADMIN ONLY)
// ========================================================
async function renderStaff(container) {
  if (!CAN.manageStaff()) {
    container.innerHTML = `<div class="empty-state">Access denied: Superadmin role required.</div>`;
    return;
  }

  container.innerHTML = `
    <div style="display:flex; flex-direction:column; gap:20px;">
      <div style="background:#EFF6FF; border:1px solid #BFDBFE; border-radius:10px; padding:16px; font-size:13px; color:#1E40AF;">
        <strong>Staff Provisioning Architecture:</strong><br>
        • <strong>Layer 1 (Spark Tier):</strong> Use the administrative CLI script:
        <code>node scripts/set-staff-role.js &lt;email&gt; &lt;role&gt;</code> to assign custom claims directly via the Firebase Admin SDK.<br>
        • <strong>Layer 2 (Blaze Tier):</strong> In-browser instant role assignment will activate automatically via the <code>setStaffRole</code> Cloud Function once upgraded.
      </div>

      <div class="table-container">
        <div class="table-toolbar">
          <h3 style="font-size:14px; font-weight:700;">Authorized Bank Staff Directory</h3>
        </div>
        <div id="staffListTable">Loading staff...</div>
      </div>
    </div>
  `;

  let staffList = [];
  try {
    const snap = await withTimeout(getDocs(collection(db, 'staff')), 600);
    staffList = snap.docs.map(d => ({ uid: d.id, ...d.data() }));
  } catch (err) {
    console.warn('Firestore staff offline/timeout:', err);
  }

  if (!staffList.length) {
    const cached = localStorage.getItem('utomo_staff_cache');
    staffList = cached ? JSON.parse(cached) : [...DEFAULT_STAFF_SEEDS];
    localStorage.setItem('utomo_staff_cache', JSON.stringify(staffList));
  }

  const tableEl = document.getElementById('staffListTable');
  tableEl.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Staff Name / Email</th>
          <th>Role</th>
          <th>Status</th>
          <th>UID</th>
          <th>Provisioned Date</th>
        </tr>
      </thead>
      <tbody>
        ${staffList.map(s => `
          <tr>
            <td>
              <strong>${escapeHtml(s.name || s.email?.split('@')[0])}</strong>
              <div style="font-size:11px; color:#64748B;">${escapeHtml(s.email)}</div>
            </td>
            <td><span class="badge badge-priority-normal">${escapeHtml(s.role)}</span></td>
            <td><span class="badge badge-${s.active ? 'active' : 'blocked'}">${s.active ? 'ACTIVE' : 'INACTIVE'}</span></td>
            <td><code>${escapeHtml(s.uid)}</code></td>
            <td>${escapeHtml(s.createdAt || '2026-01-01')}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

// ========================================================
// PAGE 8: CANNED RESPONSES (SUPERADMIN ONLY)
// ========================================================
async function renderCanned(container) {
  if (!CAN.manageCanned()) {
    container.innerHTML = `<div class="empty-state">Access denied: Superadmin role required.</div>`;
    return;
  }

  container.innerHTML = `
    <div class="table-container">
      <div class="table-toolbar">
        <div>
          <h3 style="font-size:14px; font-weight:700;">Canned Response Templates</h3>
          <p style="font-size:11px; color:#64748B;">Bilingual macros available to support agents during ticket conversations.</p>
        </div>
        <button class="btn btn-primary btn-sm" id="btnNewCanned">${Icons.plus} New Template</button>
      </div>
      <div id="cannedTableContainer">Loading templates...</div>
    </div>
  `;

  document.getElementById('btnNewCanned').onclick = () => window.editCannedResponse(null);

  let cannedList = [];
  try {
    const snap = await withTimeout(getDocs(collection(db, 'cannedResponses')), 600);
    cannedList = snap.docs.map(d => ({ id: d.id, ...d.data() }));
  } catch (err) {
    console.warn('Firestore canned offline/timeout:', err);
  }

  if (!cannedList.length) {
    const cached = localStorage.getItem('utomo_canned_cache');
    cannedList = cached ? JSON.parse(cached) : [...DEFAULT_CANNED_SEEDS];
    localStorage.setItem('utomo_canned_cache', JSON.stringify(cannedList));
  }
  window.__allCannedCache = cannedList;

  const tableContainer = document.getElementById('cannedTableContainer');
  tableContainer.innerHTML = `
    <table class="data-table">
      <thead>
        <tr>
          <th>Category</th>
          <th>English Title & Body</th>
          <th>Indonesian Title & Body</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        ${cannedList.map(c => `
          <tr>
            <td><span class="badge badge-priority-normal">${escapeHtml(c.category)}</span></td>
            <td style="max-width:280px;">
              <strong>${escapeHtml(c.title?.en || '-')}</strong>
              <div style="font-size:12px; color:#64748B; margin-top:4px;">${escapeHtml(c.body?.en || '')}</div>
            </td>
            <td style="max-width:280px;">
              <strong>${escapeHtml(c.title?.id || '-')}</strong>
              <div style="font-size:12px; color:#64748B; margin-top:4px;">${escapeHtml(c.body?.id || '')}</div>
            </td>
            <td>
              <div style="display:flex; gap:6px;">
                <button class="btn btn-secondary btn-sm" onclick="window.editCannedResponse('${c.id}')">Edit</button>
                <button class="btn btn-danger btn-sm" onclick="window.deleteCannedResponse('${c.id}')">Delete</button>
              </div>
            </td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

window.editCannedResponse = (templateId) => {
  const existing = (window.__allCannedCache || []).find(c => c.id === templateId) || {};
  const isEdit = Boolean(templateId);

  const bodyHtml = `
    <form id="formCannedResponse" style="display:flex; flex-direction:column; gap:12px;">
      <div class="form-group">
        <label class="form-label">Template ID *</label>
        <input type="text" id="editCannedId" class="form-input" required ${isEdit ? 'readonly' : ''} value="${escapeHtml(existing.id || '')}" placeholder="e.g. card_pin_reset">
      </div>
      <div class="form-group">
        <label class="form-label">Category *</label>
        <select id="editCannedCategory" class="form-select">
          <option value="Card problem" ${existing.category === 'Card problem' ? 'selected' : ''}>Card problem</option>
          <option value="Transaction problem" ${existing.category === 'Transaction problem' ? 'selected' : ''}>Transaction problem</option>
          <option value="Account" ${existing.category === 'Account' ? 'selected' : ''}>Account</option>
          <option value="Payment or bill" ${existing.category === 'Payment or bill' ? 'selected' : ''}>Payment or bill</option>
          <option value="QRIS" ${existing.category === 'QRIS' ? 'selected' : ''}>QRIS</option>
          <option value="General" ${existing.category === 'General' ? 'selected' : ''}>General</option>
        </select>
      </div>
      <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
        <div class="form-group">
          <label class="form-label">Title (English)</label>
          <input type="text" id="editCannedTitleEn" class="form-input" value="${escapeHtml(existing.title?.en || '')}">
        </div>
        <div class="form-group">
          <label class="form-label">Title (Indonesian)</label>
          <input type="text" id="editCannedTitleId" class="form-input" value="${escapeHtml(existing.title?.id || '')}">
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">Body Text (English)</label>
        <textarea id="editCannedBodyEn" class="form-textarea" rows="3">${escapeHtml(existing.body?.en || '')}</textarea>
      </div>
      <div class="form-group">
        <label class="form-label">Body Text (Indonesian)</label>
        <textarea id="editCannedBodyId" class="form-textarea" rows="3">${escapeHtml(existing.body?.id || '')}</textarea>
      </div>
    </form>
  `;

  showGenericModal({
    title: isEdit ? 'Edit Canned Template' : 'Create Canned Template',
    bodyHtml,
    footerHtml: `
      <button type="button" class="btn btn-secondary" id="btnCancelCanned">Cancel</button>
      <button type="button" class="btn btn-primary" id="btnSaveCanned">Save Template</button>
    `
  });

  document.getElementById('btnCancelCanned').onclick = () => {
    document.getElementById('genericModal').style.display = 'none';
  };

  document.getElementById('btnSaveCanned').onclick = async () => {
    const id = document.getElementById('editCannedId').value.trim();
    if (!id) return;
    const item = {
      id,
      category: document.getElementById('editCannedCategory').value,
      title: {
        en: document.getElementById('editCannedTitleEn').value.trim(),
        id: document.getElementById('editCannedTitleId').value.trim()
      },
      body: {
        en: document.getElementById('editCannedBodyEn').value.trim(),
        id: document.getElementById('editCannedBodyId').value.trim()
      }
    };

    let list = window.__allCannedCache || [];
    const idx = list.findIndex(c => c.id === id);
    if (idx >= 0) list[idx] = item;
    else list.push(item);
    localStorage.setItem('utomo_canned_cache', JSON.stringify(list));

    document.getElementById('genericModal').style.display = 'none';
    showToast(`Template "${id}" berhasil disimpan!`);
    renderCanned(document.getElementById('appContent'));
    try {
      await withTimeout(setDoc(doc(db, 'cannedResponses', id), item), 500);
    } catch (_) {}
  };
};

window.deleteCannedResponse = (templateId) => {
  showConfirmModal({
    title: `Delete Template: ${templateId}`,
    message: 'Are you sure you want to remove this canned response template?',
    isDestructive: true,
    requireReason: false,
    onConfirm: async () => {
      let list = (window.__allCannedCache || []).filter(c => c.id !== templateId);
      window.__allCannedCache = list;
      localStorage.setItem('utomo_canned_cache', JSON.stringify(list));
      showToast(`Template "${templateId}" berhasil dihapus.`);
      renderCanned(document.getElementById('appContent'));
      try {
        await withTimeout(deleteDoc(doc(db, 'cannedResponses', templateId)), 500);
      } catch (_) {}
    }
  });
};

// ========================================================
// PAGE 9: AUDIT LOG (SUPERADMIN ONLY)
// ========================================================
async function renderAudit(container) {
  if (!CAN.viewAuditLog()) {
    container.innerHTML = `<div class="empty-state">Access denied: Superadmin role required.</div>`;
    return;
  }

  container.innerHTML = `
    <div class="table-container">
      <div class="table-toolbar">
        <div>
          <h3 style="font-size:14px; font-weight:700;">Immutable System Audit Trail</h3>
          <p style="font-size:11px; color:#64748B;">Append-only security log of every administrative staff action.</p>
        </div>
        <div class="search-input-group">
          ${Icons.search}
          <input type="text" id="auditSearchInput" placeholder="Filter by action, actor, or target ID...">
        </div>
      </div>
      <div id="auditTableContainer">Loading audit trail...</div>
    </div>
  `;

  const searchInput = document.getElementById('auditSearchInput');
  const tableContainer = document.getElementById('auditTableContainer');

  let allLogs = [];
  try {
    const snap = await withTimeout(getDocs(query(collection(db, 'audit_logs'), orderBy('createdAt', 'desc'), limit(100))), 600);
    allLogs = snap.docs.map(d => ({ id: d.id, ...d.data() }));
  } catch (err) {
    console.warn('Firestore audit offline/timeout:', err);
  }

  if (!allLogs.length) {
    const cached = localStorage.getItem('utomo_audit_cache');
    allLogs = cached ? JSON.parse(cached) : [
      { id: 'log-1', action: 'UPDATE_BRANCH', actorEmail: 'admin@bankutomo.co.id', actorRole: 'superadmin', targetType: 'branch', targetId: 'branch-1', reason: 'Pembaruan alamat gedung cabang', createdAt: new Date(Date.now() - 1800000).toISOString() },
      { id: 'log-2', action: 'ADMIN_LOGIN', actorEmail: 'admin@bankutomo.co.id', actorRole: 'superadmin', targetType: 'session', targetId: 'demo-superadmin', reason: 'Sign-in to Console', createdAt: new Date(Date.now() - 3600000).toISOString() },
      { id: 'log-3', action: 'SYSTEM_START', actorEmail: 'system', actorRole: 'superadmin', targetType: 'system', targetId: 'core-service', reason: 'Back-Office Portal initialized', createdAt: new Date(Date.now() - 7200000).toISOString() }
    ];
    localStorage.setItem('utomo_audit_cache', JSON.stringify(allLogs));
  }
  window.__allAuditCache = allLogs;

  const draw = (list) => {
    if (list.length === 0) {
      tableContainer.innerHTML = `<div class="empty-state">No audit logs found.</div>`;
      return;
    }

    tableContainer.innerHTML = `
      <table class="data-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Action</th>
            <th>Staff Actor</th>
            <th>Target</th>
            <th>Reason</th>
            <th>Details</th>
          </tr>
        </thead>
        <tbody>
          ${list.map(l => `
            <tr>
              <td><code>${formatDate(l.createdAt)}</code></td>
              <td><span class="badge badge-priority-normal">${escapeHtml(l.action)}</span></td>
              <td>
                <strong>${escapeHtml(l.actorEmail || 'staff')}</strong>
                <div style="font-size:11px; color:#64748B;">Role: ${escapeHtml(l.actorRole || '-')}</div>
              </td>
              <td><code>${escapeHtml(l.targetType)}:${escapeHtml(l.targetId)}</code></td>
              <td><div style="max-width:240px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escapeHtml(l.reason || '-')}</div></td>
              <td>
                <button class="btn btn-secondary btn-sm" onclick="window.viewAuditDetail('${l.id}')">Inspect</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  };

  searchInput.oninput = () => {
    const q = searchInput.value.toLowerCase().trim();
    const filtered = allLogs.filter(l =>
      !q ||
      (l.action && l.action.toLowerCase().includes(q)) ||
      (l.actorEmail && l.actorEmail.toLowerCase().includes(q)) ||
      (l.targetId && l.targetId.toLowerCase().includes(q)) ||
      (l.reason && l.reason.toLowerCase().includes(q))
    );
    draw(filtered);
  };

  draw(allLogs);
}

window.viewAuditDetail = (logId) => {
  const log = (window.__allAuditCache || []).find(l => l.id === logId);
  if (!log) return;

  const bodyHtml = `
    <div style="display:flex; flex-direction:column; gap:12px; font-size:13px;">
      <div><strong>Action:</strong> <code>${escapeHtml(log.action)}</code></div>
      <div><strong>Actor:</strong> ${escapeHtml(log.actorEmail)} (${escapeHtml(log.actorRole || 'staff')})</div>
      <div><strong>Target:</strong> ${escapeHtml(log.targetType)}:${escapeHtml(log.targetId)}</div>
      <div><strong>Timestamp:</strong> ${formatDate(log.createdAt)}</div>
      <div style="font-size:12px; color:#475569; margin-top:4px;">Reason: <strong>${escapeHtml(log.reason || 'None provided')}</strong></div>

      <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-top:8px;">
        <div>
          <strong style="color:#DC2626; font-size:12px;">BEFORE:</strong>
          <pre style="background:#F8FAFC; border:1px solid #E2E8F0; padding:10px; border-radius:6px; font-size:11px; max-height:200px; overflow:auto;">${escapeHtml(JSON.stringify(log.before || {}, null, 2))}</pre>
        </div>
        <div>
          <strong style="color:#059669; font-size:12px;">AFTER:</strong>
          <pre style="background:#F8FAFC; border:1px solid #E2E8F0; padding:10px; border-radius:6px; font-size:11px; max-height:200px; overflow:auto;">${escapeHtml(JSON.stringify(log.after || {}, null, 2))}</pre>
        </div>
      </div>
    </div>
  `;

  showGenericModal({
    title: `Audit Entry: ${log.action}`,
    bodyHtml
  });
};

// PAGE 10: BRANCH & ATM NETWORK LOCATIONS (EDITABLE ADDRESSES)
// ========================================================
const DEFAULT_BRANCH_SEEDS = [
  {
    id: "branch-1",
    name: "KC Sudirman Utama",
    type: "KC",
    address: "Jl. Jend. Sudirman Kav 21, Jakarta Pusat",
    city: "Jakarta Pusat",
    distance: "0.8 km",
    hours: "Buka: 08:00 - 15:30 • ATM 24 Jam",
    services: "Layanan Teller • ATM 24 Jam • Tarik Tunai",
    latitude: -6.2146,
    longitude: 106.8217,
    isCardlessSupported: true
  },
  {
    id: "branch-2",
    name: "KCP Plaza Senayan",
    type: "KCP",
    address: "Plaza Senayan Lt. LG Unit 12, Jakarta Pusat",
    city: "Jakarta Pusat",
    distance: "1.4 km",
    hours: "Buka: 10:00 - 19:00 • CDM & ATM Tersedia",
    services: "CDM & ATM Tersedia • Tarik Tunai",
    latitude: -6.2253,
    longitude: 106.7997,
    isCardlessSupported: true
  },
  {
    id: "branch-3",
    name: "ATM Center Grand Indonesia",
    type: "ATM",
    address: "East Mall Basement 1, Thamrin, Jakarta Pusat",
    city: "Jakarta Pusat",
    distance: "2.3 km",
    hours: "Buka 24 Jam • Mendukung Tarik Tunai Tanpa Kartu",
    services: "Mendukung Tarik Tunai Tanpa Kartu",
    latitude: -6.1950,
    longitude: 106.8210,
    isCardlessSupported: true
  },
  {
    id: "branch-4",
    name: "KC Thamrin Boulevard",
    type: "KC",
    address: "Jl. M.H. Thamrin No. 28-30, Jakarta Pusat",
    city: "Jakarta Pusat",
    distance: "3.1 km",
    hours: "Buka: 08:00 - 16:00 • ATM & Valas",
    services: "Layanan Teller • ATM 24 Jam • Penukaran Valas",
    latitude: -6.1895,
    longitude: 106.8236,
    isCardlessSupported: true
  },
  {
    id: "branch-5",
    name: "KCP Kuningan City",
    type: "KCP",
    address: "Kuningan City Mall Lt. G-08, Jakarta Selatan",
    city: "Jakarta Selatan",
    distance: "3.8 km",
    hours: "Buka: 09:00 - 18:00 • CDM Tersedia",
    services: "Setor Tarik Tunai • Konsultasi CS",
    latitude: -6.2238,
    longitude: 106.8288,
    isCardlessSupported: true
  },
  {
    id: "branch-6",
    name: "ATM Center Pacific Place",
    type: "ATM",
    address: "SCBD Lot 3-5, Jl. Jend. Sudirman, Jakarta Selatan",
    city: "Jakarta Selatan",
    distance: "4.5 km",
    hours: "Buka 24 Jam • Tarik & Setor Tunai",
    services: "Mendukung Tarik Tunai Tanpa Kartu",
    latitude: -6.2246,
    longitude: 106.8098,
    isCardlessSupported: true
  }
];

async function renderBranches(container) {
  container.innerHTML = `<div style="text-align:center; padding:40px; color:#64748B;">Loading branch & ATM network...</div>`;

  let branchList = [];
  try {
    const snap = await withTimeout(getDocs(collection(db, 'branches')), 1000);
    branchList = snap.docs.map(d => ({ docId: d.id, ...d.data() }));
  } catch (err) {
    console.warn('Firestore branches collection fetch warning, using local cache:', err);
  }

  if (!branchList.length) {
    const cached = localStorage.getItem('utomo_branches_cache');
    if (cached) {
      try { branchList = JSON.parse(cached); } catch (_) {}
    }
    if (!branchList.length) {
      branchList = [...DEFAULT_BRANCH_SEEDS];
      localStorage.setItem('utomo_branches_cache', JSON.stringify(branchList));
    }
  }

  try {
    // Store in global window for easy access
    window.__currentBranches = branchList;

    const canEdit = CAN.manageBranches();

    const kcCount = branchList.filter(b => b.type === 'KC').length;
    const kcpCount = branchList.filter(b => b.type === 'KCP').length;
    const atmCount = branchList.filter(b => b.type === 'ATM').length;

    container.innerHTML = `
      <div style="display:flex; flex-direction:column; gap:20px;">
        <!-- KPI METRICS -->
        <div class="kpi-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
          <div class="kpi-card">
            <div class="kpi-label">Total Network Locations</div>
            <div class="kpi-value">${branchList.length}</div>
            <div class="kpi-subtext">Active physical points</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Kantor Cabang (KC)</div>
            <div class="kpi-value" style="color:#2563EB;">${kcCount}</div>
            <div class="kpi-subtext">Full teller & forex services</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Cabang Pembantu (KCP)</div>
            <div class="kpi-value" style="color:#7C3AED;">${kcpCount}</div>
            <div class="kpi-subtext">Sub-branches & cash deposit</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">ATM Centers</div>
            <div class="kpi-value" style="color:#059669;">${atmCount}</div>
            <div class="kpi-subtext">24/7 cardless cash points</div>
          </div>
        </div>

        <!-- TABLE WITH TOOLBAR -->
        <div class="table-container">
          <div class="table-toolbar">
            <div class="table-filter-group" style="flex:1;">
              <div class="search-input-group" style="min-width:280px;">
                ${Icons.search}
                <input type="text" id="branchSearchInput" placeholder="Search branch, address, city...">
              </div>
              <select id="branchTypeFilter" class="form-input" style="width:160px; padding:7px 12px; font-size:13px;">
                <option value="ALL">All Types (${branchList.length})</option>
                <option value="KC">Kantor Cabang (KC)</option>
                <option value="KCP">Cabang Pembantu (KCP)</option>
                <option value="ATM">ATM Center</option>
              </select>
            </div>

            <div style="display:flex; gap:10px;">
              ${branchList.length === 0 ? `
                <button type="button" class="btn btn-secondary btn-sm" id="btnSeedDefaultBranches">
                  ${Icons.refresh} Seed Standard Locations
                </button>
              ` : `
                <button type="button" class="btn btn-secondary btn-sm" id="btnSeedDefaultBranches" title="Sync default sample points">
                  ${Icons.refresh} Reset/Seed Defaults
                </button>
              `}
              ${canEdit ? `
                <button type="button" class="btn btn-primary btn-sm" id="btnAddBranch">
                  ${Icons.plus} Add New Branch/ATM
                </button>
              ` : ''}
            </div>
          </div>

          <div id="branchTableContent" style="overflow-x:auto;">
            <!-- Drawn via drawBranches -->
          </div>
        </div>
      </div>
    `;

    const tableContent = document.getElementById('branchTableContent');
    const searchInput = document.getElementById('branchSearchInput');
    const typeFilter = document.getElementById('branchTypeFilter');
    const btnAdd = document.getElementById('btnAddBranch');
    const btnSeed = document.getElementById('btnSeedDefaultBranches');

    if (btnAdd) {
      btnAdd.onclick = () => window.openBranchModal(null);
    }
    if (btnSeed) {
      btnSeed.onclick = () => window.seedDefaultBranches();
    }

    const draw = (list) => {
      if (!list.length) {
        tableContent.innerHTML = `
          <div style="padding:48px; text-align:center; color:#64748B;">
            <div style="font-size:15px; font-weight:600; margin-bottom:6px;">No branch locations found</div>
            <p style="font-size:13px; max-width:400px; margin:0 auto 16px;">
              No locations match your filter or the database collection is empty.
            </p>
            <button class="btn btn-secondary btn-sm" onclick="window.seedDefaultBranches()">Seed Standard Jakarta Branches</button>
          </div>
        `;
        return;
      }

      tableContent.innerHTML = `
        <table class="data-table">
          <thead>
            <tr>
              <th>Branch / Location Name</th>
              <th>Type</th>
              <th>Full Address & City</th>
              <th>Operating Hours & Services</th>
              <th>Coordinates</th>
              <th>Cardless Support</th>
              <th style="text-align:right;">Actions</th>
            </tr>
          </thead>
          <tbody>
            ${list.map(b => {
              const typeColor = b.type === 'KC' ? '#2563EB' : (b.type === 'KCP' ? '#7C3AED' : '#059669');
              const typeBg = b.type === 'KC' ? '#EFF6FF' : (b.type === 'KCP' ? '#F5F3FF' : '#ECFDF5');
              return `
                <tr>
                  <td>
                    <div style="font-weight:600; color:#0F172A;">${escapeHtml(b.name)}</div>
                    <div style="font-size:11px; color:#64748B;">ID: <code>${escapeHtml(b.id || b.docId)}</code> • ${escapeHtml(b.distance || '0.8 km')}</div>
                  </td>
                  <td>
                    <span class="badge" style="background:${typeBg}; color:${typeColor}; border:1px solid ${typeColor}22;">
                      ${escapeHtml(b.type || 'KC')}
                    </span>
                  </td>
                  <td style="max-width:260px;">
                    <div style="font-size:13px; font-weight:500; color:#1E293B;">${escapeHtml(b.address || '-')}</div>
                    <div style="font-size:11px; color:#64748B;">${escapeHtml(b.city || 'Jakarta')}</div>
                  </td>
                  <td style="max-width:240px;">
                    <div style="font-size:12px; color:#0F172A;">${escapeHtml(b.hours || '08:00 - 15:30')}</div>
                    <div style="font-size:11px; color:#64748B;">${escapeHtml(b.services || '-')}</div>
                  </td>
                  <td style="font-size:11px; color:#64748B; font-family:monospace;">
                    ${Number(b.latitude || 0).toFixed(4)}, ${Number(b.longitude || 0).toFixed(4)}
                  </td>
                  <td>
                    ${b.isCardlessSupported !== false ? `
                      <span class="badge badge-active">Supported</span>
                    ` : `
                      <span class="badge badge-closed">Card Only</span>
                    `}
                  </td>
                  <td style="text-align:right; white-space:nowrap;">
                    ${canEdit ? `
                      <button class="btn btn-secondary btn-sm" onclick="window.openBranchModal('${b.docId || b.id}')" title="Edit Address & Location" style="margin-right:6px;">
                        ${Icons.edit} Edit
                      </button>
                      <button class="btn btn-danger btn-sm" onclick="window.deleteBranch('${b.docId || b.id}')" title="Delete Location">
                        ${Icons.trash} Delete
                      </button>
                    ` : `
                      <span style="font-size:11px; color:#94A3B8;">Read-only</span>
                    `}
                  </td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      `;
    };

    const filterBranches = () => {
      const q = (searchInput.value || '').toLowerCase().trim();
      const type = typeFilter.value;

      const filtered = branchList.filter(b => {
        const matchesQuery = !q || 
          (b.name && b.name.toLowerCase().includes(q)) ||
          (b.address && b.address.toLowerCase().includes(q)) ||
          (b.city && b.city.toLowerCase().includes(q)) ||
          (b.services && b.services.toLowerCase().includes(q));

        const matchesType = type === 'ALL' || b.type === type;
        return matchesQuery && matchesType;
      });

      draw(filtered);
    };

    searchInput.oninput = filterBranches;
    typeFilter.onchange = filterBranches;

    draw(branchList);
  } catch (err) {
    container.innerHTML = `<div class="empty-state">Error loading branches: ${err.message}</div>`;
  }
}

window.seedDefaultBranches = async () => {
  showConfirmModal({
    title: 'Seed Branch & ATM Locations',
    message: 'This will seed standard Jakarta branch and ATM network locations to Firestore so they can be immediately updated and synchronized to mobile apps.',
    isDestructive: false,
    requireReason: false,
    onConfirm: async () => {
      try {
        for (const item of DEFAULT_BRANCH_SEEDS) {
          try {
            await withTimeout(setDoc(doc(db, 'branches', item.id), item), 600);
          } catch (_) {}
        }
        try {
          await withTimeout(logAudit('SEED_BRANCHES', 'branches', 'network', null, { count: DEFAULT_BRANCH_SEEDS.length }, 'Admin seeded default locations'), 600);
        } catch (_) {}
        localStorage.setItem('utomo_branches_cache', JSON.stringify(DEFAULT_BRANCH_SEEDS));
        showToast('Berhasil men-seed lokasi cabang & ATM default!');
        renderBranches(document.getElementById('appContent'));
      } catch (err) {
        showToast(`Gagal seeding: ${err.message}`, 'error');
      }
    }
  });
};

window.openBranchModal = (docId) => {
  const isEdit = Boolean(docId);
  const branch = isEdit ? (window.__currentBranches || []).find(b => b.docId === docId || b.id === docId) : null;

  const title = isEdit ? `Edit Branch: ${branch?.name || docId}` : 'Add New Branch / ATM Location';

  const defaultId = branch?.id || `branch-${Date.now()}`;
  const defaultName = branch?.name || '';
  const defaultType = branch?.type || 'KC';
  const defaultAddress = branch?.address || '';
  const defaultCity = branch?.city || 'Jakarta Pusat';
  const defaultDistance = branch?.distance || '1.0 km';
  const defaultHours = branch?.hours || '08:00 - 15:30';
  const defaultServices = branch?.services || 'Layanan Teller • ATM 24 Jam • Tarik Tunai';
  const defaultLat = branch?.latitude != null ? branch.latitude : -6.2146;
  const defaultLng = branch?.longitude != null ? branch.longitude : 106.8217;
  const defaultCardless = branch?.isCardlessSupported !== false;

  const bodyHtml = `
    <form id="branchEditForm" style="display:flex; flex-direction:column; gap:16px;">
      <input type="hidden" id="branchDocId" value="${escapeHtml(branch?.docId || defaultId)}">

      <div style="display:grid; grid-template-columns:1fr 140px; gap:12px;">
        <div class="form-group">
          <label class="form-label" for="branchName">Branch / ATM Name *</label>
          <input type="text" id="branchName" class="form-input" required value="${escapeHtml(defaultName)}" placeholder="e.g. KC Sudirman Utama">
        </div>
        <div class="form-group">
          <label class="form-label" for="branchType">Type *</label>
          <select id="branchType" class="form-input">
            <option value="KC" ${defaultType === 'KC' ? 'selected' : ''}>KC (Kantor Cabang)</option>
            <option value="KCP" ${defaultType === 'KCP' ? 'selected' : ''}>KCP (Cabang Pembantu)</option>
            <option value="ATM" ${defaultType === 'ATM' ? 'selected' : ''}>ATM Center</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label" for="branchAddress">Full Street Address * (Editable for Mobile App)</label>
        <textarea id="branchAddress" class="form-textarea" rows="2" required placeholder="e.g. Jl. Jend. Sudirman Kav 21, Gedung Utomo Tower">${escapeHtml(defaultAddress)}</textarea>
      </div>

      <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
        <div class="form-group">
          <label class="form-label" for="branchCity">City / Regency *</label>
          <input type="text" id="branchCity" class="form-input" required value="${escapeHtml(defaultCity)}" placeholder="e.g. Jakarta Pusat">
        </div>
        <div class="form-group">
          <label class="form-label" for="branchDistance">Distance Display</label>
          <input type="text" id="branchDistance" class="form-input" value="${escapeHtml(defaultDistance)}" placeholder="e.g. 0.8 km">
        </div>
      </div>

      <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
        <div class="form-group">
          <label class="form-label" for="branchHours">Operating Hours</label>
          <input type="text" id="branchHours" class="form-input" value="${escapeHtml(defaultHours)}" placeholder="e.g. Buka: 08:00 - 15:30 • ATM 24 Jam">
        </div>
        <div class="form-group">
          <label class="form-label" for="branchServices">Offered Services</label>
          <input type="text" id="branchServices" class="form-input" value="${escapeHtml(defaultServices)}" placeholder="e.g. Tarik Tunai • Setor Tunai • Valas">
        </div>
      </div>

      <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px;">
        <div class="form-group">
          <label class="form-label" for="branchLatitude">Latitude (GPS)</label>
          <input type="text" id="branchLatitude" class="form-input" required value="${defaultLat}">
        </div>
        <div class="form-group">
          <label class="form-label" for="branchLongitude">Longitude (GPS)</label>
          <input type="text" id="branchLongitude" class="form-input" required value="${defaultLng}">
        </div>
      </div>

      <div class="form-group" style="display:flex; align-items:center; gap:10px; margin-top:4px;">
        <input type="checkbox" id="branchCardless" ${defaultCardless ? 'checked' : ''} style="width:16px; height:16px; accent-color:#2563EB;">
        <label for="branchCardless" style="font-size:13px; font-weight:600; color:#1E293B; cursor:pointer;">
          Support Cardless Cash Withdrawal (Tarik Tunai Tanpa Kartu)
        </label>
      </div>

      <div class="form-group">
        <label class="form-label" for="branchAuditReason">Administrative Change Reason *</label>
        <input type="text" id="branchAuditReason" class="form-input" required placeholder="Reason for updating or creating this location...">
      </div>
    </form>
  `;

  const footerHtml = `
    <button type="button" class="btn btn-secondary" id="btnModalCancel">Cancel</button>
    <button type="button" class="btn btn-primary" id="btnSaveBranch">${isEdit ? 'Save Changes' : 'Create Location'}</button>
  `;

  showGenericModal({
    title,
    bodyHtml,
    footerHtml
  });

  document.getElementById('btnModalCancel').onclick = () => {
    document.getElementById('genericModal').style.display = 'none';
  };

  document.getElementById('btnSaveBranch').onclick = async () => {
    const targetDocId = document.getElementById('branchDocId')?.value?.trim();
    const name = document.getElementById('branchName')?.value?.trim();
    const type = document.getElementById('branchType')?.value;
    const address = document.getElementById('branchAddress')?.value?.trim();
    const city = document.getElementById('branchCity')?.value?.trim();
    const distance = document.getElementById('branchDistance')?.value?.trim() || '1.0 km';
    const hours = document.getElementById('branchHours')?.value?.trim() || '08:00 - 15:30';
    const services = document.getElementById('branchServices')?.value?.trim() || 'Tarik Tunai';
    const latRaw = document.getElementById('branchLatitude')?.value?.trim() || '-6.2146';
    const lngRaw = document.getElementById('branchLongitude')?.value?.trim() || '106.8217';
    const latitude = parseFloat(latRaw.replace(',', '.')) || -6.2146;
    const longitude = parseFloat(lngRaw.replace(',', '.')) || 106.8217;
    const isCardlessSupported = document.getElementById('branchCardless')?.checked ?? true;
    const reason = document.getElementById('branchAuditReason')?.value?.trim() || 'Updated location';

    if (!name || !address || !city) {
      showToast('Mohon lengkapi Nama Cabang, Alamat, dan Kota.', 'error');
      return;
    }

    const savePayload = {
      id: targetDocId,
      name,
      type,
      address,
      city,
      distance,
      hours,
      services,
      latitude,
      longitude,
      isCardlessSupported,
      updatedAt: serverTimestamp(),
      updatedBy: state.currentUser.email
    };

    const saveBtn = document.getElementById('btnSaveBranch');
    saveBtn.disabled = true;
    saveBtn.textContent = 'Saving...';

    try {
      try {
        await withTimeout(setDoc(doc(db, 'branches', targetDocId), savePayload), 700);
        await withTimeout(logAudit(
          isEdit ? 'UPDATE_BRANCH' : 'CREATE_BRANCH',
          'branch',
          targetDocId,
          branch || null,
          savePayload,
          reason
        ), 700);
      } catch (firestoreErr) {
        console.warn('Firestore write warning/offline mode:', firestoreErr);
      }

      // Sync local cache
      const cached = localStorage.getItem('utomo_branches_cache');
      let currentList = cached ? JSON.parse(cached) : [...DEFAULT_BRANCH_SEEDS];
      const idx = currentList.findIndex(b => (b.id === targetDocId || b.docId === targetDocId));
      if (idx >= 0) {
        currentList[idx] = { ...currentList[idx], ...savePayload };
      } else {
        currentList.push(savePayload);
      }
      localStorage.setItem('utomo_branches_cache', JSON.stringify(currentList));

      document.getElementById('genericModal').style.display = 'none';
      showToast(`Lokasi "${name}" berhasil disimpan! Tersinkron ke aplikasi mobile.`);
      renderBranches(document.getElementById('appContent'));
    } catch (err) {
      showToast(`Gagal menyimpan: ${err.message}`, 'error');
      saveBtn.disabled = false;
      saveBtn.textContent = isEdit ? 'Save Changes' : 'Create Location';
    }
  };
};

window.deleteBranch = (docId) => {
  const branch = (window.__currentBranches || []).find(b => b.docId === docId || b.id === docId);
  const name = branch ? branch.name : docId;

  showConfirmModal({
    title: `Delete Branch Location: ${name}`,
    message: `Are you sure you want to remove "${name}" from the network? It will no longer appear on the ATM/branch selection map in the mobile app.`,
    isDestructive: true,
    requireReason: true,
    onConfirm: async (reason) => {
      try {
        try {
          await withTimeout(deleteDoc(doc(db, 'branches', docId)), 700);
          await withTimeout(logAudit('DELETE_BRANCH', 'branch', docId, branch || null, null, reason), 700);
        } catch (fErr) {
          console.warn('Firestore delete offline mode:', fErr);
        }
        const cached = localStorage.getItem('utomo_branches_cache');
        if (cached) {
          let currentList = JSON.parse(cached).filter(b => b.id !== docId && b.docId !== docId);
          localStorage.setItem('utomo_branches_cache', JSON.stringify(currentList));
        }
        showToast(`Cabang "${name}" berhasil dihapus.`);
        renderBranches(document.getElementById('appContent'));
      } catch (err) {
        showToast(`Error deleting branch: ${err.message}`, 'error');
      }
    }
  });
};

// Start application
initAuth();

