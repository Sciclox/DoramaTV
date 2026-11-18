const https = require('https');
const fs = require('fs');

https.get('https://doramasflix.in/episodios/kill-me-love-me-1x9', { 
    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } 
}, res => {
    let html = '';
    res.on('data', c => html += c);
    res.on('end', () => {
        fs.writeFileSync('ep9_full.html', html);
        console.log('Saved ep9_full.html, length:', html.length);
        const matches = html.match(/self\.__next_f\.push\(\[1,\"([\s\S]*?)\"\]\)/g);
        console.log('RSC chunks:', matches ? matches.length : 0);
        
        // Find any server or embed strings
        const words = ['dood', 'do7go', 'playmogo', 'primeload', 'byse', 'streamtape', 'filemoon', 'vidhide', 'ok.ru'];
        words.forEach(w => {
            const count = (html.match(new RegExp(w, 'gi')) || []).length;
            console.log(`Word ${w}: ${count} matches`);
        });
    });
});
