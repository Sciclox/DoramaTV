const fs = require('fs');
const html = fs.readFileSync('streamtape_sample.html', 'utf8');
const scripts = [...html.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/gi)];
scripts.forEach((s, i) => {
    if (s[1].includes('token=') || s[1].includes('videolink') || s[1].includes('document.getElementById')) {
        console.log(`Script ${i}:`, s[1].trim());
    }
});
