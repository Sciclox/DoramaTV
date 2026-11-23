import requests, re, time

url = 'https://do7go.com/e/b5ovrqpcsfws'
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Referer': 'https://do7go.com/'
}
r = requests.get(url, headers=headers)
m = re.search(r"/pass_md5/([^\'\"]+)", r.text)
token_url = "https://do7go.com/pass_md5/" + m.group(1)
r2 = requests.get(token_url, headers=headers)
stream_base = r2.text.strip()
stream_url = stream_base + "1234567890?token=" + m.group(1).split('/')[-1] + f"&expiry={int(time.time()*1000)}"
print("Stream URL:", stream_url)

# Test without referer
resp1 = requests.get(stream_url, headers={'User-Agent': 'Mozilla/5.0'}, stream=True)
print("No referer status:", resp1.status_code)

# Test with Referer https://do7go.com/
resp2 = requests.get(stream_url, headers={'User-Agent': 'Mozilla/5.0', 'Referer': 'https://do7go.com/'}, stream=True)
print("With do7go referer status:", resp2.status_code)
