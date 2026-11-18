const fs = require('fs');
const html = fs.readFileSync('ep9.html', 'utf8');

console.log('--- Search for Dood / Primeload / Filemoon contexts ---');
const idxDood = html.indexOf('Dood');
if (idxDood !== -1) {
    console.log('Around Dood:');
    console.log(html.substring(Math.max(0, idxDood - 200), idxDood + 300));
}

const idxPrime = html.indexOf('Primeload');
if (idxPrime !== -1) {
    console.log('\nAround Primeload:');
    console.log(html.substring(Math.max(0, idxPrime - 200), idxPrime + 300));
}
