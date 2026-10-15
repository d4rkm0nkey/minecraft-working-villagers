// Icons standing for a group ("any axe") cycle through their members, like the recipe
// book does. The interval looks the icons up each time, so it survives instant loading.
setInterval(() => {
  for (const cycle of document.querySelectorAll(".mc-cycle")) {
    const images = cycle.querySelectorAll("img");
    const active = [...images].findIndex((img) => img.classList.contains("is-active"));
    images[active]?.classList.remove("is-active");
    images[(active + 1) % images.length].classList.add("is-active");
  }
}, 1300);
