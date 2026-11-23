import requests, re
url = 'https://do7go.com/e/b5ovrqpcsfws'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Referer': 'https://do7go.com/'
}
r = requests.get(url, headers=headers)
print('status:', r.status_code)
m = re.search(r"/pass_md5/[^'\"]+", r.text)
if m:
    pass_url = "https://do7go.com" + m.group(0)
    print('pass url:', pass_url)
    r2 = requests.get(pass_url, headers=headers)
    print('pass_md5 resp:', r2.status_code, r2.text[:100])
else:
    print('no pass_md5')
