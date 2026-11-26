// Gaya aplikasi — disisipkan lewat JavaScript agar cukup 4 berkas.
(function () {
  const f = document.createElement('link');
  f.rel = 'stylesheet';
  f.href = 'https://fonts.googleapis.com/css2?family=Lexend:wght@400;600&family=Amiri:wght@700&display=swap';
  document.head.appendChild(f);
  const s = document.createElement('style');
  s.textContent = `
:root{--pine:#12372f;--sage:#eaf1ec;--paper:#fbfdfb;--brass:#b8872f;--ink:#1c2b27;--line:#cfdcd4;--bad:#a63d2f}
@media(prefers-color-scheme:dark){:root{--pine:#9fd1bf;--sage:#10201b;--paper:#162a24;--brass:#e0b45c;--ink:#e6f0eb;--line:#2d4a41;--bad:#ff8f7e}}
*{box-sizing:border-box}
body{margin:0;background:var(--sage);color:var(--ink);font:400 15px/1.55 Lexend,system-ui,sans-serif}
.top{display:flex;align-items:center;gap:16px;flex-wrap:wrap;padding:14px 20px;border-bottom:3px solid var(--brass);background:var(--paper)}
.top h1{margin:0;font:700 26px Amiri,serif;color:var(--pine)}
nav{display:flex;gap:6px;flex:1;flex-wrap:wrap}
nav button{background:none;border:0;padding:8px 12px;border-radius:6px;color:var(--ink);cursor:pointer;font:inherit}
nav button.on{background:var(--pine);color:var(--paper)}
main{max-width:980px;margin:20px auto;padding:0 16px}
.card{background:var(--paper);border:1px solid var(--line);border-radius:10px;padding:20px;margin-bottom:16px}
#login{max-width:400px;margin:40px auto}
h2,h3{margin:0 0 12px;color:var(--pine)}
label{display:block;margin:10px 0;font-weight:600;font-size:13px}
input,select,textarea{display:block;width:100%;margin-top:4px;padding:9px 10px;border:1px solid var(--line);border-radius:6px;background:var(--paper);color:var(--ink);font:inherit}
button{font:inherit;cursor:pointer}
button:focus-visible,input:focus-visible,select:focus-visible,textarea:focus-visible{outline:2px solid var(--brass);outline-offset:2px}
.primary{background:var(--pine);color:var(--paper);border:0;border-radius:6px;padding:10px 18px}
.ghost{background:none;border:1px solid var(--line);color:var(--ink);border-radius:6px;padding:6px 12px}
.danger{color:var(--bad)}
.tabs{display:flex;gap:6px}.tab{flex:1;padding:8px;border:1px solid var(--line);background:none;color:var(--ink);border-radius:6px}
.tab.on{border-color:var(--brass);box-shadow:inset 0 -3px var(--brass)}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px}
.stat{border-left:4px solid var(--brass);padding:4px 12px}.stat b{display:block;font-size:22px;color:var(--pine)}
table{width:100%;border-collapse:collapse;font-size:14px}
th,td{text-align:left;padding:8px 6px;border-bottom:1px solid var(--line);vertical-align:top}
.scroll{overflow-x:auto}
.msg{color:var(--bad);min-height:1.2em;margin:8px 0 0}
.ar{font:700 18px Amiri,serif}
.empty{color:var(--ink);opacity:.7}
.bar{height:8px;background:var(--line);border-radius:4px;overflow:hidden}.bar i{display:block;height:100%;background:var(--brass)}
@media(max-width:560px){.top h1{font-size:22px}}
`;
  document.head.appendChild(s);
})();
