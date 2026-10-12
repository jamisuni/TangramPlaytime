#!/usr/bin/env python3
"""V-01 - release safety of the app APK (WO-003 TASK-015; tasks.md § WO-003 › Seams S-16). Guardrails
G-01 (no permission, no network code in product code; REQ-010) and G-08 (the DEV aid of REQ-046 is
debug-only). Builds nothing; reads the APKs of `:app:assembleRelease :app:assembleDebug`. Default, on
app/build/outputs/apk/release/app-release-unsigned.apk:
  1. `aapt2 dump permissions` names the package and shows no uses-permission.
  2. No classes*.dex holds the debug canary (read from play/src/debug/.../DebugCanary.kt).
  3. No Lokhttp3/ class exists and no class under Lapp/tangramplaytime/ references Ljava/net/. The trap:
     kotlin.io and androidx.core.net reference java.net.URL, so a whole-dex string scan is a false RED;
     G-01 says "in product code": `dexdump -d` is read class by class, each reference owned by its class.
--positive-control, on app/build/outputs/apk/debug/app-debug.apk: the scanner must SEE its markers, the
  canary and a Ljava/net/ reference outside product code (the reference scan reads code). Exit 0 only then.
One PASS/FAIL line per check, then a verdict line; exit 0 pass, 1 fail, 2 setup error. Stdlib only;
build-tools/36.0.0 is found through ANDROID_HOME, ANDROID_SDK_ROOT or local.properties."""
import os, re, subprocess, sys, tempfile, zipfile  # noqa: E401 - stdlib only, the 150-line budget

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
RELEASE_APK = "app/build/outputs/apk/release/app-release-unsigned.apk"
DEBUG_APK = "app/build/outputs/apk/debug/app-debug.apk"
CANARY_SRC = "play/src/debug/kotlin/app/tangramplaytime/play/DebugCanary.kt"
PACKAGE, PRODUCT, NET, OKHTTP = "app.tangramplaytime", "Lapp/tangramplaytime/", "Ljava/net/", "Lokhttp3/"


class Setup(Exception):
    """The verifier cannot run (no SDK, APK or canary): exit 2, never a PASS."""


def sdk_dir():
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            return os.environ[var]
    props = os.path.join(ROOT, "local.properties")
    if os.path.isfile(props):
        for line in open(props, encoding="utf-8"):
            if line.strip().startswith("sdk.dir="):
                return line.split("=", 1)[1].strip().replace("\\:", ":").replace("\\\\", "\\")
    raise Setup("no Android SDK: set ANDROID_HOME or sdk.dir in local.properties")


def tool(name):
    base = os.path.join(sdk_dir(), "build-tools", "36.0.0", name)
    for path in (base + ".exe", base):
        if os.path.isfile(path):
            return path
    raise Setup("%s not found under build-tools/36.0.0" % name)


def canary():
    path = os.path.join(ROOT, CANARY_SRC)
    m = re.search(r'const val MARKER: String = "([^"]+)"', open(path, encoding="utf-8").read()) \
        if os.path.isfile(path) else None
    if not m:
        raise Setup("no `const val MARKER` string in %s" % CANARY_SRC)
    return m.group(1)


def dex_files(apk, into):
    with zipfile.ZipFile(apk) as z:
        names = sorted(n for n in z.namelist() if re.fullmatch(r"classes\d*\.dex", n))
        for n in names:
            z.extract(n, into)
    if not names:
        raise Setup("no classes*.dex in %s" % apk)
    return [os.path.join(into, n) for n in names]


def dex_with(dexes, text):
    return [os.path.basename(d) for d in dexes if text.encode("utf-8") in open(d, "rb").read()]


def scan_references(dexes):
    """`dexdump -d`, class by class: product classes, okhttp3 classes, Ljava/net/ references by owner."""
    product, okhttp, net_product, net_elsewhere = set(), set(), {}, 0
    for dex in dexes:
        proc = subprocess.Popen([tool("dexdump"), "-d", dex], stdout=subprocess.PIPE,
                                stderr=subprocess.DEVNULL, encoding="utf-8", errors="replace")
        owner = ""
        for line in proc.stdout:
            if "Class descriptor" in line:
                m = re.search(r"'(L[^']*;)'", line)
                owner = m.group(1) if m else ""
                if owner.startswith(PRODUCT):
                    product.add(owner)
                if owner.startswith(OKHTTP):
                    okhttp.add(owner)
            elif NET in line:
                if owner.startswith(PRODUCT):
                    net_product[owner] = net_product.get(owner, 0) + 1
                else:
                    net_elsewhere += 1
        if proc.wait() != 0:
            raise Setup("dexdump failed on %s" % os.path.basename(dex))
    return product, okhttp, net_product, net_elsewhere


def manifest_check(apk):
    r = subprocess.run([tool("aapt2"), "dump", "permissions", apk], capture_output=True, text=True)
    if r.returncode != 0 or ("package: %s" % PACKAGE) not in r.stdout:
        raise Setup("aapt2 could not read the manifest: %s" % (r.stderr or r.stdout).strip()[:200])
    uses = [l.strip() for l in r.stdout.splitlines() if l.strip().startswith("uses-") and "permission" in l]
    return not uses, "1 manifest (aapt2 dump permissions): " + (
        "; ".join(uses) if uses else "package %s declares no uses-permission" % PACKAGE)


def release_checks(apk, dexes, marker):
    results = [manifest_check(apk)]
    hit = dex_with(dexes, marker)
    results.append((not hit, "2 debug canary %r: %s" % (marker, "FOUND in " + ", ".join(hit) if hit
                                                        else "in none of %d dex file(s)" % len(dexes))))
    product, okhttp, net_product, net_elsewhere = scan_references(dexes)
    detail = ("%d okhttp3 class(es) %s; %d Ljava/net/ reference(s) from %d product class(es) %s; %d outside "
              "product code not counted (G-01: product code)" % (len(okhttp), sorted(okhttp)[:3],
              sum(net_product.values()), len(product), sorted(net_product)[:5], net_elsewhere))
    if not product:
        detail = "no class under %s found - the scan read nothing" % PRODUCT
    return results + [(bool(product) and not okhttp and not net_product, "3 network code: " + detail)]


def positive_control(dexes, marker):
    hit = dex_with(dexes, marker)
    net_elsewhere = scan_references(dexes)[3]
    return [(bool(hit), "pc canary %r: %s" % (marker, "seen in " + ", ".join(hit) if hit
                                              else "NOT seen in any of %d dex file(s)" % len(dexes))),
            (net_elsewhere > 0, "pc reference scan: %d Ljava/net/ reference(s) seen outside product code "
                                "(kotlin.io, androidx.core.net)" % net_elsewhere)]


def main(argv):
    pc = "--positive-control" in argv
    rel = DEBUG_APK if pc else RELEASE_APK
    apk = os.path.join(ROOT, rel)
    try:
        if not os.path.isfile(apk):
            raise Setup("%s not found - build it first (:app:assemble%s)" % (rel, "Debug" if pc else "Release"))
        with tempfile.TemporaryDirectory() as tmp:
            dexes, marker = dex_files(apk, tmp), canary()
            results = positive_control(dexes, marker) if pc else release_checks(apk, dexes, marker)
    except Setup as e:
        print("SETUP ERROR %s\nV-01 FAIL (setup) on %s" % (e, rel))
        return 2
    for ok, text in results:
        print("%s %s" % ("PASS" if ok else "FAIL", text))
    passed = sum(1 for ok, _ in results if ok)
    verdict = "PASS" if passed == len(results) else "FAIL"
    print("V-01 %s %s%d/%d checks on %s" % (verdict, "positive control " if pc else "", passed, len(results), rel))
    return 0 if verdict == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
