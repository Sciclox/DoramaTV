const https = require('https');

https.get('https://primeload.co/embed/wgsyRcARSOU7', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://doramasflix.in/'
    }
}, res => {
    console.log('Primeload status:', res.statusCode);
    console.log('Primeload headers:', res.headers);
    let html = '';
    res.on('data', d => html += d);
    res.on('end', () => {
        console.log('Primeload HTML length:', html.length);
        console.log(html.substring(0, 500));
    });
}).on('error', e => console.error(e));
