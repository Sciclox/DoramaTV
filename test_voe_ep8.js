const https = require('https');
https.get('https://voe.sx/e/iihm9qmtkrb1', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        console.log(html);
    });
});
