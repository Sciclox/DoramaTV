const fs = require('fs');
const html = fs.readFileSync('ep9_full.html', 'utf8');
const idx = html.toLowerCase().indexOf('listservers');
console.log('Index:', idx);
if (idx !== -1) {
    console.log(html.substring(idx - 50, idx + 800));
}
