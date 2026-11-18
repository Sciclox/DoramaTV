const https = require('https');
https.get('https://doramasflix.in/episodios/kill-me-love-me-1x9', { 
    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } 
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        const idx = html.indexOf('<script id="__NEXT_DATA__"');
        if (idx !== -1) {
            const end = html.indexOf('</script>', idx);
            const jsonStr = html.substring(html.indexOf('>', idx) + 1, end);
            const parsed = JSON.parse(jsonStr);
            const ep = parsed.props?.pageProps?.episode;
            console.log('Episode ID:', ep?._id);
            console.log('links_online count:', ep?.links_online?.length);
            console.log('links_online:', JSON.stringify(ep?.links_online, null, 2));
        } else {
            console.log('No NEXT_DATA');
        }
    });
});
