"""Run against isolated staging using two provisioned synthetic customer access keys."""
import json
import os
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

base = os.environ["STAGING_URL"].rstrip("/")
alice, bob = (os.environ[name] for name in ("STAGING_ALICE", "STAGING_BOB"))

def call(route, key, body=None, request_key=None, expected=200):
    headers = {"Content-Type": "application/json"}
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
        assert response.status == expected, (route, expected, response.status)
        return json.load(response)

assert call("session", alice)["customerId"] != call("session", bob)["customerId"]
call("transactions", None, expected=401)
for route in ("deposits", "withdrawals"):
    body = {"amount": 1300, "playerId": "TEST0001"}
    if route == "withdrawals":
        body.update(bank="Synthetic Bank", accountNumber="00000000", securityCode="test-only")
    key = str(uuid.uuid4())
    first = call(route, alice, body, key, 201)
    # Replay the same logical request after discarding the first response.
    replay = call(route, alice, body, key)
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
call("transactions/TXN-" + str(uuid.uuid4()), alice, expected=404)
print("PASS: two customers; deposit and withdrawal replay; ownership, unknown reference and response fields")
