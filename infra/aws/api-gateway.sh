#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Crea el AWS API Gateway (HTTP API): ÚNICA entrada pública al backend.
#
#   Frontend (local, http://localhost:5173)
#     -> https://<id>.execute-api.<region>.amazonaws.com
#          ANY     /api/{proxy+}  -> EC2-A:8080 BFF   autorizador JWT Entra ID (1ª validación)
#          OPTIONS /api/{proxy+}  -> EC2-A:8080 BFF   sin autorizador (preflight CORS)
#     BFF (Spring Security, 2ª validación) -> EC2-B:8082-8084 microservicios (IP privada)
#
# Uso (AWS CloudShell o AWS CLI con credenciales del Learner Lab):
#   export AWS_REGION=us-east-1
#   export ENTRA_TENANT_ID=<tenant-id>
#   export ENTRA_API_CLIENT_ID=<client-id-de-gestorcitas-api>
#   export BFF_HOST=<IP_ELASTICA_EC2_BFF>       # IP elástica de EC2-A (gestorcitas-bff)
#   bash infra/aws/api-gateway.sh
# ---------------------------------------------------------------------------
set -euo pipefail

: "${AWS_REGION:?Define AWS_REGION (ej. us-east-1)}"
: "${ENTRA_TENANT_ID:?Define ENTRA_TENANT_ID}"
: "${ENTRA_API_CLIENT_ID:?Define ENTRA_API_CLIENT_ID}"
: "${BFF_HOST:?Define BFF_HOST (IP elástica de EC2-A)}"
API_NAME="${API_NAME:-gestorcitas-http-api}"
ISSUER="https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0"
AWS="aws --region ${AWS_REGION} --output text"

echo ">> 1/5 Creando HTTP API '${API_NAME}'"
API_ID=$($AWS apigatewayv2 create-api --name "$API_NAME" --protocol-type HTTP --query ApiId)
ENDPOINT=$($AWS apigatewayv2 get-api --api-id "$API_ID" --query ApiEndpoint)
echo "   ApiId=$API_ID  Endpoint=$ENDPOINT"

echo ">> 2/5 CORS (el frontend corre local)"
$AWS apigatewayv2 update-api --api-id "$API_ID" --cors-configuration '{
  "AllowOrigins": ["http://localhost:5173", "http://localhost"],
  "AllowMethods": ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
  "AllowHeaders": ["authorization", "content-type"],
  "MaxAge": 3600
}' >/dev/null

echo ">> 3/5 Autorizador JWT: issuer=${ISSUER} audience=${ENTRA_API_CLIENT_ID}"
AUTH_ID=$($AWS apigatewayv2 create-authorizer --api-id "$API_ID" --name entra-id-jwt \
  --authorizer-type JWT --identity-source '$request.header.Authorization' \
  --jwt-configuration "Audience=${ENTRA_API_CLIENT_ID},Issuer=${ISSUER}" --query AuthorizerId)

echo ">> 4/5 Integración y rutas hacia el BFF (${BFF_HOST}:8080)"
INT_BFF=$($AWS apigatewayv2 create-integration --api-id "$API_ID" --integration-type HTTP_PROXY \
  --integration-method ANY --integration-uri "http://${BFF_HOST}:8080/api/{proxy}" \
  --payload-format-version 1.0 --query IntegrationId)
$AWS apigatewayv2 create-route --api-id "$API_ID" --route-key 'ANY /api/{proxy+}' \
  --authorization-type JWT --authorizer-id "$AUTH_ID" --authorization-scopes access_as_user \
  --target "integrations/${INT_BFF}" >/dev/null
$AWS apigatewayv2 create-route --api-id "$API_ID" --route-key 'OPTIONS /api/{proxy+}' \
  --authorization-type NONE --target "integrations/${INT_BFF}" >/dev/null

echo ">> 5/5 Stage \$default con auto-deploy"
$AWS apigatewayv2 create-stage --api-id "$API_ID" --stage-name '$default' --auto-deploy >/dev/null

cat <<MSG

=====================================================================
 API Gateway listo:  ${ENDPOINT}

 En tu Mac, frontend/.env:
     VITE_API_BASE_URL=${ENDPOINT}
 y reinicia:  npm run dev
=====================================================================
MSG
