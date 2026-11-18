const https = require('https');

function testUrl(url, headers = {}) {
    return new Promise((resolve) => {
        const u = new URL(url);
        const req = https.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                'Referer': 'https://doramasflix.in/',
                ...headers
            }
        }, res => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, length: data.length, body: data.substring(0, 500) }));
        });
        req.on('error', e => resolve({ error: e.message }));
    });
}

async function run() {
    console.log('Testing Ep 4 Vidhide:');
    const vid = await testUrl('https://callistanise.com/embed/zfrbpcqwdlh1');
    console.log('Vidhide:', vid.status, vid.length);

    console.log('Testing Ep 4 VOE:');
    const voe = await testUrl('https://voe.sx/e/fvnpes64pjiy');
    console.log('VOE:', voe.status, voe.length);

    console.log('Testing Ep 4 Primeload:');
    const prime = await testUrl('https://primeload.co/embed/j2O4HbGVjFnG');
    console.log('Primeload:', prime.status, prime.length);
}
run();
