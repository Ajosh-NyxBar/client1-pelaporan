#!/bin/bash
###############################################################################
# Comprehensive endpoint testing
###############################################################################

BASE_LOCAL="http://127.0.0.1:3000"
BASE_NGINX="http://127.0.0.1"
BASE_PUBLIC="http://103.247.10.60"

PASS=0
FAIL=0

ok()   { echo "  ✅ PASS: $1"; PASS=$((PASS+1)); }
ko()   { echo "  ❌ FAIL: $1"; FAIL=$((FAIL+1)); }

echo "==================================================================="
echo "🧪 COMPREHENSIVE ENDPOINT TESTING"
echo "==================================================================="

# T1
echo ""
echo "▶ T1: GET / (direct)"
R=$(curl -s ${BASE_LOCAL}/)
echo "  $R"
echo "$R" | grep -q "running" && ok "Health Direct" || ko "Health Direct"

# T2
echo ""
echo "▶ T2: GET / (Nginx)"
R=$(curl -s ${BASE_NGINX}/)
echo "  $R"
echo "$R" | grep -q "running" && ok "Health Nginx" || ko "Health Nginx"

# T3
echo ""
echo "▶ T3: GET / (Public IP)"
R=$(curl -s ${BASE_PUBLIC}/)
echo "  $R"
echo "$R" | grep -q "running" && ok "Public URL" || ko "Public URL"

# T4
echo ""
echo "▶ T4: POST /api/auth/login (teknisi1)"
R=$(curl -s -X POST ${BASE_LOCAL}/api/auth/login -H "Content-Type: application/json" -d '{"username":"teknisi1","password":"password123"}')
echo "  $R"
TOKEN=$(echo "$R" | grep -oP '"token"\s*:\s*"\K[^"]+')
[ -n "$TOKEN" ] && ok "Login Teknisi" || ko "Login Teknisi"

# T5
echo ""
echo "▶ T5: POST /api/auth/login (admin1)"
R=$(curl -s -X POST ${BASE_LOCAL}/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin1","password":"password123"}')
echo "  $R"
ADMIN_TOKEN=$(echo "$R" | grep -oP '"token"\s*:\s*"\K[^"]+')
[ -n "$ADMIN_TOKEN" ] && ok "Login Admin" || ko "Login Admin"

# T6
echo ""
echo "▶ T6: POST /api/auth/login (helpdesk1)"
R=$(curl -s -X POST ${BASE_LOCAL}/api/auth/login -H "Content-Type: application/json" -d '{"username":"helpdesk1","password":"password123"}')
echo "  $R"
HELP_TOKEN=$(echo "$R" | grep -oP '"token"\s*:\s*"\K[^"]+')
[ -n "$HELP_TOKEN" ] && ok "Login Helpdesk" || ko "Login Helpdesk"

# T7
echo ""
echo "▶ T7: GET /api/auth/profile"
R=$(curl -s ${BASE_LOCAL}/api/auth/profile -H "Authorization: Bearer $TOKEN")
echo "  $R"
echo "$R" | grep -q "teknisi1" && ok "Profile" || ko "Profile"

# T8
echo ""
echo "▶ T8: GET /api/reports/stats"
R=$(curl -s ${BASE_LOCAL}/api/reports/stats -H "Authorization: Bearer $TOKEN")
echo "  $R"
echo "$R" | grep -qi "success" && ok "Stats" || ko "Stats"

# T9
echo ""
echo "▶ T9: GET /api/reports"
R=$(curl -s ${BASE_LOCAL}/api/reports -H "Authorization: Bearer $TOKEN")
echo "  ${R:0:300}"
echo "$R" | grep -qi "success" && ok "List Reports" || ko "List Reports"

# T10
echo ""
echo "▶ T10: GET /api/users (admin)"
R=$(curl -s ${BASE_LOCAL}/api/users -H "Authorization: Bearer $ADMIN_TOKEN")
echo "  ${R:0:400}"
echo "$R" | grep -qi "success" && ok "Users Admin" || ko "Users Admin"

# T11
echo ""
echo "▶ T11: GET /api/users (teknisi expect 403)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_LOCAL}/api/users -H "Authorization: Bearer $TOKEN")
echo "  HTTP $CODE"
[ "$CODE" = "403" ] && ok "RBAC 403" || ko "RBAC got $CODE"

# T12
echo ""
echo "▶ T12: Login WRONG password (expect 401)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_LOCAL}/api/auth/login -H "Content-Type: application/json" -d '{"username":"teknisi1","password":"wrong"}')
echo "  HTTP $CODE"
[ "$CODE" = "401" ] && ok "Wrong Pass 401" || ko "Wrong Pass got $CODE"

# T13
echo ""
echo "▶ T13: GET /api/reports NO TOKEN (expect 401)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_LOCAL}/api/reports)
echo "  HTTP $CODE"
[ "$CODE" = "401" ] && ok "No Token 401" || ko "No Token got $CODE"

# T14
echo ""
echo "▶ T14: GET /api/reports INVALID TOKEN (expect 401)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_LOCAL}/api/reports -H "Authorization: Bearer fake.invalid.token")
echo "  HTTP $CODE"
[ "$CODE" = "401" ] && ok "Invalid Token 401" || ko "Invalid Token got $CODE"

# T15
echo ""
echo "▶ T15: GET /nonexistent (expect 404)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_LOCAL}/nonexistent)
echo "  HTTP $CODE"
[ "$CODE" = "404" ] && ok "404 NotFound" || ko "404 got $CODE"

# T16
echo ""
echo "▶ T16: GET /api/reports/9999 (expect 404)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_LOCAL}/api/reports/9999 -H "Authorization: Bearer $TOKEN")
echo "  HTTP $CODE"
[ "$CODE" = "404" ] && ok "Report 404" || ko "Report 404 got $CODE"

# T17
echo ""
echo "▶ T17: Public via Nginx POST /api/auth/login"
R=$(curl -s -X POST ${BASE_PUBLIC}/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin1","password":"password123"}')
echo "  ${R:0:400}"
echo "$R" | grep -q "token" && ok "Public Login" || ko "Public Login"

# T18
echo ""
echo "▶ T18: GET /api/auth/profile via PUBLIC IP"
PUB_TOKEN=$(echo "$R" | grep -oP '"token"\s*:\s*"\K[^"]+')
R2=$(curl -s ${BASE_PUBLIC}/api/auth/profile -H "Authorization: Bearer $PUB_TOKEN")
echo "  $R2"
echo "$R2" | grep -q "admin1" && ok "Public Profile" || ko "Public Profile"

# T19
echo ""
echo "▶ T19: PUT /api/auth/change-password (then revert)"
R=$(curl -s -X PUT ${BASE_LOCAL}/api/auth/change-password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"old_password":"password123","new_password":"newPass456"}')
echo "  $R"
echo "$R" | grep -qi "success\|berhasil" && ok "Change Password" || ko "Change Password"
# Revert
R=$(curl -s -X POST ${BASE_LOCAL}/api/auth/login -H "Content-Type: application/json" -d '{"username":"teknisi1","password":"newPass456"}')
NEW_TOKEN=$(echo "$R" | grep -oP '"token"\s*:\s*"\K[^"]+')
curl -s -X PUT ${BASE_LOCAL}/api/auth/change-password \
  -H "Authorization: Bearer $NEW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"old_password":"newPass456","new_password":"password123"}' > /dev/null
echo "  (password reverted to password123)"

# T20
echo ""
echo "▶ T20: CORS preflight OPTIONS"
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X OPTIONS ${BASE_LOCAL}/api/auth/login \
  -H "Origin: http://example.com" -H "Access-Control-Request-Method: POST")
echo "  HTTP $CODE"
[ "$CODE" = "204" ] || [ "$CODE" = "200" ] && ok "CORS preflight" || ko "CORS got $CODE"

# Summary
echo ""
echo "==================================================================="
echo "📊 TEST SUMMARY: PASS=$PASS  FAIL=$FAIL"
echo "==================================================================="
echo ""
echo "📊 PM2 STATUS"
pm2 list
echo ""
echo "📜 PM2 RECENT LOGS"
tail -n 20 ~/.pm2/logs/laporan-operasional-api-out.log 2>/dev/null
echo ""
echo "==================================================================="
echo "✅ DEPLOYMENT FULLY TESTED"
echo "🌐 Public URL  : http://103.247.10.60/"
echo "🌐 API Base    : http://103.247.10.60/api"
echo "👤 Default Login: teknisi1, admin1, helpdesk1 (pass: password123)"
echo "==================================================================="
