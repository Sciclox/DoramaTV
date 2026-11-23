import requests, re

url = "https://bysefujedu.com/e/cx38cbosofx1"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36',
    'Referer': 'https://bysefujedu.com/'
}
r = requests.get(url, headers=headers)
print("Status:", r.status_code)
print("Length:", len(r.text))
print("Has packed JS:", "eval(function(p,a,c,k,e,d)" in r.text)
print("Has m3u8:", ".m3u8" in r.text)
print("Has iframe:", "<iframe" in r.text)
if "eval(function(p,a,c,k,e,d)" in r.text:
    print("Found packer")
