const https = require('https');

function postGql(query, variables) {
    return new Promise((resolve, reject) => {
        const payload = JSON.stringify({ query, variables });
        const req = https.request('https://user-api.seriesapi.co/graphql', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Origin': 'https://doramasflix.in',
                'Referer': 'https://doramasflix.in/'
            }
        }, res => {
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => resolve(JSON.parse(data)));
        });
        req.on('error', reject);
        req.write(payload);
        req.end();
    });
}

function decodeB64(str) {
    return Buffer.from(str.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
}

function decodeJwt(jwtUrl) {
    if (!jwtUrl) return null;
    let token = jwtUrl;
    if (jwtUrl.includes('/e/')) {
        token = jwtUrl.split('/e/')[1].split('&')[0];
    }
    const parts = token.split('.');
    if (parts.length >= 2) {
        try {
            const payload = JSON.parse(decodeB64(parts[1]));
            if (payload.link) {
                let dec = decodeB64(payload.link).trim();
                if (!dec.startsWith('http')) {
                    try {
                        const dec2 = decodeB64(dec).trim();
                        if (dec2.startsWith('http')) dec = dec2;
                    } catch(e){}
                }
                return dec;
            }
        } catch(e) {}
    }
    return jwtUrl;
}

async function run() {
    const epsRes = await postGql('query Eps($id: ID!) { paginationEpisode(page: 1, limit: 35, filter: { serie_id: $id, season_number: 1 }, sort: NUMBER_ASC) { items { _id episode_number slug } } }', { id: '670b74efa35a6c33cfba1425' });
    const items = epsRes.data?.paginationEpisode?.items || [];
    items.sort((a,b) => a.episode_number - b.episode_number);

    for (const ep of items.slice(0, 15)) {
        await new Promise(r => setTimeout(r, 200));
        const res = await postGql('query Links($id: ID!) { getEpisodeLinks(id: $id) { links_online { server lang link is_recommended } } }', { id: ep._id });
        const links = res.data?.getEpisodeLinks?.links_online || [];
        const decoded = links.map(l => ({ server: l.server, is_recommended: l.is_recommended, url: decodeJwt(l.link) }));
        console.log(`Cap ${ep.episode_number} (${ep.slug}):`, decoded);
    }
}
run();
