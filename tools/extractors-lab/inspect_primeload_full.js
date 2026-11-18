const https = require('https');

https.get('https://primeload.co/embed/wgsyRcARSOU7', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://doramasflix.in/'
    }
}, res => {
    let html = '';
    res.on('data', d => html += d);
    res.on('end', () => console.log(html));
});
