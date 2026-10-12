const express = require('express');
const cors = require('cors');
const { initSession, getStatus } = require('./waService');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));
const { createRequireAdmin } = require('./http-auth');
const requireAdmin = createRequireAdmin((token, revoked) => require('firebase-admin/auth').getAuth().verifyIdToken(token, revoked));

app.get('/api/wa/status', requireAdmin, (req, res) => {
    res.json(getStatus());
});

app.post('/api/wa/connect', requireAdmin, (req, res) => {
    Promise.resolve().then(() => initSession()).then(() => res.json({ message: 'Initializing connection...' })).catch(() => res.status(500).json({ error: 'Connection failed' }));
});

const PORT = process.env.PORT || 8080;

app.listen(PORT, () => {
    console.log(`Server WA JCL berjalan di port ${PORT}`);
    // Otomatis mulai koneksi WA saat server berjalan
    initSession();
});
