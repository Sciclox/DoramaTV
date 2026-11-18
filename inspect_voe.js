const fs = require('fs');
const html = fs.readFileSync('ariana_sample.html', 'utf8');

const regex = /<script\b[^>]*type=["']application\/json["'][^>]*>([\s\S]*?)<\/script>/gi;
let m;
let count = 0;
while ((m = regex.exec(html)) !== null) {
    count++;
    console.log(`--- JSON #${count} ---`);
    console.log(m[1].trim().slice(0, 500));
}
if (count === 0) {
    console.log("No application/json scripts found!");
    // check all scripts
    const sRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
    let sMatch;
    let sIdx = 0;
    while ((sMatch = sRegex.exec(html)) !== null) {
        sIdx++;
        const content = sMatch[1].trim();
        if (content.length > 50 && !content.includes('gtag') && !content.includes('bootstrap')) {
            console.log(`--- Script #${sIdx} (${content.length} chars) ---`);
            console.log(content.slice(0, 300));
        }
    }
}
