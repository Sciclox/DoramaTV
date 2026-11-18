const https = require('https');
https.get('https://doramasflix.in/episodios/kill-me-love-me-1x9', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        console.log('STATUS:', res.statusCode);
        const idx = html.indexOf('__NEXT_DATA__');
        console.log('INDEX:', idx);
        if (idx !== -1) {
            const start = html.indexOf('>', idx) + 1;
            const end = html.indexOf('</script>', start);
            const data = JSON.parse(html.substring(start, end));
            console.log('keys in pageProps:', Object.keys(data.props?.pageProps || {}));
            const ep = data.props?.pageProps?.episode;
            console.log('episode keys:', ep ? Object.keys(ep) : null);
            console.log('episode.players:', ep?.players);
            console.log('episode.servers:', ep?.servers);
            console.log('episode.links_online:', JSON.stringify(ep?.links_online, null, 2));
        } else {
            console.log('HTML length:', html.length, 'preview:', html.substring(0, 500));
        }
    });
});
