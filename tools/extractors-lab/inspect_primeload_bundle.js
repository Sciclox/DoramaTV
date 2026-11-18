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
        console.log('JS length:', data.length);
        // Find play, autoplay, challenge, socket, etc.
        const matches = data.match(/(autoplay|challenge|nonce|socket|vjs|player)/gi);
        console.log('Keyword counts:', matches?.reduce((acc, k) => { acc[k] = (acc[k]||0)+1; return acc; }, {}));
        // Look for videojs initialization
        const vjsIdx = data.indexOf('videojs(');
        if (vjsIdx !== -1) {
            console.log('videojs snippet:', data.substring(vjsIdx - 100, vjsIdx + 200));
        }
        const apiIdx = data.indexOf('/api/v1');
        if (apiIdx !== -1) {
            console.log('api snippet:', data.substring(apiIdx - 100, apiIdx + 200));
        }
    });
});
