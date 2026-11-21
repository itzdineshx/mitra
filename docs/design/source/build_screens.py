#!/usr/bin/env python3
"""Mitra screens (docs/UI_SPEC.md). Builds screens.html + all screens in one PNG.
Run: pip install playwright && playwright install chromium && python build_screens.py
Output files are written next to this script. Copy the PNG to ../all-screens.png and crop the phones for ../screens/."""
import pathlib, asyncio

OUT = pathlib.Path(__file__).parent

ICONS = {
    "mic": '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/>',
    "check": '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
    "x": '<path d="M6 6l12 12M18 6L6 18"/>',
    "chev": '<path d="M9 6l6 6-6 6"/>',
    "back": '<path d="M15 5l-7 7 7 7"/>',
    "shield": '<path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6z"/>',
    "lock": '<rect x="5" y="11" width="14" height="9" rx="2.5"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    "home": '<path d="M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z"/>',
    "history": '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5M12 7v5l3 2"/>',
    "sliders": '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
    "cpu": '<rect x="7" y="7" width="10" height="10" rx="2.5"/><path d="M9 3v4M15 3v4M9 17v4M15 17v4M3 9h4M3 15h4M17 9h4M17 15h4"/>',
    "bell": '<path d="M6 16v-5a6 6 0 0 1 12 0v5l2 2H4z"/><path d="M10 20a2 2 0 0 0 4 0"/>',
    "layers": '<path d="M12 3l9 5-9 5-9-5z"/><path d="M3 13l9 5 9-5"/>',
    "battery": '<rect x="3" y="8" width="16" height="9" rx="2.5"/><path d="M21 11v3"/>',
    "access": '<circle cx="12" cy="5" r="2"/><path d="M5 9l7 1 7-1M12 10v5l-3 5M12 15l3 5"/>',
    "send": '<path d="M4 12l16-8-6 16-3-7z"/>',
    "globe": '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>',
    "eye": '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
    "speaker": '<path d="M4 10v4h4l5 4V6l-5 4z"/><path d="M16 9a4 4 0 0 1 0 6"/>',
    "lang": '<path d="M4 6h9M8 4v2M6 6c1 4 3 6 6 8M12 6c-1 4-4 7-8 9"/><path d="M13 20l4-9 4 9M14.5 17h5"/>',
    "zap": '<path d="M13 3L5 14h6l-1 7 8-11h-6z"/>',
    "download": '<path d="M12 4v11m0 0l-4-4m4 4l4-4M5 20h14"/>',
    "alert": '<path d="M12 3l10 17H2z"/><path d="M12 10v4M12 17v.5"/>',
    "wifi": '<path d="M2 9a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0"/><circle cx="12" cy="19" r=".6"/>',
}


def ic(name, size=22, color="currentColor", sw=1.9, fill="none"):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="{fill}" stroke="{color}" '
            f'stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round">{ICONS[name]}</svg>')


def orb(size, ripples=False):
    r = ""
    if ripples:
        r = (f'<div class="rip" style="inset:-{int(size*.28)}px"></div>'
             f'<div class="rip" style="inset:-{int(size*.58)}px;opacity:.55"></div>')
    return f'<div class="orbw" style="width:{size}px;height:{size}px">{r}<div class="orb"></div></div>'


def sbar(dark=False):
    c = "#fff" if dark else "#101828"
    return (f'<div class="sb" style="color:{c}"><span>9:41</span><i class="cam"></i>'
            f'<span class="sbr">{ic("wifi",15,c,2)}{ic("battery",18,c,2)}</span></div>')


def tabbar(active):
    items = [("home", "Home"), ("history", "History"), ("sliders", "Setup")]
    h = "".join(f'<div class="tab{" on" if k == active else ""}">{ic(k, 22)}<span>{l}</span></div>' for k, l in items)
    return f'<div class="tabbar">{h}</div>'


def tog(on):
    return f'<div class="tog{" on" if on else ""}"></div>'


def screen(inner, dark=False):
    return f'<div class="phone"><div class="scr">{inner}{sbar(dark)}</div></div>'


def cell(num, title, note, scr):
    return f'<div class="cell">{scr}<div class="cap"><b>{num}</b><span>{title}</span></div><p>{note}</p></div>'


# ------------------------------------------------------------ fake apps behind the assistant
def bg_home():
    cols = ["#FFD9D9", "#D9E6FF", "#D9F3E3", "#FFEBC2", "#E6DCFF", "#D5F2F0"]
    icons = "".join(f'<div class="ai"><i style="background:{cols[i % 6]}"></i><u></u></div>' for i in range(20))
    return f'<div class="wall"></div><div class="aig">{icons}</div>'


def bg_browser(mode):
    top = '<div class="url">' + ic("lock", 15, "#12B76A", 2.2) + '<span>[portal].gov.in</span></div>'
    if mode == "result":
        body = '''<div class="pg"><div class="hb" style="width:58%"></div><div class="hb s" style="width:36%"></div>
          <div class="fl">Application ID</div><div class="fi on">••••••••</div>
          <div class="fl">Result</div><div class="fi res">Status: Under review<em></em></div>
          <div class="gb">Check status<span class="tap"></span></div></div>'''
    else:
        body = '''<div class="pg"><div class="hb" style="width:58%"></div><div class="hb s" style="width:36%"></div>
          <div class="fl">Application ID</div><div class="fi on">••••••••</div>
          <div class="fl">Security code</div><div class="capimg"></div><div class="fi ring">Type the code</div></div>'''
    return f'<div class="web">{top}{body}</div>'


def bg_chat(sent=False):
    bub = '<div class="bb l" style="width:46%"></div><div class="bb r" style="width:36%"></div><div class="bb l" style="width:58%"></div>'
    if sent:
        bub += '<div class="bb r sent" style="width:78%">Scholarship status: Under review. — sent via Mitra</div>'
    inp = '' if sent else '<div class="typed">Scholarship status: Under review. — sent via Mitra</div>'
    return (f'<div class="web"><div class="chh"><i></i><b>Priya</b></div><div class="chb">{bub}</div>'
            f'<div class="chi">{inp or "<div class=ph>Message</div>"}<span></span></div></div>')


# ------------------------------------------------------------ A: over any app
def a1():
    inner = bg_home() + f'''<div class="fade"></div><div class="glow"></div>
      <div class="vtxt">
        <div class="lbl"><i class="pulse"></i>Listening</div>
        <div class="big">Check my scholarship status and send it to <span class="g">Priya on What</span><span class="caret"></span></div>
      </div>
      <div class="orbpos">{orb(64, True)}</div>
      <div class="micro">{ic("lock", 13, "#98A2B3", 2)} On this phone</div>'''
    return cell("1", "Listening", "Say “Hey Mitra” on any screen. Your words appear as you speak.", screen(inner))


def a2():
    inner = bg_browser("result") + f'''<div class="capsule">
        {orb(40)}
        <div class="ct"><b>Reading the result</b><small>Step 2 of 5</small><div class="pr"><i style="width:40%"></i></div></div>
        <div class="stopb"><i></i></div></div>'''
    return cell("2", "Working", "The real app stays on screen. Mitra shows what it is doing and where it taps.", screen(inner))


def a3():
    inner = bg_chat() + '<div class="dim"></div>' + f'''<div class="card">
        <div class="ci" style="background:#EAF0FF;color:#2F5BFF">{ic("send", 22)}</div>
        <h3>Send to Priya?</h3><div class="sub">WhatsApp</div>
        <div class="msg">Scholarship status: Under review. — sent via Mitra</div>
        <div class="say">{ic("mic", 15, "#667085", 2)} Say “yes” or “no”</div>
        <div class="btns"><div class="b2">Don’t send</div><div class="b1">Send</div></div></div>'''
    return cell("3", "Asks before sending", "Sending, submitting and deleting always need a yes, by voice or tap.", screen(inner))


def a4():
    inner = bg_browser("captcha") + '<div class="dim" style="background:rgba(16,24,40,.28)"></div>' + f'''<div class="card">
        <div class="ci" style="background:#FEF0C7;color:#B54708">{ic("lock", 22)}</div>
        <h3>Your turn</h3>
        <p class="pt">Type the security code yourself. Mitra never types codes or passwords.</p>
        <div class="btns" style="margin-top:20px"><div class="b1" style="flex:1">I’m done</div></div>
        <div class="txt red">Stop</div></div>'''
    return cell("4", "Your turn", "Passwords, OTPs, CAPTCHAs and payments are always handed to you.", screen(inner))


def a5():
    inner = bg_chat(sent=True) + f'''<div class="fade"></div><div class="glow" style="opacity:.6"></div>
      <div class="vtxt">
        <div class="okc">{ic("check", 22, "#fff", 2.6)}</div>
        <div class="big" style="margin-top:14px">Done. I sent it to Priya.</div>
        <div class="mut15">4 steps · saved in History</div>
      </div>
      <div class="orbpos">{orb(52)}</div>'''
    return cell("5", "Done", "A short spoken and written result. The task is saved to History.", screen(inner))


# ------------------------------------------------------------ B: the app
def b1():
    def row(t, m, kind):
        col = {"g": ("#E7F8EF", "#067647", "check"), "a": ("#FEF0C7", "#B54708", "clock")}[kind]
        return (f'<div class="lr"><div class="si" style="background:{col[0]};color:{col[1]}">{ic(col[2], 18, col[1], 2.4)}</div>'
                f'<div><b>{t}</b><small>{m}</small></div></div>')
    inner = f'''<div class="page bgp">
      <div class="gr">Good evening</div><h1>How can I help?</h1>
      <div class="hero">{orb(116, True)}</div>
      <div class="hsay">Say “Hey Mitra”</div><div class="hsub">or tap the orb to speak</div>
      <div class="sh"><b>Recent</b><span>See all</span></div>
      <div class="gcard">{row("Scholarship status sent to Priya", "Done · 10:42", "g")}{row("Electricity bill ready to pay", "Waiting for you", "a")}{row("Read my unread mails", "Done · Yesterday", "g")}</div>
    </div>{tabbar("home")}'''
    return cell("6", "Home", "Quiet by default. The orb starts a task; recent tasks sit below.", screen(inner))


def b2():
    inner = f'''<div class="page bgp">
      <h1 style="margin-top:14px">History</h1>
      <div class="seg"><span class="on">All</span><span>Done</span><span>Blocked</span></div>
      <div class="gl">Today</div>
      <div class="gcard pad">
        <div class="lr" style="padding:0 0 12px"><div class="si" style="background:#E7F8EF;color:#067647">{ic("check", 18, "#067647", 2.4)}</div>
          <div><b>Scholarship status to Priya</b><small>10:42 · 4 steps</small></div></div>
        <div class="tl"><div><i class="dg"></i><span>Opened [portal].gov.in</span></div>
          <div><i class="dg"></i><span>Read the result</span></div>
          <div><i class="dg"></i><span>Opened WhatsApp, found Priya</span></div>
          <div><i class="da"></i><span>Asked you · you said “yes”</span></div></div></div>
      <div class="gcard" style="margin-top:10px"><div class="lr"><div class="si" style="background:#FEF0C7;color:#B54708">{ic("clock", 18, "#B54708", 2.4)}</div>
        <div><b>Pay my electricity bill</b><small>Waiting · you enter the UPI PIN</small></div></div></div>
      <div class="gl">Yesterday</div>
      <div class="gcard"><div class="lr"><div class="si" style="background:#FEE4E2;color:#B42318">{ic("shield", 18, "#B42318", 2.2)}</div>
        <div><b>A message tried to give Mitra orders</b><small>Blocked · rule R7</small></div></div></div>
    </div>{tabbar("history")}'''
    return cell("7", "History", "Every task with what you said and what Mitra did, including blocked attacks.", screen(inner))


def b3():
    def row(icn, t, right="", tint="#EAF0FF", col="#2F5BFF", toggle=None):
        r = tog(toggle) if toggle is not None else (f'<span class="rv">{right}</span>{ic("chev", 18, "#C3C8D1", 2.2)}')
        return (f'<div class="sr"><div class="st" style="background:{tint};color:{col}">{ic(icn, 19)}</div><b>{t}</b>{r}</div>')
    inner = f'''<div class="page bgp">
      <h1 style="margin-top:14px">Setup</h1>
      <div class="gcard hero2"><div class="ring"><b>4</b><small>of 5</small></div>
        <div><b>Almost ready</b><small>One permission is left</small></div></div>
      <div class="gcard gap">
        {row("lock", "Permissions", '<em class="dot">1</em>', "#FEF0C7", "#B54708")}
        {row("cpu", "On-device AI", "Ready")}</div>
      <div class="gcard gap">
        {row("mic", "“Hey Mitra”", toggle=True)}
        {row("lang", "Language", "English")}
        {row("speaker", "Speak replies", toggle=True)}</div>
      <div class="gcard gap">
        {row("shield", "Safety rules", "", "#E7F8EF", "#067647")}
        {row("eye", "Privacy", "", "#F2F4F7", "#667085")}</div>
    </div>{tabbar("sliders")}'''
    return cell("8", "Setup", "One place for everything about the phone. Only what is needed, nothing else.", screen(inner))


def b4():
    def prow(icn, t, s, ok, extra=""):
        right = f'<div class="okd">{ic("check", 16, "#fff", 2.8)}</div>' if ok else '<div class="turn">Turn on</div>'
        return (f'<div class="pr2"><div class="prh"><div class="st">{ic(icn, 19)}</div><div class="pm"><b>{t}</b><small>{s}</small></div>{right}</div>{extra}</div>')
    steps = f'''<div class="how"><div><i>1</i><span>App info › ⋮ › <b>Allow restricted settings</b></span></div>
      <div><i>2</i><span>Accessibility › <b>Mitra</b> › On</span></div></div>'''
    inner = f'''<div class="page bgp">
      <div class="bk">{ic("back", 22, "#2F5BFF", 2.4)}<span>Setup</span></div>
      <h1 style="margin-top:8px">Permissions</h1>
      <p class="lead">Mitra asks only for what it needs.</p>
      <div class="gcard">
        {prow("mic", "Microphone", "To hear you", True)}
        {prow("bell", "Notifications", "Shows when Mitra listens", True)}
        {prow("access", "Control apps", "Tap and type for you", False, steps)}
        {prow("layers", "Show over apps", "For the panel and Stop", True)}
        {prow("battery", "Battery", "Keeps “Hey Mitra” on", False)}
      </div>
      <div class="foot">{ic("lock", 14, "#98A2B3", 2)} Mitra never reads passwords, PINs or OTPs.</div>
    </div>'''
    return cell("9", "Permissions", "Each permission is explained in one line. Status updates when you return from Settings.", screen(inner))


def b5():
    def row(icn, t, s, right):
        return (f'<div class="sr"><div class="st">{ic(icn, 19)}</div><div class="pm"><b>{t}</b><small>{s}</small></div>{right}</div>')
    inner = f'''<div class="page bgp">
      <div class="bk">{ic("back", 22, "#2F5BFF", 2.4)}<span>Setup</span></div>
      <h1 style="margin-top:8px">On-device AI</h1>
      <div class="dark">
        <div class="dglow"></div>
        <div class="dtop"><div><b>Gemma 4 E2B</b><small>2.58 GB · works offline</small></div><span class="rdy">Ready</span></div>
        <div class="stats"><div><b>0.6<u>s</u></b><small>First word</small></div><div><b>18<u>/s</u></b><small>Speed</small></div><div><b>2.1<u>GB</u></b><small>Memory</small></div></div>
      </div>
      <div class="gcard gap">
        {row("mic", "Speech and wake word", "English · 122 MB", '<div class="okd">' + ic("check", 16, "#fff", 2.8) + '</div>')}
        {row("download", "Smaller model", "About 1 GB, for slower phones", ic("chev", 18, "#C3C8D1", 2.2))}
        {row("globe", "Cloud help", "Off. Always asks first", tog(False))}
      </div>
      <div class="ghost">Run speed test</div>
    </div>'''
    return cell("10", "On-device AI", "Shows the model, its speed and its memory use. Everything runs on the phone.", screen(inner))


CSS = """
*{box-sizing:border-box;margin:0}
:root{--ink:#101828;--mut:#667085;--faint:#98A2B3;--line:#EAECF0;--bg:#F6F7FB;--acc:#2F5BFF;--accs:#EAF0FF}
body{background:#fff;font-family:'DM Sans','Inter',sans-serif;color:var(--ink);-webkit-font-smoothing:antialiased}
.canvas{width:2300px;padding:72px 80px 64px;background:linear-gradient(180deg,#F7F8FC,#fff 40%)}
.hd h1{font-size:60px;letter-spacing:-1.6px;font-weight:700}
.hd p{font-size:23px;color:var(--mut);margin-top:12px;line-height:1.45;max-width:1300px}
.sec{margin-top:64px;display:flex;align-items:baseline;gap:16px;margin-bottom:26px}
.sec b{font-size:30px;font-weight:700;letter-spacing:-.5px}.sec span{font-size:19px;color:var(--mut)}
.grid{display:grid;grid-template-columns:repeat(5,380px);gap:56px}
.cell .cap{display:flex;align-items:center;gap:10px;margin-top:22px}
.cell .cap b{width:28px;height:28px;border-radius:50%;background:var(--ink);color:#fff;font-size:14px;display:flex;align-items:center;justify-content:center}
.cell .cap span{font-size:20px;font-weight:700}
.cell>p{font-size:15.5px;line-height:1.5;color:var(--mut);margin-top:6px;width:360px}
.phone{width:380px;height:800px;border-radius:52px;background:#1B1F2A;padding:8px;box-shadow:0 30px 60px -20px rgba(16,24,40,.35),0 0 0 1px rgba(16,24,40,.06)}
.scr{position:relative;width:364px;height:784px;border-radius:44px;overflow:hidden;background:#fff}
.sb{position:absolute;top:0;left:0;right:0;height:40px;display:flex;justify-content:space-between;align-items:center;padding:6px 30px 0;font-size:14px;font-weight:700;z-index:20}
.sbr{display:flex;gap:6px;align-items:center}.cam{position:absolute;left:50%;top:12px;width:12px;height:12px;border-radius:50%;background:#101828;margin-left:-6px}
/* aurora orb */
.orbw{position:relative;flex:none}
.orb{position:absolute;inset:0;border-radius:50%;background:radial-gradient(circle at 30% 22%,rgba(255,255,255,.95) 0,rgba(255,255,255,0) 34%),conic-gradient(from 200deg at 50% 55%,#2F5BFF,#7C5CFF,#22D3EE,#5B8CFF,#2F5BFF);box-shadow:0 0 0 1px rgba(255,255,255,.4) inset,0 10px 30px rgba(79,70,229,.40),0 0 70px rgba(34,211,238,.30)}
.rip{position:absolute;border-radius:50%;border:1.5px solid rgba(47,91,255,.22);background:radial-gradient(circle,rgba(47,91,255,.05),rgba(47,91,255,0) 70%)}
/* fake apps */
.wall{position:absolute;inset:0;background:linear-gradient(170deg,#DCE6FF,#EFE9FF 55%,#E3F6F1)}
.aig{position:absolute;top:70px;left:0;right:0;padding:0 26px;display:grid;grid-template-columns:repeat(4,1fr);gap:26px 12px}
.ai i{display:block;width:58px;height:58px;border-radius:18px;margin:0 auto}.ai u{display:block;width:30px;height:6px;border-radius:3px;background:rgba(16,24,40,.12);margin:8px auto 0}
.web{position:absolute;inset:0;background:#fff}
.url{margin:46px 18px 0;height:46px;border-radius:23px;background:#F2F4F7;display:flex;align-items:center;gap:8px;padding:0 18px;font-size:15px;color:var(--ink);font-weight:500}
.pg{padding:30px 24px}.hb{height:18px;border-radius:6px;background:#E4E7EC}.hb.s{margin-top:10px;height:12px;opacity:.7}
.fl{font-size:13.5px;color:var(--mut);margin:26px 0 8px;font-weight:600}
.fi{height:52px;border-radius:14px;background:#F2F4F7;display:flex;align-items:center;padding:0 16px;font-size:15px;color:var(--faint)}
.fi.on{background:var(--accs);color:var(--acc);font-weight:700;letter-spacing:2px}
.fi.res{background:#fff;color:var(--ink);font-weight:700;box-shadow:0 0 0 2.5px var(--acc),0 0 0 8px rgba(47,91,255,.12);position:relative}
.fi.ring{background:#fff;box-shadow:0 0 0 2.5px #F79009,0 0 0 8px rgba(247,144,9,.15)}
.capimg{height:46px;border-radius:12px;margin-bottom:10px;background:repeating-linear-gradient(135deg,#EDE3C4,#EDE3C4 7px,#F6EFD8 7px,#F6EFD8 14px)}
.gb{margin-top:30px;display:inline-block;background:#344054;color:#fff;border-radius:14px;padding:15px 24px;font-size:15px;font-weight:600;position:relative}
.tap{position:absolute;left:88%;top:78%;width:56px;height:56px;margin:-28px;border-radius:50%;background:rgba(47,91,255,.20);border:2.5px solid var(--acc);box-shadow:0 0 0 10px rgba(47,91,255,.10)}
.chh{display:flex;align-items:center;gap:12px;margin-top:40px;padding:12px 20px;border-bottom:1px solid var(--line)}.chh i{width:38px;height:38px;border-radius:50%;background:#D0D5DD}.chh b{font-size:17px}
.chb{padding:20px}.bb{height:40px;border-radius:18px;margin-bottom:12px}.bb.l{background:#F2F4F7}.bb.r{background:#DCE6FF;margin-left:auto}
.bb.sent{height:auto;padding:12px 16px;background:var(--acc);color:#fff;font-size:15px;font-weight:500;border-radius:18px 18px 4px 18px;line-height:1.35}
.chi{position:absolute;left:0;right:0;bottom:0;padding:12px 14px 20px;display:flex;gap:10px;align-items:center}.chi span{width:44px;height:44px;border-radius:50%;background:#B8C7F5;flex:none}
.typed,.ph{flex:1;border-radius:24px;background:#F2F4F7;padding:13px 18px;font-size:14.5px;color:var(--faint)}.typed{background:var(--accs);color:var(--ink);box-shadow:0 0 0 2px var(--acc)}
.dim{position:absolute;inset:0;background:rgba(16,24,40,.42);z-index:3}
/* voice states */
.fade{position:absolute;inset:0;z-index:2;background:linear-gradient(to top,#fff 0%,#fff 36%,rgba(255,255,255,.82) 54%,rgba(255,255,255,0) 78%)}
.glow{position:absolute;left:-40px;right:-40px;bottom:-110px;height:330px;z-index:2;background:radial-gradient(ellipse 60% 62% at 50% 100%,rgba(124,92,255,.38),rgba(47,91,255,.20) 50%,rgba(34,211,238,0) 78%)}
.vtxt{position:absolute;left:28px;right:28px;bottom:190px;z-index:4}
.lbl{display:flex;align-items:center;gap:8px;font-size:15px;font-weight:700;color:var(--acc);margin-bottom:12px}
.pulse{width:9px;height:9px;border-radius:50%;background:var(--acc);box-shadow:0 0 0 5px rgba(47,91,255,.16)}
.big{font-size:31px;line-height:1.2;font-weight:700;letter-spacing:-.9px}.big .g{color:#B0B7C3}
.caret{display:inline-block;width:3px;height:30px;background:var(--acc);border-radius:2px;margin-left:3px;vertical-align:-5px}
.orbpos{position:absolute;left:0;right:0;bottom:78px;z-index:5;display:flex;justify-content:center}
.micro{position:absolute;left:0;right:0;bottom:28px;z-index:5;text-align:center;font-size:13px;color:var(--faint);display:flex;gap:6px;justify-content:center;align-items:center;font-weight:500}
.okc{width:44px;height:44px;border-radius:50%;background:#12B76A;display:flex;align-items:center;justify-content:center;box-shadow:0 8px 20px rgba(18,183,106,.35)}
.mut15{font-size:16px;color:var(--mut);margin-top:10px}
/* capsule + cards */
.capsule{position:absolute;left:16px;right:16px;bottom:30px;z-index:6;height:84px;border-radius:42px;background:#fff;box-shadow:0 18px 50px -8px rgba(16,24,40,.30),0 0 0 1px rgba(16,24,40,.05);display:flex;align-items:center;gap:14px;padding:0 16px 0 20px}
.ct{flex:1}.ct b{font-size:17px;font-weight:700;display:block;letter-spacing:-.2px}.ct small{font-size:13.5px;color:var(--mut);display:block;margin-top:1px}
.pr{height:4px;border-radius:2px;background:var(--line);margin-top:9px;overflow:hidden}.pr i{display:block;height:100%;background:var(--acc);border-radius:2px}
.stopb{width:48px;height:48px;border-radius:50%;background:#FEE4E2;display:flex;align-items:center;justify-content:center;flex:none}.stopb i{width:16px;height:16px;border-radius:4px;background:#F04438}
.card{position:absolute;left:12px;right:12px;bottom:12px;z-index:6;background:#fff;border-radius:36px;padding:26px 24px 22px;box-shadow:0 -10px 50px rgba(16,24,40,.25)}
.ci{width:48px;height:48px;border-radius:50%;display:flex;align-items:center;justify-content:center}
.card h3{font-size:28px;letter-spacing:-.8px;margin-top:16px;font-weight:700}.sub{font-size:15px;color:var(--mut);margin-top:2px}
.msg{background:#F2F4F7;border-radius:20px;padding:16px 18px;font-size:17px;line-height:1.35;font-weight:500;margin-top:18px}
.say{display:flex;align-items:center;justify-content:center;gap:6px;font-size:14px;color:var(--mut);margin:18px 0 14px;font-weight:500}
.btns{display:flex;gap:12px}.b1,.b2{height:58px;border-radius:20px;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:700}
.b1{background:var(--acc);color:#fff;flex:1.2;box-shadow:0 8px 20px -6px rgba(47,91,255,.55)}.b2{background:#F2F4F7;color:var(--ink);flex:1}
.pt{font-size:17px;line-height:1.45;color:var(--mut);margin-top:8px}
.txt{text-align:center;font-size:16.5px;font-weight:700;margin-top:16px}.txt.red{color:#D92D20}
/* app screens */
.page{position:absolute;inset:0;padding:48px 22px 0}.bgp{background:var(--bg)}
h1{font-size:36px;letter-spacing:-1.2px;font-weight:700}
.gr{font-size:16px;color:var(--mut);margin-top:12px;font-weight:500}.page h1+.hero{margin-top:12px}
.hero{display:flex;justify-content:center;margin:34px 0 24px;height:140px;align-items:center}
.hsay{text-align:center;font-size:20px;font-weight:700;letter-spacing:-.4px}.hsub{text-align:center;font-size:15px;color:var(--mut);margin-top:3px}
.sh{display:flex;justify-content:space-between;align-items:center;margin:26px 4px 10px}.sh b{font-size:17px}.sh span{font-size:15px;color:var(--acc);font-weight:600}
.gcard{background:#fff;border-radius:24px;box-shadow:0 1px 2px rgba(16,24,40,.04),0 8px 24px -12px rgba(16,24,40,.10)}
.gcard.pad{padding:18px}.gap{margin-top:14px}
.lr{display:flex;align-items:center;gap:14px;padding:13px 16px}.lr+.lr{border-top:1px solid var(--line)}
.lr b{font-size:15.5px;display:block;font-weight:600;letter-spacing:-.2px;line-height:1.25}.lr small{font-size:13.5px;color:var(--mut);display:block;margin-top:2px}
.si{width:38px;height:38px;border-radius:50%;display:flex;align-items:center;justify-content:center;flex:none}
.tabbar{position:absolute;left:18px;right:18px;bottom:18px;height:66px;border-radius:33px;background:#fff;box-shadow:0 14px 40px -8px rgba(16,24,40,.28),0 0 0 1px rgba(16,24,40,.04);display:flex;padding:7px;z-index:10}
.tab{flex:1;border-radius:26px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;color:var(--faint);font-size:11.5px;font-weight:600}.tab.on{background:var(--accs);color:var(--acc)}
.seg{display:flex;gap:8px;margin:16px 0 4px}.seg span{padding:8px 16px;border-radius:999px;font-size:14.5px;font-weight:600;color:var(--mut);background:#fff}.seg .on{background:var(--ink);color:#fff}
.gl{font-size:14px;font-weight:700;color:var(--mut);margin:20px 4px 10px}
.tl{margin-left:19px;border-left:2px solid var(--line);padding-left:20px}.tl div{position:relative;font-size:14.5px;padding:5px 0;color:#344054;font-weight:500}
.tl i{position:absolute;left:-27px;top:11px;width:10px;height:10px;border-radius:50%;border:2px solid #fff}.dg{background:#12B76A}.da{background:#F79009}
.hero2{display:flex;align-items:center;gap:16px;padding:16px 18px;margin-top:14px}.hero2 b{font-size:19px;display:block;letter-spacing:-.4px}.hero2 small{font-size:14px;color:var(--mut)}
.ring{width:60px;height:60px;border-radius:50%;background:conic-gradient(#12B76A 0 80%,#E4E7EC 0);display:flex;flex-direction:column;align-items:center;justify-content:center;position:relative;flex:none}
.ring::before{content:'';position:absolute;inset:6px;background:#fff;border-radius:50%}.ring b,.ring small{position:relative;line-height:1}.ring b{font-size:20px}.ring small{font-size:10.5px;margin-top:1px}
.sr{display:flex;align-items:center;gap:14px;padding:12px 16px}.sr+.sr{border-top:1px solid var(--line)}
.sr>b{flex:1;font-size:16px;font-weight:600;letter-spacing:-.2px}.rv{font-size:15px;color:var(--mut)}
.st{width:38px;height:38px;border-radius:12px;background:var(--accs);color:var(--acc);display:flex;align-items:center;justify-content:center;flex:none}
.pm{flex:1}.pm b{display:block;font-size:16px;font-weight:600;letter-spacing:-.2px}.pm small{display:block;font-size:13.5px;color:var(--mut);margin-top:1px}
.dot{font-style:normal;background:#F79009;color:#fff;border-radius:50%;width:22px;height:22px;display:inline-flex;align-items:center;justify-content:center;font-size:12.5px;font-weight:700;margin-right:6px}
.tog{width:50px;height:30px;border-radius:15px;background:#D0D5DD;position:relative;flex:none}.tog.on{background:#12B76A}
.tog::after{content:'';position:absolute;top:3px;left:3px;width:24px;height:24px;border-radius:50%;background:#fff;box-shadow:0 2px 4px rgba(16,24,40,.2)}.tog.on::after{left:23px}
.bk{display:flex;align-items:center;gap:2px;color:var(--acc);font-weight:600;font-size:17px;margin-top:6px}
.lead{font-size:16px;color:var(--mut);margin:6px 2px 18px}
.pr2{padding:13px 16px}.pr2+.pr2{border-top:1px solid var(--line)}.prh{display:flex;align-items:center;gap:14px}
.okd{width:28px;height:28px;border-radius:50%;background:#12B76A;display:flex;align-items:center;justify-content:center;flex:none}
.turn{background:var(--acc);color:#fff;border-radius:999px;padding:8px 16px;font-size:14px;font-weight:700;flex:none}
.how{background:#F4F7FF;border-radius:16px;padding:14px;margin-top:12px}.how div{display:flex;gap:10px;align-items:center;font-size:14px;line-height:1.35;color:#344054}.how div+div{margin-top:10px}
.how i{width:22px;height:22px;border-radius:50%;background:var(--acc);color:#fff;font-style:normal;font-size:12px;font-weight:700;display:flex;align-items:center;justify-content:center;flex:none}
.foot{position:absolute;left:0;right:0;bottom:34px;display:flex;gap:7px;justify-content:center;align-items:center;font-size:13.5px;color:var(--faint);font-weight:500}
.dark{position:relative;overflow:hidden;margin-top:16px;border-radius:28px;padding:22px 22px 20px;background:linear-gradient(150deg,#101828,#1D2B64);color:#fff}
.dglow{position:absolute;right:-60px;top:-70px;width:220px;height:220px;border-radius:50%;background:radial-gradient(circle,rgba(124,92,255,.65),rgba(34,211,238,.18) 55%,rgba(34,211,238,0) 72%)}
.dtop{position:relative;display:flex;justify-content:space-between;align-items:flex-start}.dtop b{font-size:24px;letter-spacing:-.6px;display:block}.dtop small{font-size:14px;color:#B9C2E0;display:block;margin-top:3px}
.rdy{background:rgba(18,183,106,.2);color:#6CE9A6;border-radius:999px;padding:5px 12px;font-size:13px;font-weight:700}
.stats{position:relative;display:flex;margin-top:26px}.stats div{flex:1}.stats b{font-size:32px;letter-spacing:-1px;display:block}.stats u{text-decoration:none;font-size:15px;color:#B9C2E0;margin-left:2px;font-weight:600}.stats small{font-size:13px;color:#B9C2E0}
.ghost{margin-top:16px;height:54px;border-radius:18px;background:#fff;display:flex;align-items:center;justify-content:center;font-size:17px;font-weight:700;color:var(--acc);box-shadow:0 1px 2px rgba(16,24,40,.04),0 8px 24px -12px rgba(16,24,40,.10)}
"""


def build():
    a = [a1(), a2(), a3(), a4(), a5()]
    b = [b1(), b2(), b3(), b4(), b5()]
    html = f'''<!doctype html><html><head><meta charset="utf-8"><style>{CSS}</style></head><body><div class="canvas">
<div class="hd"><h1>Mitra</h1><p>Say “Hey Mitra” on any screen. Your words appear live, the AI thinks on the phone, and Mitra does the task in front of you. The app itself only holds tasks, history and setup.</p></div>
<div class="sec"><b>Over any app</b><span>Nothing to open. Appears when you speak.</span></div><div class="grid">{"".join(a)}</div>
<div class="sec"><b>Inside the Mitra app</b><span>Small on purpose.</span></div><div class="grid">{"".join(b)}</div>
</div></body></html>'''
    (OUT / "screens.html").write_text(html, encoding="utf-8")


async def render():
    from playwright.async_api import async_playwright
    async with async_playwright() as p:
        import os
        b = await p.chromium.launch(executable_path="/usr/bin/google-chrome") if os.path.exists("/usr/bin/google-chrome") else await p.chromium.launch()
        pg = await b.new_page(viewport={"width": 2300, "height": 1000}, device_scale_factor=1.5)
        await pg.goto("file://" + str(OUT / "screens.html"))
        await pg.wait_for_timeout(900)
        await pg.screenshot(path=str(OUT / "screens.png"), full_page=True)
        await b.close()


if __name__ == "__main__":
    build()
    asyncio.run(render())
    print("done")
