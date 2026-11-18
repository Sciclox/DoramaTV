async function test() {
    const url = 'https://bysefujedu.com/e/jvzq9gifhnq7';
    const resp = await fetch(url, {
        headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
            'Referer': 'https://doramasflix.in/'
        }
    });
    const text = await resp.text();
    console.log('Status:', resp.status);
    console.log('Text length:', text.length);

    // Check for packed JS
    const hasEval = text.includes('eval(function(p,a,c,k,e,d)');
    console.log('Has eval p,a,c,k:', hasEval);

    // Look for iframe or script sources
    const scripts = [...text.matchAll(/<script[^>]*src=["']([^"']+)["']/gi)].map(m => m[1]);
    console.log('Scripts:', scripts);

    const iframes = [...text.matchAll(/<iframe[^>]*src=["']([^"']+)["']/gi)].map(m => m[1]);
    console.log('Iframes:', iframes);
}
test();
