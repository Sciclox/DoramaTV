const https = require('https');

https.get('https://primeload.co/player/foAJIhOBjecq?ref=https%3A%2F%2Fdoramasflix.in%2F', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
        'Referer': 'https://primeload.co/embed/foAJIhOBjecq'
    }
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        const rootMatch = html.match(/<div id="root"([^>]+)>/i);
        if (rootMatch) {
            console.log('root div attributes:\n', rootMatch[1]);
        }
    });
});
