#!/usr/bin/env bash
# PreToolUse hook for WalletAI: blocks dangerous shell commands before execution.
# Receives tool call JSON on stdin; exit code 2 = block the call.

input=$(cat)

block() {
  printf 'BLOCKED by pre-tool-use hook: %s\n' "$1" >&2
  exit 2
}

check() {
  if printf '%s' "$input" | grep -qEi "$1"; then
    block "$2"
  fi
}

# --- destructive filesystem operations ---
check 'rm[[:space:]]+-[a-z]*r[a-z]*f|rm[[:space:]]+-[a-z]*f[a-z]*r' 'рекурсивное принудительное удаление (rm -rf)'
check 'Remove-Item[^;]*-Recurse[^;]*-Force|Remove-Item[^;]*-Force[^;]*-Recurse' 'рекурсивное принудительное удаление (Remove-Item)'
check '(^|[^a-z])del[[:space:]]+/[sq]|rmdir[[:space:]]+/s|format[[:space:]]+[a-z]:|mkfs' 'разрушительная команда Windows'

# --- git history / remote protection ---
check 'git[[:space:]]+push[^"]*(--force|-f[[:space:]]|--force-with-lease)' 'force push запрещён'
check 'git[[:space:]]+push' 'git push выполняет только владелец вручную'
check 'git[[:space:]]+reset[[:space:]]+--hard' 'git reset --hard запрещён'
check 'git[[:space:]]+clean' 'git clean запрещён'
check 'git[[:space:]]+rebase' 'git rebase запрещён (линейная история правится только владельцем)'
check 'git[[:space:]]+(branch|push)[^"]*(-D|--delete)[^"]*(main|master|develop)' 'удаление защищённых веток запрещено'

# --- secrets protection ---
check '(cat|type|more|less|head|tail|Get-Content|gc)[[:space:]][^|;&]*\.env([^.a-z]|$)' 'чтение .env (секреты) запрещено'
check '(cat|type|more|less|head|tail|Get-Content|gc)[[:space:]][^|;&]*local\.properties' 'чтение local.properties запрещено'
check '(cat|type|more|less|head|tail|Get-Content|gc|cp|copy|mv|move)[[:space:]][^|;&]*\.(jks|keystore)' 'доступ к keystore запрещён'
check 'GEMINI_API_KEY[[:space:]]*=' 'запись/вывод API-ключа в команде запрещены'

# --- remote code execution ---
check '(curl|wget|iwr|Invoke-WebRequest)[^|;&]*\|[[:space:]]*(ba|z|da)?sh' 'скачивание и исполнение скриптов из сети запрещено'
check '(curl|wget)[^|;&]*\|[[:space:]]*(sudo|powershell|iex)' 'скачивание и исполнение скриптов из сети запрещено'
check 'iex[[:space:]]*\(|Invoke-Expression' 'Invoke-Expression запрещён'

exit 0
