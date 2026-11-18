const https = require('https');
function getFollow(url) {
    https.get(url, {
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://doramasflix.in/'
        }
    }, res => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
            console.log('Redirect to:', res.headers.location);
            getFollow(res.headers.location);
            return;
        }
        let html = '';
        res.on('data', c => html += c);
        res.on('end', () => {
            console.log('FINAL STATUS:', res.statusCode);
            const passMatch = html.match(/\/pass_md5\/[^'"]+/);
            console.log('pass_md5 match:', passMatch ? passMatch[0] : 'None');
        });
    });
}
getFollow('https://do7go.com/e/b5ovrqpcsfws');
