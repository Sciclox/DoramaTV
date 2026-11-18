const https = require('https');
function getFollow(url, cookie) {
    const u = new URL(url);
    const options = {
        hostname: u.hostname,
        path: u.pathname + u.search,
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
            'Referer': 'https://doramasflix.in/'
        }
    };
    if (cookie) options.headers['Cookie'] = cookie;

    https.get(options, res => {
        let setCookie = res.headers['set-cookie'];
        let newCookie = cookie;
        if (setCookie) {
            newCookie = setCookie.map(c => c.split(';')[0]).join('; ');
        }
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
            console.log('Redirect to:', res.headers.location, 'cookie:', newCookie);
            getFollow(res.headers.location, newCookie);
            return;
        }
        let html = '';
        res.on('data', c => html += c);
        res.on('end', () => {
            console.log('STATUS:', res.statusCode, 'len:', html.length);
            const passMatch = html.match(/\/pass_md5\/[^'"]+/);
            console.log('pass_md5 match:', passMatch ? passMatch[0] : 'None');
            if (passMatch) {
                // Fetch pass_md5
                const passPath = passMatch[0];
                const passOpt = {
                    hostname: u.hostname,
                    path: passPath,
                    headers: {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                        'Referer': url
                    }
                };
                if (newCookie) passOpt.headers['Cookie'] = newCookie;
                https.get(passOpt, passRes => {
                    let body = '';
                    passRes.on('data', c => body += c);
                    passRes.on('end', () => {
                        console.log('PASS_MD5 BODY:', body);
                    });
                });
            }
        });
    });
}
getFollow('https://do7go.com/e/b5ovrqpcsfws');
