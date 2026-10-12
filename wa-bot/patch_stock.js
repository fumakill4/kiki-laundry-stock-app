if (!process.env.FIREBASE_WEB_API_KEY || !process.env.FIREBASE_ID_TOKEN) throw new Error('Provide authenticated maintenance credentials outside the source code');
const https = require('https');

function patchItem(itemId, totalOut) {
    const data = JSON.stringify({
        fields: {
            totalOut: { integerValue: totalOut.toString() }
        }
    });

    const options = {
        hostname: 'firestore.googleapis.com',
        port: 443,
        path: `/v1/projects/laundry-stock-app/databases/(default)/documents/items/${itemId}?updateMask.fieldPaths=totalOut&key=${process.env.FIREBASE_WEB_API_KEY}`,
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${process.env.FIREBASE_ID_TOKEN}`,
            'Content-Length': data.length
        }
    };

    const req = https.request(options, res => {
        console.log(`Status for ${itemId}: ${res.statusCode}`);
        res.on('data', d => process.stdout.write(d));
    });

    req.on('error', error => console.error(error));
    req.write(data);
    req.end();
}

// Sunlight: startingStock is 20. Target remaining is 12. So totalOut = 20 - 12 = 8.
patchItem('item_sunlight', 8);

// Super Pel: startingStock is 29. Target remaining is 28. So totalOut = 29 - 28 = 1.
patchItem('item_super_pel', 1);

