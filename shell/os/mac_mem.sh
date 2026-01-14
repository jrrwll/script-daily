#!/usr/bin/env bash

ps -eo pid,pmem,rss,args -m | head -21 | awk '
NR==1 {
    printf "%-8s %-6s %-10s %s\n", "PID", "%MEM", "RSS(MB)", "COMMAND"
}
NR>1 {
    rss_mb = $3 / 1024

    cmd = ""
    for (i = 4; i <= NF; i++) {
        if (i > 4) cmd = cmd " "
        cmd = cmd $i
    }
	if (length(cmd) > 100) {
        cmd = substr(cmd, 1, 97) "..."
    }
    printf "%-8s %-6s %-10.1f %s\n", $1, $2, rss_mb, cmd
}'
