#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# AWS API Gateway (HTTP API): ÚNICA entrada pública al backend.
#
#   Navegador -> https://<id>.execute-api.<region>.amazonaws.com
#          GET      /  y  /{proxy+}                 -> EC2-C:80   frontend (nginx)   público (solo si FRONT_HOST)
#          <MÉTODO> /api/...  (25 rutas explícitas)  -> EC2-A:8080 BFF   autorizador JWT (1ª validación)
#          ANY      /api/{proxy+}                   -> EC2-A:8080 BFF   autorizador JWT (rutas no listadas: el BFF las niega)
#          OPTIONS  /api/{proxy+}                   -> sin autorizador (preflight CORS)
#     BFF (Spring Security, 2ª validación + roles) -> EC2-B:8082-8084 microservicios (IP privada)
#
# Respuestas de la 1ª validación (API Gateway):
#   401  sin token, firma inválida, token vencido, issuer o audience incorrectos
#   403  token válido pero sin el scope access_as_user
# El BFF responde 403 cuando el rol del usuario no permite la ruta.
#
# Uso (AWS CloudShell, región us-east-1):
#   export AWS_REGION=us-east-1
#   export ENTRA_TENANT_ID=<tenant-id>
#   export ENTRA_API_CLIENT_ID=<client-id-de-gestorcitas-api>
#   export BFF_HOST=<DNS público de EC2-A>   # ej. ec2-1-2-3-4.compute-1.amazonaws.com
#   export API_ID=<id existente>              # opcional: reutiliza la API (misma URL)
#   export ENTRA_ISSUER=<issuer>              # opcional: Entra External ID (ver docs/01-entra-id.md)
#   export FRONT_HOST=<DNS público de EC2-C>  # opcional: publica el frontend (nginx :80) en la misma URL
#   bash infra/aws/api-gateway.sh
# ---------------------------------------------------------------------------
set -euo pipefail

: "${AWS_REGION:?Define AWS_REGION (ej. us-east-1)}"
: "${ENTRA_TENANT_ID:?Define ENTRA_TENANT_ID}"
: "${ENTRA_API_CLIENT_ID:?Define ENTRA_API_CLIENT_ID}"
: "${BFF_HOST:?Define BFF_HOST (DNS público de EC2-A)}"
API_NAME="${API_NAME:-gestorcitas-http-api}"
ISSUER="${ENTRA_ISSUER:-https://login.microsoftonline.com/${ENTRA_TENANT_ID}/v2.0}"
SCOPE="${ENTRA_REQUIRED_SCOPE:-access_as_user}"
AWS="aws --region ${AWS_REGION} --output text"

# Endpoints del BFF (método y path). Cada uno es una ruta protegida con JWT.
ROUTES=(
  "GET /api/me"
  "GET /api/admin/dashboard"
  "GET /api/catalogo/especialidades"
  "POST /api/catalogo/especialidades"
  "PUT /api/catalogo/especialidades/{id}"
  "PATCH /api/catalogo/especialidades/{id}/activar"
  "DELETE /api/catalogo/especialidades/{id}"
  "GET /api/citas"
  "POST /api/citas"
  "GET /api/citas/mias"
  "GET /api/citas/agenda"
  "GET /api/citas/disponibilidad"
  "PATCH /api/citas/{id}/cancelar"
  "PATCH /api/citas/{id}/confirmar"
  "PATCH /api/citas/{id}/atender"
  "POST /api/usuarios/me/sincronizar"
  "GET /api/usuarios/me"
  "PUT /api/usuarios/me/perfil-paciente"
  "PUT /api/usuarios/me/perfil-medico"
  "GET /api/usuarios"
  "GET /api/usuarios/medicos"
  "POST /api/usuarios/medicos"
  "PATCH /api/usuarios/{id}/activar"
  "PATCH /api/usuarios/{id}/desactivar"
  "PUT /api/usuarios/{id}/perfil-medico"
)

if [[ -n "${API_ID:-}" ]]; then
  echo ">> 1/6 Reutilizando la API ${API_ID} (se reemplazan rutas, integración y autorizador)"
  for r in $($AWS apigatewayv2 get-routes --api-id "$API_ID" --query 'Items[].RouteId'); do
    $AWS apigatewayv2 delete-route --api-id "$API_ID" --route-id "$r"
  done
  for a in $($AWS apigatewayv2 get-authorizers --api-id "$API_ID" --query 'Items[].AuthorizerId'); do
    $AWS apigatewayv2 delete-authorizer --api-id "$API_ID" --authorizer-id "$a"
  done
  for i in $($AWS apigatewayv2 get-integrations --api-id "$API_ID" --query 'Items[].IntegrationId'); do
    $AWS apigatewayv2 delete-integration --api-id "$API_ID" --integration-id "$i"
  done
else
  echo ">> 1/6 Creando HTTP API '${API_NAME}'"
  API_ID=$($AWS apigatewayv2 create-api --name "$API_NAME" --protocol-type HTTP --query ApiId)
fi
ENDPOINT=$($AWS apigatewayv2 get-api --api-id "$API_ID" --query ApiEndpoint)
echo "   ApiId=$API_ID  Endpoint=$ENDPOINT"

echo ">> 2/6 CORS: frontend publicado (${ENDPOINT}) y frontend local de desarrollo"
$AWS apigatewayv2 update-api --api-id "$API_ID" --cors-configuration "{
  \"AllowOrigins\": [\"${ENDPOINT}\", \"http://localhost:5173\"],
  \"AllowMethods\": [\"GET\", \"POST\", \"PUT\", \"PATCH\", \"DELETE\", \"OPTIONS\"],
  \"AllowHeaders\": [\"authorization\", \"content-type\"],
  \"MaxAge\": 3600
}" >/dev/null

echo ">> 3/6 Autorizador JWT: issuer=${ISSUER} audience=${ENTRA_API_CLIENT_ID}"
AUTH_ID=$($AWS apigatewayv2 create-authorizer --api-id "$API_ID" --name entra-id-jwt \
  --authorizer-type JWT --identity-source '$request.header.Authorization' \
  --jwt-configuration "Audience=${ENTRA_API_CLIENT_ID},Issuer=${ISSUER}" --query AuthorizerId)

echo ">> 4/6 Integración con el BFF (http://${BFF_HOST}:8080, reenvía el mismo path)"
INT_BFF=$($AWS apigatewayv2 create-integration --api-id "$API_ID" --integration-type HTTP_PROXY \
  --integration-method ANY --integration-uri "http://${BFF_HOST}:8080" \
  --request-parameters '{"overwrite:path":"$request.path"}' \
  --payload-format-version 1.0 --query IntegrationId)

echo ">> 5/6 Rutas (${#ROUTES[@]} con JWT + comodín /api + preflight)"
for route in "${ROUTES[@]}"; do
  $AWS apigatewayv2 create-route --api-id "$API_ID" --route-key "$route" \
    --authorization-type JWT --authorizer-id "$AUTH_ID" --authorization-scopes "$SCOPE" \
    --target "integrations/${INT_BFF}" >/dev/null
  echo "   JWT  $route"
done
$AWS apigatewayv2 create-route --api-id "$API_ID" --route-key 'ANY /api/{proxy+}' \
  --authorization-type JWT --authorizer-id "$AUTH_ID" --authorization-scopes "$SCOPE" \
  --target "integrations/${INT_BFF}" >/dev/null
echo "   JWT  ANY /api/{proxy+}  (rutas no listadas -> el BFF responde 403)"
$AWS apigatewayv2 create-route --api-id "$API_ID" --route-key 'OPTIONS /api/{proxy+}' \
  --authorization-type NONE --target "integrations/${INT_BFF}" >/dev/null
echo "   NONE OPTIONS /api/{proxy+}"

if [[ -n "${FRONT_HOST:-}" ]]; then
  echo "   Frontend publicado desde http://${FRONT_HOST}:80"
  INT_FRONT=$($AWS apigatewayv2 create-integration --api-id "$API_ID" --integration-type HTTP_PROXY \
    --integration-method GET --integration-uri "http://${FRONT_HOST}:80" \
    --request-parameters '{"overwrite:path":"$request.path"}' \
    --payload-format-version 1.0 --query IntegrationId)
  for route in 'GET /' 'GET /{proxy+}'; do
    $AWS apigatewayv2 create-route --api-id "$API_ID" --route-key "$route" \
      --authorization-type NONE --target "integrations/${INT_FRONT}" >/dev/null
    echo "   NONE $route  (frontend)"
  done
fi

echo ">> 6/6 Stage \$default con auto-deploy"
if ! $AWS apigatewayv2 get-stage --api-id "$API_ID" --stage-name '$default' >/dev/null 2>&1; then
  $AWS apigatewayv2 create-stage --api-id "$API_ID" --stage-name '$default' --auto-deploy >/dev/null
fi

cat <<MSG

=====================================================================
 API Gateway listo:  ${ENDPOINT}
 Prueba sin token (debe dar 401):
     curl -i ${ENDPOINT}/api/me
 En tu Mac, frontend/.env:  VITE_API_BASE_URL=${ENDPOINT}
 Si publicaste el frontend (FRONT_HOST):
   - Entra ID > gestorcitas-frontend > Autenticación > SPA: agrega ${ENDPOINT} y ${ENDPOINT}/login
   - EC2-A .env: CORS_ALLOWED_ORIGINS=http://localhost:5173,${ENDPOINT}  y  docker compose -f docker-compose.ec2-bff.yml up -d
   - Abre ${ENDPOINT}
=====================================================================
MSG
