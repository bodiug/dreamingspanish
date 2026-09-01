const wsUrl = process.argv[2];
const expression = process.argv.slice(3).join(" ");

if (!wsUrl || !expression) {
  console.error("Usage: node tools/eval-webview.mjs ws://... '(() => ...)()'");
  process.exit(1);
}

const ws = new WebSocket(wsUrl);
const timeout = setTimeout(() => {
  console.error("Timed out waiting for DevTools response");
  process.exit(2);
}, 5000);

ws.addEventListener("open", () => {
  ws.send(JSON.stringify({
    id: 1,
    method: "Runtime.evaluate",
    params: {
      expression,
      returnByValue: true,
      awaitPromise: true,
    },
  }));
});

ws.addEventListener("message", (event) => {
  clearTimeout(timeout);
  const data = JSON.parse(event.data);
  console.log(JSON.stringify(data.result?.result?.value ?? data, null, 2));
  ws.close();
});
