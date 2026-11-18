const fs = require('fs');
if (fs.existsSync('dump.xml')) {
    const xml = fs.readFileSync('dump.xml', 'utf8');
    const texts = [...xml.matchAll(/text="([^"]+)"/g)].map(m => m[1]);
    console.log('UI Texts on screen:', texts);
} else {
    console.log('No dump.xml');
}
