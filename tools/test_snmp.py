import requests, subprocess, json

BASE = "http://localhost:8080"
r = requests.get(BASE + "/captchaImage")
uuid = r.json()["uuid"]
result = subprocess.run(["redis-cli", "-a", "Chinaunicom@1358", "GET", "captcha_codes:" + uuid], capture_output=True, text=True)
answer = result.stdout.strip().strip('"')
login = requests.post(BASE + "/login", json={"username":"admin","password":"admin123","uuid":uuid,"code":answer})
token = login.json()["token"]
h = {"Authorization": "Bearer " + token}

# SNMP Status
r = requests.get(BASE + "/device/snmp/status", headers=h)
print("SNMP Mode:", r.json().get("data", {}).get("mode", "N/A"))

# Device list
r = requests.get(BASE + "/device/list?pageSize=5", headers=h)
devices = r.json().get("rows", [])
print("Devices:", len(devices))

for d in devices:
    did = d["deviceId"]
    name = d["deviceName"]
    print("\n--- Device %s: %s ---" % (did, name))

    r = requests.get(BASE + "/device/snmp/info/" + str(did), headers=h)
    print("  Info:", r.json())

    r = requests.get(BASE + "/device/snmp/ports/" + str(did), headers=h)
    print("  Ports:", r.json().get("code"), len(r.json().get("data", [])))

    r = requests.get(BASE + "/device/snmp/get/" + str(did) + "?oid=1.3.6.1.2.1.1.5.0", headers=h)
    print("  GET:", r.json())
