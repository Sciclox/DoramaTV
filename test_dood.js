const https = require('https');
https.get('https://do7go.com/e/b5ovrqpcsfws', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://doramasflix.in/'
    }
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        console.log('STATUS:', res.statusCode);
        const passMatch = html.match(/\/pass_md5\/[^'"]+/);
        console.log('pass_md5 match:', passMatch ? passMatch[0] : 'None');
        const tokenMatch = html.match(/makePlay[^)]*\)/);
        console.log('makePlay match:', tokenMatch ? tokenMatch[0] : 'None');
    });
});
