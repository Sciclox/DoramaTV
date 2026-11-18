async function test() {
    try {
        const res = await fetch('https://callistanise.com/embed/pfctbbd1opew', {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
                'Referer': 'https://tudorama.com/'
            }
        });
        const html = await res.text();
        console.log('Vidhide status:', res.status, 'HTML len:', html.length);
        const m = html.match(/sources:\s*\[([\s\S]*?)\]/);
        console.log('Sources:', m ? m[1] : 'none');
        const m3u8 = html.match(/https?:\/\/[^"'\s]+\.m3u8[^"'\s]*/);
        console.log('M3U8 match:', m3u8 ? m3u8[0] : 'none');
        const fileMatch = html.match(/file:\s*["']([^"']+)["']/);
        console.log('file match:', fileMatch ? fileMatch[1] : 'none');
    } catch (e) {
        console.error(e);
    }
}
test();
