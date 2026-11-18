const fs = require('fs');
const html = fs.readFileSync('vidhide_sample.html', 'utf8');

const sRegex = /<script\b[^>]*>([\s\S]*?)<\/script>/gi;
let m;
let idx = 0;
while ((m = sRegex.exec(html)) !== null) {
    idx++;
    const s = m[1].trim();
    if (s.includes('eval(function(p,a,c,k,e,d)') || s.includes('sources') || s.includes('m3u8')) {
        console.log(`Script #${idx} (${s.length} chars):`, s.slice(0, 300));
        if (s.includes('eval(function(p,a,c,k,e,d)')) {
            // Let's unpack it!
            const match = s.match(/eval\(function\(p,a,c,k,e,d\)\{[\s\S]*?\}\s*\(\s*'(.*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.*?)'\.split\('\|'\)/);
            if (match) {
                let p = match[1].replace(/\\'/g, "'").replace(/\\\\/g, "\\");
                const a = parseInt(match[2], 10);
                let c = parseInt(match[3], 10);
                const k = match[4].split("|");
                function encodeBase(c, a) {
                    return c < a ? (c > 35 ? String.fromCharCode(c + 29) : c.toString(36)) : encodeBase(Math.floor(c / a), a) + encodeBase(c % a, a);
                }
                while (c > 0) {
                    c--;
                    const token = (c < k.length && k[c]) ? k[c] : encodeBase(c, a);
                    const baseStr = encodeBase(c, a);
                    p = p.replace(new RegExp(`\\b${baseStr}\\b`, 'g'), token);
                }
                console.log("UNPACKED VIDHIDE JS:");
                console.log(p.slice(0, 1000));
                const m3u8 = p.match(/https?:\/\/[^\s"']+\.m3u8[^\s"']*/);
                if (m3u8) {
                    console.log("FOUND VIDHIDE M3U8:", m3u8[0]);
                }
            }
        }
    }
}
