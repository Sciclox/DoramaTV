const fs = require('fs');
const html = fs.readFileSync('ep9.html', 'utf8');

// Find all occurrences of Dood or server names
const reg = /"name":"([^"]+)"/g;
let m;
const names = new Set();
while ((m = reg.exec(html)) !== null) {
    if (m[1].length < 30) names.add(m[1]);
}
console.log('Short names in HTML:', [...names].filter(n => /dood|primeload|filemoon|streamwish|okru|voe/i.test(n)));
