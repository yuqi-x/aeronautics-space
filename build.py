# -*- coding: utf-8 -*-
"""
Aeronautics: Space —— 免 Gradle 构建脚本

为什么不用 Gradle：本机提交内存被顶满（19MB 余量），ModDevGradle 需要
fork NeoForm 反编译器（>3GB），起不来。

替代方案：PCL 装 NeoForge 时留下了 official mappings 的完整 MC jar
(libraries/net/minecraft/client/1.21.1/client-1.21.1-official.jar)，
用它 + NeoForge 自身的 jar 直接 javac。
"""
import os, sys, json, shutil, subprocess, glob

ROOT = os.path.dirname(os.path.abspath(__file__))
MC   = r"D:\PCL_2.10.3\.minecraft"
LIB  = os.path.join(MC, "libraries")
JDK  = r"C:\Java\jdk-21\bin"
JAVAC= os.path.join(JDK, "javac.exe")
JAR  = os.path.join(JDK, "jar.exe")

BUILD = os.path.join(ROOT, "build")
CLASSES = os.path.join(BUILD, "classes")
OUT_JAR = os.path.join(BUILD, "libs", "aeronautics_space-0.1.0.jar")

def log(*a):
    print(*a, flush=True)

# ---------- 1. 组装 classpath ----------
cps = []

# 1a-pre. NeoForge 本体必须最先加入！！
# 它的 client.jar 里是【打过补丁的 MC 类】（如 Entity 实现了 IAttachmentHolder，
# 有 getData/setData）。若把 MC 官方 jar 排在前面，javac 会拿到未打补丁的原版类，
# 导致 getData/setData 等 NeoForge 扩展方法「找不到符号」。
_neo_root = os.path.join(LIB, "net", "neoforged", "neoforge")
_neo_ver = None
if os.path.isdir(_neo_root):
    _vers = sorted(os.listdir(_neo_root))
    if _vers:
        _neo_ver = _vers[-1]
if _neo_ver:
    for _j in sorted(glob.glob(os.path.join(_neo_root, _neo_ver, "*.jar")),
                     key=lambda p: (0 if "client" in os.path.basename(p) else 1)):
        cps.append(_j)
    log(f"[cp] NeoForge {_neo_ver} 前置（patched MC 类优先）")

# 1a. 版本 json 里声明的库
vjson = os.path.join(MC, "versions", "机械动力：航空学", "机械动力：航空学.json")
if os.path.exists(vjson):
    data = json.loads(open(vjson, encoding="utf-8").read())
    miss = 0
    for l in data.get("libraries", []):
        name = l.get("name", "")
        if ":" not in name:
            continue
        parts = name.split(":")
        group, art = parts[0], parts[1]
        ver = parts[2] if len(parts) > 2 else ""
        cls_ = parts[3] if len(parts) > 3 else None
        if cls_ and any(k in cls_ for k in ("natives-linux", "natives-macos", "linux-", "osx")):
            continue
        fn = f"{art}-{ver}" + (f"-{cls_}" if cls_ else "") + ".jar"
        p = os.path.join(LIB, group.replace(".", os.sep), art, ver, fn)
        if os.path.exists(p):
            cps.append(p)
        else:
            miss += 1
    log(f"[cp] 版本库: {len(cps)} 个可用, {miss} 个缺失(多为其它平台 natives)")

# 1b. official-mappings 的完整 MC jar（关键）
official = os.path.join(LIB, "net", "minecraft", "client", "1.21.1", "client-1.21.1-official.jar")
if os.path.exists(official):
    cps.append(official)
    log(f"[cp] MC official jar ✓ {os.path.getsize(official):,} bytes")
else:
    log("[cp] !! 找不到 client-1.21.1-official.jar"); sys.exit(1)

# 1c. NeoForge 本体（PCL 的版本 json 不列它，手动取本机最高的 21.1.x）
neo_root = os.path.join(LIB, "net", "neoforged", "neoforge")
neo_ver = None
if os.path.isdir(neo_root):
    vers = sorted(os.listdir(neo_root))
    if vers:
        neo_ver = vers[-1]
log(f"[cp] NeoForge 版本: {neo_ver}")
if neo_ver:
    nd = os.path.join(neo_root, neo_ver)
    for j in glob.glob(os.path.join(nd, "*.jar")):
        cps.append(j)
        log(f"      + {os.path.basename(j)}")

# 1d. 本地模组 jar（sable / companion / veil / aeronautics / create / aeroworks）
for j in glob.glob(os.path.join(ROOT, "libs", "*.jar")):
    cps.append(j)

cps = [c for c in dict.fromkeys(cps) if os.path.exists(c)]
log(f"[cp] classpath 合计 {len(cps)} 个 jar")

# ---------- 2. 编译 ----------
src = os.path.join(ROOT, "src", "main", "java")
res = os.path.join(ROOT, "src", "main", "resources")
shutil.rmtree(CLASSES, ignore_errors=True)
os.makedirs(CLASSES, exist_ok=True)

java_files = []
for dp, dn, fn in os.walk(src):
    for f in fn:
        if f.endswith(".java"):
            java_files.append(os.path.join(dp, f))
log(f"[javac] 源文件 {len(java_files)} 个")

argf = os.path.join(BUILD, "javac.args")
os.makedirs(BUILD, exist_ok=True)
with open(argf, "w", encoding="utf-8") as fh:
    fh.write("-encoding\nUTF-8\n")
    fh.write("-proc:none\n")            # 无注解处理器（不用 refmap）
    fh.write("-nowarn\n")
    fh.write("-Xlint:none\n")
    fh.write("-source\n21\n-target\n21\n")
    # argfile 里反斜杠是转义符，全部换成正斜杠；classpath 整行加引号防空格拆断
    fh.write('-cp\n"' + ";".join(p.replace("\\", "/") for p in cps) + '"\n')
    fh.write("-d\n" + CLASSES + "\n")
    for jf in java_files:
        fh.write('"' + jf.replace("\\", "/") + '"\n')

r = subprocess.run([JAVAC, "-J-Xmx384m", "-J-XX:+UseSerialGC",
                    "-J-XX:TieredStopAtLevel=1", "-J-XX:CICompilerCount=1",
                    "-J-XX:MaxMetaspaceSize=160m", "-J-XX:ReservedCodeCacheSize=48m",
                    "-J-Xss1m", "@" + argf], capture_output=True, text=True,
                   errors="replace", encoding="utf-8")
log("---- javac stdout ----"); log(r.stdout.strip() or "(空)")
if r.stderr.strip():
    log("---- javac stderr ----"); log(r.stderr.strip())
if r.returncode != 0:
    log(f"[javac] 失败 exit={r.returncode}")
    sys.exit(r.returncode)
log("[javac] 编译成功 ✓")

# ---------- 3. 资源 + 打包 ----------
for dp, dn, fn in os.walk(res):
    rel = os.path.relpath(dp, res)
    dst = os.path.join(CLASSES, rel) if rel != "." else CLASSES
    os.makedirs(dst, exist_ok=True)
    for f in fn:
        shutil.copy2(os.path.join(dp, f), os.path.join(dst, f))

shutil.rmtree(os.path.dirname(OUT_JAR), ignore_errors=True)
os.makedirs(os.path.dirname(OUT_JAR), exist_ok=True)
r = subprocess.run([JAR, "--create", "--file", OUT_JAR, "-C", CLASSES, "."],
                   capture_output=True, text=True, errors="replace", encoding="utf-8")
if r.returncode != 0:
    log("[jar] 失败:", r.stdout, r.stderr); sys.exit(r.returncode)
log(f"[jar] 产出 {OUT_JAR} ({os.path.getsize(OUT_JAR):,} bytes)")
log("BUILD OK")
