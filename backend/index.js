const express = require('express');
const multer = require('multer');
const cors = require('cors');
const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
const { v4: uuidv4 } = require('uuid');
require('dotenv').config();

// Initialize Express app
const app = express();
app.use(cors());
app.use(express.json());

// Cloudflare R2 Configuration
const s3 = new S3Client({
    region: 'auto',
    endpoint: `https://${process.env.R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
    credentials: {
        accessKeyId: process.env.R2_ACCESS_KEY_ID || '',
        secretAccessKey: process.env.R2_SECRET_ACCESS_KEY || '',
    },
});

// Configure multer for memory storage
const upload = multer({ 
    storage: multer.memoryStorage(),
    limits: { fileSize: 100 * 1024 * 1024 }, // 100MB limit for videos and photos
});

// --- API ROUTES ---

// Health Check Endpoint
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', message: 'Backend server is running properly with R2!' });
});

// Proxy logic to keep Supabase API key hidden
const REAL_SUPABASE_URL = process.env.SUPABASE_URL; // e.g., https://your-project.supabase.co
const REAL_SUPABASE_KEY = process.env.SUPABASE_KEY;

app.use(['/auth/v1', '/rest/v1'], async (req, res) => {
    try {
        if (!REAL_SUPABASE_URL || !REAL_SUPABASE_KEY) {
            return res.status(500).json({ error: 'Supabase URL or Key not set on the server.' });
        }

        // Fix double trailing slashes just in case, originalUrl contains /auth/v1/...
        const baseUrl = REAL_SUPABASE_URL.endsWith('/') ? REAL_SUPABASE_URL.slice(0, -1) : REAL_SUPABASE_URL;
        const targetUrl = new URL(req.originalUrl, baseUrl).toString();
        
        const headers = new Headers();
        // Copy relevant headers from the client
        if (req.headers.authorization) {
            headers.append('Authorization', req.headers.authorization);
        } else {
            headers.append('Authorization', `Bearer ${REAL_SUPABASE_KEY}`);
        }
        
        headers.append('apikey', REAL_SUPABASE_KEY);
        // Ensure X-Client-Info matches
        if (req.headers['x-client-info']) headers.append('X-Client-Info', req.headers['x-client-info']);
        if (req.headers['content-type']) headers.append('Content-Type', req.headers['content-type']);
        if (req.headers.accept) headers.append('Accept', req.headers.accept);

        const fetchOptions = {
            method: req.method,
            headers: headers,
        };

        if (['POST', 'PUT', 'PATCH'].includes(req.method)) {
            // Forward body if present
            if (req.headers['content-type'] && req.headers['content-type'].includes('application/json')) {
                fetchOptions.body = JSON.stringify(req.body);
            } else if (Object.keys(req.body || {}).length > 0) {
                 fetchOptions.body = JSON.stringify(req.body);
            }
        }

        const response = await fetch(targetUrl, fetchOptions);
        
        const contentType = response.headers.get('content-type');
        let data;
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
            return res.status(response.status).json(data);
        } else {
            data = await response.text();
            return res.status(response.status).send(data);
        }
    } catch (error) {
        console.error('Proxy Error:', error);
        return res.status(500).json({ error: 'Failed proxy request.', details: error.message });
    }
});

// Media Upload Endpoint (Video/Photo)
app.post('/api/upload', upload.single('media'), async (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No media file provided.' });
    }
    
    try {
        const fileExtension = req.file.originalname.split('.').pop();
        const fileName = `${uuidv4()}.${fileExtension}`;
        
        const command = new PutObjectCommand({
            Bucket: process.env.R2_BUCKET_NAME,
            Key: fileName,
            Body: req.file.buffer,
            ContentType: req.file.mimetype,
        });

        await s3.send(command);
        
        const bucketDomain = process.env.R2_CUSTOM_DOMAIN || `https://pub-${process.env.R2_ACCOUNT_ID}.r2.dev`;
        const fileUrl = `${bucketDomain}/${fileName}`;
        
        res.status(201).json({
            message: 'Media uploaded successfully to R2',
            media: {
                filename: fileName,
                url: fileUrl,
                mimetype: req.file.mimetype,
                uploadTime: new Date().toISOString()
            }
        });
    } catch (error) {
        console.error('R2 Upload Error:', error);
        res.status(500).json({ error: 'Failed to upload media to Cloudflare R2 server.' });
    }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is successfully running on port ${PORT}`);
    console.log(`Ready for Render deployment with Cloudflare R2!`);
});
