import { useMemo, useState } from "react";
import { ArrowRight, Bell, Check, ChevronRight, Clock3, Copy, LockKeyhole, Menu, Search, ShieldCheck, Sparkles, Upload, WalletCards, X, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/_core/hooks/useAuth";
import { startLogin } from "@/const";
import { trpc } from "@/lib/trpc";

const copy = {
  en: { nav: ["How it works", "Deposit", "Withdraw", "Track", "Support"], heroKicker: "OFFICIAL AGENT OPERATIONS // SRI LANKA", heroTitle: "Your money moves at the speed of trust.", heroText: "A clear, trackable bridge between your 1xBet wallet and Fast Cash agents. Submit once. See every status. Stay in control.", deposit: "Start a deposit", withdraw: "Request withdrawal", track: "Track request", live: "AGENTS ONLINE", secure: "Protected by design", steps: ["Choose a route", "Send the details", "Watch the timeline"], support: "Need a human? WhatsApp support is available during service hours.", responsible: "18+ only. Gambling should be entertainment. Never use borrowed money, and stop if it stops being fun." },
  si: { nav: ["ක්‍රියාකාරීත්වය", "Deposit", "Withdraw", "Track", "Support"], heroKicker: "නිල නියෝජිත මෙහෙයුම් // ශ්‍රී ලංකාව", heroTitle: "විශ්වාසය වගේ වේගයෙන් මුදල් ගමන් කරයි.", heroText: "ඔබගේ 1xBet wallet එක සහ Fast Cash නියෝජිතයන් අතර පැහැදිලි, track කළ හැකි මාර්ගයක්. එක් වරක් submit කරන්න. සෑම status එකක්ම බලන්න.", deposit: "Deposit ආරම්භ කරන්න", withdraw: "Withdrawal ඉල්ලන්න", track: "Request track කරන්න", live: "නියෝජිතයන් online", secure: "ආරක්ෂාව මුලින්", steps: ["මාර්ගය තෝරන්න", "විස්තර යවන්න", "Timeline බලන්න"], support: "මිනිස් සහායක් අවශ්‍යද? සේවා වේලාවන්හි WhatsApp සහාය ඇත.", responsible: "වයස 18+ සඳහා පමණි. සූදුව විනෝදාස්වාදයක් පමණක් විය යුතුය. ණයට ගත් මුදල් භාවිතා නොකරන්න." },
  ta: { nav: ["எப்படி வேலை செய்கிறது", "Deposit", "Withdraw", "Track", "Support"], heroKicker: "அதிகாரப்பூர்வ முகவர் சேவை // இலங்கை", heroTitle: "நம்பிக்கையின் வேகத்தில் பணம் நகரும்.", heroText: "உங்கள் 1xBet wallet மற்றும் Fast Cash முகவர்களுக்கு இடையில் தெளிவான, track செய்யக்கூடிய பாலம்.", deposit: "Deposit தொடங்கு", withdraw: "Withdrawal கேள்", track: "Request track செய்", live: "முகவர்கள் online", secure: "பாதுகாப்பு முதலில்", steps: ["வழியை தேர்ந்தெடு", "விவரங்களை அனுப்பு", "Timeline பார்"], support: "மனித உதவி வேண்டுமா? சேவை நேரத்தில் WhatsApp உதவி உள்ளது.", responsible: "18+ மட்டும். சூதாட்டம் பொழுதுபோக்காக இருக்க வேண்டும். கடன் பணத்தை பயன்படுத்த வேண்டாம்." },
};

const demoRequests = [
  { reference: "TXN-7F42A", type: "DEPOSIT", amount: "25,000", status: "PROCESSING", date: "Today · 10:42" },
  { reference: "TXN-19AC2", type: "WITHDRAWAL", amount: "12,500", status: "COMPLETED", date: "Yesterday · 18:20" },
];

function StatusPill({ status }: { status: string }) {
  return <Badge className={`status-pill status-${status.toLowerCase()}`}>{status}</Badge>;
}

function RequestForm({ kind, onClose }: { kind: "DEPOSIT" | "WITHDRAWAL"; onClose: () => void }) {
  const [amount, setAmount] = useState("");
  const [playerId, setPlayerId] = useState("");
  const [reference, setReference] = useState("");
  const [receiptKey, setReceiptKey] = useState("");
  const [securityCode, setSecurityCode] = useState("");
  const [bank, setBank] = useState("");
  const [accountNumber, setAccountNumber] = useState("");
  const [contactNumber, setContactNumber] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("Bank transfer");
  const [submitted, setSubmitted] = useState(false);
  const submitRequest = trpc.requests.create.useMutation();
  const uploadReceipt = trpc.requests.uploadReceipt.useMutation();
  const isDeposit = kind === "DEPOSIT";
  const handleReceipt = (file: File | null) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async () => {
      const dataUrl = String(reader.result || "");
      const uploaded = await uploadReceipt.mutateAsync({ fileName: file.name, dataUrl });
      setReceiptKey(uploaded.key);
    };
    reader.readAsDataURL(file);
  };
  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    const result = await submitRequest.mutateAsync({ type: kind, amount: Number(amount), playerId, paymentMethod: isDeposit ? paymentMethod : "Bank payout", securityCode: isDeposit ? undefined : securityCode, bank: isDeposit ? undefined : bank, accountNumber: isDeposit ? undefined : accountNumber, contactNumber: isDeposit ? undefined : contactNumber, receiptKey: receiptKey || undefined });
    setReference(result.reference);
    setSubmitted(true);
  };
  if (submitted) return <div className="modal-card success-card"><div className="success-icon"><Check /></div><p className="eyebrow">REQUEST ACCEPTED</p><h2>Signal received.</h2><p>Your request reference is <strong>{reference}</strong>. Keep it safe — it is your control key.</p><div className="timeline-mini"><span>SUBMITTED</span><i /><span>UNDER REVIEW</span><i /><span>COMPLETE</span></div><Button onClick={onClose} className="neon-button w-full">Back to dashboard</Button></div>;
  return <div className="modal-card"><button className="close-button" onClick={onClose} aria-label="Close"><X /></button><p className="eyebrow">{isDeposit ? "DEPOSIT CHANNEL" : "WITHDRAWAL CHANNEL"}</p><h2>{isDeposit ? "Load your player wallet" : "Route funds to your bank"}</h2><p className="muted">All requests receive a unique reference and a visible status timeline.</p><form onSubmit={handleSubmit} className="request-form"><label>Amount (LKR)<Input required min={1000} max={500000} type="number" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="25,000" /></label><label>1xBet Player ID<Input required value={playerId} onChange={(e) => setPlayerId(e.target.value)} placeholder="e.g. 123456789" /></label>{isDeposit ? <><label>Payment method<select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}><option>Bank transfer</option><option>iPay wallet</option><option>Cash deposit</option></select></label><label>Receipt reference (optional)<Input value={reference} onChange={(e) => setReference(e.target.value)} placeholder="Bank slip reference" /></label><label className="upload-box"><Upload />Attach receipt screenshot<input type="file" accept="image/*" onChange={(e) => handleReceipt(e.target.files?.[0] || null)} /></label></> : <><label>Security code<Input required value={securityCode} onChange={(e) => setSecurityCode(e.target.value)} placeholder="From 1xBet Cash withdrawal" /></label><label>Bank name<Input required value={bank} onChange={(e) => setBank(e.target.value)} placeholder="Your bank" /></label><label>Bank account number<Input required value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} placeholder="Enter carefully" /></label><label>Contact number<Input value={contactNumber} onChange={(e) => setContactNumber(e.target.value)} placeholder="07X XXX XXXX" /></label></>}<Button disabled={submitRequest.isPending} className="neon-button w-full">{submitRequest.isPending ? "Submitting…" : "Submit secure request"}<ArrowRight /></Button>{submitRequest.error && <p className="error-text">{submitRequest.error.message}</p>}</form></div>;
}

function Timeline({ status }: { status: string }) {
  const stages = ["SUBMITTED", "PROCESSING", "COMPLETED"];
  const current = status === "REJECTED" ? 1 : status === "COMPLETED" ? 3 : status === "PROCESSING" ? 2 : 1;
  return <div className="request-timeline">{stages.map((stage, index) => <div className={`timeline-step ${index < current ? "active" : ""}`} key={stage}><span>{index < current ? <Check size={13} /> : index + 1}</span><small>{stage}</small></div>)}</div>;
}

function ProfileCard({ user }: { user: any }) {
  const [name, setName] = useState(user?.name || "");
  const [playerId, setPlayerId] = useState("");
  const update = trpc.profile.update.useMutation();
  if (!user) return null;
  return <div className="profile-card"><div><p className="eyebrow">CUSTOMER PROFILE // SAVED DETAILS</p><h3>Move faster next time.</h3><p className="muted">Save your name and Player ID to prefill future requests.</p></div><div className="profile-fields"><Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Full name" /><Input value={playerId} onChange={(e) => setPlayerId(e.target.value)} placeholder="1xBet Player ID" /><Button className="neon-button" disabled={!name || !playerId || update.isPending} onClick={() => update.mutate({ name, playerId })}>{update.isPending ? "Saving…" : "Save profile"}</Button></div></div>;
}

function SettingsPanel() {
  const settingsQuery = trpc.settings.public.useQuery();
  const save = trpc.settings.update.useMutation();
  const [whatsappNumber, setWhatsappNumber] = useState("+94765865387");
  const [minTransaction, setMinTransaction] = useState("1000");
  const [maxTransaction, setMaxTransaction] = useState("500000");
  const [promoCode, setPromoCode] = useState("VGSL");
  return <div className="settings-panel"><div><p className="eyebrow">CONFIG // SERVICE CONTROL</p><h3>Network settings</h3><p className="muted">Update limits and support details without editing code.</p></div><div className="profile-fields"><Input value={whatsappNumber} onChange={(e) => setWhatsappNumber(e.target.value)} placeholder="WhatsApp number" /><Input value={minTransaction} onChange={(e) => setMinTransaction(e.target.value)} placeholder="Minimum LKR" type="number" /><Input value={maxTransaction} onChange={(e) => setMaxTransaction(e.target.value)} placeholder="Maximum LKR" type="number" /><Input value={promoCode} onChange={(e) => setPromoCode(e.target.value)} placeholder="Promo code" /><Button className="pink-button" disabled={save.isPending} onClick={() => save.mutate({ whatsappNumber, minTransaction: Number(minTransaction), maxTransaction: Number(maxTransaction), promoCode, accounts: settingsQuery.data?.accounts?.map((account: any) => ({ label: account.label, accountNumber: account.accountNumber, method: account.method, active: account.active })) ?? [] })}>{save.isPending ? "Saving…" : "Save settings"}</Button></div></div>;
}

function ReconciliationPanel() {
  const [provider, setProvider] = useState<"BOC" | "PEOPLES_BANK" | "LOLC" | "SAMPATH" | "IPAY">("BOC");
  const [summary, setSummary] = useState<string>("");
  const importStatement = trpc.reconciliation.importStatement.useMutation();
  const queue = trpc.reconciliation.queue.useQuery();
  const resolve = trpc.reconciliation.resolve.useMutation();
  const handleFile = (file: File | undefined) => { if (!file) return; const reader = new FileReader(); reader.onload = async () => { try { const data = String(reader.result || "").split(",")[1] || ""; const result = await importStatement.mutateAsync({ provider, fileName: file.name, data }); setSummary(`${result.rows} rows · ${result.verified} verified · ${result.ambiguous} ambiguous · ${result.unmatched} unmatched`); queue.refetch(); } catch (error) { setSummary(error instanceof Error ? error.message : "Import failed"); } }; reader.readAsDataURL(file); };
  return <div className="reconciliation-panel"><div><p className="eyebrow">RECON // STATEMENT IMPORT</p><h3>Match bank activity</h3><p className="muted">Upload a CSV or Excel statement. Files are parsed server-side, hashed for duplicate protection, and stored as protected references.</p></div><div className="recon-controls"><select value={provider} onChange={(e) => setProvider(e.target.value as typeof provider)}><option value="BOC">BOC</option><option value="PEOPLES_BANK">People's Bank</option><option value="LOLC">LOLC</option><option value="SAMPATH">Sampath Bank</option><option value="IPAY">iPay</option></select><label className="upload-box"><Upload />{importStatement.isPending ? "Processing statement…" : "Upload CSV / Excel"}<input type="file" accept=".csv,.xls,.xlsx" onChange={(e) => handleFile(e.target.files?.[0])} /></label></div>{summary && <p className="track-result"><Check size={15} /> {summary}</p>}<div className="recon-queue">{(queue.data ?? []).slice(0, 6).map((row: any) => <div className="ops-row" key={row.id}><div><strong>#{row.externalId}</strong><small>{row.reference || row.description || "No reference"}</small></div><b>{row.direction} · LKR {Number(row.amount).toLocaleString()}</b><StatusPill status={row.verificationStatus} /><span className="queue-actions">{row.verificationStatus !== "VERIFIED" && <Button size="sm" variant="ghost" onClick={() => resolve.mutate({ id: row.id, action: "IGNORE", note: "Reviewed by admin" })}>Ignore</Button>}</span></div>)}</div></div>;
}

function AdminConsole({ isAdmin }: { isAdmin: boolean }) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [reason, setReason] = useState("");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const updateStatus = trpc.admin.updateStatus.useMutation();
  const adminQuery = trpc.admin.list.useQuery({ search: query, status, type: "ALL", paymentMethod: "ALL" }, { enabled: isAdmin });
  const filtered = (adminQuery.data?.map((item: any) => ({ ...item, amount: Number(item.amount).toLocaleString(), date: item.createdAt ? new Date(item.createdAt).toLocaleString() : "" })) ?? demoRequests).filter((item: any) => `${item.reference} ${item.type}`.toLowerCase().includes(query.toLowerCase()) && (status === "ALL" || item.status === status));
  if (!isAdmin) return null;
  return <section className="admin-console" id="admin"><div className="section-heading"><div><p className="eyebrow pink">OPS // ADMIN</p><h2>Control room</h2></div><Badge className="live-badge">● LIVE QUEUE</Badge></div><div className="admin-toolbar"><div className="search-wrap"><Search size={16} /><Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search reference or Player ID" /></div><select value={status} onChange={(e) => setStatus(e.target.value)}><option>ALL</option><option>PENDING</option><option>PROCESSING</option><option>COMPLETED</option><option>REJECTED</option></select></div><div className="ops-table">{filtered.map((item: any) => <div className="ops-row" key={item.reference}><div><strong>{item.reference}</strong><small>{item.type} · {item.date}</small></div><b>LKR {item.amount}</b><StatusPill status={item.status} /><Button variant="ghost" size="sm" onClick={() => { setSelectedId(item.id); setReason(item.status === "REJECTED" ? (item.rejectionReason || "Payment could not be verified.") : ""); }}>Review <ChevronRight size={15} /></Button></div>)}</div><ReconciliationPanel /><SettingsPanel /><div className="rejection-box"><div><p className="eyebrow pink">DECISION NOTE</p><h3>Rejection reason</h3><p className="muted">Required when a request cannot be verified. The customer sees this in their timeline and notification centre.</p></div><Textarea value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Enter a clear reason for the customer…" /><Button className="pink-button" disabled={!selectedId || !reason.trim() || updateStatus.isPending} onClick={() => selectedId && updateStatus.mutate({ id: selectedId, status: "REJECTED", rejectionReason: reason.trim() })}>{updateStatus.isPending ? "Saving…" : "Save decision"}</Button></div></section>;
}

export default function Home() {
  const { user, loading, isAuthenticated } = useAuth();
  const [lang, setLang] = useState<keyof typeof copy>("en");
  const [menu, setMenu] = useState(false);
  const [modal, setModal] = useState<"DEPOSIT" | "WITHDRAWAL" | null>(null);
  const [track, setTrack] = useState("");
  const [tracked, setTracked] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const text = copy[lang];
  const requestsQuery = trpc.requests.mine.useQuery(undefined, { enabled: isAuthenticated });
  const notificationsQuery = trpc.requests.notifications.useQuery(undefined, { enabled: isAuthenticated });
  const notifications = notificationsQuery.data ?? [];
  const displayRequests = isAuthenticated ? (requestsQuery.data ?? []) : demoRequests;
  return <div className="site-shell"><div className="noise" /><header className="topbar"><a href="#top" className="brand"><span className="brand-mark"><Zap size={18} /></span><span>FAST<span className="pink-text">CASH</span></span><small>LK // AGENT NETWORK</small></a><nav className={menu ? "open" : ""}>{text.nav.map((item, index) => <a key={item} href={`#${["how", "deposit", "withdraw", "track", "support"][index]}`} onClick={() => setMenu(false)}>{item}</a>)}</nav><div className="top-actions"><div className="language-switcher">{(["en", "si", "ta"] as const).map((item) => <button className={lang === item ? "active" : ""} onClick={() => setLang(item)} key={item}>{item.toUpperCase()}</button>)}</div>{isAuthenticated ? <div className="notification-wrap"><button className="icon-button" onClick={() => setNotificationsOpen(!notificationsOpen)} aria-label="Notifications"><Bell size={17} /><i /></button>{notificationsOpen && <div className="notification-panel"><strong>Notification centre</strong>{notifications.map((item: any) => <div className={item.readAt ? "notification" : "notification unread"} key={item.id}><span /><div><b>{item.title}</b><small>{item.message}</small></div></div>)}</div>}</div> : <Button onClick={() => startLogin()} variant="outline" className="login-button">Sign in <ArrowRight size={15} /></Button>}<button className="menu-button" onClick={() => setMenu(!menu)}><Menu /></button></div></header><main id="top"><section className="hero"><div className="hero-copy"><p className="eyebrow">{text.heroKicker}</p><h1>{text.heroTitle}</h1><p className="hero-text">{text.heroText}</p><div className="hero-actions"><Button className="neon-button" onClick={() => setModal("DEPOSIT")}>{text.deposit}<ArrowRight /></Button><Button className="ghost-button" onClick={() => setModal("WITHDRAWAL")}>{text.withdraw}<WalletCards /></Button></div><div className="trust-row"><span><ShieldCheck size={16} /> {text.secure}</span><span><Clock3 size={16} /> 15–30 min typical review</span></div></div><div className="hero-visual"><div className="hud-ring ring-one" /><div className="hud-ring ring-two" /><div className="core-card"><div className="core-top"><span>FC / 01</span><span className="signal"><i /> {text.live}</span></div><div className="core-balance"><small>NETWORK BALANCE</small><strong>LKR <em>486,920</em></strong></div><div className="core-bars"><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /></div><div className="core-bottom"><span>SECURE ROUTE</span><span>ENCRYPTED // 256</span></div></div><div className="corner corner-tl" /><div className="corner corner-br" /></div></section><section className="ticker"><span>FAST CASH NETWORK</span><b>///</b><span>WALASMULLA · BELIATTA ROAD 24/7</span><b>///</b><span>REQUESTS TRACKED END TO END</span><b>///</b><span>FAST CASH NETWORK</span></section><section className="how-section" id="how"><div className="section-heading"><div><p className="eyebrow">THE FLOW // 03 STEPS</p><h2>Clear by default.</h2></div><p className="section-note">No black boxes. No guessing. Every request gets a reference, an owner, and a visible timeline.</p></div><div className="steps-grid">{text.steps.map((step, index) => <div className="step-card" key={step}><span>0{index + 1}</span><Sparkles size={18} /><h3>{step}</h3><p>{["Choose deposit or withdrawal and enter the amount.", "Attach a receipt when needed. We store a protected reference, not loose files.", "See processing, completion, or a clear rejection reason in one place."][index]}</p></div>)}</div></section><section className="dashboard-section" id="track"><div className="dashboard-intro"><p className="eyebrow pink">CUSTOMER SPACE // YOUR CONTROL KEY</p><h2>One view for every request.</h2><p>Sign in to save your Player ID, submit faster next time, and receive status updates without chasing an agent.</p>{!isAuthenticated && <Button onClick={() => startLogin()} className="pink-button">Create customer profile <ArrowRight /></Button>}</div><ProfileCard user={user} /><Card className="request-card"><CardHeader><CardTitle>Recent requests</CardTitle><Badge variant="outline">{isAuthenticated ? "LIVE" : "PREVIEW"}</Badge></CardHeader><CardContent>{displayRequests.length === 0 ? <p className="muted">No requests yet. Start a deposit or withdrawal flow to create your first tracked request.</p> : displayRequests.slice(0, 3).map((item: any) => <div className="request-row" key={item.reference}><div className="request-main"><span className="request-icon">{item.type === "DEPOSIT" ? <ArrowRight /> : <WalletCards />}</span><div><strong>{item.reference}</strong><small>{item.type} · {item.createdAt ? new Date(item.createdAt).toLocaleString() : item.date}</small></div></div><div className="request-amount"><b>LKR {Number(item.amount).toLocaleString()}</b><StatusPill status={item.status} /></div><Timeline status={item.status} />{item.rejectionReason && <p className="error-text">Reason: {item.rejectionReason}</p>}</div>)}<div className="track-box"><Input value={track} onChange={(e) => setTrack(e.target.value)} placeholder="Paste request reference…" /><Button variant="outline" onClick={() => setTracked(Boolean(track))}><Search size={15} /> Track</Button></div>{tracked && <p className="track-result"><Check size={15} /> Request found. Current status: <strong>PROCESSING</strong></p>}</CardContent></Card></section><section className="service-grid"><div className="service-panel" id="deposit"><div className="service-number">01</div><p className="eyebrow">DEPOSIT // FUND YOUR WALLET</p><h2>From bank to balance.</h2><p>Choose an active agent account, transfer, attach your receipt, and keep the reference until your Player ID is credited.</p><Button className="neon-button" onClick={() => setModal("DEPOSIT")}>Open deposit flow <ArrowRight /></Button></div><div className="service-panel pink-panel" id="withdraw"><div className="service-number">02</div><p className="eyebrow">WITHDRAWAL // CASH OUT</p><h2>From wallet to bank.</h2><p>Use the 1xBet Cash security code, confirm your bank details, and follow the payout timeline from approval to completion.</p><Button className="pink-button" onClick={() => setModal("WITHDRAWAL")}>Open withdrawal flow <ArrowRight /></Button></div></section><AdminConsole isAdmin={user?.role === "admin"} /><section className="support-section" id="support"><div><p className="eyebrow">SUPPORT // HUMAN CHANNEL</p><h2>Questions? Route them here.</h2><p>{text.support}</p></div><div className="support-actions"><a className="whatsapp-link" href="https://wa.me/94765865387" target="_blank" rel="noreferrer">Open WhatsApp <ArrowRight /></a><a href="#privacy">Privacy centre <ArrowRight /></a></div></section><section className="footer-info" id="privacy"><div><b>FAST<span className="pink-text">CASH</span></b><p>Deposit and withdrawal support for 1xBet players in Sri Lanka.</p></div><div><strong>Safety</strong><p>{text.responsible}</p></div><div><strong>Privacy</strong><p>We use request data only to process support. Receipt uploads are handled as protected file references alongside transaction records.</p></div></section></main>{modal && <div className="modal-backdrop"><RequestForm kind={modal} onClose={() => setModal(null)} /></div>}<footer><span>© 2026 FAST CASH / ALL SYSTEMS MONITORED</span><span>PRIVACY · RESPONSIBLE GAMBLING · SUPPORT</span></footer></div>;
}
