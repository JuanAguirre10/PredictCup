# Banderas de países

Pon aquí las imágenes de bandera, **nombradas por el código FIFA en minúscula** y en `.jpg`:

```
bra.jpg   usa.jpg   arg.jpg   esp.jpg   ...
```

El componente `<Bandera>` arma la ruta `/flags/{codigo}.jpg`. Si una imagen falta,
cae automáticamente al emoji, así que puedes ir añadiéndolas de a poco.

> La extensión por defecto es `jpg`. Si las tuvieras en otro formato, cambia
> `NEXT_PUBLIC_FLAGS_EXT` (p. ej. `png` o `webp`) en el build del frontend.

## Códigos FIFA del seed (48)

A: mex rsa kor cze · B: can bih qat sui · C: bra mar hai sco · D: usa par aus tur
E: ger cuw civ ecu · F: ned jpn swe tun · G: bel egy irn nzl · H: esp cpv ksa uru
I: fra sen irq nor · J: arg alg aut jor · K: por cod uzb col · L: eng cro gha pan

## Usar Cloudflare/R2 en vez de esta carpeta

Sube las imágenes (mismo nombre) a tu bucket y define en el build del frontend:

```
NEXT_PUBLIC_FLAGS_BASE=https://<tu-dominio-r2>
```
