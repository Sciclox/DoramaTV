const https = require('https');

function getHtml(url) {
    return new Promise((resolve) => {
        https.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
                'Referer': 'https://doramasflix.in/'
            }
        }, res => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                console.log('Redirecting to:', res.headers.location);
                resolve(getHtml(res.headers.location));
                return;
            }
            let html = '';
            res.on('data', c => html += c);
            res.on('end', () => resolve({ html, url }));
        });
    });
}

(async () => {
    const { html, url } = await getHtml('https://do7go.com/e/b5ovrqpcsfws');
    console.log('Final URL:', url);
    const pass = html.match(/\/pass_md5\/[^'"]+/);
    console.log('pass_md5 match:', pass ? pass[0] : null);
    
    // Find script with pass_md5
    const scripts = html.split('<script');
    for (const s of scripts) {
        if (s.includes('pass_md5')) {
            console.log('--- SCRIPT WITH pass_md5 ---');
            console.log(s.substring(0, 800));
        }
    }
})();
