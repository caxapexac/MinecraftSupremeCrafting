#!/usr/bin/env python3
"""
Tiny Minecraft RCON client — stdlib only, no pip deps.

Speaks the protocol Mojang's server expects (Source RCON):
  packet = [length:int32_le][request_id:int32_le][type:int32_le][payload:utf8][\\0][\\0]
  type 3 = SERVERDATA_AUTH (login)
  type 2 = SERVERDATA_EXECCOMMAND
  type 0 = SERVERDATA_RESPONSE_VALUE (replies, plus type-2 echo)
  failed auth replies with request_id = -1

Defaults match our `ensureDevRconConfig` Gradle task: 127.0.0.1:25575,
password `supreme-dev-rcon`. Override via env or CLI flags if needed.

Usage:
    python scripts/rcon.py "list"
    python scripts/rcon.py "give Dev minecraft:diamond 64"
    python scripts/rcon.py "give @a supreme_crafting:supreme_table"

    # Pipe multiple commands (one per line):
    echo -e "list\\ngamemode creative Dev" | python scripts/rcon.py -

Exit codes:
    0  success (auth + every command got a non-error response)
    1  connect or auth failure
    2  one or more commands returned an empty response (often means perms)
"""
from __future__ import annotations

import argparse
import os
import socket
import struct
import sys

DEFAULT_HOST = os.environ.get("MC_RCON_HOST", "127.0.0.1")
DEFAULT_PORT = int(os.environ.get("MC_RCON_PORT", "25575"))
DEFAULT_PASSWORD = os.environ.get("MC_RCON_PASSWORD", "supreme-dev-rcon")

TYPE_AUTH = 3
TYPE_COMMAND = 2
TYPE_RESPONSE = 0


def _pack(req_id: int, ptype: int, payload: str) -> bytes:
    body = struct.pack("<ii", req_id, ptype) + payload.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(body)) + body


def _recv_exact(sock: socket.socket, n: int) -> bytes:
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("server closed mid-packet")
        buf += chunk
    return buf


def _recv_packet(sock: socket.socket) -> tuple[int, int, str]:
    length = struct.unpack("<i", _recv_exact(sock, 4))[0]
    body = _recv_exact(sock, length)
    req_id, ptype = struct.unpack("<ii", body[:8])
    payload = body[8:-2].decode("utf-8", errors="replace")
    return req_id, ptype, payload


def run(host: str, port: int, password: str, commands: list[str]) -> int:
    with socket.create_connection((host, port), timeout=5) as sock:
        # Auth
        sock.sendall(_pack(1, TYPE_AUTH, password))
        req_id, _ptype, _ = _recv_packet(sock)
        if req_id == -1:
            print("auth failed: wrong password?", file=sys.stderr)
            return 1

        # Commands
        any_empty = False
        for i, cmd in enumerate(commands, start=2):
            sock.sendall(_pack(i, TYPE_COMMAND, cmd))
            _, _, reply = _recv_packet(sock)
            stripped = reply.strip()
            if stripped:
                print(stripped)
            else:
                any_empty = True
                print(f"(empty reply for: {cmd})", file=sys.stderr)
        return 2 if any_empty else 0


def main() -> int:
    p = argparse.ArgumentParser(description="Send commands to a Minecraft server via RCON.")
    p.add_argument("command", nargs="?",
                   help="Command to run. Use `-` to read commands from stdin (one per line). "
                        "Omit to read a single command from stdin.")
    p.add_argument("--host", default=DEFAULT_HOST, help=f"default: {DEFAULT_HOST}")
    p.add_argument("--port", type=int, default=DEFAULT_PORT, help=f"default: {DEFAULT_PORT}")
    p.add_argument("--password", default=DEFAULT_PASSWORD,
                   help="default: from MC_RCON_PASSWORD env or fallback")
    args = p.parse_args()

    if args.command is None or args.command == "-":
        commands = [line.strip() for line in sys.stdin if line.strip()]
    else:
        commands = [args.command]

    if not commands:
        print("no commands given", file=sys.stderr)
        return 1

    try:
        return run(args.host, args.port, args.password, commands)
    except (ConnectionError, OSError) as e:
        print(f"rcon error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
