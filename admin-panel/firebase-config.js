// Firebase Web SDK Configuration
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js";
import { getDatabase } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js";
import { getFunctions } from "https://www.gstatic.com/firebasejs/10.12.0/firebase-functions.js";

const firebaseConfig = {
  apiKey: "AIzaSyC4NMzriZ3LV9XE2bfaxubxImTsrPqz5C0",
  authDomain: "utomobank-ab445.firebaseapp.com",
  databaseURL: "https://utomobank-ab445-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "utomobank-ab445",
  storageBucket: "utomobank-ab445.firebasestorage.app",
  messagingSenderId: "170492840437",
  appId: "1:170492840437:web:da5821c52ea1ce6563f8bc",
  measurementId: "G-PVZJYGT50P"
};

export const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const rtdb = getDatabase(app);
export const functions = getFunctions(app);
