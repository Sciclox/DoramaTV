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
function decodeB64(str) { return Buffer.from(str.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8'); }
function decodeJwt(jwtUrl) {
    if (!jwtUrl) return null;
    let token = jwtUrl.includes('/e/') ? jwtUrl.split('/e/')[1].split('&')[0] : jwtUrl;
    const parts = token.split('.');
    if (parts.length >= 2) {
        try {
            const p = JSON.parse(decodeB64(parts[1]));
            if (p.link) {
                let d = decodeB64(p.link).trim();
                try { let d2 = decodeB64(d).trim(); if (d2.startsWith('http')) d = d2; } catch(e){}
                return d;
            }
        } catch(e){}
    }
    return jwtUrl;
}

(async () => {
    const epsRes = await postGql('query Eps($id: ID!) { paginationEpisode(page: 1, limit: 35, filter: { serie_id: $id, season_number: 1 }, sort: NUMBER_ASC) { items { _id episode_number slug } } }', { id: '670b74efa35a6c33cfba1425' });
    const ep9 = epsRes.data.paginationEpisode.items.find(e => e.episode_number === 9);
    
    const web = await postGql('query Web($id: ID!) { getEpisodeLinks(id: $id) { links_online { server lang link is_recommended } } }', { id: ep9._id });
    console.log('--- WEB LINKS ---');
    (web.data?.getEpisodeLinks?.links_online || []).forEach(l => console.log('server:', l.server, 'lang:', l.lang, 'url:', decodeJwt(l.link)));

    const app = await postGql('query App($id: ID!) { getEpisodeLinks(id: $id, app: "com.asiapp.doramasgo") { links_online { server lang link is_recommended } } }', { id: ep9._id });
    console.log('--- APP LINKS (doramasgo) ---');
    (app.data?.getEpisodeLinks?.links_online || []).forEach(l => console.log('server:', l.server, 'lang:', l.lang, 'url:', decodeJwt(l.link)));
})();
