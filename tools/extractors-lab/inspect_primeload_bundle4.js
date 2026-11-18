const https = require('https');
https.get('https://primeload.co/player-dist/assets/index-d_Oc1EoW.js', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Referer': 'https://primeload.co/player/foAJIhOBjecq'
    }
}, res => {
    let data = '';
    res.on('data', c => data += c);
    res.on('end', () => {
        const lnIdx = data.lastIndexOf('function Ln(');
        if (lnIdx !== -1) {
            console.log(data.substring(lnIdx + 1900, lnIdx + 4500));
        }
    });
});
