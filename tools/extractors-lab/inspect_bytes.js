const https = require("https");

const hdUrl = "https://vd334.okcdn.ru/?expires=1795029838560&srcIp=177.53.155.177&pr=10&srcAg=CHROME&rjz=43&ms=185.180.203.81&type=3&sig=H_RPCpygOMU&ct=0&urls=185.226.55.99&clientType=0&zs=43&id=7324293597845&rjr=8";

const req = https.get(hdUrl, { headers: { "User-Agent": "Mozilla/5.0" } }, (res) => {
  res.once("data", (chunk) => {
    console.log("Chunk length:", chunk.length);
    console.log("Hex:", chunk.slice(0, 64).toString("hex"));
    console.log("Ascii:", chunk.slice(0, 64).toString("ascii").replace(/[^\x20-\x7E]/g, '.'));
    req.destroy();
  });
});
