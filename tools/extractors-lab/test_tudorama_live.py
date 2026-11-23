import requests, re

r = requests.get('https://tudorama.com/', headers={'User-Agent': 'Mozilla/5.0'})
links = re.findall(r'href=[\'"](https://tudorama\.com/[^\'"]+)[\'"]', r.text)
for l in list(set(links))[:25]:
    print(l)
