.PHONY: dev prod scale logs stress down clean ssl build

# Levanta el stack de desarrollo (1 replica, puertos expuestos, sin monitoreo).
dev:
	docker compose -f docker-compose.yml -f docker-compose.dev.yml up

# Levanta el stack completo de produccion en segundo plano (3 replicas + monitoreo).
prod:
	docker compose up -d

# Escala el backend: make scale N=5
scale:
	docker compose up -d --scale app=$(N)

# Sigue los logs del backend.
logs:
	docker compose logs -f app

# Ejecuta la bateria de stress tests (k6) contra el stack.
stress:
	cd stress-tests && bash run.sh

# Detiene y elimina los contenedores (conserva volumenes).
down:
	docker compose down

# Detiene y elimina contenedores Y volumenes (borra datos).
clean:
	docker compose down -v

# Genera el certificado TLS autofirmado.
ssl:
	bash nginx/ssl/generar.sh

# Construye las imagenes sin levantar.
build:
	docker compose build
