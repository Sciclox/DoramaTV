const fs = require('fs');
const https = require('https');

https.get('https://doramasflix.in/episodios/kill-me-love-me-1x8', {
    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        const idx = html.indexOf('<script id="__NEXT_DATA__"');
        if (idx !== -1) {
            const start = html.indexOf('>', idx) + 1;
            const end = html.indexOf('</script>', start);
            const jsonStr = html.substring(start, end);
            const data = JSON.parse(jsonStr);
            console.log('PageProps keys:', Object.keys(data.props?.pageProps || {}));
            const ep = data.props?.pageProps?.episode;
            if (ep) {
                console.log('Episode ID:', ep._id, 'name:', ep.name);
                console.log('links_online:', JSON.stringify(ep.links_online, null, 2));
            }
        } else {
            console.log('No NEXT_DATA');
        }
    });
});
