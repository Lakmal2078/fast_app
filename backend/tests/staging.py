"""Run against isolated staging using two provisioned synthetic customer access keys."""
from concurrent.futures import ThreadPoolExecutor
import json
import os
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

base = os.environ["STAGING_URL"].rstrip("/")
alice, bob = (os.environ[name] for name in ("STAGING_ALICE", "STAGING_BOB"))

def call(route, key, body=None, request_key=None, expected=200, discard=False):
    headers = {"Content-Type": "application/json", "User-Agent": "FastCash-Staging-Test/1.0"}
    if key:
        headers["Authorization"] = "Bearer " + key
    if request_key:
        headers["Idempotency-Key"] = request_key
    req = Request(base + "/api/" + route, headers=headers,
                  data=None if body is None else json.dumps(body).encode())
    try:
        response = urlopen(req, timeout=30)
    except HTTPError as exc:
        response = exc
    with response:
        allowed = expected if isinstance(expected, tuple) else (expected,)
        assert response.status in allowed, (route, expected, response.status)
        if discard:
            return None  # Close the connection without consuming the committed reference.
        return json.load(response)

assert call("session", alice)["customerId"] != call("session", bob)["customerId"]
call("transactions", None, expected=401)
for route in ("deposits", "withdrawals"):
    body = {"amount": 1300, "playerId": "TEST0001"}
    if route == "withdrawals":
        body.update(bank="Synthetic Bank", accountNumber="00000000", securityCode="test-only")
    key = str(uuid.uuid4())
    call(route, alice, body, key, 201, discard=True)
    with ThreadPoolExecutor(max_workers=2) as workers:
        futures = [workers.submit(call, route, alice, body, key) for _ in range(2)]
        first, replay = [future.result() for future in futures]
    assert first == replay
    call(route, alice, dict(body, amount=1400), key, 409)
    other = call(route, bob, body, key, 201)
    assert first["reference"] != other["reference"]
    for owner, own, foreign in ((alice, first, other), (bob, other, first)):
        rows = call("transactions", owner)
        assert sum(x["id"] == own["id"] for x in rows) == 1
        assert all(x["id"] != foreign["id"] for x in rows)
        assert call("transactions/" + own["reference"], owner) == own
        call("transactions/" + foreign["reference"], owner, expected=404)
        assert not {"securityCode", "accountNumber", "contactNumber"} & own.keys()
    agent = os.environ.get("STAGING_AGENT")
    if agent:
        target = "agent/requests/" + first["reference"]
        call(target, alice, expected=403)
        reviewed = call(target, agent)
        assert reviewed["amount"] == body["amount"] and reviewed["playerId"] == body["playerId"]
        for _ in range(2):
            call(target + "/decision", agent, {"status":"COMPLETED"})
        call(target + "/decision", agent, {"status":"REJECTED","reason":"test"}, expected=409)
        assert call("transactions/" + first["reference"], alice)["status"] == "COMPLETED"
        assert sum(n["reference"] == first["reference"] for n in call("notifications", alice)) == 1
        assert all(n["reference"] != first["reference"] for n in call("notifications", bob))
        rejected = "agent/requests/" + other["reference"] + "/decision"
        call(rejected, agent, {"status":"REJECTED","reason":"Test rejection"})
        assert any(n["reference"] == other["reference"] and n["rejectionReason"] == "Test rejection"
                   for n in call("notifications", bob))
        print("PASS: authorized agent decisions and scoped player notices for", route)
call("transactions/TXN-" + str(uuid.uuid4()), alice, expected=404)
print("PASS: two customers; deposit and withdrawal replay; ownership, unknown reference and response fields")
