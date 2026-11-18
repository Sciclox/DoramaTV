const https = require('https');

function testUrl(url, headers = {}) {
    return new Promise((resolve) => {
        const req = https.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                'Referer': 'https://doramasflix.in/',
                ...headers
            }
        }, res => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => resolve({ status: res.statusCode, length: data.length }));
        });
        req.on('error', e => resolve({ error: e.message }));
    });
}

async function run() {
    console.log('Testing Ep 3 Vidhide:', await testUrl('https://callistanise.com/embed/w3glslehy469'));
    console.log('Testing Ep 3 Primeload:', await testUrl('https://primeload.co/embed/NNTm4plCyvN6'));
    console.log('Testing Ep 3 Dood:', await testUrl('https://do7go.com/e/8jmjw89ynqkq'));
}
run();
