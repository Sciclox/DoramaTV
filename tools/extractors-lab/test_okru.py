import urllib.request
import ssl
import json
import re
import html as html_lib

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://doramasflix.in/'
}
url = 'https://ok.ru/videoembed/8705389890197'
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, context=ctx) as r:
    page = r.read().decode('utf-8', errors='ignore')

m = re.search(r'data-options="([^"]+)"', page)
if m:
    opts = json.loads(html_lib.unescape(m.group(1)))
    flashvars = opts.get('flashvars', {})
    meta = flashvars.get('metadata')
    if isinstance(meta, str):
        meta = json.loads(meta)
    print("Videos count:", len(meta.get('videos', [])))
    for v in meta.get('videos', []):
        print(f"  {v.get('name')}: {v.get('url')[:80]}...")
    print("hlsManifestUrl:", meta.get('hlsManifestUrl'))
