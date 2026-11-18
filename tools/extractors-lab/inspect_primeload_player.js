const https = require('https');

https.get('https://primeload.co/player/wgsyRcARSOU7?ref=https%3A%2F%2Fdoramasflix.in%2F', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://primeload.co/embed/wgsyRcARSOU7'
    }
}, res => {
    let html = '';
    res.on('data', d => html += d);
    res.on('end', () => {
        console.log('Player HTML length:', html.length);
        console.log(html.substring(0, 1000));
        const m = html.match(/file:\s*["']([^"']+)["']/i) || html.match(/https?:\/\/[^\s"']+\.m3u8[^\s"']*/);
        console.log('Found stream?:', m ? m[0] : 'NONE');
    });
});
