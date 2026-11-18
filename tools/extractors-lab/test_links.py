import urllib.request
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

urls = [
    ("Primeload", "https://primeload.co/embed/NNTm4plCyvN6"),
    ("Dood", "https://do7go.com/e/8jmjw89ynqkq"),
    ("Byse/Filemoon", "https://bysefujedu.com/e/jvzq9gifhnq7"),
    ("OkRu", "https://ok.ru/videoembed/8705389890197")
]

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://doramasflix.in/'
}

for name, u in urls:
    print(f"\nTesting {name}: {u}")
    req = urllib.request.Request(u, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=8) as resp:
            content = resp.read().decode('utf-8', errors='ignore')
            status = resp.status
            print(f"  Status: {status}, length: {len(content)}")
            if "Not Found" in content or "not found" in content:
                print("  --> CONTAINS 'Not Found'!")
            if "video you are looking for is not found" in content:
                print("  --> EXACT MATCH: 'video you are looking for is not found'!!")
            # check for video streams or m3u8
            if ".m3u8" in content:
                print("  --> Contains .m3u8!")
            if "eval(function(p,a,c,k,e,d)" in content:
                print("  --> Contains PACKER code!")
    except urllib.error.HTTPError as e:
        print(f"  HTTP Error: {e.code}")
    except Exception as e:
        print(f"  Error: {e}")
