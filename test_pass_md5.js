const https = require('https');

function get(url, ref) {
    return new Promise((resolve) => {
        https.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
                'Referer': ref || 'https://playmogo.com/'
            }
        }, res => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => resolve({ data, statusCode: res.statusCode }));
        });
    });
}

(async () => {
    const embedRes = await get('https://playmogo.com/e/b5ovrqpcsfws', 'https://doramasflix.in/');
    const html = embedRes.data;
    const m = html.match(/\/pass_md5\/[^'"]+/);
    console.log('pass_md5:', m ? m[0] : null);
    if (m) {
        const passUrl = 'https://playmogo.com' + m[0];
        console.log('Fetching passUrl:', passUrl);
        const passRes = await get(passUrl, 'https://playmogo.com/e/b5ovrqpcsfws');
        console.log('passRes status:', passRes.statusCode, 'body:', passRes.data);
    }
})();
